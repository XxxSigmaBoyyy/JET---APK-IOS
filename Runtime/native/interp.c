/* interp.c - AArch64 instruction execution + block cache + run loop. */
#include "jet_internal.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <setjmp.h>
#include <signal.h>

/* Soft-fault claim flag: FALSE while running host trap / SVC code so JVM
 * implicit-null SIGSEGV inside JNI is not stolen. Nested guest re-entry
 * (RegisterNatives → jet_native_call) sets it again via jet_interp_run. */
static __thread volatile int jet_fault_active;
__thread volatile int jet_mem_guard_depth;
static inline int jet_fault_host_enter(void) {
    int s = jet_fault_active;
    jet_fault_active = 0;
    return s;
}
static inline void jet_fault_host_leave(int saved) {
    jet_fault_active = saved;
}

static int bit_fp128(const jet_insn *i);
static uint64_t bits_imm5(const jet_insn *i);

/* ---- NZCV helpers ---- */
static inline int get_n(jet_cpu *c){ return (c->nzcv >> 31)&1; }
static inline int get_z(jet_cpu *c){ return (c->nzcv >> 30)&1; }
static inline int get_c(jet_cpu *c){ return (c->nzcv >> 29)&1; }
static inline int get_v(jet_cpu *c){ return (c->nzcv >> 28)&1; }
static inline void set_nzcv(jet_cpu *c, int n, int z, int cy, int v) {
    c->nzcv = ((n&1u)<<31)|((z&1u)<<30)|((cy&1u)<<29)|((v&1u)<<28);
}
static int cond_holds(jet_cpu *c, int cond) {
    int n=get_n(c), z=get_z(c), cy=get_c(c), v=get_v(c);
    int r;
    switch (cond >> 1) {
    case 0: r = z; break;                 /* EQ/NE */
    case 1: r = cy; break;                /* CS/CC */
    case 2: r = n; break;                 /* MI/PL */
    case 3: r = v; break;                 /* VS/VC */
    case 4: r = cy && !z; break;          /* HI/LS */
    case 5: r = n == v; break;            /* GE/LT */
    case 6: r = n == v && !z; break;      /* GT/LE */
    case 7: r = 1; break;                 /* AL */
    default: r = 0; break;
    }
    return (cond & 1) ? !r : r;
}

static uint64_t mask_sf(int sf, uint64_t v) {
    return sf ? v : (v & 0xffffffffull);
}
static void set_nz_from(jet_cpu *c, int sf, uint64_t v) {
    v = mask_sf(sf, v);
    int n = sf ? (int)(v >> 63) : (int)((v >> 31) & 1);
    int z = (v == 0);
    set_nzcv(c, n, z, get_c(c), get_v(c));
}

/* Add with flags. */
static uint64_t add_with_carry(jet_cpu *c, int sf, uint64_t a, uint64_t b, int carry_in, int setf) {
    if (!sf) { a &= 0xffffffffull; b &= 0xffffffffull; }
    u128 sum = (u128)a + (u128)b + (u128)carry_in;
    uint64_t res = (uint64_t)sum;
    if (!sf) res &= 0xffffffffull;
    if (setf) {
        int n = sf ? (int)(res >> 63) : (int)((res >> 31) & 1);
        int z = (res == 0);
        int cy = sf ? (int)(sum >> 64) : (int)(sum >> 32);
        int aa = sf ? (int)(a >> 63) : (int)((a >> 31) & 1);
        int bb = sf ? (int)(b >> 63) : (int)((b >> 31) & 1);
        int rr = n;
        int v = (aa == bb) && (rr != aa);
        set_nzcv(c, n, z, cy, v);
    }
    return res;
}

static uint64_t shift_reg(int sf, uint64_t v, int type, unsigned amount) {
    unsigned bits = sf ? 64u : 32u;
    amount &= (bits - 1);
    v = mask_sf(sf, v);
    switch (type) {
    case 0: return mask_sf(sf, v << amount);                      /* LSL */
    case 1: return v >> amount;                                   /* LSR */
    case 2: {                                                     /* ASR */
        if (sf) return (uint64_t)((int64_t)v >> amount);
        return (uint64_t)(uint32_t)((int32_t)(uint32_t)v >> amount);
    }
    case 3: {                                                     /* ROR */
        return mask_sf(sf, (v >> amount) | (v << (bits - amount)));
    }
    }
    return v;
}

static uint64_t extend_reg(int sf, uint64_t v, int option, unsigned shift) {
    int len; int sgn;
    switch (option) {
    case 0: len=8;  sgn=0; break; /* UXTB */
    case 1: len=16; sgn=0; break; /* UXTH */
    case 2: len=32; sgn=0; break; /* UXTW */
    case 3: len=64; sgn=0; break; /* UXTX */
    case 4: len=8;  sgn=1; break; /* SXTB */
    case 5: len=16; sgn=1; break; /* SXTH */
    case 6: len=32; sgn=1; break; /* SXTW */
    case 7: len=64; sgn=1; break; /* SXTX */
    default: len=64; sgn=0; break;
    }
    if (len < 64) {
        uint64_t mask = (1ull << len) - 1ull;
        v &= mask;
        if (sgn && (v & (1ull << (len - 1)))) v |= ~mask;
    }
    v <<= shift;
    return mask_sf(sf, v);
}

static uint64_t decode_log_imm(const jet_insn *i) {
    uint64_t imm = 0;
    int len = 31 - __builtin_clz((i->N << 6) | ((~i->imms) & 0x3f));
    int esize = 1 << len;
    int levels = esize - 1;
    int S = i->imms & levels;
    int R = i->immr & levels;
    uint64_t welem = (1ull << (S + 1)) - 1ull;
    uint64_t mask = (esize == 64) ? ~0ull : ((1ull << esize) - 1ull);
    welem &= mask;
    uint64_t elem = ((welem >> R) | (welem << (esize - R))) & mask;
    for (int k = 0; k < 64; k += esize) imm |= elem << k;
    if (!i->sf) imm &= 0xffffffffull;
    return imm;
}

/* Bitfield move helper. */
static uint64_t bitfield_move(int sf, int opc /*0=SBFM 1=BFM 2=UBFM*/,
                              uint64_t dst, uint64_t src, int immr, int imms) {
    int bits = sf ? 64 : 32;
    uint64_t r = (uint64_t)immr, s = (uint64_t)imms;
    /* Common special cases */
    if (opc == 2 && s != 0x1f && s != 0x3f && immr == 0) {
        /* UBFX / UXT*: take bits [0..imms] */
    }
    /* General algorithm from ARM pseudocode (simplified): */
    uint64_t mask;
    if (s >= r) {
        /* in-register copy of (s-r+1) bits from position r to 0 */
        int width = (int)(s - r + 1);
        mask = (width == 64) ? ~0ull : ((1ull << width) - 1ull);
        uint64_t bot = (src >> r) & mask;
        if (opc == 0) { /* SBFM: sign extend from bit s-r */
            int sx = width - 1;
            if (bot & (1ull << sx)) bot |= ~mask;
            return mask_sf(sf, bot);
        } else if (opc == 2) { /* UBFM */
            return mask_sf(sf, bot);
        } else { /* BFM: merge into dst */
            return mask_sf(sf, (dst & ~mask) | bot);
        }
    } else {
        /* wrap: BFI / BFXIL style — bits from 0..(s) inserted at (bits-r) */
        int width = (int)s + 1;
        mask = (width == 64) ? ~0ull : ((1ull << width) - 1ull);
        int pos = bits - (int)r;
        uint64_t bot = src & mask;
        if (opc == 0) {
            /* SBFIZ: take width bits of src, sign-extend, place at `pos` (=lsb).
             * ASR never reaches here (imms=bits-1 => s>=r). */
            if (bot & (1ull << (width - 1))) bot |= ~mask;
            return mask_sf(sf, bot << pos);
        } else if (opc == 2) {
            /* UBFIZ / LSL alias: zero-extend width bits, place at pos */
            return mask_sf(sf, bot << pos);
        } else {
            /* BFI: insert width bits at pos into dst */
            uint64_t dmask = mask << pos;
            return mask_sf(sf, (dst & ~dmask) | (bot << pos));
        }
    }
}

static uint64_t addr_mode(jet_cpu *c, const jet_insn *i, int *wb_out) {
    uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
    uint64_t addr = base;
    *wb_out = 0;
    if (i->opc == 0 && i->op != OP_LDR && i->op != OP_STR &&
        i->op != OP_LDRB && i->op != OP_STRB && i->op != OP_LDRH && i->op != OP_STRH &&
        i->op != OP_LDRSB && i->op != OP_LDRSH && i->op != OP_LDRSW &&
        i->op != OP_LDR_FP && i->op != OP_STR_FP) {
        /* pair / others handle themselves */
    }
    /* For unsigned-imm / unscaled / pre / post / reg: use i->opc */
    if (i->opc == 0) {
        /* unsigned imm or unscaled stored in uimm or imm */
        if (i->uimm) addr = base + i->uimm;
        else addr = base + (int64_t)i->imm;
    } else if (i->opc == 1) { /* post */
        addr = base; *wb_out = 1;
    } else if (i->opc == 3) { /* pre */
        addr = base + (int64_t)i->imm; *wb_out = 1;
    } else if (i->opc == 4) { /* reg offset */
        uint64_t idx = jet_x(c, i->rm);
        int opt = i->cond;
        if (opt == 3 || opt == 7) { /* UXTX/SXTX / LSL */
            if (i->shift) idx <<= i->size;
        } else {
            idx = extend_reg(1, idx, opt, i->shift ? i->size : 0);
        }
        addr = base + idx;
    } else {
        addr = base + (int64_t)i->imm;
    }
    return addr;
}

static void do_wb(jet_cpu *c, const jet_insn *i, uint64_t base_plus) {
    if (i->rn == 31) c->sp = base_plus;
    else jet_set_x(c, i->rn, base_plus);
}

