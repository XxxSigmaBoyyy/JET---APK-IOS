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
    /* HINT / NOP — all hints (NOP/YIELD/WFE/WFI/SEV/SEVL/BTI/PACIASP/AUTIASP/XPACLRI…) are NOP-safe */
    if ((w & 0xFFFFF01Fu) == 0xD503201Fu) {
        unsigned hint = bits(w, 11, 5);
        o->op = hint == 0 ? OP_NOP : OP_HINT;
        o->uimm = hint;
        return 0;
    }
    /* Barriers: DMB/DSB/ISB — CRm option in 11:8; opc in 7:5 (100=DSB, 101=DMB, 110=ISB) */
    if ((w & 0xFFFFF01Fu) == 0xD503301Fu) {
        unsigned opc = bits(w, 7, 5);
        o->uimm = bits(w, 11, 8);
        if (opc == 5) { o->op = OP_DMB; return 0; }
        if (opc == 4) { o->op = OP_DSB; return 0; }
        if (opc == 6) { o->op = OP_ISB; return 0; }
        if (opc == 2) { o->op = OP_CLREX; return 0; } /* CLREX CRm=0 opc=010 */
    }
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

    /* LSE atomic memory ops (LDADD/LDCLR/LDEOR/LDSET/SWP + acquire/release):
     * size:111000 A R 1 Rs o3 0 opc Rn Rt  — bit21=1, bits29:24=111000 */
    /* bits11:10 must be 00 (distinguishes from LDR/STR register-offset which uses 10). */
    if ((w & 0x3F200C00u) == 0x38200000u) {
        int size = bits(w, 31, 30);
        int opc = bits(w, 14, 12);
        int o3 = bit(w, 15); /* SWP when o3=1 and opc=0; else LD* */
        o->size = (uint8_t)size;
        o->rd = bits(w, 4, 0);   /* Rt */
        o->rn = bits(w, 9, 5);
        o->rm = bits(w, 20, 16); /* Rs */
        o->opc = (uint8_t)((bit(w, 23) << 1) | bit(w, 22)); /* A:R for ordering */
        if (o3) {
            o->op = OP_SWP;
        } else {
            switch (opc) {
            case 0: o->op = OP_LDADD; break;
            case 1: o->op = OP_LDCLR; break;
            case 2: o->op = OP_LDEOR; break;
            case 3: o->op = OP_LDSET; break;
            /* 100..111: LDSmax/LDSmin/LDUmax/LDUmin — map to LDADD-ish via raw in exec */
            default: o->op = OP_LDADD; o->imm = opc; break;
            }
        }
        return 0;
    }

    /* Exclusive / acquire-release / LDAPR / CAS / CASP */
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
            /* CAS / CASP in exclusive group (LSE) */
            if (bits(w, 21, 21) == 1 && bits(w, 14, 10) == 0x1F) {
                o->op = OP_CAS; o->opc = (uint8_t)o0;
            } else if (bits(w, 21, 21) == 1) {
                o->op = OP_CASP; o->opc = (uint8_t)o0; /* Rt2 = ra */
            } else {
                o->op = OP_CAS; o->opc = (uint8_t)o0;
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
            /* FP/SIMD LDR/STR
             * size/opc: 0/0 B, 1/0 H, 2/0 S, 3/0 D; size=0 opc=2/3 => Q.
             * Remap Q to size==4 on every addressing mode (unsigned, unscaled,
             * pre/post, reg-offset) so execute sees a consistent width. */
            int is_load = (opc == 1 || opc == 3);
            if (size == 0 && (opc == 2 || opc == 3)) o->size = 4;
            /* bit24=1 → unsigned imm; else unscaled / pre / post / reg */
            if ((w & 0x01000000u) == 0x01000000u) {
                /* unsigned imm */
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
        else if (opc == 2 && !V) { /* PRFM (imm/reg/unscaled) */
            o->op = OP_HINT; return 0;
        }
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
    /* Ignore bit11 so both reg (11:10=00) and imm (11:10=10) forms match. */
    if ((w & 0x1FE00410u) == 0x1A400000u && bit(w, 29)) {
        o->sf = bit(w, 31);
        o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->cond = (uint8_t)bits(w, 15, 12);
        o->uimm = bits(w, 3, 0); /* nzcv */
        o->opc = bit(w, 11); /* 0=reg 1=imm */
        if (bit(w, 11)) o->rm = bits(w, 20, 16); /* imm5 in rm field */
        o->op = bit(w, 30) ? OP_CCMP : OP_CCMN;
        return 0;
    }

    /* Data-processing 1-source BEFORE 2-source: encodings share bits[28:21]=0xAC
     * but differ in bit30 (1-src=1, 2-src=0). A mask that ignores bit30 matches
     * REV/RBIT/CLZ as UDIV/SDIV with Rm=0 → result 0 (broke SHA/PBKDF2). */
    /* 1-source: sf 1 0 11010110 00000 opcode Rn Rd
     * Mask bits[30:16] only — bits[15:10] are opcode (must not be fixed to 0). */
    if (((w & 0x3FFF0000u) == 0x1AC00000u) && bit(w, 30) == 1) {
        if (bits(w, 20, 16) != 0) return -1;
        o->sf = (uint8_t)bit(w, 31);
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
    /* 2-source: sf 0 0 11010110 Rm opcode Rn Rd — require bit30=0 */
    if ((w & 0x5FE00000u) == 0x1AC00000u) {
        o->sf = (uint8_t)bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        int opcode = bits(w, 15, 10);
        switch (opcode) {
        case 2: o->op = OP_UDIV; break;
        case 3: o->op = OP_SDIV; break;
        case 8: o->op = OP_LSLV; break;
        case 9: o->op = OP_LSRV; break;
        case 10: o->op = OP_ASRV; break;
        case 11: o->op = OP_RORV; break;
        case 0x10: case 0x11: case 0x12: case 0x13:
        case 0x14: case 0x15: case 0x16: case 0x17:
            /* CRC32/CRC32C: opcode = 010 || C || sz */
            o->size = (uint8_t)(opcode & 3);
            o->N = (opcode >> 2) & 1;
            o->op = OP_CRC32;
            break;
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
        if ((w & 0xFF20FC00u) == 0x1E208800u) { o->op = OP_FNMUL; return 0; }
    }
    /* FMOV (scalar, immediate): ftype in 23:22, imm8 in 20:13, Rn=00000, op=100 */
    if ((w & 0xFF201C00u) == 0x1E201000u && bits(w, 9, 5) == 0) {
        o->op = OP_FMOV_I;
        o->size = (uint8_t)bits(w, 23, 22); /* 0=S, 1=D, 3=H */
        o->rd = bits(w, 4, 0);
        o->uimm = bits(w, 20, 13); /* imm8 */
        return 0;
    }
    /* FP 1-source */
    if ((w & 0xFF3FFC00u) == 0x1E204000u) { o->op = OP_FMOV_F; o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E20C000u) { o->op = OP_FABS;  o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E214000u) { o->op = OP_FNEG;  o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    if ((w & 0xFF3FFC00u) == 0x1E21C000u) { o->op = OP_FSQRT; o->size=bits(w,23,22); o->rd=bits(w,4,0); o->rn=bits(w,9,5); return 0; }
    /* FRINT* family (scalar): opcode in bits20:15 */
    if ((w & 0xFF200C00u) == 0x1E200000u && bit(w, 21) && bits(w, 14, 10) == 0x10) {
        unsigned opc = bits(w, 20, 15);
        /* FRINTN=8 FRINTP=9 FRINTM=10 FRINTZ=11 FRINTA=12 FRINTX=14 FRINTI=15 */
        if (opc >= 8 && opc <= 15 && opc != 13) {
            o->op = OP_FRINT; o->size = bits(w,23,22);
            o->opc = (uint8_t)opc;
            o->rd = bits(w,4,0); o->rn = bits(w,9,5); return 0;
        }
    }
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
        /* Opcode polarity matches GNU objdump / llvm (111=to FP, 110=to GPR). */
        if (rmode == 0 && opc == 7) {
            /* GPR → FP: FMOV Sd,Wn / Dd,Xn */
            o->op = OP_FMOV_G; o->opc = 0; return 0;
        }
        if (rmode == 0 && opc == 6) {
            /* FP → GPR: FMOV Wd,Sn / Xd,Dn */
            o->op = OP_FMOV_G; o->opc = 1; return 0;
        }
        /* FMOV Vd.D[1],Xn / Xd,Vn.D[1]: rmode=01 opc=110/111, type=10 */
        if (rmode == 1 && o->size == 2 && opc == 7) {
            o->op = OP_FMOV_G; o->opc = 2; return 0; /* Xn → Vd.D[1] (objdump-confirmed) */
        }
        if (rmode == 1 && o->size == 2 && opc == 6) {
            o->op = OP_FMOV_G; o->opc = 3; return 0; /* Vn.D[1] → Xd */
        }
        if (rmode == 0 && opc == 2) { o->op = OP_SCVTF; return 0; }
        if (rmode == 0 && opc == 3) { o->op = OP_UCVTF; return 0; }
        if (rmode == 3 && opc == 0) { o->op = OP_FCVTZS; return 0; }
        if (rmode == 3 && opc == 1) { o->op = OP_FCVTZU; return 0; }
        /* FCVT{N,P,M,A}{S,U}: rmode 00/01/10/00 with opc 000..101 */
        if (opc <= 5) {
            /* Map all FP→int directed conversions to FCVTZS/U with opc tagging mode */
            o->op = (opc & 1) ? OP_FCVTZU : OP_FCVTZS;
            o->opc = (uint8_t)((rmode << 4) | opc); /* preserve rounding mode */
            return 0;
        }
        /* fallthrough for other conversion forms */
    }

    /* FP↔fixed-point conversions (scale form, bit21=0): FCVTZS/U, SCVTF, UCVTF */
    if ((w & 0x1F200000u) == 0x1E000000u && bit(w, 21) == 0) {
        o->sf = bit(w, 31);
        o->size = (uint8_t)bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->imm = (int32_t)bits(w, 15, 10); /* scale */
        int rmode = bits(w, 20, 19);
        int opc = bits(w, 18, 16);
        if (rmode == 3 && opc == 0) { o->op = OP_FCVTZS; return 0; }
        if (rmode == 3 && opc == 1) { o->op = OP_FCVTZU; return 0; }
        if (rmode == 0 && opc == 2) { o->op = OP_SCVTF; return 0; }
        if (rmode == 0 && opc == 3) { o->op = OP_UCVTF; return 0; }
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
    if ((w & 0xBF20FC00u) == 0x0E208400u) { o->op = OP_ADD_V; goto vec_3; }
    if ((w & 0xBF20FC00u) == 0x2E208400u) { o->op = OP_SUB_V; goto vec_3; }
    if (0) {
    vec_3:
        o->regsize = bit(w, 30);
        o->size = bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        return 0;
    }

    /* ASIMD shift by immediate: SSHR/USHR/SHL.
     * Must be matched before MOVI (overlapping bit11:10=01). */
    if ((w & 0xBF80FC00u) == 0x2F000400u) { /* USHR U=1 opcode=000001 (Q ignored) */
        o->op = OP_USHR;
        o->regsize = bit(w, 30);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->imm = bits(w, 22, 16); /* immh:immb */
        o->size = bits(w, 22, 19); /* immh */
        return 0;
    }
    if ((w & 0xBF80FC00u) == 0x0F000400u) { /* SSHR U=0 opcode=000001 */
        o->op = OP_SSHR;
        o->regsize = bit(w, 30);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->imm = bits(w, 22, 16);
        o->size = bits(w, 22, 19);
        return 0;
    }
    if ((w & 0xBF80FC00u) == 0x0F005400u || (w & 0xBF80FC00u) == 0x2F005400u) {
        o->op = OP_SHL;
        o->regsize = bit(w, 30);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5);
        o->imm = bits(w, 22, 16);
        o->size = bits(w, 22, 19);
        return 0;
    }
    /* EXT Vd.16B, Vn.16B, Vm.16B, #index */
    if ((w & 0xBFE08400u) == 0x2E000000u) {
        o->op = OP_EXT_V;
        o->regsize = bit(w, 30);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        o->imm = bits(w, 14, 11); /* imm4 */
        return 0;
    }
    /* TBL / TBX — bit12=0 TBL, =1 TBX; bits14:13 = len (nregs-1). */
    if ((w & 0xBFE08C00u) == 0x0E000000u) {
        o->op = bit(w, 12) ? OP_TBX : OP_TBL;
        o->regsize = bit(w, 30);
        o->rd = bits(w, 4, 0); o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        o->opc = (uint8_t)bits(w, 14, 13);
        return 0;
    }

    /* MOVI / MVNI / ORR-imm / FMOV-imm (ASIMD modified immediate) */
    /* ASIMD modified immediate: bits[11:10]=01; cmode in 15:12 varies */
    if ((w & 0x9FF80400u) == 0x0F000400u) {
        o->regsize = bit(w, 30);
        o->rd = bits(w,4,0);
        o->uimm = (bits(w,18,16)<<5) | bits(w,9,5); /* abcdefgh */
        o->opc = bits(w, 15, 12); /* cmode */
        o->size = bit(w, 29); /* op bit */
        /* cmode=1111: FMOV vector immediate (op=0 → 2S/4S, op=1 → 2D) */
        if (bits(w, 15, 12) == 0xF)
            o->op = OP_FMOV_V;
        else
            o->op = OP_MOVI_V;
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
    /* DUP (element): Vd.T, Vn.ts[index] — bits15:10=000001 */
    if ((w & 0xBFE0FC00u) == 0x0E000400u) {
        o->op = OP_DUP; o->regsize = bit(w,30);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 */
        o->opc = 0; /* from vector */
        return 0;
    }
    /* DUP (general): Vd.T, Rn — bits15:10=000011 */
    if ((w & 0xBFE0FC00u) == 0x0E000C00u) {
        o->op = OP_DUP; o->regsize = bit(w,30);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 → size */
        o->opc = 1; /* from GPR */
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

    /* LD1/ST1 multiple: Q=bit30 (masked), post-index=bit23, L=bit22.
     * After clearing Q, patterns are 0x0C000000 (ST), 0x0C400000 (LD),
     * 0x0C800000 (ST post/reg), 0x0CC00000 (LD post/reg). */
    {
        uint32_t ld1m = w & 0xBFE00000u;
        if (ld1m == 0x0C000000u || ld1m == 0x0C400000u ||
            ld1m == 0x0C800000u || ld1m == 0x0CC00000u) {
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
    }


    /* ---- ASIMD permute: ZIP/UZP/TRN ----
     * 0 Q 0 01110 size 0 Rm 0 opc 10 Rn Rd */
    if ((w & 0xBF208C00u) == 0x0E000800u) {
        int po = bits(w, 14, 12); /* 1=UZP1 2=TRN1 3=ZIP1 5=UZP2 6=TRN2 7=ZIP2 */
        switch (po) {
        case 1: o->op = OP_UZP1; break;
        case 2: o->op = OP_TRN1; break;
        case 3: o->op = OP_ZIP1; break;
        case 5: o->op = OP_UZP2; break;
        case 6: o->op = OP_TRN2; break;
        case 7: o->op = OP_ZIP2; break;
        default: goto asimd_done_miss;
        }
        o->regsize = bit(w, 30);
        o->size = bits(w, 23, 22);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        return 0;
    }
asimd_done_miss: (void)0;

    /* ---- ASIMD three-same (vector + FP): bit21=1, bit10=1 ----
     * Existing AND/ORR/EOR/BIC/ADD/SUB matched above. */
    
    /* ASIMD Dot Product (FEAT_DotProd / FEAT_I8MM): UDOT/SDOT/USDOT vector.
     * 0 Q U 01110 size=10 0 Rm opcode 1 Rn Rd; opcode 100101 = SDOT/UDOT, 100111 = USDOT */
    if ((w & 0x9F20F400u) == 0x0E009400u) {
        int opc = bits(w, 15, 10);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->regsize = bit(w, 30); /* Q */
        o->N = bit(w, 29);       /* U */
        o->size = 2;
        if (opc == 0x25) { /* 100101 */
            o->op = o->N ? OP_UDOT : OP_SDOT;
            return 0;
        }
        if (opc == 0x27 && !o->N) { /* 100111 USDOT */
            o->op = OP_USDOT;
            return 0;
        }
    }

    /* CRC32 / CRC32C (FEAT_CRC32): sf 00 11010110 Rm 010 C sz Rn Rd */
    if ((w & 0x3FE0E000u) == 0x1AC04000u) {
        o->sf = bit(w, 31);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->size = (uint8_t)bits(w, 11, 10); /* sz: B/H/W/X */
        o->N = bit(w, 12);                   /* C: CRC32C vs CRC32 */
        o->op = OP_CRC32;
        return 0;
    }

    if ((w & 0x9F200400u) == 0x0E200400u) {
        o->op = OP_ASIMD_3SAME;
        o->regsize = bit(w, 30);
        o->N = (uint8_t)bit(w, 29);          /* U */
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)bits(w, 15, 10);   /* 6-bit opcode */
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        return 0;
    }

    /* ---- ASIMD three-different (long/wide/narrow) ----
     * bit21=1, bits11:10=00 */
    if ((w & 0x9F200C00u) == 0x0E200000u) {
        o->op = OP_ASIMD_3DIFF;
        o->regsize = bit(w, 30);             /* Q: low/high half of source */
        o->N = (uint8_t)bit(w, 29);          /* U */
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)bits(w, 15, 12);   /* 4-bit opcode */
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        return 0;
    }

    /* ---- ASIMD two-reg misc + across lanes ----
     * bit21=1, bits11:10=10; opcode in bits20:12 */
    if ((w & 0x9F200C00u) == 0x0E200800u) {
        unsigned op9 = bits(w, 20, 12); /* 9-bit */
        o->regsize = bit(w, 30);
        o->N = (uint8_t)bit(w, 29);
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)(op9 & 0x1f);      /* bits16:12 */
        o->uimm = op9;                       /* full bits20:12 */
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        /* across-lanes: bits20:17 == 0b110x (integer) or FP with 10000 */
        unsigned top = bits(w, 20, 17);
        if (top == 0xC || top == 0xD || top == 0xE || top == 0xF ||
            (top == 0x8 && (bits(w,16,12) >= 0xC || bits(w,15,12) >= 0xC))) {
            /* Heuristic: treat bits20:19==11 as across; also FP across
             * (bits20:16==10000 with FP opcodes). Refined in exec. */
        }
        /* Across-lanes: bits20:17 == 1000 (opcode in bits16:12).
         * Two-reg misc: bits20:17 == 0000. */
        if (bits(w, 20, 17) == 0x8)
            o->op = OP_ASIMD_ACROSS;
        else
            o->op = OP_ASIMD_2MISC;
        return 0;
    }

    /* ---- ASIMD shift by immediate (immh != 0) ----
     * U Q 01111 immh:immb opcode 1 Rn Rd.  Must not steal MOVI (immh==0). */
    if ((w & 0x9F800400u) == 0x0F000400u && bits(w, 22, 19) != 0) {
        o->op = OP_ASIMD_SHIFT;
        o->regsize = bit(w, 30);
        o->N = (uint8_t)bit(w, 29);          /* U */
        o->opc = (uint8_t)bits(w, 15, 11);   /* 5-bit opcode */
        o->imm = (int32_t)bits(w, 22, 16);   /* immh:immb */
        o->size = (uint8_t)bits(w, 22, 19);  /* immh */
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        return 0;
    }

    /* ---- ASIMD by element ----
     * 0 Q U 01111 size L M Rm opcode H 0 Rn Rd  (bit10=0) */
    if ((w & 0x9F000400u) == 0x0F000000u) {
        /* shift-imm has bit10=1; by-elem has bit10=0 and uses size:L:M:H */
        o->op = OP_ASIMD_BYELEM;
        o->regsize = bit(w, 30);
        o->N = (uint8_t)bit(w, 29);
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)bits(w, 15, 12);
        o->rm = bits(w, 20, 16);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        /* index bits H:L:M scattered: H=bit11, L=bit21, M=bit20 for some sizes.
         * Pack as uimm = (H<<2)|(L<<1)|M with raw bits; exec interprets by size. */
        o->uimm = (bit(w, 11) << 2) | (bit(w, 21) << 1) | bit(w, 20);
        o->imm = (int32_t)((bit(w, 11) << 3) | (bit(w, 21) << 2) | bits(w, 20, 16)); /* H:L:Rm for lookup */
        return 0;
    }

    /* ---- LD1R..LD4R (single-structure replicate) ----
     * Q 001101 1 L 0 R 00 0 opcode size Rn Rd  / post-index variants */
    {
        uint32_t m = w & 0xBF9FF000u;
        /* LD1R no-offset: xD40C000 (size in 11:10, Q in 30) */
        /* LD1R..LD4R: bits15:13 in {110,111}, R=bit21 selects 1/2 vs 3/4 */
        if ((w & 0xBF9FF000u) == 0x0D40C000u ||
            (w & 0xBF9FF000u) == 0x0D40E000u ||
            (w & 0xBF80F000u) == 0x0D80C000u ||
            (w & 0xBF80F000u) == 0x0D80E000u) {
            /* replicate; post-index if bit23=1 */
            o->op = OP_LD1R;
            o->regsize = bit(w, 30);
            o->size = bits(w, 11, 10);
            o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
            o->opc = (uint8_t)((bit(w, 21) << 2) | bits(w, 14, 13)); /* R:opcode → nregs */
            /* opcode 110 → LD1R; 111 → LD3R; R=1 opcode 110 → LD2R; R=1 opcode 111 → LD4R */
            {
                int R = bit(w, 21);
                int opc = bits(w, 15, 13);
                int nregs = 1;
                if (opc == 6) nregs = R ? 2 : 1;
                else if (opc == 7) nregs = R ? 4 : 3;
                o->opc = (uint8_t)(nregs - 1);
            }
            o->uimm = bit(w, 23); /* post-index? */
            return 0;
        }
        /* single-structure lane LD1/ST1 {Vt}.{B|H|S|D}[index], [Xn] */
        if ((w & 0xBF000000u) == 0x0D000000u) {
            int L = bit(w, 22);
            o->op = L ? OP_LD1_LANE : OP_ST1_LANE;
            o->regsize = bit(w, 30);
            o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
            o->size = bits(w, 11, 10);
            o->opc = (uint8_t)bits(w, 15, 13); /* opcode → nregs / scale */
            o->uimm = bit(w, 23); /* post */
            o->imm = (int32_t)((bit(w, 30) << 3) | bits(w, 12, 10)); /* rough index pack; refined in exec via raw */
            o->raw = w; /* keep raw for precise index decode */
            return 0;
        }
        (void)m;
    }

    /* ---- ASIMD scalar three-same / two-reg / pairwise / shift (5e/7e) ---- */
    if ((w & 0xDF200400u) == 0x5E200400u) {
        o->op = OP_ASIMD_3SAME;
        o->regsize = 1; /* scalar uses full width of element */
        o->N = (uint8_t)bit(w, 29);
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)bits(w, 15, 10);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5); o->rm = bits(w,20,16);
        o->sf = 1; /* mark scalar */
        return 0;
    }
    if ((w & 0xDF200C00u) == 0x5E200800u) {
        /* scalar 2misc / pairwise */
        unsigned op9 = bits(w, 20, 12);
        o->N = (uint8_t)bit(w, 29);
        o->size = (uint8_t)bits(w, 23, 22);
        o->opc = (uint8_t)(op9 & 0x1f);
        o->uimm = op9;
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->regsize = 1;
        o->sf = 1;
        if (bits(w, 20, 17) == 0x8)
            o->op = OP_ASIMD_ACROSS; /* scalar pairwise / across */
        else
            o->op = OP_ASIMD_2MISC;
        return 0;
    }
    if ((w & 0xDF800400u) == 0x5F000400u && bits(w, 22, 19) != 0) {
        o->op = OP_ASIMD_SHIFT;
        o->regsize = 1; o->sf = 1;
        o->N = (uint8_t)bit(w, 29);
        o->opc = (uint8_t)bits(w, 15, 11);
        o->imm = (int32_t)bits(w, 22, 16);
        o->size = (uint8_t)bits(w, 22, 19);
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        return 0;
    }



    /* PRFM (immediate / register) — prefetch, treat as hint/nop */
    if ((w & 0xFFC00000u) == 0xF9800000u || /* unsigned offset */
        (w & 0xFFE00C00u) == 0xF8A00800u || /* reg offset */
        (w & 0xFFC00000u) == 0xF8C00000u) { /* unscaled? */
        o->op = OP_HINT;
        return 0;
    }


    /* FCCMP / FCCMPE */
    if ((w & 0xFF200C10u) == 0x1E200400u) {
        o->op = OP_FCCMP;
        o->size = bits(w, 23, 22);
        o->rd = 0; /* nzcv */
        o->rn = bits(w, 9, 5); o->rm = bits(w, 20, 16);
        o->cond = bits(w, 15, 12);
        o->opc = bits(w, 4, 0); /* nzcv imm + bit4=E */
        o->uimm = bits(w, 3, 0); /* nzcv */
        return 0;
    }


    /* ASIMD scalar copy/DUP element: mov Dd, Vn.D[i] etc. */
    if ((w & 0xFFE0FC00u) == 0x5E000400u) {
        o->op = OP_DUP;
        o->regsize = 1; o->sf = 1; /* scalar */
        o->rd = bits(w,4,0); o->rn = bits(w,9,5);
        o->uimm = bits(w, 20, 16); /* imm5 */
        o->opc = 0;
        return 0;
    }

    (void)op0;
    return -1;
}
