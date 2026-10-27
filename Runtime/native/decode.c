/* decode.c - AArch64 instruction decoder → jet_insn. */
#include "jet_internal.h"

static int bit(uint32_t x, int i) { return (x >> i) & 1; }
static uint32_t bits(uint32_t x, int hi, int lo) {
    return (x >> lo) & ((1u << (hi - lo + 1)) - 1u);
}
static int32_t sbits(uint32_t x, int hi, int lo) {
    uint32_t u = bits(x, hi, lo);
    int w = hi - lo + 1;
    if (u & (1u << (w - 1))) return (int32_t)(u | (~0u << w));
    return (int32_t)u;
}

/* Decode logical immediate. Returns 0 on success. */
static int decode_logical_imm(int sf, int N, int immr, int imms, uint64_t *out) {
    int len = 31 - __builtin_clz((N << 6) | ((~imms) & 0x3f));
    if (len < 1) return -1;
    int esize = 1 << len;
    int levels = esize - 1;
    int S = imms & levels;
    int R = immr & levels;
    if (S == levels) return -1;
    uint64_t welem = ((1ull << (S + 1)) - 1ull);
    /* ROR welem by R within esize */
    uint64_t mask = (esize == 64) ? ~0ull : ((1ull << esize) - 1ull);
    welem &= mask;
    uint64_t elem = ((welem >> R) | (welem << (esize - R))) & mask;
    uint64_t imm = 0;
    for (int i = 0; i < 64; i += esize) imm |= elem << i;
    if (!sf) imm &= 0xffffffffull;
    *out = imm;
    return 0;
}