/* ---- execute one insn; returns 1 if PC already updated (branch) ---- */
static int exec_one(jet_ctx *ctx, jet_cpu *c, const jet_insn *i) {
    uint64_t pc = c->pc;

    switch (i->op) {

    case OP_CRC32: {
        /* Poly: CRC32 = 0x04C11DB7 (reflected 0xEDB88320), CRC32C = 0x1EDC6F41 (reflected 0x82F63B78) */
        static const uint32_t poly_crc = 0xEDB88320u, poly_c = 0x82F63B78u;
        uint32_t poly = i->N ? poly_c : poly_crc;
        uint32_t crc = (uint32_t)jet_x(c, i->rn);
        uint64_t data = jet_x(c, i->rm);
        int nbytes = 1 << i->size; /* 1,2,4,8 */
        if (i->size == 3 && !i->sf) nbytes = 4; /* W form */
        for (int b = 0; b < nbytes; b++) {
            crc ^= (uint32_t)((data >> (8*b)) & 0xff);
            for (int k = 0; k < 8; k++)
                crc = (crc >> 1) ^ (poly & (uint32_t)-(int32_t)(crc & 1u));
        }
        jet_set_x(c, i->rd, crc);
        return 0;
    }

    case OP_NOP: case OP_HINT: case OP_PRFM_LIT:
        return 0;

    case OP_MOVZ:
        jet_set_x(c, i->rd, mask_sf(i->sf, (uint64_t)i->uimm << i->shift));
        return 0;
    case OP_MOVN:
        jet_set_x(c, i->rd, mask_sf(i->sf, ~((uint64_t)i->uimm << i->shift)));
        return 0;
    case OP_MOVK: {
        uint64_t v = jet_x(c, i->rd);
        uint64_t mask = ~(0xffffull << i->shift);
        if (!i->sf) { v &= 0xffffffffull; mask |= 0xffffffff00000000ull; }
        v = (v & mask) | ((uint64_t)i->uimm << i->shift);
        jet_set_x(c, i->rd, mask_sf(i->sf, v));
        return 0;
    }

    case OP_ADR:
        jet_set_x(c, i->rd, pc + (int64_t)i->imm);
        return 0;
    case OP_ADRP:
        jet_set_x(c, i->rd, (pc & ~0xfffull) + ((int64_t)i->imm << 12));
        return 0;

    case OP_ADDI: case OP_ADDIS: case OP_SUBI: case OP_SUBIS: {
        uint64_t a = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        uint64_t b = (uint64_t)i->uimm;
        int is_sub = (i->op == OP_SUBI || i->op == OP_SUBIS);
        int setf = (i->op == OP_ADDIS || i->op == OP_SUBIS);
        uint64_t res = add_with_carry(c, i->sf, a, is_sub ? ~b : b,
                                      is_sub ? 1 : 0, setf);
        if (i->rd == 31 && !setf) c->sp = res;
        else jet_set_x(c, i->rd, res);
        return 0;
    }

    case OP_ANDI: case OP_ORRI: case OP_EORI: case OP_ANDIS: {
        uint64_t imm = decode_log_imm(i);
        uint64_t a = jet_x(c, i->rn);
        uint64_t r;
        if (i->op == OP_ANDI || i->op == OP_ANDIS) r = a & imm;
        else if (i->op == OP_ORRI) r = a | imm;
        else r = a ^ imm;
        r = mask_sf(i->sf, r);
        jet_set_x(c, i->rd, r);
        if (i->op == OP_ANDIS) set_nz_from(c, i->sf, r), set_nzcv(c, get_n(c), get_z(c), 0, 0);
        return 0;
    }

    case OP_AND: case OP_ORR: case OP_EOR: case OP_ANDS:
    case OP_BIC: case OP_ORN: case OP_EON: case OP_BICS: {
        uint64_t a = jet_x(c, i->rn);
        uint64_t b = shift_reg(i->sf, jet_x(c, i->rm), i->shift, i->uimm);
        if (i->op == OP_BIC || i->op == OP_ORN || i->op == OP_EON || i->op == OP_BICS)
            b = mask_sf(i->sf, ~b);
        uint64_t r;
        if (i->op == OP_AND || i->op == OP_ANDS || i->op == OP_BIC || i->op == OP_BICS) r = a & b;
        else if (i->op == OP_ORR || i->op == OP_ORN) r = a | b;
        else r = a ^ b;
        r = mask_sf(i->sf, r);
        jet_set_x(c, i->rd, r);
        if (i->op == OP_ANDS || i->op == OP_BICS)
            set_nzcv(c,
                i->sf ? (int)(r>>63) : (int)((r>>31)&1),
                r == 0, 0, 0);
        return 0;
    }

    case OP_ADD: case OP_ADDS: case OP_SUB: case OP_SUBS: {
        uint64_t a = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        uint64_t b;
        if (i->opc == 1) /* extended */ b = extend_reg(i->sf, jet_x(c, i->rm), i->cond, i->uimm);
        else if (i->opc == 2) { /* ADC/SBC */
            b = jet_x(c, i->rm);
            int is_sub = (i->op == OP_SUB || i->op == OP_SUBS);
            int setf = (i->op == OP_ADDS || i->op == OP_SUBS);
            uint64_t res = add_with_carry(c, i->sf, a, is_sub ? ~b : b,
                                          is_sub ? get_c(c) : get_c(c), setf);
            /* For ADC carry_in = C; for SBC carry_in = C (with ~b). OK. */
            jet_set_x(c, i->rd, res);
            return 0;
        } else b = shift_reg(i->sf, jet_x(c, i->rm), i->shift, i->uimm);
        int is_sub = (i->op == OP_SUB || i->op == OP_SUBS);
        int setf = (i->op == OP_ADDS || i->op == OP_SUBS);
        uint64_t res = add_with_carry(c, i->sf, a, is_sub ? ~b : b, is_sub ? 1 : 0, setf);
        if (i->rd == 31 && !setf) c->sp = res;
        else jet_set_x(c, i->rd, res);
        return 0;
    }

    case OP_ASRV: case OP_LSLV: case OP_LSRV: case OP_RORV: {
        uint64_t a = jet_x(c, i->rn);
        unsigned sh = (unsigned)(jet_x(c, i->rm) & (i->sf ? 63 : 31));
        int t = i->op == OP_LSLV ? 0 : i->op == OP_LSRV ? 1 : i->op == OP_ASRV ? 2 : 3;
        jet_set_x(c, i->rd, shift_reg(i->sf, a, t, sh));
        return 0;
    }
    case OP_EXTR: {
        uint64_t lo = jet_x(c, i->rm), hi = jet_x(c, i->rn);
        unsigned bits = i->sf ? 64u : 32u;
        unsigned lsb = i->uimm;
        uint64_t r;
        if (!i->sf) { lo &= 0xffffffffull; hi &= 0xffffffffull; }
        if (lsb == 0) r = lo;
        else if (i->sf) r = (lo >> lsb) | (hi << (bits - lsb));
        else r = ((lo | (hi << 32)) >> lsb) & 0xffffffffull; /* approx */
        if (i->sf) r = (lo >> lsb) | (hi << (64 - lsb));
        else {
            uint64_t pair = (hi << 32) | (lo & 0xffffffffull);
            r = (pair >> lsb) & 0xffffffffull;
        }
        jet_set_x(c, i->rd, mask_sf(i->sf, r));
        return 0;
    }

    case OP_MADD: case OP_MSUB: {
        uint64_t n = jet_x(c, i->rn), m = jet_x(c, i->rm), a = jet_x(c, i->ra);
        if (!i->sf) { n=(uint32_t)n; m=(uint32_t)m; a=(uint32_t)a; }
        int64_t prod = (int64_t)n * (int64_t)m; /* actually unsigned mul for MADD */
        uint64_t up = n * m;
        uint64_t r = (i->op == OP_MADD) ? (a + up) : (a - up);
        jet_set_x(c, i->rd, mask_sf(i->sf, r));
        (void)prod;
        return 0;
    }
    case OP_SMADDL: case OP_SMSUBL: {
        int64_t n = (int32_t)jet_x(c, i->rn), m = (int32_t)jet_x(c, i->rm);
        int64_t a = (int64_t)jet_x(c, i->ra);
        int64_t r = (i->op == OP_SMADDL) ? a + n*m : a - n*m;
        jet_set_x(c, i->rd, (uint64_t)r);
        return 0;
    }
    case OP_UMADDL: case OP_UMSUBL: {
        uint64_t n = (uint32_t)jet_x(c, i->rn), m = (uint32_t)jet_x(c, i->rm);
        uint64_t a = jet_x(c, i->ra);
        uint64_t r = (i->op == OP_UMADDL) ? a + n*m : a - n*m;
        jet_set_x(c, i->rd, r);
        return 0;
    }
    case OP_SMULH: {
        int64_t n = (int64_t)jet_x(c, i->rn), m = (int64_t)jet_x(c, i->rm);
        s128 p = (s128)n * (s128)m;
        jet_set_x(c, i->rd, (uint64_t)((u128)p >> 64));
        return 0;
    }
    case OP_UMULH: {
        u128 p = (u128)jet_x(c, i->rn) * (u128)jet_x(c, i->rm);
        jet_set_x(c, i->rd, (uint64_t)(p >> 64));
        return 0;
    }
    case OP_SDIV: {
        int64_t n = i->sf ? (int64_t)jet_x(c, i->rn) : (int64_t)(int32_t)jet_x(c, i->rn);
        int64_t m = i->sf ? (int64_t)jet_x(c, i->rm) : (int64_t)(int32_t)jet_x(c, i->rm);
        int64_t r = m == 0 ? 0 : n / m;
        jet_set_x(c, i->rd, mask_sf(i->sf, (uint64_t)r));
        return 0;
    }
    case OP_UDIV: {
        uint64_t n = mask_sf(i->sf, jet_x(c, i->rn));
        uint64_t m = mask_sf(i->sf, jet_x(c, i->rm));
        jet_set_x(c, i->rd, m == 0 ? 0 : n / m);
        return 0;
    }
    case OP_CLZ: {
        uint64_t v = mask_sf(i->sf, jet_x(c, i->rn));
        int r = v == 0 ? (i->sf ? 64 : 32) : (i->sf ? __builtin_clzll(v) : __builtin_clz((uint32_t)v));
        jet_set_x(c, i->rd, (uint64_t)r);
        return 0;
    }
    case OP_CLS: {
        uint64_t v = mask_sf(i->sf, jet_x(c, i->rn));
        int bits = i->sf ? 64 : 32;
        int sign = (int)((v >> (bits - 1)) & 1);
        int cnt = 0;
        for (int b = bits - 2; b >= 0; b--) {
            if (((v >> b) & 1) != (unsigned)sign) break;
            cnt++;
        }
        jet_set_x(c, i->rd, (uint64_t)cnt);
        return 0;
    }
    case OP_RBIT: {
        uint64_t v = jet_x(c, i->rn);
        if (!i->sf) v = (uint32_t)v;
        v = ((v & 0xAAAAAAAAAAAAAAAAULL) >> 1) | ((v & 0x5555555555555555ULL) << 1);
        v = ((v & 0xCCCCCCCCCCCCCCCCULL) >> 2) | ((v & 0x3333333333333333ULL) << 2);
        v = ((v & 0xF0F0F0F0F0F0F0F0ULL) >> 4) | ((v & 0x0F0F0F0F0F0F0F0FULL) << 4);
        v = ((v & 0xFF00FF00FF00FF00ULL) >> 8) | ((v & 0x00FF00FF00FF00FFULL) << 8);
        v = ((v & 0xFFFF0000FFFF0000ULL) >> 16)| ((v & 0x0000FFFF0000FFFFULL) << 16);
        if (i->sf) v = (v >> 32) | (v << 32);
        else v >>= 32;
        jet_set_x(c, i->rd, mask_sf(i->sf, v));
        return 0;
    }
    case OP_REV: {
        uint64_t v = jet_x(c, i->rn);
        if (i->sf) v = __builtin_bswap64(v);
        else v = __builtin_bswap32((uint32_t)v);
        jet_set_x(c, i->rd, v);
        return 0;
    }
    case OP_REV16: {
        uint64_t v = jet_x(c, i->rn), r = 0;
        int bytes = i->sf ? 8 : 4;
        for (int b = 0; b < bytes; b += 2) {
            uint64_t w = (v >> (8*b)) & 0xffff;
            w = ((w & 0xff) << 8) | ((w >> 8) & 0xff);
            r |= w << (8*b);
        }
        jet_set_x(c, i->rd, mask_sf(i->sf, r));
        return 0;
    }
    case OP_REV32: {
        uint64_t v = jet_x(c, i->rn);
        uint32_t lo = __builtin_bswap32((uint32_t)v);
        uint32_t hi = __builtin_bswap32((uint32_t)(v >> 32));
        jet_set_x(c, i->rd, ((uint64_t)hi << 32) | lo);
        return 0;
    }

    case OP_CSEL: case OP_CSINC: case OP_CSINV: case OP_CSNEG: {
        uint64_t a = jet_x(c, i->rn), b = jet_x(c, i->rm);
        if (cond_holds(c, i->cond)) jet_set_x(c, i->rd, mask_sf(i->sf, a));
        else {
            if (i->op == OP_CSINC) b = mask_sf(i->sf, b + 1);
            else if (i->op == OP_CSINV) b = mask_sf(i->sf, ~b);
            else if (i->op == OP_CSNEG) b = mask_sf(i->sf, ~b + 1);
            jet_set_x(c, i->rd, mask_sf(i->sf, b));
        }
        return 0;
    }
    case OP_CCMP: case OP_CCMN: {
        if (cond_holds(c, i->cond)) {
            uint64_t a = jet_x(c, i->rn);
            uint64_t b = i->opc ? (uint64_t)i->rm /* imm5 stored */ : jet_x(c, i->rm);
            if (i->opc) b = bits_imm5(i); /* fix below */
            b = (i->raw >> 16) & 0x1f; /* imm5 if opc, else use rm — re-read */
            if (((i->raw >> 11) & 1) == 0) b = jet_x(c, i->rm);
            else b = (i->raw >> 16) & 0x1f;
            int is_sub = (i->op == OP_CCMP);
            add_with_carry(c, i->sf, a, is_sub ? ~b : b, is_sub ? 1 : 0, 1);
        } else {
            set_nzcv(c, (i->uimm>>3)&1, (i->uimm>>2)&1, (i->uimm>>1)&1, i->uimm&1);
        }
        return 0;
    }

    case OP_SBFM: case OP_BFM: case OP_UBFM: {
        int opc = i->op == OP_SBFM ? 0 : i->op == OP_BFM ? 1 : 2;
        uint64_t dst = jet_x(c, i->rd), src = jet_x(c, i->rn);
        /* Handle common aliases more carefully */
        int bits = i->sf ? 64 : 32;
        int R = i->immr, S = i->imms;
        uint64_t result;
        if (opc == 2 && S == bits - 1) {
            /* LSR */
            result = mask_sf(i->sf, src >> R);
        } else if (opc == 2 && S + 1 == R) {
            /* LSL */
            result = mask_sf(i->sf, src << (bits - R));
        } else if (opc == 0 && S == bits - 1) {
            /* ASR */
            if (i->sf) result = (uint64_t)((int64_t)src >> R);
            else result = (uint64_t)(uint32_t)((int32_t)(uint32_t)src >> R);
        } else if (opc == 0 && R == 0 && (S == 7 || S == 15 || S == 31)) {
            /* SXTB/SXTH/SXTW */
            int w = S + 1;
            uint64_t m = (1ull << w) - 1ull;
            result = src & m;
            if (result & (1ull << (w - 1))) result |= ~m;
            result = mask_sf(i->sf, result);
        } else if (opc == 2 && R == 0 && (S == 7 || S == 15)) {
            result = src & ((1ull << (S + 1)) - 1ull);
        } else {
            result = bitfield_move(i->sf, opc, dst, src, R, S);
        }
        jet_set_x(c, i->rd, result);
        return 0;
    }

    /* ---- loads/stores ---- */
    case OP_LDR_LIT: {
        uint64_t addr = pc + (int64_t)i->imm;
        jet_set_x(c, i->rd, i->sf ? ld8(addr) : ld4(addr));
        return 0;
    }
    case OP_LDRSW_LIT: {
        uint64_t addr = pc + (int64_t)i->imm;
        jet_set_x(c, i->rd, (uint64_t)(int64_t)(int32_t)ld4(addr));
        return 0;
    }

    case OP_LDR: case OP_LDRB: case OP_LDRH: case OP_LDRSB: case OP_LDRSH: case OP_LDRSW:
    case OP_STR: case OP_STRB: case OP_STRH: {
        int wb = 0;
        uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        uint64_t addr;
        if (i->opc == 0) addr = base + (i->uimm ? i->uimm : (uint64_t)(int64_t)i->imm);
        else if (i->opc == 1) { addr = base; wb = 1; }
        else if (i->opc == 3) { addr = base + (int64_t)i->imm; wb = 1; }
        else if (i->opc == 4) {
            uint64_t idx = jet_x(c, i->rm);
            int opt = i->cond;
            if (opt == 3 || opt == 7) { if (i->shift) idx <<= i->size; }
            else idx = extend_reg(1, idx, opt, i->shift ? i->size : 0);
            addr = base + idx;
        } else addr = base + (int64_t)i->imm;

        if (i->op == OP_STR)  { if (i->sf) st8(addr, jet_x(c, i->rd)); else st4(addr, (uint32_t)jet_x(c, i->rd)); }
        else if (i->op == OP_STRB) st1(addr, (uint8_t)jet_x(c, i->rd));
        else if (i->op == OP_STRH) st2(addr, (uint16_t)jet_x(c, i->rd));
        else if (i->op == OP_LDR)  jet_set_x(c, i->rd, i->sf ? ld8(addr) : ld4(addr));
        else if (i->op == OP_LDRB) jet_set_x(c, i->rd, ld1(addr));
        else if (i->op == OP_LDRH) jet_set_x(c, i->rd, ld2(addr));
        else if (i->op == OP_LDRSB) {
            int8_t v = (int8_t)ld1(addr);
            jet_set_x(c, i->rd, i->sf ? (uint64_t)(int64_t)v : (uint64_t)(uint32_t)(int32_t)v);
        } else if (i->op == OP_LDRSH) {
            int16_t v = (int16_t)ld2(addr);
            jet_set_x(c, i->rd, i->sf ? (uint64_t)(int64_t)v : (uint64_t)(uint32_t)(int32_t)v);
        } else if (i->op == OP_LDRSW) {
            jet_set_x(c, i->rd, (uint64_t)(int64_t)(int32_t)ld4(addr));
        }
        if (wb) {
            uint64_t nb = base + (int64_t)i->imm;
            if (i->rn == 31) c->sp = nb; else jet_set_x(c, i->rn, nb);
        }
        return 0;
    }

    case OP_LDP: case OP_LDPSW: case OP_STP: {
        uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        int mode = i->opc; /* 1=post 2=signed 3=pre */
        uint64_t addr = (mode == 1) ? base : base + (int64_t)i->imm;
        int step = i->sf ? 8 : 4;
        if (i->op == OP_STP) {
            if (i->sf) { st8(addr, jet_x(c, i->rd)); st8(addr+8, jet_x(c, i->ra)); }
            else { st4(addr, (uint32_t)jet_x(c, i->rd)); st4(addr+4, (uint32_t)jet_x(c, i->ra)); }
        } else if (i->op == OP_LDPSW) {
            jet_set_x(c, i->rd, (uint64_t)(int64_t)(int32_t)ld4(addr));
            jet_set_x(c, i->ra, (uint64_t)(int64_t)(int32_t)ld4(addr+4));
        } else {
            if (i->sf) { jet_set_x(c, i->rd, ld8(addr)); jet_set_x(c, i->ra, ld8(addr+8)); }
            else { jet_set_x(c, i->rd, ld4(addr)); jet_set_x(c, i->ra, ld4(addr+4)); }
        }
        if (mode == 1 || mode == 3) {
            uint64_t nb = base + (int64_t)i->imm;
            if (i->rn == 31) c->sp = nb; else jet_set_x(c, i->rn, nb);
        }
        (void)step;
        return 0;
    }

    case OP_LDXR: case OP_LDAXR: case OP_LDAR: case OP_LDLAR: {
        uint64_t addr = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        uint64_t v = 0;
        void *p = G2H(addr);
        switch (i->size) {
        case 0: v = __atomic_load_n((uint8_t *)p, __ATOMIC_ACQUIRE); break;
        case 1: v = __atomic_load_n((uint16_t *)p, __ATOMIC_ACQUIRE); break;
        case 2: v = __atomic_load_n((uint32_t *)p, __ATOMIC_ACQUIRE); break;
        case 3: v = __atomic_load_n((uint64_t *)p, __ATOMIC_ACQUIRE); break;
        }
        jet_set_x(c, i->rd, v);
        if (i->op == OP_LDXR || i->op == OP_LDAXR) {
            pthread_mutex_lock(&ctx->excl_mu);
            ctx->excl_valid = 1; ctx->excl_addr = addr; ctx->excl_val = v;
            pthread_mutex_unlock(&ctx->excl_mu);
        }
        return 0;
    }
    case OP_STXR: case OP_STLXR: case OP_STLR: case OP_STLLR: {
        uint64_t addr = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        void *p = G2H(addr);
        uint64_t v = jet_x(c, i->rd);
        int ok = 1;
        if (i->op == OP_STXR || i->op == OP_STLXR) {
            /* Emulate exclusive store with host CAS against monitored value. */
            pthread_mutex_lock(&ctx->excl_mu);
            ok = ctx->excl_valid && ctx->excl_addr == addr;
            uint64_t expected = ctx->excl_val;
            ctx->excl_valid = 0;
            pthread_mutex_unlock(&ctx->excl_mu);
            if (ok) {
                int success = 0;
                switch (i->size) {
                case 0: {
                    uint8_t e = (uint8_t)expected, n = (uint8_t)v;
                    success = __atomic_compare_exchange_n((uint8_t *)p, &e, n, 0,
                                                         __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                    break;
                }
                case 1: {
                    uint16_t e = (uint16_t)expected, n = (uint16_t)v;
                    success = __atomic_compare_exchange_n((uint16_t *)p, &e, n, 0,
                                                         __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                    break;
                }
                case 2: {
                    uint32_t e = (uint32_t)expected, n = (uint32_t)v;
                    success = __atomic_compare_exchange_n((uint32_t *)p, &e, n, 0,
                                                         __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                    break;
                }
                case 3: {
                    uint64_t e = expected, n = v;
                    success = __atomic_compare_exchange_n((uint64_t *)p, &e, n, 0,
                                                         __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                    break;
                }
                }
                ok = success;
            }
            jet_set_x(c, i->rm, ok ? 0 : 1);
        } else {
            switch (i->size) {
            case 0: __atomic_store_n((uint8_t *)p, (uint8_t)v, __ATOMIC_RELEASE); break;
            case 1: __atomic_store_n((uint16_t *)p, (uint16_t)v, __ATOMIC_RELEASE); break;
            case 2: __atomic_store_n((uint32_t *)p, (uint32_t)v, __ATOMIC_RELEASE); break;
            case 3: __atomic_store_n((uint64_t *)p, v, __ATOMIC_RELEASE); break;
            }
        }
        return 0;
    }
    case OP_CAS: case OP_CASP: case OP_SWP: case OP_LDADD: case OP_LDCLR:
    case OP_LDEOR: case OP_LDSET: {
        uint64_t addr = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        void *p = G2H(addr);
        uint64_t rs = jet_x(c, i->rm), rt = jet_x(c, i->rd);
        uint64_t old = 0;
        if (i->op == OP_CASP) {
            /* Pair CAS: compare (Rs, Rs+1) with mem[addr:addr+sz*2], swap (Rt, Rt+1). */
            int pair_sz = (i->size == 2) ? 4 : 8; /* W pair or X pair */
            if (pair_sz == 4) {
                uint64_t expect = (jet_x(c, i->rm) & 0xffffffffull) |
                                  ((jet_x(c, i->rm | 1) & 0xffffffffull) << 32);
                uint64_t desire = (jet_x(c, i->rd) & 0xffffffffull) |
                                  ((jet_x(c, i->rd | 1) & 0xffffffffull) << 32);
                uint64_t e = expect;
                __atomic_compare_exchange_n((uint64_t *)p, &e, desire, 0,
                                            __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                jet_set_x(c, i->rm, (uint32_t)e);
                jet_set_x(c, i->rm | 1, (uint32_t)(e >> 32));
            } else {
                /* 128-bit: use two 64-bit words under excl mutex (host may lack __int128 atomics). */
                pthread_mutex_lock(&ctx->excl_mu);
                uint64_t lo = __atomic_load_n((uint64_t *)p, __ATOMIC_RELAXED);
                uint64_t hi = __atomic_load_n((uint64_t *)p + 1, __ATOMIC_RELAXED);
                uint64_t rs0 = jet_x(c, i->rm), rs1 = jet_x(c, i->rm | 1);
                if (lo == rs0 && hi == rs1) {
                    __atomic_store_n((uint64_t *)p, jet_x(c, i->rd), __ATOMIC_RELAXED);
                    __atomic_store_n((uint64_t *)p + 1, jet_x(c, i->rd | 1), __ATOMIC_RELAXED);
                }
                jet_set_x(c, i->rm, lo);
                jet_set_x(c, i->rm | 1, hi);
                pthread_mutex_unlock(&ctx->excl_mu);
                __atomic_thread_fence(__ATOMIC_SEQ_CST);
            }
            return 0;
        }
        switch (i->size) {
        case 0: {
            uint8_t e, n, r;
            if (i->op == OP_CAS) {
                e = (uint8_t)rs; n = (uint8_t)rt;
                __atomic_compare_exchange_n((uint8_t *)p, &e, n, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                old = e;
            } else if (i->op == OP_SWP) {
                old = __atomic_exchange_n((uint8_t *)p, (uint8_t)rt, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDADD) {
                old = __atomic_fetch_add((uint8_t *)p, (uint8_t)rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDCLR) {
                old = __atomic_fetch_and((uint8_t *)p, (uint8_t)~rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDEOR) {
                old = __atomic_fetch_xor((uint8_t *)p, (uint8_t)rs, __ATOMIC_ACQ_REL);
            } else { /* LDSET */
                old = __atomic_fetch_or((uint8_t *)p, (uint8_t)rs, __ATOMIC_ACQ_REL);
            }
            (void)r;
            break;
        }
        case 1: {
            uint16_t e, n;
            if (i->op == OP_CAS) {
                e = (uint16_t)rs; n = (uint16_t)rt;
                __atomic_compare_exchange_n((uint16_t *)p, &e, n, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                old = e;
            } else if (i->op == OP_SWP) {
                old = __atomic_exchange_n((uint16_t *)p, (uint16_t)rt, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDADD) {
                old = __atomic_fetch_add((uint16_t *)p, (uint16_t)rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDCLR) {
                old = __atomic_fetch_and((uint16_t *)p, (uint16_t)~rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDEOR) {
                old = __atomic_fetch_xor((uint16_t *)p, (uint16_t)rs, __ATOMIC_ACQ_REL);
            } else {
                old = __atomic_fetch_or((uint16_t *)p, (uint16_t)rs, __ATOMIC_ACQ_REL);
            }
            break;
        }
        case 2: {
            uint32_t e, n;
            if (i->op == OP_CAS) {
                e = (uint32_t)rs; n = (uint32_t)rt;
                __atomic_compare_exchange_n((uint32_t *)p, &e, n, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                old = e;
            } else if (i->op == OP_SWP) {
                old = __atomic_exchange_n((uint32_t *)p, (uint32_t)rt, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDADD) {
                old = __atomic_fetch_add((uint32_t *)p, (uint32_t)rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDCLR) {
                old = __atomic_fetch_and((uint32_t *)p, (uint32_t)~rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDEOR) {
                old = __atomic_fetch_xor((uint32_t *)p, (uint32_t)rs, __ATOMIC_ACQ_REL);
            } else {
                old = __atomic_fetch_or((uint32_t *)p, (uint32_t)rs, __ATOMIC_ACQ_REL);
            }
            break;
        }
        default: {
            uint64_t e, n;
            if (i->op == OP_CAS) {
                e = rs; n = rt;
                __atomic_compare_exchange_n((uint64_t *)p, &e, n, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
                old = e;
            } else if (i->op == OP_SWP) {
                old = __atomic_exchange_n((uint64_t *)p, rt, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDADD) {
                old = __atomic_fetch_add((uint64_t *)p, rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDCLR) {
                old = __atomic_fetch_and((uint64_t *)p, ~rs, __ATOMIC_ACQ_REL);
            } else if (i->op == OP_LDEOR) {
                old = __atomic_fetch_xor((uint64_t *)p, rs, __ATOMIC_ACQ_REL);
            } else {
                old = __atomic_fetch_or((uint64_t *)p, rs, __ATOMIC_ACQ_REL);
            }
            break;
        }
        }
        /* CAS/SWP/LD*: Rs (rm) receives the loaded value; Rt unchanged for CAS. */
        jet_set_x(c, i->rm, old);
        return 0;
    }

    /* ---- branches ---- */
    case OP_B:
        c->pc = pc + (int64_t)i->imm; return 1;
    case OP_BL:
        c->x[30] = pc + 4;
        c->pc = pc + (int64_t)i->imm; return 1;
    case OP_BR: {
        uint64_t t = jet_x(c, i->rn);
        if (!t) {
            snprintf(c->err_msg, sizeof c->err_msg,
                     "BR X%d to NULL at pc=%llx", i->rn, (unsigned long long)pc);
            c->err_code = 3; c->stop = 1; return 1;
        }
        c->pc = t; return 1;
    }
    case OP_BLR: {
        uint64_t t = jet_x(c, i->rn);
        if (!t) {
            snprintf(c->err_msg, sizeof c->err_msg,
                     "BLR X%d to NULL at pc=%llx (x0=%llx)", i->rn,
                     (unsigned long long)pc, (unsigned long long)c->x[0]);
            c->err_code = 3; c->stop = 1; return 1;
        }
        c->x[30] = pc + 4;
        c->pc = t; return 1;
    }
    case OP_RET: {
        int rn = i->rn ? i->rn : 30;
        uint64_t t = jet_x(c, rn);
        if (!t) {
            snprintf(c->err_msg, sizeof c->err_msg,
                     "RET to NULL at pc=%llx", (unsigned long long)pc);
            c->err_code = 3; c->stop = 1; return 1;
        }
        c->pc = t; return 1;
    }
    case OP_BCOND:
        if (cond_holds(c, i->cond)) { c->pc = pc + (int64_t)i->imm; return 1; }
        return 0;
    case OP_CBZ: {
        uint64_t v = mask_sf(i->sf, jet_x(c, i->rd));
        if (v == 0) { c->pc = pc + (int64_t)i->imm; return 1; }
        return 0;
    }
    case OP_CBNZ: {
        uint64_t v = mask_sf(i->sf, jet_x(c, i->rd));
        if (v != 0) { c->pc = pc + (int64_t)i->imm; return 1; }
        return 0;
    }
    case OP_TBZ: {
        uint64_t v = jet_x(c, i->rd);
        if (((v >> i->uimm) & 1) == 0) { c->pc = pc + (int64_t)i->imm; return 1; }
        return 0;
    }
    case OP_TBNZ: {
        uint64_t v = jet_x(c, i->rd);
        if (((v >> i->uimm) & 1) != 0) { c->pc = pc + (int64_t)i->imm; return 1; }
        return 0;
    }

    case OP_SVC: {
        /* Linux AArch64 syscall ABI: x8=nr, x0..x5 args. Emulate via libc;
         * never pass Linux numbers to host syscall() (wrong on Darwin). */
        extern int64_t jet_emulate_linux_syscall(jet_ctx *ctx, jet_cpu *cpu,
                                                 uint64_t nr, uint64_t a0, uint64_t a1,
                                                 uint64_t a2, uint64_t a3, uint64_t a4,
                                                 uint64_t a5);
        /* Host path: drop soft-fault claim so JVM/host SIGSEGV is not stolen. */
        int svc_saved_active = jet_fault_host_enter();
        int64_t r = jet_emulate_linux_syscall(ctx, c, c->x[8],
                                              c->x[0], c->x[1], c->x[2],
                                              c->x[3], c->x[4], c->x[5]);
        jet_fault_host_leave(svc_saved_active);
        c->x[0] = (uint64_t)r;
        return 0;
    }
    case OP_BRK: case OP_HLT:
        snprintf(c->err_msg, sizeof c->err_msg, "%s #%u at pc=%llx",
                 i->op == OP_BRK ? "BRK" : "HLT", i->uimm, (unsigned long long)pc);
        c->err_code = 1; c->stop = 1; return 1;

    case OP_MRS: {
        /* sysreg encoding in uimm = bits(19:5) of insn = o0:op1:CRn:CRm:op2 compressed.
         * Common: TPIDR_EL0 = 3,3,13,0,2 → encoding check via raw. */
        uint32_t sys = (i->raw >> 5) & 0x7fff;
        uint64_t val = 0;
        /* TPIDR_EL0: op0=3 op1=3 CRn=13 CRm=0 op2=2 → bits: */
        if ((i->raw & 0xFFFFFFE0u) == 0xD53BD040u) val = c->tpidr_el0;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD53B4200u) val = ((uint64_t)c->nzcv); /* NZCV */
        else if ((i->raw & 0xFFFFFFE0u) == 0xD53B4400u) val = c->fpcr;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD53B4420u) val = c->fpsr;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD53BE040u) { /* CNTVCT_EL0 */
            struct timespec ts; clock_gettime(CLOCK_MONOTONIC, &ts);
            val = (uint64_t)ts.tv_sec * 1000000000ull + (uint64_t)ts.tv_nsec;
        } else if ((i->raw & 0xFFFFFFE0u) == 0xD53B0020u) { /* CTR_EL0 */
            val = 0x80038003; /* reasonable stub */
        } else if ((i->raw & 0xFFFFFFE0u) == 0xD53B00A0u) { /* DCZID_EL0: no DC ZVA */
            val = 0x10; /* D bit set => DC ZVA prohibited */
        } else if ((i->raw & 0xFFFFFFE0u) == 0xD53BE000u) { /* CNTFRQ_EL0 */
            val = 1000000000ull; /* 1 GHz virtual counter freq */
        } else if ((i->raw & 0xFFFFFFE0u) == 0xD53BD060u) { /* TPIDRRO_EL0 */
            val = c->tpidr_el0; /* mirror writable TLS; bionic rarely distinguishes */
        } else if ((i->raw & 0xFFFFFFE0u) == 0xD5300000u) { /* MIDR_EL1 (emulated) */
            /* Implementer=ARM (0x41), part=Cortex-A53 (0xD03), revision=0 */
            val = 0x410FD034ull;
        } else {
            jet_log(ctx, 1, "MRS unknown sysreg raw=%08x pc=%llx", i->raw, (unsigned long long)pc);
        }
        jet_set_x(c, i->rd, val);
        (void)sys;
        return 0;
    }
    case OP_MSR: {
        uint64_t val = jet_x(c, i->rd);
        if ((i->raw & 0xFFFFFFE0u) == 0xD51BD040u) c->tpidr_el0 = val;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD51B4200u) c->nzcv = (uint32_t)val & 0xF0000000u;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD51B4400u) c->fpcr = (uint32_t)val;
        else if ((i->raw & 0xFFFFFFE0u) == 0xD51B4420u) c->fpsr = (uint32_t)val;
        else jet_log(ctx, 1, "MSR unknown sysreg raw=%08x", i->raw);
        return 0;
    }
    case OP_SYS:
        /* DC ZVA etc. — no-op if prohibited */
        return 0;
    case OP_DMB: case OP_DSB:
        __atomic_thread_fence(__ATOMIC_SEQ_CST);
        return 0;
    case OP_ISB:
        /* No host I-cache to flush for the interpreter; still seq_cst fence. */
        __atomic_thread_fence(__ATOMIC_SEQ_CST);
        return 0;
    case OP_CLREX:
        pthread_mutex_lock(&ctx->excl_mu);
        ctx->excl_valid = 0;
        pthread_mutex_unlock(&ctx->excl_mu);
        return 0;

    /* ---- FP scalar (minimal) ---- */
    case OP_FMOV_F:
        c->v[i->rd][0] = c->v[i->rn][0];
        c->v[i->rd][1] = (i->size == 2) ? c->v[i->rn][1] : 0;
        return 0;
    case OP_FMOV_G: {
        /* opc 0: GPR→FP[0], 1: FP[0]→GPR, 2: GPR→Vd.D[1], 3: Vn.D[1]→GPR */
        if (i->opc == 0) {
            uint64_t v = jet_x(c, i->rn);
            if (i->size == 0) v &= 0xffffffffull;
            c->v[i->rd][0] = v;
            c->v[i->rd][1] = 0;
        } else if (i->opc == 1) {
            uint64_t v = c->v[i->rn][0];
            if (i->size == 0) v &= 0xffffffffull;
            jet_set_x(c, i->rd, v);
        } else if (i->opc == 2) {
            c->v[i->rd][1] = jet_x(c, i->rn);
        } else { /* opc == 3 */
            jet_set_x(c, i->rd, c->v[i->rn][1]);
        }
        return 0;
    }
    case OP_FMOV_I: {
        /* VFPExpandImm(imm8) → H/S/D; upper lanes of Vd zeroed */
        uint8_t imm8 = (uint8_t)i->uimm;
        int ftype = i->size; /* 0=S, 1=D, 3=H */
        int N = (ftype == 1) ? 64 : (ftype == 3) ? 16 : 32;
        int E = (N == 16) ? 5 : (N == 32) ? 8 : 11;
        int F = N - E - 1;
        uint64_t sign = (imm8 >> 7) & 1u;
        uint64_t bit6 = (imm8 >> 6) & 1u;
        uint64_t exp = ((1u - bit6) << (E - 1)) |
                       ((bit6 ? ((1ull << (E - 3)) - 1) : 0) << 2) |
                       ((imm8 >> 4) & 3u);
        uint64_t frac = ((uint64_t)(imm8 & 0xF)) << (F - 4);
        uint64_t bitsv = (sign << (N - 1)) | (exp << F) | frac;
        if (N < 64) bitsv &= (1ull << N) - 1;
        c->v[i->rd][0] = bitsv;
        c->v[i->rd][1] = 0; /* upper 64 zeroed for scalar FMOV */
        return 0;
    }
    case OP_FMOV_V: {
        /* ASIMD FMOV imm: cmode=1111; op=0 → 32-bit VFPExpand per lane;
         * op=1 → 64-bit VFPExpand (2D only, Q=1). */
        uint8_t imm8 = (uint8_t)i->uimm;
        int opbit = i->size;
        uint8_t out[16] = {0};
        if (opbit) {
            int E = 11, F = 52;
            uint64_t sign = (imm8 >> 7) & 1u;
            uint64_t bit6 = (imm8 >> 6) & 1u;
            uint64_t exp = ((1u - bit6) << (E - 1)) |
                           ((bit6 ? ((1ull << (E - 3)) - 1) : 0) << 2) |
                           ((imm8 >> 4) & 3u);
            uint64_t frac = ((uint64_t)(imm8 & 0xF)) << (F - 4);
            uint64_t v = (sign << 63) | (exp << F) | frac;
            memcpy(out, &v, 8);
            if (i->regsize) memcpy(out + 8, &v, 8);
        } else {
            int E = 8, F = 23;
            uint64_t sign = (imm8 >> 7) & 1u;
            uint64_t bit6 = (imm8 >> 6) & 1u;
            uint64_t exp = ((1u - bit6) << (E - 1)) |
                           ((bit6 ? ((1ull << (E - 3)) - 1) : 0) << 2) |
                           ((imm8 >> 4) & 3u);
            uint64_t frac = ((uint64_t)(imm8 & 0xF)) << (F - 4);
            uint32_t v = (uint32_t)((sign << 31) | (exp << F) | frac);
            int n = i->regsize ? 4 : 2;
            for (int k = 0; k < n; k++) memcpy(out + 4 * k, &v, 4);
        }
        memcpy(&c->v[i->rd][0], out, 16);
        if (!i->regsize) c->v[i->rd][1] = 0;
        return 0;
    }
    case OP_FADD: case OP_FSUB: case OP_FMUL: case OP_FNMUL: case OP_FDIV:
    case OP_FMAX: case OP_FMIN: case OP_FMAXNM: case OP_FMINNM: {
        if (i->size == 0) {
            float a, b, r;
            memcpy(&a, &c->v[i->rn][0], 4); memcpy(&b, &c->v[i->rm][0], 4);
            switch (i->op) {
            case OP_FADD: r=a+b; break; case OP_FSUB: r=a-b; break;
            case OP_FMUL: r=a*b; break; case OP_FNMUL: r=-(a*b); break; case OP_FDIV: r=a/b; break;
            case OP_FMAX: case OP_FMAXNM: r=fmaxf(a,b); break;
            default: r=fminf(a,b); break;
            }
            uint32_t bits; memcpy(&bits, &r, 4);
            c->v[i->rd][0] = bits; c->v[i->rd][1] = 0;
        } else {
            double a, b, r;
            memcpy(&a, &c->v[i->rn][0], 8); memcpy(&b, &c->v[i->rm][0], 8);
            switch (i->op) {
            case OP_FADD: r=a+b; break; case OP_FSUB: r=a-b; break;
            case OP_FMUL: r=a*b; break; case OP_FNMUL: r=-(a*b); break; case OP_FDIV: r=a/b; break;
            case OP_FMAX: case OP_FMAXNM: r=fmax(a,b); break;
            default: r=fmin(a,b); break;
            }
            memcpy(&c->v[i->rd][0], &r, 8); c->v[i->rd][1] = 0;
        }
        return 0;
    }
    case OP_FABS: case OP_FNEG: case OP_FSQRT: {
        if (i->size == 0) {
            float a; memcpy(&a, &c->v[i->rn][0], 4);
            float r = i->op==OP_FABS?fabsf(a):i->op==OP_FNEG?-a:sqrtf(a);
            uint32_t bits; memcpy(&bits,&r,4);
            c->v[i->rd][0]=bits; c->v[i->rd][1]=0;
        } else {
            double a; memcpy(&a, &c->v[i->rn][0], 8);
            double r = i->op==OP_FABS?fabs(a):i->op==OP_FNEG?-a:sqrt(a);
            memcpy(&c->v[i->rd][0], &r, 8); c->v[i->rd][1]=0;
        }
        return 0;
    }
    case OP_FRINT: {
        if (i->size == 0) {
            float a; uint32_t u=(uint32_t)c->v[i->rn][0]; memcpy(&a,&u,4); float r=a;
            switch (i->opc) {
            case 8: r=roundf(a); break; /* nearest ties even approx roundf */
            case 9: r=ceilf(a); break;
            case 10: r=floorf(a); break;
            case 11: r=truncf(a); break;
            case 12: r=roundf(a); break;
            case 14: case 15: r=nearbyintf(a); break;
            }
            memcpy(&u,&r,4); c->v[i->rd][0]=u; c->v[i->rd][1]=0;
        } else {
            double a; memcpy(&a,&c->v[i->rn][0],8); double r=a;
            switch (i->opc) {
            case 8: r=round(a); break;
            case 9: r=ceil(a); break;
            case 10: r=floor(a); break;
            case 11: r=trunc(a); break;
            case 12: r=round(a); break;
            case 14: case 15: r=nearbyint(a); break;
            }
            memcpy(&c->v[i->rd][0],&r,8); c->v[i->rd][1]=0;
        }
        return 0;
    }

    case OP_FCMP: {
        int with_zero = (i->opc == 16 || i->opc == 24);
        if (i->size == 0) {
            float a, b=0; memcpy(&a, &c->v[i->rn][0], 4);
            if (!with_zero) memcpy(&b, &c->v[i->rm][0], 4);
            if (isnan(a) || isnan(b)) set_nzcv(c, 0,0,1,1);
            else if (a < b) set_nzcv(c, 1,0,0,0);
            else if (a > b) set_nzcv(c, 0,0,1,0);
            else set_nzcv(c, 0,1,1,0);
        } else {
            double a, b=0; memcpy(&a, &c->v[i->rn][0], 8);
            if (!with_zero) memcpy(&b, &c->v[i->rm][0], 8);
            if (isnan(a) || isnan(b)) set_nzcv(c, 0,0,1,1);
            else if (a < b) set_nzcv(c, 1,0,0,0);
            else if (a > b) set_nzcv(c, 0,0,1,0);
            else set_nzcv(c, 0,1,1,0);
        }
        return 0;

    case OP_FCCMP: {
        /* if cond then FCMP Rn,Rm else NZCV = uimm */
        int ok = 0;
        unsigned nz = c->nzcv;
        unsigned n=(nz>>31)&1, z=(nz>>30)&1, cflg=(nz>>29)&1, v=(nz>>28)&1;
        switch (i->cond) {
        case 0x0: ok = (z==1); break; case 0x1: ok = (z==0); break;
        case 0x2: ok = (cflg==1); break; case 0x3: ok = (cflg==0); break;
        case 0x4: ok = (n==1); break; case 0x5: ok = (n==0); break;
        case 0x6: ok = (v==1); break; case 0x7: ok = (v==0); break;
        case 0x8: ok = (cflg==1 && z==0); break; case 0x9: ok = (!(cflg==1 && z==0)); break;
        case 0xA: ok = (n==v); break; case 0xB: ok = (n!=v); break;
        case 0xC: ok = (z==0 && n==v); break; case 0xD: ok = (!(z==0 && n==v)); break;
        case 0xE: ok = 1; break; case 0xF: ok = 1; break;
        }
        if (!ok) {
            unsigned imm = i->uimm & 15;
            c->nzcv = ((imm&8)<<28) | ((imm&4)<<29) | ((imm&2)<<28) | ((imm&1)<<28);
            /* fix packing: N Z C V = imm[3:0] in bits 31:28 */
            c->nzcv = (imm << 28);
            return 0;
        }
        /* fall through into FCMP body by duplicating briefly */
        jet_insn t = *i; t.op = OP_FCMP; t.opc = (i->opc & 0x10) ? 8 : 0; /* E bit */
        /* reuse: compare rn vs rm */
        if (i->size == 0) {
            float a, b; uint32_t aa=(uint32_t)(c->v[i->rn][0]), bb=(uint32_t)(c->v[i->rm][0]);
            memcpy(&a,&aa,4); memcpy(&b,&bb,4);
            if (a<b) c->nzcv = 0x80000000u; else if (a>b) c->nzcv = 0x20000000u;
            else if (a==b) c->nzcv = 0x60000000u; else c->nzcv = 0x30000000u;
        } else {
            double a, b; memcpy(&a,&c->v[i->rn][0],8); memcpy(&b,&c->v[i->rm][0],8);
            if (a<b) c->nzcv = 0x80000000u; else if (a>b) c->nzcv = 0x20000000u;
            else if (a==b) c->nzcv = 0x60000000u; else c->nzcv = 0x30000000u;
        }
        return 0;
    }
    }
    case OP_FCSEL: {
        if (cond_holds(c, i->cond)) {
            c->v[i->rd][0]=c->v[i->rn][0]; c->v[i->rd][1]=c->v[i->rn][1];
        } else {
            c->v[i->rd][0]=c->v[i->rm][0]; c->v[i->rd][1]=c->v[i->rm][1];
        }
        return 0;
    }
    case OP_SCVTF: case OP_UCVTF: {
        if (i->size == 0) {
            float r = (i->op == OP_SCVTF)
                ? (float)(i->sf ? (int64_t)jet_x(c,i->rn) : (int32_t)jet_x(c,i->rn))
                : (float)(i->sf ? jet_x(c,i->rn) : (uint32_t)jet_x(c,i->rn));
            uint32_t bits; memcpy(&bits,&r,4); c->v[i->rd][0]=bits; c->v[i->rd][1]=0;
        } else {
            double r = (i->op == OP_SCVTF)
                ? (double)(i->sf ? (int64_t)jet_x(c,i->rn) : (int32_t)jet_x(c,i->rn))
                : (double)(i->sf ? jet_x(c,i->rn) : (uint32_t)jet_x(c,i->rn));
            memcpy(&c->v[i->rd][0], &r, 8); c->v[i->rd][1]=0;
        }
        return 0;
    }
    case OP_FCVTZS: case OP_FCVTZU: {
        int fbits = 0;
        if (i->imm > 0) fbits = 64 - (int)i->imm; /* fixed-point scale encoding */
        if (fbits < 0) fbits = 0;
        if (i->size == 0) {
            float a; uint32_t u=(uint32_t)c->v[i->rn][0]; memcpy(&a,&u,4);
            if (fbits) a = a * (float)(1ull << fbits);
            uint64_t r = (i->op == OP_FCVTZS) ? (uint64_t)(int64_t)a : (uint64_t)(unsigned long long)a;
            jet_set_x(c, i->rd, mask_sf(i->sf, r));
        } else {
            double a; memcpy(&a, &c->v[i->rn][0], 8);
            if (fbits) a = a * (double)(1ull << fbits);
            uint64_t r = (i->op == OP_FCVTZS) ? (uint64_t)(int64_t)a : (uint64_t)a;
            jet_set_x(c, i->rd, mask_sf(i->sf, r));
        }
        return 0;
    }
    case OP_FMADD: case OP_FMSUB: case OP_FNMADD: case OP_FNMSUB: {
        if (i->size == 0) {
            float n,m,a,r; memcpy(&n,&c->v[i->rn][0],4); memcpy(&m,&c->v[i->rm][0],4); memcpy(&a,&c->v[i->ra][0],4);
            if (i->op==OP_FMADD) r=a+n*m; else if (i->op==OP_FMSUB) r=a-n*m;
            else if (i->op==OP_FNMADD) r=-a-n*m; else r=-a+n*m;
            uint32_t bits; memcpy(&bits,&r,4); c->v[i->rd][0]=bits; c->v[i->rd][1]=0;
        } else {
            double n,m,a,r; memcpy(&n,&c->v[i->rn][0],8); memcpy(&m,&c->v[i->rm][0],8); memcpy(&a,&c->v[i->ra][0],8);
            if (i->op==OP_FMADD) r=a+n*m; else if (i->op==OP_FMSUB) r=a-n*m;
            else if (i->op==OP_FNMADD) r=-a-n*m; else r=-a+n*m;
            memcpy(&c->v[i->rd][0],&r,8); c->v[i->rd][1]=0;
        }
        return 0;
    }
    case OP_FCVT: {
        /* between S/D/H — handle S<->D */
        int src = i->size, dst_op = i->opc;
        if (src == 0 && (dst_op == 1 || dst_op == 0x5)) { /* S->D : opcode 010101? */
            float a; memcpy(&a,&c->v[i->rn][0],4); double r=a;
            memcpy(&c->v[i->rd][0],&r,8); c->v[i->rd][1]=0;
        } else if (src == 1) {
            double a; memcpy(&a,&c->v[i->rn][0],8); float r=(float)a;
            uint32_t bits; memcpy(&bits,&r,4); c->v[i->rd][0]=bits; c->v[i->rd][1]=0;
        }
        return 0;
    }

    case OP_LDR_FP: case OP_LDUR_FP: {
        /* FP LDR size:
         *  - literal (0x1C......): size 0=S(4) 1=D(8) 2=Q(16)
         *  - otherwise: size 0=B 1=H 2=S 3=D, size 4 (opc=2 remapped)=Q
         * LDR of <16 bytes must zero the high bits of Vd (ARM Armv8). */
        int is_lit = ((i->raw & 0x3F000000u) == 0x1C000000u);
        uint64_t base, addr;
        int wb = 0;
        if (is_lit) {
            addr = pc + (int64_t)i->imm;
            base = 0;
        } else if (i->op == OP_LDUR_FP) {
            base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
            addr = base + (int64_t)i->imm;
        } else if (i->opc == 1) { /* post-index */
            base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
            addr = base; wb = 1;
        } else if (i->opc == 3) { /* pre-index */
            base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
            addr = base + (int64_t)i->imm; wb = 1;
        } else if (i->opc == 4) { /* register offset */
            base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
            uint64_t idx = jet_x(c, i->rm);
            int opt = i->cond;
            int sh = i->uimm ? i->size : 0; /* S bit stashed in uimm for FP path */
            if (opt == 3 || opt == 7) { if (sh) idx <<= i->size; }
            else idx = extend_reg(1, idx, opt, (unsigned)sh);
            addr = base + idx;
        } else {
            base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
            /* opc 0: unsigned (uimm) or unscaled (imm); decode zeroes the unused */
            addr = base + (i->uimm ? (uint64_t)i->uimm : (uint64_t)(int64_t)i->imm);
        }
        int nbytes;
        if (is_lit) {
            nbytes = (i->size == 0) ? 4 : (i->size == 1) ? 8 : 16;
        } else {
            switch (i->size) {
            case 4: nbytes = 16; break;
            case 3: nbytes = 8; break;
            case 2: nbytes = 4; break;
            case 1: nbytes = 2; break;
            default: nbytes = 1; break;
            }
        }
        c->v[i->rd][0] = 0; c->v[i->rd][1] = 0;
        if (nbytes == 16) {
            c->v[i->rd][0] = ld8(addr); c->v[i->rd][1] = ld8(addr + 8);
        } else if (nbytes == 8) {
            c->v[i->rd][0] = ld8(addr);
        } else if (nbytes == 4) {
            c->v[i->rd][0] = ld4(addr);
        } else if (nbytes == 2) {
            c->v[i->rd][0] = ld2(addr);
        } else {
            c->v[i->rd][0] = ld1(addr);
        }
        if (wb) {
            uint64_t nb = base + (int64_t)i->imm;
            if (i->rn == 31) c->sp = nb; else jet_set_x(c, i->rn, nb);
        }
        (void)base;
        return 0;
    }
    case OP_STR_FP: case OP_STUR_FP: {
        /* Same size mapping as LDR_FP (non-literal). Q is size==4. */
        uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        uint64_t addr; int wb = 0;
        if (i->op == OP_STUR_FP) addr = base + (int64_t)i->imm;
        else if (i->opc == 1) { addr = base; wb = 1; }
        else if (i->opc == 3) { addr = base + (int64_t)i->imm; wb = 1; }
        else if (i->opc == 4) {
            uint64_t idx = jet_x(c, i->rm);
            int opt = i->cond;
            int sh = i->uimm ? i->size : 0;
            if (opt == 3 || opt == 7) { if (sh) idx <<= i->size; }
            else idx = extend_reg(1, idx, opt, (unsigned)sh);
            addr = base + idx;
        } else addr = base + (i->uimm ? (uint64_t)i->uimm : (uint64_t)(int64_t)i->imm);
        int nbytes;
        switch (i->size) {
        case 4: nbytes = 16; break;
        case 3: nbytes = 8; break;
        case 2: nbytes = 4; break;
        case 1: nbytes = 2; break;
        default: nbytes = 1; break;
        }
        if (nbytes == 16) {
            st8(addr, c->v[i->rd][0]); st8(addr + 8, c->v[i->rd][1]);
        } else if (nbytes == 8) {
            st8(addr, c->v[i->rd][0]);
        } else if (nbytes == 4) {
            st4(addr, (uint32_t)c->v[i->rd][0]);
        } else if (nbytes == 2) {
            st2(addr, (uint16_t)c->v[i->rd][0]);
        } else {
            st1(addr, (uint8_t)c->v[i->rd][0]);
        }
        if (wb) {
            uint64_t nb = base + (int64_t)i->imm;
            if (i->rn == 31) c->sp = nb; else jet_set_x(c, i->rn, nb);
        }
        return 0;
    }
    case OP_LDP_FP: case OP_STP_FP: {
        uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        int mode = i->opc;
        uint64_t addr = (mode == 1) ? base : base + (int64_t)i->imm;
        int sz = i->size; /* 2=S 3=D 4=Q */
        int stride = (sz <= 2) ? 4 : (sz == 3) ? 8 : 16;
        if (i->op == OP_STP_FP) {
            if (stride == 16) {
                st8(addr, c->v[i->rd][0]); st8(addr+8, c->v[i->rd][1]);
                st8(addr+16, c->v[i->ra][0]); st8(addr+24, c->v[i->ra][1]);
            } else if (stride == 8) {
                st8(addr, c->v[i->rd][0]); st8(addr+8, c->v[i->ra][0]);
            } else {
                st4(addr, (uint32_t)c->v[i->rd][0]); st4(addr+4, (uint32_t)c->v[i->ra][0]);
            }
        } else {
            if (stride == 16) {
                c->v[i->rd][0]=ld8(addr); c->v[i->rd][1]=ld8(addr+8);
                c->v[i->ra][0]=ld8(addr+16); c->v[i->ra][1]=ld8(addr+24);
            } else if (stride == 8) {
                c->v[i->rd][0]=ld8(addr); c->v[i->rd][1]=0;
                c->v[i->ra][0]=ld8(addr+8); c->v[i->ra][1]=0;
            } else {
                c->v[i->rd][0]=ld4(addr); c->v[i->rd][1]=0;
                c->v[i->ra][0]=ld4(addr+4); c->v[i->ra][1]=0;
            }
        }
        if (mode == 1 || mode == 3) {
            uint64_t nb = base + (int64_t)i->imm;
            if (i->rn==31) c->sp=nb; else jet_set_x(c,i->rn,nb);
        }
        return 0;
    }

    /* ASIMD subset */
    case OP_AND_V: case OP_ORR_V: case OP_EOR_V: case OP_BIC_V: {
        uint64_t a0=c->v[i->rn][0], a1=c->v[i->rn][1];
        uint64_t b0=c->v[i->rm][0], b1=c->v[i->rm][1];
        if (i->op == OP_BIC_V) { b0=~b0; b1=~b1; }
        if (i->op == OP_AND_V || i->op == OP_BIC_V) {
            c->v[i->rd][0]=a0&b0; c->v[i->rd][1]=i->regsize?a1&b1:0;
        } else if (i->op == OP_ORR_V) {
            c->v[i->rd][0]=a0|b0; c->v[i->rd][1]=i->regsize?a1|b1:0;
        } else {
            c->v[i->rd][0]=a0^b0; c->v[i->rd][1]=i->regsize?a1^b1:0;
        }
        if (!i->regsize) c->v[i->rd][1]=0;
        return 0;
    }
    case OP_ADD_V: case OP_SUB_V: {
        /* treat as 2x u64 for size=3, else byte-wise for size=0 (memcpy-ish) */
        if (i->size == 3) {
            if (i->op == OP_ADD_V) {
                c->v[i->rd][0]=c->v[i->rn][0]+c->v[i->rm][0];
                c->v[i->rd][1]=i->regsize?c->v[i->rn][1]+c->v[i->rm][1]:0;
            } else {
                c->v[i->rd][0]=c->v[i->rn][0]-c->v[i->rm][0];
                c->v[i->rd][1]=i->regsize?c->v[i->rn][1]-c->v[i->rm][1]:0;
            }
        } else {
            int esize = 8 << i->size;
            int n = (i->regsize ? 128 : 64) / esize;
            uint8_t out[16], a[16], b[16];
            memcpy(a, &c->v[i->rn][0], 16); memcpy(b, &c->v[i->rm][0], 16);
            for (int k = 0; k < n; k++) {
                uint64_t av=0,bv=0;
                memcpy(&av, a + k*(esize/8), esize/8);
                memcpy(&bv, b + k*(esize/8), esize/8);
                uint64_t rv = (i->op==OP_ADD_V) ? av+bv : av-bv;
                memcpy(out + k*(esize/8), &rv, esize/8);
            }
            if (!i->regsize) memset(out+8, 0, 8);
            memcpy(&c->v[i->rd][0], out, 16);
        }
        return 0;
    }
    case OP_MOVI_V: {
        /* cmode + abcdefgh → replicate. i->size holds the op bit (bit29).
         * cmode=1110 op=0: MOVI Vd.8B/16B, #imm (byte replicate)
         * cmode=1110 op=1: MOVI Vd.D/2D with per-byte 00/FF expand
         * cmode=0xxx: 32-bit shifted; cmode=01xx/10xx per ARM ARM. */
        uint8_t imm = (uint8_t)i->uimm;
        uint8_t out[16] = {0};
        int cmode = i->opc;
        int opbit = i->size; /* decode stored bit29 here for MOVI */
        if (cmode == 0xE) {
            if (opbit) {
                /* 64-bit expanded imm8 → each bit selects 0x00 or 0xFF in that byte */
                uint64_t v = 0;
                for (int b = 0; b < 8; b++) if (imm & (1u << b)) v |= 0xFFull << (8*b);
                memcpy(out, &v, 8);
                if (i->regsize) memcpy(out+8, &v, 8);
            } else {
                /* byte replicate — used by HMAC ipad/opad (movi vN.16b, #0x36/#0x5c) */
                memset(out, imm, i->regsize ? 16 : 8);
            }
        } else if ((cmode & 0xC) == 0x0) { /* 32-bit shifted */
            uint32_t v = (uint32_t)imm << (8 * ((cmode >> 1) & 3));
            for (int k = 0; k < (i->regsize?4:2); k++) memcpy(out+4*k, &v, 4);
        } else if ((cmode & 0xC) == 0x4) {
            uint16_t v = (uint16_t)(imm << (8 * ((cmode >> 1) & 1)));
            for (int k = 0; k < (i->regsize?8:4); k++) memcpy(out+2*k, &v, 2);
        } else if ((cmode & 0xE) == 0xC) {
            /* cmode=110x: 32-bit shifting ones (MVNI-ish forms); approximate as shifted */
            uint32_t v = (uint32_t)imm << (8 * ((cmode >> 1) & 1));
            for (int k = 0; k < (i->regsize?4:2); k++) memcpy(out+4*k, &v, 4);
        } else {
            memset(out, imm, i->regsize ? 16 : 8);
        }
        memcpy(&c->v[i->rd][0], out, 16);
        if (!i->regsize) c->v[i->rd][1]=0;
        return 0;
    }
    case OP_INS: {
        int imm5 = (int)i->uimm;
        int esize, index;
        if (imm5 & 1) { esize=8; index=imm5>>1; }
        else if (imm5 & 2) { esize=16; index=imm5>>2; }
        else if (imm5 & 4) { esize=32; index=imm5>>3; }
        else { esize=64; index=imm5>>4; }
        uint8_t dst[16]; memcpy(dst, &c->v[i->rd][0], 16);
        if (i->opc == 1) {
            /* from GPR */
            uint64_t v = jet_x(c, i->rn);
            memcpy(dst + index * (esize/8), &v, esize/8);
        } else {
            uint8_t src[16]; memcpy(src, &c->v[i->rn][0], 16);
            int index2 = (int)i->imm;
            /* imm4 is index2; for element size, index2 uses low bits of imm4 */
            if (esize == 8) index2 = (int)i->imm;
            else if (esize == 16) index2 = (int)i->imm >> 1;
            else if (esize == 32) index2 = (int)i->imm >> 2;
            else index2 = (int)i->imm >> 3;
            memcpy(dst + index*(esize/8), src + index2*(esize/8), esize/8);
        }
        memcpy(&c->v[i->rd][0], dst, 16);
        return 0;
    }
    case OP_DUP: {
        int imm5 = (int)i->uimm;
        int esize, index;
        if (imm5 & 1) { esize=8; index=imm5>>1; }
        else if (imm5 & 2) { esize=16; index=imm5>>2; }
        else if (imm5 & 4) { esize=32; index=imm5>>3; }
        else { esize=64; index=imm5>>4; }
        uint8_t out[16] = {0};
        uint8_t elem[8] = {0};
        if (i->opc == 1) {
            uint64_t g = jet_x(c, i->rn);
            memcpy(elem, &g, esize/8);
        } else {
            uint8_t src[16];
            memcpy(src, &c->v[i->rn][0], 16);
            memcpy(elem, src + index * (esize/8), esize/8);
        }
        int n = (i->regsize?128:64)/esize;
        for (int k=0;k<n;k++) memcpy(out + k*(esize/8), elem, esize/8);
        if (!i->regsize) memset(out+8,0,8);
        memcpy(&c->v[i->rd][0], out, 16);
        return 0;
    }
    case OP_UMOV: case OP_SMOV: {
        int imm5 = i->uimm;
        int esize, index;
        if (imm5 & 1) { esize=8; index=imm5>>1; }
        else if (imm5 & 2) { esize=16; index=imm5>>2; }
        else if (imm5 & 4) { esize=32; index=imm5>>3; }
        else { esize=64; index=imm5>>4; }
        uint8_t src[16]; memcpy(src, &c->v[i->rn][0], 16);
        uint64_t v = 0; memcpy(&v, src + index*(esize/8), esize/8);
        if (i->op == OP_SMOV) {
            int sh = 64 - esize;
            v = (uint64_t)((int64_t)(v << sh) >> sh);
        }
        jet_set_x(c, i->rd, v);
        return 0;
    }
    case OP_LD1: case OP_ST1: {
        /* Minimal: 1-4 registers, 16B elements, no lane variants. */
        uint64_t base = (i->rn == 31) ? c->sp : jet_x(c, i->rn);
        int opcode = i->opc;
        int nregs = 1;
        if (opcode == 0x7) nregs = 1;
        else if (opcode == 0xA) nregs = 2;
        else if (opcode == 0x6) nregs = 3;
        else if (opcode == 0x2) nregs = 4;
        else nregs = 1;
        int bytes = i->regsize ? 16 : 8;
        /* size bits also encode; prefer .16B when Q=1 size=00 */
        if (i->regsize) bytes = 16;
        for (int r = 0; r < nregs; r++) {
            int vt = (i->rd + r) & 31;
            if (i->op == OP_LD1) {
                c->v[vt][0] = ld8(base + (uint64_t)r*bytes);
                c->v[vt][1] = (bytes==16) ? ld8(base + (uint64_t)r*bytes + 8) : 0;
            } else {
                st8(base + (uint64_t)r*bytes, c->v[vt][0]);
                if (bytes==16) st8(base + (uint64_t)r*bytes + 8, c->v[vt][1]);
            }
        }
        /* Post-index (LD1/ST1): bit23=1. Rm=31 → imm = nregs*bytes; else offset=Xm. */
        if ((i->raw & (1u << 23)) != 0) {
            uint64_t off = (i->rm == 31)
                ? (uint64_t)nregs * (uint64_t)bytes
                : jet_x(c, i->rm);
            if (i->rn == 31) c->sp = base + off; else jet_set_x(c, i->rn, base + off);
        }
        return 0;
    }

    case OP_EXT_V: {
        int nbytes = i->regsize ? 16 : 8;
        int idx = (int)i->imm;
        uint8_t a[16], b[16], out[16];
        memcpy(a, &c->v[i->rn][0], 8); memcpy(a+8, &c->v[i->rn][1], 8);
        memcpy(b, &c->v[i->rm][0], 8); memcpy(b+8, &c->v[i->rm][1], 8);
        for (int k = 0; k < nbytes; k++) {
            int s = k + idx;
            out[k] = (s < nbytes) ? a[s] : b[s - nbytes];
        }
        if (!i->regsize) memset(out+8, 0, 8);
        memcpy(&c->v[i->rd][0], out, 8); memcpy(&c->v[i->rd][1], out+8, 8);
        return 0;
    }
    case OP_USHR: case OP_SSHR: case OP_SHL: {
        /* immh:immb in i->imm. esize from highest set bit of immh. */
        unsigned immhb = (unsigned)i->imm;
        unsigned immh = (immhb >> 3) & 0xf;
        int esize, shift, elements, reg_bytes = i->regsize ? 16 : 8;
        if (immh & 0x8) { esize = 64; }
        else if (immh & 0x4) { esize = 32; }
        else if (immh & 0x2) { esize = 16; }
        else if (immh & 0x1) { esize = 8; }
        else { /* reserved */ return 0; }
        elements = (reg_bytes * 8) / esize;
        if (i->op == OP_SHL) shift = (int)immhb - esize;
        else shift = (esize * 2) - (int)immhb; /* SSHR/USHR */
        if (shift < 0) shift = 0;
        if (shift > esize) shift = esize;
        uint8_t src[16], dst[16];
        memcpy(src, &c->v[i->rn][0], 8); memcpy(src+8, &c->v[i->rn][1], 8);
        memset(dst, 0, 16);
        for (int e = 0; e < elements; e++) {
            uint64_t val = 0;
            int off = e * (esize/8);
            memcpy(&val, src + off, esize/8);
            if (esize < 64) val &= (esize==8)?0xffull:(esize==16)?0xffffull:0xffffffffull;
            if (i->op == OP_SHL) {
                val <<= shift;
            } else if (i->op == OP_SSHR) {
                int64_t s;
                if (esize==8) s = (int8_t)val;
                else if (esize==16) s = (int16_t)val;
                else if (esize==32) s = (int32_t)val;
                else s = (int64_t)val;
                val = (uint64_t)(s >> shift);
            } else {
                val >>= shift;
            }
            if (esize < 64) val &= (esize==8)?0xffull:(esize==16)?0xffffull:0xffffffffull;
            memcpy(dst + off, &val, esize/8);
        }
        memcpy(&c->v[i->rd][0], dst, 8); memcpy(&c->v[i->rd][1], dst+8, 8);
        if (!i->regsize) c->v[i->rd][1] = 0;
        return 0;
    }
    case OP_TBL: case OP_TBX: {
        /* Table lookup: Vd.16B = TBL {Vn..}, Vm.16B indices. opc = nregs-1. */
        int nregs = (int)i->opc + 1;
        if (nregs < 1) nregs = 1;
        if (nregs > 4) nregs = 4;
        uint8_t table[64];
        memset(table, 0, sizeof table);
        for (int r = 0; r < nregs; r++) {
            int vt = (i->rn + r) & 31;
            memcpy(table + r*16, &c->v[vt][0], 8);
            memcpy(table + r*16 + 8, &c->v[vt][1], 8);
        }
        uint8_t idx[16], out[16];
        memcpy(idx, &c->v[i->rm][0], 8); memcpy(idx+8, &c->v[i->rm][1], 8);
        if (i->op == OP_TBX) {
            memcpy(out, &c->v[i->rd][0], 8); memcpy(out+8, &c->v[i->rd][1], 8);
        } else memset(out, 0, 16);
        int nbytes = i->regsize ? 16 : 8;
        int lim = nregs * 16;
        for (int k = 0; k < nbytes; k++) {
            unsigned ii = idx[k];
            if (ii < (unsigned)lim) out[k] = table[ii];
            else if (i->op == OP_TBL) out[k] = 0;
            /* TBX: leave out[k] unchanged when out of range */
        }
        memcpy(&c->v[i->rd][0], out, 8);
        c->v[i->rd][1] = i->regsize ? *(uint64_t*)(out+8) : 0;
        return 0;
    }

    case OP_ZIP1: case OP_ZIP2: case OP_UZP1: case OP_UZP2:
    case OP_TRN1: case OP_TRN2:
    case OP_ASIMD_3SAME: case OP_ASIMD_3DIFF: case OP_ASIMD_2MISC:
    case OP_ASIMD_ACROSS: case OP_ASIMD_SHIFT: case OP_ASIMD_BYELEM:
    case OP_ASIMD_PAIRWISE:
    case OP_UDOT: case OP_SDOT: case OP_USDOT:
    case OP_LD1R: case OP_LD1_LANE: case OP_ST1_LANE:
    case OP_SSHLL: case OP_USHLL: case OP_XTN: case OP_CNT:
    case OP_ADDV: case OP_UADDLV: case OP_ADDP_V: case OP_MUL_V:
    case OP_CMEQ: case OP_CMGT: case OP_CMGE: case OP_CMHI: case OP_CMHS: case OP_CMTST:
    case OP_REV64_V: case OP_REV32_V: case OP_REV16_V:
        return jet_asimd_exec(c, ctx, i);

    default:
        snprintf(c->err_msg, sizeof c->err_msg,
                 "unimplemented op=%d raw=%08x pc=%llx",
                 (int)i->op, i->raw, (unsigned long long)pc);
        c->err_code = 1; c->stop = 1; return 1;
    }
}

/* silence unused helper warnings */
static int bit_fp128(const jet_insn *i) { (void)i; return 0; }
static uint64_t bits_imm5(const jet_insn *i) { return (i->raw >> 16) & 0x1f; }

/* ---- block cache ---- */
jet_block *jet_block_get(jet_ctx *ctx, uint64_t pc) {
    uint32_t idx = (uint32_t)((pc >> 2) & ctx->block_mask);
    jet_block *b = &ctx->blocks[idx];
    if (b->pc == pc && b->n) { ctx->block_hits++; return b; }
    ctx->block_misses++;
    b->pc = pc;
    b->n = 0;
    uint64_t p = pc;
    for (uint32_t n = 0; n < JET_BLOCK_MAX_INS; n++) {
        if (jet_is_stub(ctx, p)) break;
        uint32_t raw = ld4(p);
        jet_insn ins;
        if (jet_decode(raw, &ins) != 0) {
            ins.op = OP_UNDEF; ins.raw = raw;
        }
        b->ins[n] = ins;
        b->n = n + 1;
        /* end block on branches / calls / undef */
        if (ins.op == OP_B || ins.op == OP_BL || ins.op == OP_BR || ins.op == OP_BLR ||
            ins.op == OP_RET || ins.op == OP_BCOND || ins.op == OP_CBZ || ins.op == OP_CBNZ ||
            ins.op == OP_TBZ || ins.op == OP_TBNZ || ins.op == OP_UNDEF ||
            ins.op == OP_SVC || ins.op == OP_BRK || ins.op == OP_HLT)
            break;
        p += 4;
    }
    return b;
}

/* AAPCS64 callee-saved snapshot for host traps.
 * Partner v9 CRITICAL: after fstat, guest x20 was 0. Traps must preserve
 * x19–x28, FP(x29), SP, and v8–v15 (d8–d15 lower 64). Only x0/x1 (x8 if
 * IPRIN) and d0–d7 may change for the call's return. Nested host→guest→host
 * uses stack-local snapshots here plus full save in jet_native_call_ex. */
typedef struct {
    uint64_t x19_28[10];
    uint64_t x29, sp, x30;
    uint64_t d8_15[8];
} jet_callee_saved;

static void jet_save_callee(const jet_cpu *c, jet_callee_saved *s) {
    for (int i = 0; i < 10; i++) s->x19_28[i] = c->x[19 + i];
    s->x29 = c->x[29];
    s->sp = c->sp;
    s->x30 = c->x[30];
    for (int i = 0; i < 8; i++) s->d8_15[i] = c->v[8 + i][0];
}
static void jet_restore_callee(jet_cpu *c, const jet_callee_saved *s) {
    for (int i = 0; i < 10; i++) c->x[19 + i] = s->x19_28[i];
    c->x[29] = s->x29;
    c->sp = s->sp;
    c->x[30] = s->x30;
    for (int i = 0; i < 8; i++) {
        c->v[8 + i][0] = s->d8_15[i];
        /* leave high 64 of v8–v15 alone (AAPCS64 only requires lower 64) */
    }
}
static int jet_trap_abi_check_enabled(void) {
    static int once, en;
    if (!once) {
        const char *e = getenv("JET_TRAP_ABI_CHECK");
#if !defined(NDEBUG)
        en = 1;
#else
        en = 0;
#endif
        if (e && e[0] == '1') en = 1;
        if (e && e[0] == '0') en = 0;
        once = 1;
    }
    return en;
}
static void jet_check_callee(jet_ctx *ctx, jet_cpu *c, const jet_callee_saved *s,
                             const char *trap_name) {
    if (!jet_trap_abi_check_enabled()) return;
    char diff[512]; size_t n = 0;
    diff[0] = 0;
    for (int i = 0; i < 10; i++) {
        if (c->x[19 + i] != s->x19_28[i]) {
            n += (size_t)snprintf(diff + n, sizeof diff - n, " x%d:%llx->%llx",
                    19 + i, (unsigned long long)s->x19_28[i],
                    (unsigned long long)c->x[19 + i]);
            if (n >= sizeof diff) break;
        }
    }
    if (c->x[29] != s->x29)
        n += (size_t)snprintf(diff + n, sizeof diff - n, " x29:%llx->%llx",
                (unsigned long long)s->x29, (unsigned long long)c->x[29]);
    if (c->sp != s->sp)
        n += (size_t)snprintf(diff + n, sizeof diff - n, " sp:%llx->%llx",
                (unsigned long long)s->sp, (unsigned long long)c->sp);
    for (int i = 0; i < 8 && n < sizeof diff; i++) {
        if (c->v[8 + i][0] != s->d8_15[i])
            n += (size_t)snprintf(diff + n, sizeof diff - n, " d%d:%llx->%llx",
                    8 + i, (unsigned long long)s->d8_15[i],
                    (unsigned long long)c->v[8 + i][0]);
    }
    if (diff[0]) {
        jet_log(ctx, 0, "TRAP ABI VIOLATION %s:%s", trap_name ? trap_name : "?", diff);
        fprintf(stderr, "[jet:E] TRAP ABI VIOLATION %s:%s\n",
                trap_name ? trap_name : "?", diff);
        fflush(stderr);
        abort();
    }
}

/* ---- dispatch a stub trap ---- */
static int dispatch_stub(jet_ctx *ctx, jet_cpu *c, int idx) {
    if (idx == JET_STUB_RETURN) { c->stop = 1; return 1; }
    if (idx < 0 || idx >= ctx->ntraps) {
        snprintf(c->err_msg, sizeof c->err_msg, "bad stub index %d", idx);
        c->err_code = 1; c->stop = 1; return 1;
    }
    jet_trap *t = &ctx->traps[idx];
    if (t->is_data) {
        snprintf(c->err_msg, sizeof c->err_msg, "branched to data import %s", t->name);
        c->err_code = 1; c->stop = 1; return 1;
    }
    if (!t->fn) {
        snprintf(c->err_msg, sizeof c->err_msg, "null trap fn for %s", t->name ? t->name : "?");
        c->err_code = 1; c->stop = 1; return 1;
    }
    jet_args a = {0};
    for (int i = 0; i < 8; i++) a.x[i] = c->x[i];
    a.x8 = c->x[8]; /* forward AAPCS64 indirect result to host shim */
    for (int i = 0; i < 8; i++) memcpy(&a.d[i], &c->v[i][0], 8);
    a.stack = (const uint64_t *)G2H(c->sp);
    if (t->name) {
        snprintf(ctx->last_trap_name, sizeof ctx->last_trap_name, "%s", t->name);
        ctx->last_trap_pc = c->pc;
        jet_log(ctx, 3, "trap %s pc=%llx x0=%llx x1=%llx x8=%llx sp=%llx lr=%llx",
                t->name, (unsigned long long)c->pc,
                (unsigned long long)c->x[0], (unsigned long long)c->x[1],
                (unsigned long long)c->x[8], (unsigned long long)c->sp,
                (unsigned long long)c->x[30]);
    }
    /* Snapshot callee-saved BEFORE host trap (nest-safe: locals on C stack). */
    jet_callee_saved saved;
    jet_save_callee(c, &saved);
    /* Host trap: clear jet_fault_active so JNIEnv→JVM / bionic / callbacks
     * that raise SIGSEGV (implicit null checks) are not claimed by soft-fault.
     * Nested jet_native_call from host re-enters interp and sets active again;
     * restore the saved value on return so outer guest stays claimable. */
    int trap_saved_active = jet_fault_host_enter();
    uint64_t ret = t->fn(ctx, &a, t->user);
    jet_fault_host_leave(trap_saved_active);
    /* longjmp restores the full guest frame itself — do not clobber it. */
    if (c->jumping) {
        c->jumping = 0;
        if (c->stop) return 1;
        /* h_longjmp already set x0, pc/LR; just continue at restored PC. */
        if (c->pc == 0) {
            snprintf(c->err_msg, sizeof c->err_msg, "longjmp to PC=0");
            c->err_code = 3; c->stop = 1;
        }
        return 1;
    }
    /* Assert then restore: only x0 (and d0 for FP returns) may take the result.
     * x8 IPRIN is an input pointer — contents behind it may change, not x8. */
    jet_check_callee(ctx, c, &saved, t->name);
    jet_restore_callee(c, &saved);
    if (c->stop) {
        /* Host trap requested stop (abort/stack_chk/etc.) — do not resume at LR. */
        return 1;
    }
    if (t->flags & JET_HOST_RET_F64) {
        c->v[0][0] = ret; c->v[0][1] = 0;
    } else if (t->flags & JET_HOST_RET_F32) {
        c->v[0][0] = ret & 0xffffffffull; c->v[0][1] = 0;
    } else {
        c->x[0] = ret;
    }
    /* return to LR (restored) */
    c->pc = c->x[30];
    if (c->pc == 0) {
        snprintf(c->err_msg, sizeof c->err_msg,
                 "trap %s returned to LR=0", t->name ? t->name : "?");
        c->err_code = 3; c->stop = 1;
    }
    return 1;
}


/* Soft-fault guest memory accesses: SIGSEGV/SIGBUS → stop with error.
 *
 * JVM-safe (Zero / no-JIT rely on SIGSEGV for null checks & stack banging):
 *  - Host handlers installed lazily on interpreter entry only.
 *  - On each entry, re-check: if we are no longer the chained head, save the
 *    newer handler as previous and re-install ourselves on top (libjsig /
 *    -XX:-UseSignalChaining compatible). Never clobber a newer handler.
 *  - jet_fault_active is cleared around every host trap / SVC dispatch and
 *    restored on return (nested guest re-entry sets it again in interp_run).
 *  - Claim ONLY when active AND (fault PC site is a guarded load/store helper
 *    via jet_mem_guard_depth, OR si_addr is in a guest-mapped range). Host
 *    PCs never claim. Everything else is forwarded
 *    to the saved previous handler exactly (SA_SIGINFO vs plain, SA_ONSTACK
 *    preserved on install, SIG_IGN → return, SIG_DFL → reset+re-raise).
 *  - Guest sigaction/signal/sigaltstack/pthread_sigmask stay emulated in
 *    host_bionic and never install on the host process.
 */
/* Nesting: RegisterNatives callback → jet_native_call re-enters interp. */
#define JET_FAULT_NEST_MAX 8
static __thread sigjmp_buf jet_fault_jmps[JET_FAULT_NEST_MAX];
static __thread int jet_fault_depth;             /* # active jet_interp_run frames */
/* jet_fault_active / jet_mem_guard_depth / host_enter|leave: defined at file top. */
static __thread jet_cpu *jet_fault_cpu;
static __thread jet_ctx *jet_fault_ctx;
static __thread jet_cpu *jet_fault_cpu_stack[JET_FAULT_NEST_MAX];
static __thread jet_ctx *jet_fault_ctx_stack[JET_FAULT_NEST_MAX];
static __thread int jet_fault_active_stack[JET_FAULT_NEST_MAX];

static struct sigaction jet_prev_segv, jet_prev_bus;
static int jet_fault_have_prev_segv, jet_fault_have_prev_bus;
static int jet_fault_installed;
static pthread_mutex_t jet_fault_mu = PTHREAD_MUTEX_INITIALIZER;

static void jet_fault_handler(int sig, siginfo_t *si, void *uctx);

static int jet_sa_is_ours(const struct sigaction *sa) {
    if (sa->sa_flags & SA_SIGINFO)
        return sa->sa_sigaction == jet_fault_handler;
    return (void *)sa->sa_handler == (void *)jet_fault_handler;
}

/* True if addr falls in an identity-mapped guest region we know about. */
static int jet_fault_addr_in_guest(jet_ctx *ctx, void *addr) {
    if (!ctx || !addr) return 0;
    uintptr_t a = (uintptr_t)addr;
    /* Stub page */
    if (ctx->stub_page && ctx->stub_page_size) {
        uintptr_t b = (uintptr_t)ctx->stub_page;
        if (a >= b && a < b + ctx->stub_page_size) return 1;
    }
    /* Loaded ELF images */
    for (jet_lib *L = ctx->libs; L; L = L->next) {
        if (!L->base || !L->map_size) continue;
        uintptr_t b = (uintptr_t)L->base;
        if (a >= b && a < b + L->map_size) return 1;
    }
    /* Guest stacks + TLS blobs on known CPUs */
    if (ctx->main_cpu) {
        jet_cpu *c = ctx->main_cpu;
        if (c->stack_base && c->stack_size) {
            uintptr_t b = (uintptr_t)G2H(c->stack_base);
            if (a >= b && a < b + c->stack_size) return 1;
        }
        if (c->tls_alloc) {
            /* TLS alloc is small; treat the TCB page conservatively */
            uintptr_t b = (uintptr_t)c->tls_alloc;
            if (a >= b && a < b + 4096) return 1;
        }
    }
    return 0;
}

/* Claim only when this thread is in guest execution (active) AND the fault
 * is from an interpreter guarded load/store (mem_guard_depth) OR si_addr
 * falls in a known guest mapping. Host PCs (JNI/JVM/bionic) never claim. */
static int jet_fault_should_claim(jet_ctx *ctx, siginfo_t *si) {
    if (!jet_fault_active || !jet_fault_cpu) return 0;
    if (jet_mem_guard_depth > 0) return 1;
    void *addr = (si && (si->si_signo == SIGSEGV || si->si_signo == SIGBUS))
                     ? si->si_addr : NULL;
    if (addr && jet_fault_addr_in_guest(ctx, addr)) return 1;
    return 0;
}

static void jet_fault_forward(int sig, siginfo_t *si, void *uctx) {
    struct sigaction *prev = (sig == SIGBUS) ? &jet_prev_bus : &jet_prev_segv;
    int have = (sig == SIGBUS) ? jet_fault_have_prev_bus : jet_fault_have_prev_segv;
    if (!have) {
        /* No previous handler — restore DFL and return so the faulting insn
         * re-executes under SIG_DFL (honest crash). Never raise() here:
         * SA_NODEFER would re-enter this handler. */
        struct sigaction dfl;
        memset(&dfl, 0, sizeof dfl);
        dfl.sa_handler = SIG_DFL;
        sigemptyset(&dfl.sa_mask);
        sigaction(sig, &dfl, NULL);
        return;
    }
    if (prev->sa_flags & SA_SIGINFO) {
        if (prev->sa_sigaction) prev->sa_sigaction(sig, si, uctx);
        return;
    }
    if (prev->sa_handler == SIG_IGN) return;
    if (prev->sa_handler == SIG_DFL || prev->sa_handler == NULL) {
        /* Restore prior (DFL) and return; kernel re-delivers on insn re-exec.
         * Do NOT raise() — SA_NODEFER would recurse into jet_fault_handler. */
        sigaction(sig, prev, NULL);
        return;
    }
    prev->sa_handler(sig);
}

static void jet_fault_handler(int sig, siginfo_t *si, void *uctx) {
    if (!jet_fault_should_claim(jet_fault_ctx, si)) {
        jet_fault_forward(sig, si, uctx);
        return;
    }
    if (jet_fault_ctx) {
        for (int i = 0; i < 8; i++) jet_fault_ctx->fault_x[i] = jet_fault_cpu->x[i];
        jet_fault_ctx->fault_sp = jet_fault_cpu->sp;
        jet_fault_ctx->fault_lr = jet_fault_cpu->x[30];
        jet_fault_ctx->fault_pc = jet_fault_cpu->pc;
    }
    snprintf(jet_fault_cpu->err_msg, sizeof jet_fault_cpu->err_msg,
             "guest fault sig=%d addr=%p pc=%llx lr=%llx sp=%llx x0=%llx x1=%llx last_trap=%s",
             sig, si ? si->si_addr : NULL,
             (unsigned long long)jet_fault_cpu->pc,
             (unsigned long long)jet_fault_cpu->x[30],
             (unsigned long long)jet_fault_cpu->sp,
             (unsigned long long)jet_fault_cpu->x[0],
             (unsigned long long)jet_fault_cpu->x[1],
             (jet_fault_ctx && jet_fault_ctx->last_trap_name[0])
                ? jet_fault_ctx->last_trap_name : "-");
    /* Error string is applied after siglongjmp in jet_interp_run. */
    jet_fault_cpu->err_code = 2;
    jet_fault_cpu->stop = 1;
    if (jet_fault_depth < 1 || jet_fault_depth > JET_FAULT_NEST_MAX) {
        jet_fault_forward(sig, si, uctx);
        return;
    }
    siglongjmp(jet_fault_jmps[jet_fault_depth - 1], 1);
}

static void jet_fault_install_one(int sig, struct sigaction *prev_out, int *have_prev) {
    struct sigaction cur, our;
    memset(&our, 0, sizeof our);
    our.sa_sigaction = jet_fault_handler;
    our.sa_flags = SA_SIGINFO | SA_NODEFER;
    sigemptyset(&our.sa_mask);

    if (sigaction(sig, NULL, &cur) != 0) return;

    if (jet_fault_installed && jet_sa_is_ours(&cur)) {
        /* Already head of chain — keep saved previous untouched. */
        return;
    }

    /* Preserve SA_ONSTACK from whoever we displace so alt-stack delivery
     * still works for the previous (JVM) handler when we forward. */
    if (cur.sa_flags & SA_ONSTACK)
        our.sa_flags |= SA_ONSTACK;

    /* Save current (prior JVM / test / DFL) then install ourselves on top. */
    *prev_out = cur;
    *have_prev = 1;
    sigaction(sig, &our, NULL);
}

static void jet_fault_ensure_installed(void) {
    pthread_mutex_lock(&jet_fault_mu);
    jet_fault_install_one(SIGSEGV, &jet_prev_segv, &jet_fault_have_prev_segv);
    jet_fault_install_one(SIGBUS,  &jet_prev_bus,  &jet_fault_have_prev_bus);
    jet_fault_installed = 1;
    pthread_mutex_unlock(&jet_fault_mu);
}

int jet_interp_run(jet_ctx *ctx, jet_cpu *cpu) {
    cpu->stop = 0;
    cpu->err_code = 0;

    /* Lazy install / re-chain on every guest entry. */
    jet_fault_ensure_installed();

    int nest = jet_fault_depth;
    if (nest >= JET_FAULT_NEST_MAX) {
        snprintf(cpu->err_msg, sizeof cpu->err_msg, "soft-fault nest overflow");
        cpu->err_code = 1; cpu->stop = 1;
        jet_set_error(ctx, "%s", cpu->err_msg);
        return -1;
    }
    jet_fault_cpu_stack[nest] = cpu;
    jet_fault_ctx_stack[nest] = ctx;
    jet_fault_active_stack[nest] = jet_fault_active;
    jet_fault_cpu = cpu;
    jet_fault_ctx = ctx;
    jet_fault_active = 1;
    jet_fault_depth = nest + 1;

    if (sigsetjmp(jet_fault_jmps[nest], 1) != 0) {
        jet_mem_guard_depth = 0; /* longjmp skipped leave of guarded load/store */
        jet_fault_depth = nest;
        jet_fault_active = jet_fault_active_stack[nest];
        if (nest > 0) {
            jet_fault_cpu = jet_fault_cpu_stack[nest - 1];
            jet_fault_ctx = jet_fault_ctx_stack[nest - 1];
        } else {
            jet_fault_cpu = NULL;
            jet_fault_ctx = NULL;
        }
        jet_set_error(ctx, "%s", cpu->err_msg);
        return -1;
    }

    while (!cpu->stop) {
        if (cpu->pc == 0) {
            snprintf(cpu->err_msg, sizeof cpu->err_msg, "branch to NULL");
            cpu->err_code = 3; cpu->stop = 1;
            break;
        }
        if (jet_is_stub(ctx, cpu->pc)) {
            int idx = jet_stub_index(ctx, cpu->pc);
            dispatch_stub(ctx, cpu, idx);
            continue;
        }
        jet_block *b = jet_block_get(ctx, cpu->pc);
        for (uint32_t i = 0; i < b->n && !cpu->stop; i++) {
            if (ctx->cfg.trace) {
                jet_log(ctx, 3, "pc=%llx raw=%08x op=%d x0=%llx x1=%llx sp=%llx",
                        (unsigned long long)cpu->pc, b->ins[i].raw, (int)b->ins[i].op,
                        (unsigned long long)cpu->x[0], (unsigned long long)cpu->x[1],
                        (unsigned long long)cpu->sp);
            }
            if (b->ins[i].op == OP_UNDEF) {
                snprintf(cpu->err_msg, sizeof cpu->err_msg,
                         "undef insn raw=%08x pc=%llx", b->ins[i].raw,
                         (unsigned long long)cpu->pc);
                jet_set_error(ctx, "%s", cpu->err_msg);
                cpu->err_code = 1; cpu->stop = 1;
                break;
            }
            int branched = exec_one(ctx, cpu, &b->ins[i]);
            ctx->insn_count++;
            if (cpu->stop) break;
            if (branched) break;
            cpu->pc += 4;
            /* if next pc is stub, break to outer */
            if (jet_is_stub(ctx, cpu->pc)) break;
        }
    }
    jet_fault_depth = nest;
    jet_fault_active = jet_fault_active_stack[nest];
    if (nest > 0) {
        jet_fault_cpu = jet_fault_cpu_stack[nest - 1];
        jet_fault_ctx = jet_fault_ctx_stack[nest - 1];
    } else {
        jet_fault_cpu = NULL;
        jet_fault_ctx = NULL;
        jet_mem_guard_depth = 0;
    }
    if (cpu->err_code) {
        jet_set_error(ctx, "%s", cpu->err_msg);
        return -1;
    }
    return 0;
}