int jet_decode(uint32_t w, jet_insn *o) {
    memset(o, 0, sizeof(*o));
    o->raw = w;
    o->op = OP_UNDEF;
    o->rd = bits(w, 4, 0);
    o->rn = bits(w, 9, 5);
    o->rm = bits(w, 20, 16);
    o->ra = bits(w, 14, 10);

    /* ---- branches, sys, exceptions (top 6 = 0101xx / 011xxx / 1101xx) ---- */
    uint32_t op0 = bits(w, 28, 25);

    /* Unconditional branch imm: 0x0yyyyyyy with bits[30]=0/1 for B/BL, bits[31]=0 */
    if ((w & 0x7C000000u) == 0x14000000u) {
        o->op = bit(w, 31) ? OP_BL : OP_B;
        o->imm = sbits(w, 25, 0) << 2;
        return 0;
    }
    /* CBZ/CBNZ */
    if ((w & 0x7E000000u) == 0x34000000u) {
        o->op = bit(w, 24) ? OP_CBNZ : OP_CBZ;
        o->sf = bit(w, 31);
        o->rd = bits(w, 4, 0); /* Rt */
        o->imm = sbits(w, 23, 5) << 2;
        return 0;
    }
    /* TBZ/TBNZ */
    if ((w & 0x7E000000u) == 0x36000000u) {
        o->op = bit(w, 24) ? OP_TBNZ : OP_TBZ;
        o->rd = bits(w, 4, 0);
        o->uimm = bits(w, 31, 31) << 5 | bits(w, 23, 19); /* bit position */
        o->imm = sbits(w, 18, 5) << 2;
        return 0;
    }
    /* B.cond */
    if ((w & 0xFF000010u) == 0x54000000u) {
        o->op = OP_BCOND;
        o->cond = bits(w, 3, 0);
        o->imm = sbits(w, 23, 5) << 2;
        return 0;
    }
    /* BR / BLR / RET */
    if ((w & 0xFFFFFC1Fu) == 0xD61F0000u) { o->op = OP_BR;  o->rn = bits(w, 9, 5); return 0; }
    if ((w & 0xFFFFFC1Fu) == 0xD63F0000u) { o->op = OP_BLR; o->rn = bits(w, 9, 5); return 0; }
    if ((w & 0xFFFFFC1Fu) == 0xD65F0000u) { o->op = OP_RET; o->rn = bits(w, 9, 5); return 0; }
    /* HINT / NOP */
    if ((w & 0xFFFFF01Fu) == 0xD503201Fu) { o->op = OP_NOP; return 0; }
    if ((w & 0xFFFFF000u) == 0xD5032000u) { o->op = OP_HINT; o->uimm = bits(w, 11, 5); return 0; }
    /* SVC / BRK / HLT */
    if ((w & 0xFFE0001Fu) == 0xD4000001u) { o->op = OP_SVC; o->uimm = bits(w, 20, 5); return 0; }
    if ((w & 0xFFE0001Fu) == 0xD4200000u) { o->op = OP_BRK; o->uimm = bits(w, 20, 5); return 0; }
    if ((w & 0xFFE0001Fu) == 0xD4400000u) { o->op = OP_HLT; o->uimm = bits(w, 20, 5); return 0; }
    /* MRS / MSR (EL0 regs of interest) */
    if ((w & 0xFFF00000u) == 0xD5300000u) { /* MRS */
        o->op = OP_MRS; o->rd = bits(w, 4, 0);
        o->uimm = bits(w, 19, 5); /* sysreg encoding */
        return 0;
    }
    if ((w & 0xFFF00000u) == 0xD5100000u) { /* MSR */
        o->op = OP_MSR; o->rd = bits(w, 4, 0); /* Rt */
        o->uimm = bits(w, 19, 5);
        return 0;
    }
    /* SYS / DC ZVA: 1101 0101 0000 1... */
    if ((w & 0xFFF80000u) == 0xD5080000u) {
        o->op = OP_SYS;
        o->uimm = bits(w, 20, 5); /* op1:CRn:CRm:op2 compressed differently */
        o->rn = bits(w, 9, 5); /* Rt for DC ZVA */
        o->opc = (int)bits(w, 20, 5);
        return 0;
    }

    /* ---- PC-rel addressing ---- */
    if ((w & 0x1F000000u) == 0x10000000u) {
        o->op = bit(w, 31) ? OP_ADRP : OP_ADR;
        o->rd = bits(w, 4, 0);
        int32_t imm = (sbits(w, 23, 5) << 2) | (int32_t)bits(w, 30, 29);
        o->imm = imm;
        return 0;
    }

    /* ---- Add/sub immediate ---- */
    if ((w & 0x1F000000u) == 0x11000000u) {
        int sf = bit(w, 31), S = bit(w, 29), op = bit(w, 30);
        o->sf = (uint8_t)sf;
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->uimm = bits(w, 21, 10);
        if (bit(w, 22)) o->uimm <<= 12;
        if (!op && !S) o->op = OP_ADDI;
        else if (!op && S) o->op = OP_ADDIS;
        else if (op && !S) o->op = OP_SUBI;
        else o->op = OP_SUBIS;
        return 0;
    }

    /* ---- Logical immediate ---- */
    if ((w & 0x1F800000u) == 0x12000000u) {
        int sf = bit(w, 31), N = bit(w, 22);
        int opc = bits(w, 30, 29);
        o->sf = (uint8_t)sf; o->N = (uint8_t)N;
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->immr = (uint8_t)bits(w, 21, 16);
        o->imms = (uint8_t)bits(w, 15, 10);
        uint64_t imm;
        if (decode_logical_imm(sf, N, o->immr, o->imms, &imm) != 0) return -1;
        /* stash imm in uimm (low 32) and opc as high marker — use imm field via pointer trick:
         * store full 64-bit into v-like: put low in uimm, high in (uint32)imm cast — better:
         * use the 32-bit imm as low and keep full in a side... We'll recompute at exec.
         * Store N/immr/imms; exec recomputes. */
        switch (opc) {
        case 0: o->op = OP_ANDI; break;
        case 1: o->op = OP_ORRI; break;
        case 2: o->op = OP_EORI; break;
        case 3: o->op = OP_ANDIS; break;
        }
        return 0;
    }

    /* ---- Move wide ---- */
    if ((w & 0x1F800000u) == 0x12800000u) {
        int sf = bit(w, 31), opc = bits(w, 30, 29), hw = bits(w, 22, 21);
        o->sf = (uint8_t)sf; o->rd = bits(w, 4, 0);
        o->uimm = bits(w, 20, 5);
        o->shift = (uint8_t)(hw * 16);
        if (opc == 0) o->op = OP_MOVN;
        else if (opc == 2) o->op = OP_MOVZ;
        else if (opc == 3) o->op = OP_MOVK;
        else return -1;
        return 0;
    }

    /* ---- Bitfield ---- */
    if ((w & 0x1F800000u) == 0x13000000u) {
        int sf = bit(w, 31), opc = bits(w, 30, 29);
        o->sf = (uint8_t)sf; o->N = (uint8_t)bit(w, 22);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->immr = (uint8_t)bits(w, 21, 16);
        o->imms = (uint8_t)bits(w, 15, 10);
        if (opc == 0) o->op = OP_SBFM;
        else if (opc == 1) o->op = OP_BFM;
        else if (opc == 2) o->op = OP_UBFM;
        else return -1;
        return 0;
    }

    /* ---- Extract ---- */
    if ((w & 0x1F800000u) == 0x13800000u) {
        o->sf = (uint8_t)bit(w, 31);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        o->uimm = bits(w, 15, 10);
        o->op = OP_EXTR;
        return 0;
    }

    /* ---- Load/store ---- */
    /* Literal */
    if ((w & 0x3B000000u) == 0x18000000u) {
        int opc = bits(w, 31, 30);
        o->rd = bits(w, 4, 0);
        o->imm = sbits(w, 23, 5) << 2;
        if (opc == 0) { o->op = OP_LDR_LIT; o->sf = 0; }
        else if (opc == 1) { o->op = OP_LDR_LIT; o->sf = 1; }
        else if (opc == 2) { o->op = OP_LDRSW_LIT; o->sf = 1; }
        else { o->op = OP_PRFM_LIT; }
        return 0;
    }
    /* FP literal: opc:2 011 100 imm19 Rt  (bits[29:24]=011100) */
    if ((w & 0x3F000000u) == 0x1C000000u) {
        o->op = OP_LDR_FP;
        o->rd = bits(w, 4, 0);
        o->imm = sbits(w, 23, 5) << 2;
        o->size = (uint8_t)bits(w, 31, 30); /* 00=S 01=D 10=Q */
        o->opc = 1; /* literal */
        o->rn = 31; /* unused for literal; mark */
        return 0;
    }

    /* Pair (post/pre/signed offset) — integer */
    if ((w & 0x3A000000u) == 0x28000000u) {
        int opc = bits(w, 31, 30);
        int V = bit(w, 26);
        int L = bit(w, 22);
        int mode = bits(w, 24, 23); /* 1=post 2=signed 3=pre; 0=writeback-less reserved for some */
        o->rd = bits(w, 4, 0);   /* Rt */
        o->ra = bits(w, 14, 10); /* Rt2 */
        o->rn = bits(w, 9, 5);
        int scale = (opc == 0) ? 2 : (opc == 2 || opc == 1) ? 3 : 4;
        if (V) {
            /* FP pair */
            o->size = (uint8_t)((opc << 2) /* simplified */);
            scale = 2 + opc; /* opc 00=S(4) 01=D(8) 10=Q(16) */
            o->imm = sbits(w, 21, 15) << scale;
            o->op = L ? OP_LDP_FP : OP_STP_FP;
            o->opc = (uint8_t)mode;
            o->scale = (uint8_t)scale;
            o->regsize = (opc == 2) ? 1 : 0;
            o->size = (opc == 0) ? 2 : (opc == 1) ? 3 : 4;
            return 0;
        }
        if (opc == 1 && L) { o->op = OP_LDPSW; scale = 2; }
        else if (opc == 0) { o->op = L ? OP_LDP : OP_STP; o->sf = 0; scale = 2; }
        else if (opc == 2) { o->op = L ? OP_LDP : OP_STP; o->sf = 1; scale = 3; }
        else return -1;
        o->imm = sbits(w, 21, 15) << scale;
        o->opc = (uint8_t)mode; /* addressing */
        o->scale = (uint8_t)scale;
        return 0;
    }

    /* Exclusive / acquire-release / LDAPR */
    if ((w & 0x3F000000u) == 0x08000000u) {
        int size = bits(w, 31, 30);
        int L = bit(w, 22);
        int o0 = bit(w, 15);
        int ordered = bit(w, 23); /* rough */
        o->size = (uint8_t)size;
        o->rd = bits(w, 4, 0);  /* Rt */
        o->rn = bits(w, 9, 5);
        o->rm = bits(w, 20, 16); /* Rs for store exclusive status */
        o->ra = bits(w, 14, 10); /* Rt2 */
        /* Distinguish LDXR/STXR/LDAXR/STLXR/LDAR/STLR */
        uint32_t key = bits(w, 23, 21);
        if (!L && bits(w, 14, 10) == 0x1F && bits(w, 20, 16) != 0x1F) {
            /* STXR family */
            o->op = o0 ? OP_STLXR : OP_STXR;
        } else if (L && bits(w, 20, 16) == 0x1F && bits(w, 14, 10) == 0x1F) {
            o->op = o0 ? OP_LDAXR : OP_LDXR;
        } else if (L && bits(w, 20, 16) == 0x1F && !o0) {
            o->op = OP_LDAR;
        } else if (!L && bits(w, 20, 16) == 0x1F && bits(w, 14, 10) == 0x1F) {
            o->op = OP_STLR;
        } else {
            /* CAS / SWP / LDADD etc. (LSE) — mark as CAS-ish and decode opc */
            if ((w & 0x3F20FC00u) == 0x08207C00u || (w & 0x3F208C00u) == 0x08208C00u) {
                o->op = OP_CAS; o->opc = (uint8_t)o0;
            } else if ((w & 0x3F20FC00u) == 0x38208000u) {
                o->op = OP_SWP;
            } else {
                o->op = OP_LDADD; /* catch-all; exec looks at raw */
            }
        }
        (void)ordered; (void)key;
        return 0;
    }

    /* Load/store register (unsigned imm / reg / unscaled) */
    if ((w & 0x3B000000u) == 0x39000000u ||  /* unsigned imm */
        (w & 0x3B200C00u) == 0x38000000u ||  /* unscaled */
        (w & 0x3B200C00u) == 0x38000400u ||  /* post-index */
        (w & 0x3B200C00u) == 0x38000C00u ||  /* pre-index */
        (w & 0x3B200C00u) == 0x38200800u) {  /* register offset */
        int size = bits(w, 31, 30);
        int V = bit(w, 26);
        int opc = bits(w, 23, 22);
        o->size = (uint8_t)size;
        o->rd = bits(w, 4, 0);
        o->rn = bits(w, 9, 5);
        o->sf = (size == 3) ? 1 : 0;
        if (V) {
            /* FP/SIMD LDR/STR */
            int is_load = (opc == 1 || opc == 3);
            /* bit24=1 → unsigned imm; else unscaled / pre / post / reg */
            if ((w & 0x01000000u) == 0x01000000u) {
                /* unsigned imm */
                if (size == 0 && (opc == 2 || opc == 3)) o->size = 4;
                o->uimm = bits(w, 21, 10) << ((o->size == 4) ? 4 : size);
                o->op = is_load ? OP_LDR_FP : OP_STR_FP;
                o->opc = 0; /* unsigned */
            } else if ((w & 0x00200C00u) == 0x00000000u) {
                /* unscaled (LDUR/STUR): bit21=0, bits[11:10]=00 */
                o->imm = sbits(w, 20, 12);
                o->op = is_load ? OP_LDUR_FP : OP_STUR_FP;
            } else {
                o->imm = sbits(w, 20, 12);
                o->op = is_load ? OP_LDR_FP : OP_STR_FP;
                o->opc = (uint8_t)bits(w, 11, 10); /* 1=post 3=pre */
                if ((w & 0x00200C00u) == 0x00200800u) {
                    /* register offset */
                    o->rm = bits(w, 20, 16);
                    o->cond = (uint8_t)bits(w, 15, 13); /* option */
                    o->uimm = bit(w, 12); /* S */
                    o->opc = 4;
                }
            }
            return 0;
        }
        /* Integer */
        int is_load;
        jet_op base;
        if (size == 0 && opc == 0) { base = OP_STRB; is_load = 0; }
        else if (size == 0 && opc == 1) { base = OP_LDRB; is_load = 1; }
        else if (size == 0 && opc == 2) { base = OP_LDRSB; is_load = 1; o->sf = 1; /* X */ }
        else if (size == 0 && opc == 3) { base = OP_LDRSB; is_load = 1; o->sf = 0; /* W */ }
        else if (size == 1 && opc == 0) { base = OP_STRH; is_load = 0; }
        else if (size == 1 && opc == 1) { base = OP_LDRH; is_load = 1; }
        else if (size == 1 && opc == 2) { base = OP_LDRSH; is_load = 1; o->sf = 1; }
        else if (size == 1 && opc == 3) { base = OP_LDRSH; is_load = 1; o->sf = 0; }
        else if (size == 2 && opc == 0) { base = OP_STR; is_load = 0; o->sf = 0; }
        else if (size == 2 && opc == 1) { base = OP_LDR; is_load = 1; o->sf = 0; }
        else if (size == 2 && opc == 2) { base = OP_LDRSW; is_load = 1; o->sf = 1; }
        else if (size == 3 && opc == 0) { base = OP_STR; is_load = 0; o->sf = 1; }
        else if (size == 3 && opc == 1) { base = OP_LDR; is_load = 1; o->sf = 1; }
        else return -1;
        o->op = base; (void)is_load;
        if ((w & 0x3B000000u) == 0x39000000u) {
            /* unsigned imm */
            o->uimm = bits(w, 21, 10) << size;
            o->opc = 0;
        } else if ((w & 0x3B200C00u) == 0x38200800u) {
            /* register offset */
            o->rm = bits(w, 20, 16);
            o->shift = (uint8_t)bit(w, 12); /* S: shifted by size */
            o->opc = 4; /* reg offset */
            o->cond = (uint8_t)bits(w, 15, 13); /* option */
        } else {
            /* imm9: unscaled / pre / post */
            o->imm = sbits(w, 20, 12);
            o->opc = (uint8_t)bits(w, 11, 10); /* 0=unscaled 1=post 3=pre */
        }
        return 0;
    }

    /* ---- Data-processing register ---- */
    /* Logical shifted register */
    if ((w & 0x1F000000u) == 0x0A000000u) {
        int sf = bit(w, 31), opc = bits(w, 30, 29), N = bit(w, 21);
        o->sf = (uint8_t)sf;
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        o->shift = (uint8_t)bits(w, 23, 22);
        o->uimm = bits(w, 15, 10); /* shift amount */
        if (opc == 0) o->op = N ? OP_BIC : OP_AND;
        else if (opc == 1) o->op = N ? OP_ORN : OP_ORR;
        else if (opc == 2) o->op = N ? OP_EON : OP_EOR;
        else o->op = N ? OP_BICS : OP_ANDS;
        return 0;
    }
    /* Add/sub shifted / extended */
    if ((w & 0x1F000000u) == 0x0B000000u) {
        int sf = bit(w, 31), S = bit(w, 29), op = bit(w, 30);
        o->sf = (uint8_t)sf;
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        if (bit(w, 21) == 0) {
            /* shifted */
            o->shift = (uint8_t)bits(w, 23, 22);
            o->uimm = bits(w, 15, 10);
            o->opc = 0;
        } else {
            /* extended */
            o->shift = 4; /* mark extended */
            o->cond = (uint8_t)bits(w, 15, 13); /* option */
            o->uimm = bits(w, 12, 10); /* imm3 */
            o->opc = 1;
        }
        if (!op && !S) o->op = OP_ADD;
        else if (!op && S) o->op = OP_ADDS;
        else if (op && !S) o->op = OP_SUB;
        else o->op = OP_SUBS;
        return 0;
    }
    /* ADC/SBC */
    if ((w & 0x1FE0FC00u) == 0x1A000000u) {
        o->sf = bit(w, 31); o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        int S = bit(w, 29), op = bit(w, 30);
        /* Encode as ADD/SUB with opc=2 meaning ADC/SBC */
        o->opc = 2;
        o->op = op ? OP_SUB : OP_ADD;
        if (S) o->op = op ? OP_SUBS : OP_ADDS;
        return 0;
    }

    /* Conditional select */
    if ((w & 0x1FE00800u) == 0x1A800000u) {  /* bit11 must be 0; bit10 = op2 */
        o->sf = bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->cond = (uint8_t)bits(w, 15, 12);
        int op = bit(w, 30), o2 = bit(w, 10);
        if (!op && !o2) o->op = OP_CSEL;
        else if (!op && o2) o->op = OP_CSINC;
        else if (op && !o2) o->op = OP_CSINV;
        else o->op = OP_CSNEG;
        return 0;
    }
    /* Conditional compare */
    /* CCMN/CCMP: sf op S=1 11010010 ... ; S is bit29. Mask drops sf/op. */
    if ((w & 0x1FE00C10u) == 0x1A400000u && bit(w, 29)) {
        o->sf = bit(w, 31);
        o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->cond = (uint8_t)bits(w, 15, 12);
        o->uimm = bits(w, 3, 0); /* nzcv */
        o->opc = bit(w, 11); /* 0=reg 1=imm */
        if (bit(w, 11)) o->rm = bits(w, 20, 16); /* imm5 in rm field */
        o->op = bit(w, 30) ? OP_CCMP : OP_CCMN;
        return 0;
    }

    /* Data-processing 2-source / 1-source / 3-source */
    if ((w & 0x1FE00000u) == 0x1AC00000u) {
        o->sf = bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        int opcode = bits(w, 15, 10);
        switch (opcode) {
        case 2: o->op = OP_UDIV; break;
        case 3: o->op = OP_SDIV; break;
        case 8: o->op = OP_LSLV; break;
        case 9: o->op = OP_LSRV; break;
        case 10: o->op = OP_ASRV; break;
        case 11: o->op = OP_RORV; break;
        default: return -1;
        }
        return 0;
    }
    if ((w & 0x5FFFFC00u) == 0x5AC00000u) {
        /* 1-source: sf 1 0 11010110 00000 opcode Rn Rd */
        if (bits(w, 20, 16) != 0) return -1;
        o->sf = bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        switch (bits(w, 15, 10)) {
        case 0: o->op = OP_RBIT; break;
        case 1: o->op = OP_REV16; break;
        case 2: o->op = o->sf ? OP_REV32 : OP_REV; break;
        case 3: o->op = OP_REV; break;
        case 4: o->op = OP_CLZ; break;
        case 5: o->op = OP_CLS; break;
        default: return -1;
        }
        return 0;
    }
    /* MADD/MSUB family */
    if ((w & 0x1F000000u) == 0x1B000000u) {
        o->sf = bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16); o->ra = bits(w,14,10);
        int op31 = bits(w, 30, 29); /* should be 00 for data-proc 3src of this form */
        (void)op31;
        int o0 = bit(w, 15);
        int opc = bits(w, 23, 21);
        if (opc == 0) o->op = o0 ? OP_MSUB : OP_MADD;
        else if (opc == 1) o->op = o0 ? OP_SMSUBL : OP_SMADDL;
        else if (opc == 2) o->op = OP_SMULH;
        else if (opc == 5) o->op = o0 ? OP_UMSUBL : OP_UMADDL;
        else if (opc == 6) o->op = OP_UMULH;
        else return -1;
        /* fix ra: bits 14:10 */
        o->ra = bits(w, 14, 10);
        return 0;
    }

    /* ---- FP / ASIMD (subset) ---- */
    /* FP data-processing 2-source / 1-source / compare / select */
    /* FP data-processing 2-source: M=0 S=0 type:2 opcode:4, bits[11:10]=10.
     * One mask matches FMUL/FDIV/FADD/FSUB/FMIN/FMAX (opcode in 15:12). */
    if ((w & 0xFF200C00u) == 0x1E200800u) {
        o->size = (uint8_t)bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        if ((w & 0xFF20FC00u) == 0x1E202800u) { o->op = OP_FADD; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E203800u) { o->op = OP_FSUB; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E200800u) { o->op = OP_FMUL; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E201800u) { o->op = OP_FDIV; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E204800u) { o->op = OP_FMAX; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E205800u) { o->op = OP_FMIN; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E206800u) { o->op = OP_FMAXNM; return 0; }
        if ((w & 0xFF20FC00u) == 0x1E207800u) { o->op = OP_FMINNM; return 0; }
    }
    /* FP 1-source */
    if ((w & 0xFF3FFC00u) == 0x1E204000u) { o->op = OP_FMOV_F; o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E20C000u) { o->op = OP_FABS;  o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E214000u) { o->op = OP_FNEG;  o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E21C000u) { o->op = OP_FSQRT; o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    /* FCVT between precisions */
    if ((w & 0xFF3FFC00u) == 0x1E224000u || (w & 0xFF3FFC00u) == 0x1E23C000u ||
        (w & 0xFF3FFC00u) == 0x1E22C000u) {
        o->op = OP_FCVT; o->size = bits(w,23,22); o->opc = bits(w,15,10);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); return 0;
    }
    /* FP compare */
    if ((w & 0xFF20FC1Fu) == 0x1E202000u || (w & 0xFF20FC1Fu) == 0x1E202008u ||
        (w & 0xFF20FC1Fu) == 0x1E202010u || (w & 0xFF20FC1Fu) == 0x1E202018u) {
        o->op = OP_FCMP; o->size = bits(w,23,22);
        o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->opc = bits(w,4,0); /* 0=FCMP 8=FCMPE 16=FCMP#0 24=FCMPE#0 */
        return 0;
    }
    /* FMOV / SCVTF / UCVTF / FCVTZS / FCVTZU between GPR and FP (top-level).
     * Encoding: sf 0 S 11110 type 1 rmode:opcode 000000 Rn Rd
     * Mask bits [15:10] must be 0. */
    if ((w & 0x1F20FC00u) == 0x1E200000u) {
        o->sf = bit(w, 31);
        o->size = (uint8_t)bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        int rmode = bits(w, 20, 19);
        int opc = bits(w, 18, 16);
        /* FMOV GPR<->FP: rmode=00 opc=110 (to FP) or 111 (to GPR) */
        if (rmode == 0 && opc == 7) {
            /* FP → GPR: FMOV Wd,Sn / Xd,Dn */
            o->op = OP_FMOV_G; o->opc = 1; return 0;
        }
        if (rmode == 0 && opc == 6) {
            /* GPR → FP: FMOV Sd,Wn / Dd,Xn */
            o->op = OP_FMOV_G; o->opc = 0; return 0;
        }
        /* FMOV Vd.D[1],Xn / Xd,Vn.D[1]: rmode=01 opc=110/111, type=10 — skip for now */
        if (rmode == 0 && opc == 2) { o->op = OP_SCVTF; return 0; }
        if (rmode == 0 && opc == 3) { o->op = OP_UCVTF; return 0; }
        if (rmode == 3 && opc == 0) { o->op = OP_FCVTZS; return 0; }
        if (rmode == 3 && opc == 1) { o->op = OP_FCVTZU; return 0; }
        /* fallthrough for other conversion forms */
    }
    /* FMADD family */
    if ((w & 0x1F000000u) == 0x1F000000u && bit(w, 29) == 0) {
        o->size = bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16); o->ra = bits(w,14,10);
        int o1 = bit(w, 21), o0 = bit(w, 15);
        if (!o1 && !o0) o->op = OP_FMADD;
        else if (!o1 && o0) o->op = OP_FMSUB;
        else if (o1 && !o0) o->op = OP_FNMADD;
        else o->op = OP_FNMSUB;
        return 0;
    }
    /* FCSEL */
    if ((w & 0xFF200C00u) == 0x1E200C00u) {
        o->op = OP_FCSEL; o->size = bits(w,23,22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->cond = bits(w, 15, 12);
        return 0;
    }

    /* ASIMD ORR/AND/EOR (vector) - used heavily by memcpy */
    if ((w & 0xBFE0FC00u) == 0x0E201C00u) { o->op = OP_AND_V; goto vec_3; }
    if ((w & 0xBFE0FC00u) == 0x0EA01C00u) { o->op = OP_ORR_V; goto vec_3; }
    if ((w & 0xBFE0FC00u) == 0x2E201C00u) { o->op = OP_EOR_V; goto vec_3; }
    if ((w & 0xBFE0FC00u) == 0x0E601C00u) { o->op = OP_BIC_V; goto vec_3; }
    if ((w & 0xBFE0FC00u) == 0x0E208400u) { o->op = OP_ADD_V; goto vec_3; }
    if ((w & 0xBFE0FC00u) == 0x2E208400u) { o->op = OP_SUB_V; goto vec_3; }
    if (0) {
    vec_3:
        o->regsize = bit(w, 30);
        o->size = bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        return 0;
    }

    /* MOVI / ORR-imm / FMOV-imm (ASIMD modified immediate) */
    /* ASIMD modified immediate: bits[11:10]=01; cmode in 15:12 varies */
    if ((w & 0x9FF80400u) == 0x0F000400u) {
        o->op = OP_MOVI_V;
        o->regsize = bit(w, 30);
        o->rd = bits(w,4,0);
        o->uimm = (bits(w,18,16)<<5) | bits(w,9,5); /* abcdefgh */
        o->opc = bits(w, 15, 12); /* cmode */
        o->size = bit(w, 29); /* op bit */
        return 0;
    }


    /* INS (general): Vd.ts[index], Rn — MOV Vd.D[1], Xn etc. */
    if ((w & 0xFFE0FC00u) == 0x4E001C00u) {
        o->op = OP_INS;
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 → size+index */
        o->opc = 1; /* from GPR */
        return 0;
    }
    /* INS (element): Vd.ts[index], Vn.ts[index2] */
    /* INS element requires Q=1 */
    if ((w & 0xFFE08400u) == 0x6E000400u) {
        o->op = OP_INS;
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 */
        o->imm = bits(w, 14, 11); /* imm4 */
        o->opc = 0; /* from vector element */
        return 0;
    }
    /* DUP element / general */
    if ((w & 0xBF3FEC00u) == 0x0E000C00u) {
        o->op = OP_DUP; o->regsize = bit(w,30);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 */
        return 0;
    }

    /* UMOV / SMOV */
    if ((w & 0xBFE0FC00u) == 0x0E003C00u) {
        o->op = OP_UMOV; o->regsize = bit(w,30);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16);
        return 0;
    }
    if ((w & 0xBFE0FC00u) == 0x0E002C00u) {
        o->op = OP_SMOV; o->regsize = bit(w,30);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16);
        return 0;
    }

    /* LD1/ST1 multiple / single — very common in memcpy.
     * Minimal: LD1 {Vt}.16B, [Xn] and post-index forms. */
    if ((w & 0xBFE00000u) == 0x0C400000u || (w & 0xBFE00000u) == 0x0C000000u) {
        int L = bit(w, 22);
        o->op = L ? OP_LD1 : OP_ST1;
        o->regsize = bit(w, 30);
        o->rd = bits(w,4,0);
        o->rn = bits(w,9,5);
        o->rm = bits(w,20,16);
        o->opc = bits(w, 15, 12); /* opcode = #regs / size encoding */
        o->size = bits(w, 11, 10);
        o->uimm = bit(w, 23); /* 0=no offset info in this form */
        return 0;
    }

    (void)op0;
    return -1;
}
