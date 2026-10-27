/* ASIMD encoding-group interpreter for Project JET native. */
#include "jet_internal.h"
#include <string.h>
#include <math.h>

static inline int bit(uint32_t x, int i) { return (int)((x >> i) & 1u); }
static inline uint32_t bits(uint32_t x, int hi, int lo) {
    return (x >> lo) & ((1u << (hi - lo + 1)) - 1u);
}

static inline int esize_bits(int size) { return 8 << size; }
static inline uint64_t lane_mask(int esz) {
    return (esz >= 64) ? ~0ull : ((1ull << esz) - 1ull);
}
static inline uint64_t get_lane(const uint64_t v[2], int esz, int idx) {
    int bit = idx * esz;
    if (bit >= 128) return 0;
    if (esz == 64) return v[idx & 1];
    int word = bit >> 6, off = bit & 63;
    return (v[word] >> off) & lane_mask(esz);
}
static inline void set_lane(uint64_t v[2], int esz, int idx, uint64_t val) {
    int bit = idx * esz;
    if (bit >= 128) return;
    val &= lane_mask(esz);
    if (esz == 64) { v[idx & 1] = val; return; }
    int word = bit >> 6, off = bit & 63;
    uint64_t m = lane_mask(esz) << off;
    v[word] = (v[word] & ~m) | (val << off);
}
static inline int64_t sext(uint64_t v, int esz) {
    if (esz >= 64) return (int64_t)v;
    int sh = 64 - esz;
    return ((int64_t)v << sh) >> sh;
}
static inline uint64_t sat_s(int64_t v, int esz) {
    int64_t lo = -(1ll << (esz - 1)), hi = (1ll << (esz - 1)) - 1;
    if (v < lo) v = lo; if (v > hi) v = hi;
    return (uint64_t)v & lane_mask(esz);
}
static inline uint64_t sat_u(int64_t v, int esz) {
    int64_t hi = (esz >= 63) ? (int64_t)(~0ull >> 1) : ((1ll << esz) - 1);
    if (v < 0) v = 0; if (v > hi) v = hi;
    return (uint64_t)v & lane_mask(esz);
}
static inline float f32_of(uint64_t x){ float f; uint32_t u=(uint32_t)x; memcpy(&f,&u,4); return f; }
static inline uint64_t f32_bits(float f){ uint32_t u; memcpy(&u,&f,4); return u; }
static inline double f64_of(uint64_t x){ double d; memcpy(&d,&x,8); return d; }
static inline uint64_t f64_bits(double d){ uint64_t u; memcpy(&u,&d,8); return u; }

static void do_permute(jet_cpu *c, const jet_insn *i, int which) {
    int esz = esize_bits(i->size);
    int n = ((i->regsize ? 128 : 64) / esz);
    uint64_t out[2] = {0,0};
    for (int k = 0; k < n; k++) {
        uint64_t val = 0;
        if (which <= 1) { /* ZIP1/2 */
            int src = (which == 0) ? (k >> 1) : ((n >> 1) + (k >> 1));
            val = (k & 1) ? get_lane(c->v[i->rm], esz, src)
                          : get_lane(c->v[i->rn], esz, src);
        } else if (which <= 3) { /* UZP1/2 */
            int flat = k * 2 + (which - 2);
            val = (flat < n) ? get_lane(c->v[i->rn], esz, flat)
                             : get_lane(c->v[i->rm], esz, flat - n);
        } else if (which == 4) { /* TRN1 */
            val = (k & 1) ? get_lane(c->v[i->rm], esz, k - 1)
                          : get_lane(c->v[i->rn], esz, k);
        } else { /* TRN2 */
            val = (k & 1) ? get_lane(c->v[i->rm], esz, k)
                          : get_lane(c->v[i->rn], esz, k + 1);
        }
        set_lane(out, esz, k, val);
    }
    c->v[i->rd][0] = out[0];
    c->v[i->rd][1] = i->regsize ? out[1] : 0;
}

static int exec_3same(jet_cpu *c, const jet_insn *i) {
    int U = i->N, size = i->size, Q = i->regsize;
    /* Decode packs bits15:10 (6-bit, bit0 always 1); named remaps store 5-bit opcode. */
    int opc6 = (i->opc & 1) ? i->opc : ((i->opc << 1) | 1);
    int opc = (i->opc & 1) ? (i->opc >> 1) : i->opc; /* 5-bit opcode */
    int esz = esize_bits(size);
    int n = i->sf ? 1 : ((Q ? 128 : 64) / esz);
    uint64_t out[2] = {0, 0};

    /* Whole-vector logical: opcode=0x03, size selects AND/BIC/ORR/ORN/EOR/BSL/BIT/BIF */
    if (opc == 0x03) {
        uint64_t a0=c->v[i->rn][0], a1=c->v[i->rn][1];
        uint64_t b0=c->v[i->rm][0], b1=c->v[i->rm][1];
        uint64_t d0=c->v[i->rd][0], d1=c->v[i->rd][1], r0, r1;
        if (!U) {
            if (size==0){ r0=a0&b0; r1=a1&b1; }
            else if (size==1){ r0=a0&~b0; r1=a1&~b1; }
            else if (size==2){ r0=a0|b0; r1=a1|b1; }
            else { r0=a0|~b0; r1=a1|~b1; }
        } else {
            if (size==0){ r0=a0^b0; r1=a1^b1; }
            else if (size==1){ r0=(a0&d0)|(b0&~d0); r1=(a1&d1)|(b1&~d1); } /* BSL: Vd mask */
            else if (size==2){ r0=(a0&b0)|(d0&~b0); r1=(a1&b1)|(d1&~b1); } /* BIT */
            else { r0=(d0&b0)|(a0&~b0); r1=(d1&b1)|(a1&~b1); } /* BIF */
        }
        c->v[i->rd][0]=r0; c->v[i->rd][1]=Q?r1:0; return 0;
    }

    /* Pairwise ADDP/SMAXP/UMAXP/SMINP/UMINP */
    if (opc == 0x17 || opc == 0x14 || opc == 0x15) {
        uint64_t outp[2]={0,0}; int half=n/2;
        for (int k=0;k<n;k++) {
            int src=(k<half)?i->rn:i->rm; int base=(k<half)?k:(k-half);
            uint64_t e0=get_lane(c->v[src],esz,base*2), e1=get_lane(c->v[src],esz,base*2+1), r;
            if (opc==0x17) r=(e0+e1)&lane_mask(esz);
            else if (opc==0x14) {
                if (U) r=((e0&lane_mask(esz))>(e1&lane_mask(esz)))?e0:e1;
                else r=(sext(e0,esz)>sext(e1,esz))?e0:e1;
            } else {
                if (U) r=((e0&lane_mask(esz))<(e1&lane_mask(esz)))?e0:e1;
                else r=(sext(e0,esz)<sext(e1,esz))?e0:e1;
            }
            set_lane(outp,esz,k,r&lane_mask(esz));
        }
        c->v[i->rd][0]=outp[0]; c->v[i->rd][1]=Q?outp[1]:0; return 0;
    }

    /* FP three-same (6-bit opc form >= 0x30 and selected lower 5-bit) */
    if (opc6 >= 0x30 || opc == 0x18 || opc == 0x19 || opc == 0x1B || opc == 0x1C || opc == 0x1F) {
        int fp_esz = (size & 1) ? 64 : 32;
        int fn = i->sf ? 1 : ((Q ? 128 : 64) / fp_esz);
        int sub = (size & 2) ? 1 : 0;
        int fopc = (opc6 >= 0x30) ? opc6 : opc;
        uint64_t outf[2]={0,0};
        /* FADDP vector: opc6=0x35 U=1 !sub */
        if (opc6 == 0x35 && U && !sub) {
            uint64_t outp[2]={0,0}; int half=fn/2;
            for (int k=0;k<fn;k++) {
                int src=(k<half)?i->rn:i->rm; int base=(k<half)?k:k-half;
                uint64_t e0=get_lane(c->v[src],fp_esz,base*2), e1=get_lane(c->v[src],fp_esz,base*2+1), rr;
                if (fp_esz==32) rr=f32_bits(f32_of(e0)+f32_of(e1));
                else rr=f64_bits(f64_of(e0)+f64_of(e1));
                set_lane(outp,fp_esz,k,rr);
            }
            c->v[i->rd][0]=outp[0]; c->v[i->rd][1]=Q?outp[1]:0; return 0;
        }
        for (int k=0;k<fn;k++) {
            uint64_t aa=get_lane(c->v[i->rn],fp_esz,k);
            uint64_t bb=get_lane(c->v[i->rm],fp_esz,k);
            uint64_t dd=get_lane(c->v[i->rd],fp_esz,k), rr=0;
            if (fp_esz==32) {
                float x=f32_of(aa), y=f32_of(bb), z=f32_of(dd), o=0;
                switch (fopc) {
                case 0x35: o = (U&&sub)?fabsf(x-y):(sub?(x-y):(x+y)); break;
                case 0x37: o = x*y; break;
                case 0x3F:
                    if (U) o=x/y;
                    else o=sub?((3.f-x*y)*0.5f):(2.f-x*y);
                    break;
                case 0x33: o=sub?(z-x*y):(z+x*y); break;
                case 0x3D: case 0x31: case 0x18: o=sub?fminf(x,y):fmaxf(x,y); break;
                case 0x39: case 0x1C: {
                    int cmp=!U?(x==y):(!sub?(x>=y):(x>y));
                    if (fopc==0x1C) cmp=(x==y);
                    set_lane(outf,32,k,cmp?0xffffffffull:0); continue;
                }
                case 0x1F: o=sub?((3.f-x*y)*0.5f):(2.f-x*y); break;
                case 0x1B: case 0x19: o=(fopc==0x19)?(sub?(z-x*y):(z+x*y)):(x*y); break;
                default: o=x+y; break;
                }
                rr=f32_bits(o);
            } else {
                double x=f64_of(aa), y=f64_of(bb), z=f64_of(dd), o=0;
                switch (fopc) {
                case 0x35: o=(U&&sub)?fabs(x-y):(sub?(x-y):(x+y)); break;
                case 0x37: o=x*y; break;
                case 0x3F: o=U?(x/y):(sub?((3.0-x*y)*0.5):(2.0-x*y)); break;
                case 0x33: o=sub?(z-x*y):(z+x*y); break;
                case 0x3D: case 0x31: case 0x18: o=sub?fmin(x,y):fmax(x,y); break;
                case 0x39: case 0x1C: {
                    int cmp=!U?(x==y):(!sub?(x>=y):(x>y));
                    if (fopc==0x1C) cmp=(x==y);
                    set_lane(outf,64,k,cmp?~0ull:0); continue;
                }
                case 0x1F: o=sub?((3.0-x*y)*0.5):(2.0-x*y); break;
                case 0x1B: case 0x19: o=(fopc==0x19)?(sub?(z-x*y):(z+x*y)):(x*y); break;
                default: o=x+y; break;
                }
                rr=f64_bits(o);
            }
            set_lane(outf,fp_esz,k,rr);
        }
        c->v[i->rd][0]=outf[0]; c->v[i->rd][1]=Q?outf[1]:0;
        if (i->sf){ if (fp_esz==32) c->v[i->rd][0]&=0xffffffffull; c->v[i->rd][1]=0; }
        return 0;
    }

    for (int k=0;k<n;k++) {
        uint64_t a=get_lane(c->v[i->rn],esz,k), b=get_lane(c->v[i->rm],esz,k);
        uint64_t d=get_lane(c->v[i->rd],esz,k), r=0;
        int64_t sa=sext(a,esz), sb=sext(b,esz);
        switch (opc) {
        case 0x00: /* SHADD/UHADD */
            r = U ? (((a&lane_mask(esz))+(b&lane_mask(esz)))>>1)
                  : ((uint64_t)((sa+sb)>>1)&lane_mask(esz)); break;
        case 0x01: /* SQADD/UQADD */
            r = U ? sat_u((int64_t)(a&lane_mask(esz))+(int64_t)(b&lane_mask(esz)),esz)
                  : sat_s(sa+sb,esz); break;
        case 0x02: /* SRHADD/URHADD */
            r = U ? (((a&lane_mask(esz))+(b&lane_mask(esz))+1)>>1)
                  : ((uint64_t)((sa+sb+1)>>1)&lane_mask(esz)); break;
        case 0x04: /* SHSUB/UHSUB */
            r = U ? (((a&lane_mask(esz))-(b&lane_mask(esz)))>>1)
                  : ((uint64_t)((sa-sb)>>1)&lane_mask(esz)); break;
        case 0x05: /* SQSUB/UQSUB */
            r = U ? sat_u((int64_t)(a&lane_mask(esz))-(int64_t)(b&lane_mask(esz)),esz)
                  : sat_s(sa-sb,esz); break;
        case 0x06: /* CMGT/CMHI */
            r = U ? (((a&lane_mask(esz))>(b&lane_mask(esz)))?lane_mask(esz):0)
                  : ((sa>sb)?lane_mask(esz):0); break;
        case 0x07: /* CMGE/CMHS — also logical (handled) */
            r = U ? (((a&lane_mask(esz))>=(b&lane_mask(esz)))?lane_mask(esz):0)
                  : ((sa>=sb)?lane_mask(esz):0); break;
        case 0x08: { /* SSHL/USHL */
            int sh=(int)(int8_t)(b&0xff);
            if (sh>=esz) r=0;
            else if (sh>=0) r=U?((a<<sh)&lane_mask(esz)):((uint64_t)(sa<<sh)&lane_mask(esz));
            else { int rs=-sh; r=U?((rs>=esz)?0:((a&lane_mask(esz))>>rs))
                                  :((uint64_t)(sa>>(rs>=esz?esz-1:rs))&lane_mask(esz)); }
            break; }
        case 0x09: { /* SQSHL/UQSHL */
            int sh=(int)(int8_t)(b&0xff);
            if (sh<0){ int rs=-sh; r=U?((rs>=esz)?0:((a&lane_mask(esz))>>rs))
                                      :((uint64_t)(sa>>(rs>=esz?esz-1:rs))&lane_mask(esz)); }
            else r=U?sat_u((int64_t)((a&lane_mask(esz))<<sh),esz):sat_s(sa<<sh,esz);
            break; }
        case 0x0A: case 0x0B: { /* SRSHL/URSHL / SQRSHL/UQRSHL */
            int sh=(int)(int8_t)(b&0xff);
            if (sh>=0) r=U?sat_u((int64_t)((a&lane_mask(esz))<<sh),esz):sat_s(sa<<sh,esz);
            else { int rs=-sh; if(rs>=esz) r=0;
                else if(U) r=(((a&lane_mask(esz))+(1ull<<(rs-1)))>>rs)&lane_mask(esz);
                else r=((uint64_t)((sa+(1ll<<(rs-1)))>>rs))&lane_mask(esz); }
            break; }
        case 0x0C: /* SMAX/UMAX */
            r = U ? (((a&lane_mask(esz))>(b&lane_mask(esz)))?a:b)
                  : ((sa>sb)?a:b); r&=lane_mask(esz); break;
        case 0x0D: /* SMIN/UMIN */
            r = U ? (((a&lane_mask(esz))<(b&lane_mask(esz)))?a:b)
                  : ((sa<sb)?a:b); r&=lane_mask(esz); break;
        case 0x0E: { /* SABD/UABD */
            if (U){ uint64_t aa=a&lane_mask(esz),bb=b&lane_mask(esz); r=aa>bb?aa-bb:bb-aa; }
            else { int64_t dlt=sa-sb; if(dlt<0)dlt=-dlt; r=(uint64_t)dlt&lane_mask(esz); }
            break; }
        case 0x0F: { /* SABA/UABA */
            uint64_t abd;
            if (U){ uint64_t aa=a&lane_mask(esz),bb=b&lane_mask(esz); abd=aa>bb?aa-bb:bb-aa; }
            else { int64_t dlt=sa-sb; if(dlt<0)dlt=-dlt; abd=(uint64_t)dlt; }
            r=(d+abd)&lane_mask(esz); break; }
        case 0x10: /* ADD / SUB(U=1) */
            r = U ? ((a-b)&lane_mask(esz)) : ((a+b)&lane_mask(esz)); break;
        case 0x11: /* CMTST / CMEQ(U=1) */
            r = U ? (((a&lane_mask(esz))==(b&lane_mask(esz)))?lane_mask(esz):0)
                  : ((((a&b)&lane_mask(esz))!=0)?lane_mask(esz):0); break;
        case 0x12: { /* MLA / MLS(U=1) */
            uint64_t prod=(a*b)&lane_mask(esz);
            r = U ? ((d-prod)&lane_mask(esz)) : ((d+prod)&lane_mask(esz)); break; }
        case 0x13: /* MUL / PMUL */
            r = (a*b)&lane_mask(esz); break;
        default: r = (a+b)&lane_mask(esz); break;
        }
        set_lane(out,esz,k,r);
    }
    c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0;
    if (i->sf) c->v[i->rd][1]=0;
    return 0;
}

static int exec_3diff(jet_cpu *c, const jet_insn *i) {
    int U=i->N, size=i->size, opc=i->opc, Q=i->regsize;
    int esz=esize_bits(size), desz=esz*2, n_src=64/esz, high=Q;
    uint64_t out[2]={0,0};
    int is_narrow=(opc==0x4||opc==0x6), is_wide=(opc==0x1||opc==0x3);

    if (is_narrow) {
        int sesz=esz*2, n=64/esz;
        uint64_t dst[2]={Q?c->v[i->rd][0]:0, Q?c->v[i->rd][1]:0};
        for (int k=0;k<n;k++) {
            uint64_t a=get_lane(c->v[i->rn],sesz,k), b=get_lane(c->v[i->rm],sesz,k);
            uint64_t sum = (opc==0x4) ? (a+b) : (a-b);
            if (U) sum += (1ull<<(esz-1));
            set_lane(dst, esz, Q?k+n:k, (sum>>esz)&lane_mask(esz));
        }
        c->v[i->rd][0]=dst[0]; c->v[i->rd][1]=Q?dst[1]:0; return 0;
    }
    for (int k=0;k<n_src;k++) {
        int sk=high?k+n_src:k;
        uint64_t a,b,d=get_lane(c->v[i->rd],desz,k), r=0;
        if (is_wide) {
            a=get_lane(c->v[i->rn],desz,k);
            b=get_lane(c->v[i->rm],esz,sk);
            b = U ? (b&lane_mask(esz)) : (uint64_t)sext(b,esz);
        } else {
            a=get_lane(c->v[i->rn],esz,sk); b=get_lane(c->v[i->rm],esz,sk);
            if (U){ a&=lane_mask(esz); b&=lane_mask(esz); }
            else { a=(uint64_t)sext(a,esz); b=(uint64_t)sext(b,esz); }
        }
        switch (opc) {
        case 0x0: case 0x1: r=a+b; break;
        case 0x2: case 0x3: r=a-b; break;
        case 0x5: { int64_t aa=(int64_t)a,bb=(int64_t)b,abd=aa>=bb?aa-bb:bb-aa; r=d+(uint64_t)abd; break; }
        case 0x7: { int64_t aa=(int64_t)a,bb=(int64_t)b; r=(uint64_t)(aa>=bb?aa-bb:bb-aa); break; }
        case 0x8: r=d+a*b; break;
        case 0x9: r=d+(uint64_t)sat_s((int64_t)(a*b*2),desz); break;
        case 0xA: r=d-a*b; break;
        case 0xB: r=d-(uint64_t)sat_s((int64_t)(a*b*2),desz); break;
        case 0xC: case 0xE: r=a*b; break;
        case 0xD: r=(uint64_t)sat_s((int64_t)(a*b*2),desz); break;
        default: r=a*b; break;
        }
        set_lane(out,desz,k,r&lane_mask(desz));
    }
    c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; return 0;
}

static int popcount8(uint8_t x){
    x=x-((x>>1)&0x55); x=(x&0x33)+((x>>2)&0x33); return (x+(x>>4))&0x0f;
}

static int exec_2misc(jet_cpu *c, const jet_insn *i) {
    int U=i->N, size=i->size, Q=i->regsize;
    unsigned op9=i->uimm, opc=op9&0x1f;
    int esz=esize_bits(size);
    int n=i->sf?1:((Q?128:64)/esz);
    uint64_t out[2]={0,0};

    if (opc==0x05 && size==0) { /* CNT / MVN */
        if (U) { c->v[i->rd][0]=~c->v[i->rn][0]; c->v[i->rd][1]=Q?~c->v[i->rn][1]:0; }
        else {
            uint8_t s[16], d[16]={0}; memcpy(s,&c->v[i->rn][0],16);
            int nb=Q?16:8; for(int k=0;k<nb;k++) d[k]=(uint8_t)popcount8(s[k]);
            memcpy(&c->v[i->rd][0],d,8); c->v[i->rd][1]=Q?*(uint64_t*)(d+8):0;
        }
        return 0;
    }
    /* Pairwise long */
    if ((opc==0x08||opc==0x0A) && (op9>>5)!=0) {
        int src_esz=esz, dst_esz=esz*2, sn=(Q?128:64)/src_esz, dn=sn/2;
        uint64_t outp[2]={0,0};
        if (opc==0x0A){ outp[0]=c->v[i->rd][0]; outp[1]=c->v[i->rd][1]; }
        for (int k=0;k<dn;k++) {
            uint64_t e0=get_lane(c->v[i->rn],src_esz,k*2), e1=get_lane(c->v[i->rn],src_esz,k*2+1);
            int64_t s0=U?(int64_t)(e0&lane_mask(src_esz)):sext(e0,src_esz);
            int64_t s1=U?(int64_t)(e1&lane_mask(src_esz)):sext(e1,src_esz);
            uint64_t r=(uint64_t)(s0+s1);
            if (opc==0x0A) r+=get_lane(outp,dst_esz,k);
            set_lane(outp,dst_esz,k,r&lane_mask(dst_esz));
        }
        c->v[i->rd][0]=outp[0]; c->v[i->rd][1]=Q?outp[1]:0; return 0;
    }
    /* XTN / SQXTUN / SQXTN / UQXTN */
    if (opc==0x12 || opc==0x14) {
        int dst_esz=esz, src_esz=esz*2, n_dst=64/dst_esz;
        uint64_t outn[2]={Q?c->v[i->rd][0]:0, Q?c->v[i->rd][1]:0};
        for (int k=0;k<n_dst;k++) {
            uint64_t a=get_lane(c->v[i->rn],src_esz,k), r;
            if (opc==0x12 && !U) r=a&lane_mask(dst_esz);
            else if (opc==0x12 && U) r=sat_u(sext(a,src_esz),dst_esz);
            else if (!U) r=sat_s(sext(a,src_esz),dst_esz);
            else r=sat_u((int64_t)(a&lane_mask(src_esz)),dst_esz);
            set_lane(outn,dst_esz,Q?k+n_dst:k,r);
        }
        c->v[i->rd][0]=outn[0]; c->v[i->rd][1]=outn[1]; if(!Q)c->v[i->rd][1]=0; return 0;
    }
    /* SHLL: opc=0x13 size selects */
    if (opc==0x13) {
        int src_esz=esz, dst_esz=esz*2, nsrc=64/src_esz, high=Q, sh=src_esz;
        for (int k=0;k<nsrc;k++) {
            uint64_t a=get_lane(c->v[i->rn],src_esz,high?k+nsrc:k);
            set_lane(out,dst_esz,k,((a&lane_mask(src_esz))<<sh)&lane_mask(dst_esz));
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; return 0;
    }

    /* FP conversions / frint / abs / neg / sqrt / compares #0 */
    if (opc >= 0x0C) {
        /* FCVTN / FCVTL */
        if (opc==0x16 || opc==0x17) {
            if (opc==0x17) { /* FCVTL */
                int src_esz=(size&1)?32:16, dst_esz=src_esz*2, sn=64/src_esz, high=Q;
                for (int k=0;k<sn;k++) {
                    uint64_t a=get_lane(c->v[i->rn],src_esz,high?k+sn:k);
                    if (src_esz==32) set_lane(out,64,k,f64_bits((double)f32_of(a)));
                    else {
                        uint16_t h=(uint16_t)a; int sign=(h>>15)&1, exp=(h>>10)&0x1f, mant=h&0x3ff;
                        float f; if(exp==0) f=ldexpf((float)mant,-24);
                        else if(exp==31) f=mant?NAN:INFINITY;
                        else f=ldexpf((float)(mant|0x400),exp-25);
                        if(sign) f=-f; set_lane(out,32,k,f32_bits(f));
                    }
                }
                c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; return 0;
            }
            /* FCVTN */
            int dst_esz=(size&1)?32:16, src_esz=dst_esz*2, dn=64/dst_esz;
            uint64_t outf[2]={Q?c->v[i->rd][0]:0, Q?c->v[i->rd][1]:0};
            for (int k=0;k<dn;k++) {
                uint64_t a=get_lane(c->v[i->rn],src_esz,k), r;
                if (src_esz==64) r=f32_bits((float)f64_of(a));
                else {
                    uint32_t u=(uint32_t)f32_bits(f32_of(a));
                    int sign=(u>>31)&1, exp=((u>>23)&0xff)-127+15, mant=(u>>13)&0x3ff;
                    uint16_t h; if(exp<=0) h=(uint16_t)(sign<<15);
                    else if(exp>=31) h=(uint16_t)((sign<<15)|(0x1f<<10));
                    else h=(uint16_t)((sign<<15)|(exp<<10)|mant); r=h;
                }
                set_lane(outf,dst_esz,Q?k+dn:k,r);
            }
            c->v[i->rd][0]=outf[0]; c->v[i->rd][1]=outf[1]; if(!Q)c->v[i->rd][1]=0; return 0;
        }
        int fp_esz=(size&1)?64:32, fn=i->sf?1:((Q?128:64)/fp_esz);
        uint64_t outf[2]={0,0};
        for (int k=0;k<fn;k++) {
            uint64_t a=get_lane(c->v[i->rn],fp_esz,k), r=0;
            if (fp_esz==32) {
                float x=f32_of(a), o=x;
                switch (opc) {
                case 0x0C: r=(!U?(x>0):(x>=0))?0xffffffffull:0; set_lane(outf,32,k,r); continue;
                case 0x0D: r=(!U?(x==0):(x<=0))?0xffffffffull:0; set_lane(outf,32,k,r); continue;
                case 0x0E: r=(x<0)?0xffffffffull:0; set_lane(outf,32,k,r); continue;
                case 0x0F: o=U?-x:fabsf(x); break;
                case 0x18: o=roundf(x); break;
                case 0x19: o=U?x:floorf(x); break;
                case 0x1A: { int32_t v=(int32_t)llroundf(x); set_lane(outf,32,k,(uint32_t)v); continue; }
                case 0x1B: {
                    int32_t v=(size&2)?(U?(int32_t)(x>=0?x:0):(int32_t)x)
                                      :(U?(int32_t)(x>=0?floorf(x):0):(int32_t)floorf(x));
                    set_lane(outf,32,k,(uint32_t)v); continue;
                }
                case 0x1C: o=(size&2)?(U?1.f/sqrtf(x):1.f/x):x; if(!(size&2)){ set_lane(outf,32,k,(uint32_t)(int32_t)llroundf(x)); continue; } break;
                case 0x1D:
                    if (size&2) o=U?1.f/sqrtf(x):1.f/x;
                    else o=U?(float)(uint32_t)a:(float)(int32_t)a;
                    break;
                case 0x1F: o=sqrtf(x); break;
                default: break;
                }
                r=f32_bits(o);
            } else {
                double x=f64_of(a), o=x;
                switch (opc) {
                case 0x0C: r=(!U?(x>0):(x>=0))?~0ull:0; set_lane(outf,64,k,r); continue;
                case 0x0D: r=(!U?(x==0):(x<=0))?~0ull:0; set_lane(outf,64,k,r); continue;
                case 0x0E: r=(x<0)?~0ull:0; set_lane(outf,64,k,r); continue;
                case 0x0F: o=U?-x:fabs(x); break;
                case 0x18: o=round(x); break;
                case 0x19: o=U?x:floor(x); break;
                case 0x1A: { set_lane(outf,64,k,(uint64_t)(int64_t)llround(x)); continue; }
                case 0x1B: {
                    int64_t v=(size&2)?(U?(int64_t)(x>=0?x:0):(int64_t)x)
                                      :(U?(int64_t)(x>=0?floor(x):0):(int64_t)floor(x));
                    set_lane(outf,64,k,(uint64_t)v); continue;
                }
                case 0x1D: o=(size&2)?(U?1.0/sqrt(x):1.0/x):(U?(double)(uint64_t)a:(double)(int64_t)a); break;
                case 0x1F: o=sqrt(x); break;
                default: break;
                }
                r=f64_bits(o);
            }
            set_lane(outf,fp_esz,k,r);
        }
        c->v[i->rd][0]=outf[0]; c->v[i->rd][1]=Q?outf[1]:0; return 0;
    }

    for (int k=0;k<n;k++) {
        uint64_t a=get_lane(c->v[i->rn],esz,k); int64_t sa=sext(a,esz); uint64_t r=0;
        switch (opc) {
        case 0x00: { /* REV64 */
            int elems=64/esz, base=(k*esz)/64*(64/esz), off=k-base;
            r=get_lane(c->v[i->rn],esz,base+(elems-1-off)); break; }
        case 0x01: { int elems=16/esz, base=(k*esz)/16*elems, off=k-base;
            r=get_lane(c->v[i->rn],esz,base+(elems-1-off)); break; }
        case 0x02: { int elems=32/esz, base=(k*esz)/32*elems, off=k-base;
            r=get_lane(c->v[i->rn],esz,base+(elems-1-off)); break; }
        case 0x04: { /* CLS/CLZ */
            uint64_t x=a&lane_mask(esz); int cnt=0;
            if (U){ if(!x) cnt=esz; else for(int b=esz-1;b>=0;b--){ if(x&(1ull<<b))break; cnt++; } }
            else { int sign=(int)((x>>(esz-1))&1); for(int b=esz-2;b>=0;b--){ if(((x>>b)&1)!=(unsigned)sign)break; cnt++; } }
            r=(uint64_t)cnt; break; }
        case 0x07: r = U ? sat_s(-sa,esz) : sat_s(sa<0?-sa:sa,esz); break; /* SQNEG/SQABS */
        case 0x0B: r = U ? ((uint64_t)(-sa)&lane_mask(esz)) : ((uint64_t)(sa<0?-sa:sa)&lane_mask(esz)); break;
        case 0x08: /* CMGT#0 / CMGE#0 */
            if ((op9>>5)==0) r = U ? ((sa>=0)?lane_mask(esz):0) : ((sa>0)?lane_mask(esz):0);
            break;
        case 0x09: /* CMEQ#0 / CMLE#0 */
            r = U ? ((sa<=0)?lane_mask(esz):0) : ((sa==0)?lane_mask(esz):0); break;
        case 0x0A: /* CMLT#0 */
            if ((op9>>5)==0) r = (sa<0)?lane_mask(esz):0; break;
        default: r=a; break;
        }
        set_lane(out,esz,k,r);
    }
    c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0; return 0;
}

static int exec_across(jet_cpu *c, const jet_insn *i) {
    int U=i->N, size=i->size, Q=i->regsize; unsigned opc=i->opc;
    int esz=esize_bits(size); int n=i->sf?2:((Q?128:64)/esz);

    if (opc==0x03) { /* SADDLV/UADDLV */
        int64_t s=0;
        for (int k=0;k<n;k++) {
            uint64_t a=get_lane(c->v[i->rn],esz,k);
            s += U?(int64_t)(a&lane_mask(esz)):sext(a,esz);
        }
        c->v[i->rd][0]=(uint64_t)s; c->v[i->rd][1]=0; return 0;
    }
    if (opc==0x0A || opc==0x1A || opc==0x1B) {
        uint64_t a0=get_lane(c->v[i->rn],esz,0), acc;
        int64_t best=U?(int64_t)(a0&lane_mask(esz)):sext(a0,esz);
        uint64_t bestu=a0&lane_mask(esz);
        if (opc==0x1B) {
            if (i->sf) { /* scalar ADDP */
                acc=(get_lane(c->v[i->rn],esz,0)+get_lane(c->v[i->rn],esz,1))&lane_mask(esz);
            } else {
                uint64_t sum=0; for(int k=0;k<n;k++) sum+=get_lane(c->v[i->rn],esz,k)&lane_mask(esz);
                acc=sum&lane_mask(esz);
            }
        } else {
            for (int k=1;k<n;k++) {
                uint64_t a=get_lane(c->v[i->rn],esz,k);
                if (opc==0x0A) {
                    if (U){ if((a&lane_mask(esz))>bestu) bestu=a&lane_mask(esz); }
                    else { int64_t s=sext(a,esz); if(s>best) best=s; }
                } else {
                    if (U){ if((a&lane_mask(esz))<bestu) bestu=a&lane_mask(esz); }
                    else { int64_t s=sext(a,esz); if(s<best) best=s; }
                }
            }
            acc = U ? bestu : ((uint64_t)best & lane_mask(esz));
        }
        c->v[i->rd][0]=acc; c->v[i->rd][1]=0; return 0;
    }
    /* FP across / scalar FADDP */
    if (opc==0x0C || opc==0x0F || (i->sf && (opc==0x1D || opc==0x0D))) {
        int fp_esz=(size&1)?64:32;
        if (i->sf && (opc==0x1D || opc==0x0D)) {
            if (fp_esz==32) {
                float a=f32_of(get_lane(c->v[i->rn],32,0)), b=f32_of(get_lane(c->v[i->rn],32,1));
                c->v[i->rd][0]=f32_bits(a+b);
            } else {
                double a=f64_of(get_lane(c->v[i->rn],64,0)), b=f64_of(get_lane(c->v[i->rn],64,1));
                c->v[i->rd][0]=f64_bits(a+b);
            }
            c->v[i->rd][1]=0; return 0;
        }
        int fn=(Q?128:64)/fp_esz, is_min=(size&2)?1:0;
        if (fp_esz==32) {
            float best=f32_of(get_lane(c->v[i->rn],32,0));
            for(int k=1;k<fn;k++){ float x=f32_of(get_lane(c->v[i->rn],32,k)); best=is_min?fminf(best,x):fmaxf(best,x); }
            c->v[i->rd][0]=f32_bits(best);
        } else {
            double best=f64_of(get_lane(c->v[i->rn],64,0));
            for(int k=1;k<fn;k++){ double x=f64_of(get_lane(c->v[i->rn],64,k)); best=is_min?fmin(best,x):fmax(best,x); }
            c->v[i->rd][0]=f64_bits(best);
        }
        c->v[i->rd][1]=0; return 0;
    }
    return 0;
}

static int exec_shift(jet_cpu *c, const jet_insn *i) {
    int U=i->N, opc=i->opc, Q=i->regsize;
    unsigned immhb=(unsigned)i->imm, immh=(immhb>>3)&0xf;
    int esz;
    if (immh&8) esz=64; else if (immh&4) esz=32; else if (immh&2) esz=16;
    else if (immh&1) esz=8; else return 0;
    int n=i->sf?1:(((Q?16:8)*8)/esz);
    uint64_t out[2]={0,0};

    if (opc==0x14) { /* USHLL/SSHLL */
        int src_esz=esz, dst_esz=esz*2, nsrc=64/src_esz, high=Q;
        int shift=(int)immhb-esz; if(shift<0) shift=0;
        for (int k=0;k<nsrc;k++) {
            uint64_t a=get_lane(c->v[i->rn],src_esz,high?k+nsrc:k), r;
            if (U) r=((a&lane_mask(src_esz))<<shift)&lane_mask(dst_esz);
            else r=((uint64_t)sext(a,src_esz)<<shift)&lane_mask(dst_esz);
            set_lane(out,dst_esz,k,r);
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; return 0;
    }
    if (opc>=0x10 && opc<=0x13) { /* SHRN family: esz from immh is DEST size */
        int dst_esz=esz, src_esz=esz*2;
        int shift=2*dst_esz-(int)immhb; if(shift<0)shift=0; if(shift>src_esz)shift=src_esz;
        int nd=64/dst_esz, round=(opc&1);
        out[0]=Q?c->v[i->rd][0]:0; out[1]=Q?c->v[i->rd][1]:0;
        for (int k=0;k<nd;k++) {
            uint64_t a=get_lane(c->v[i->rn],src_esz,k), r;
            if (U && opc<=0x11) { /* SQSHRUN/SQRSHRUN */
                int64_t s=sext(a,src_esz); if(round&&shift>0) s+=1ll<<(shift-1); s>>=shift;
                r=sat_u(s,dst_esz);
            } else if (!U && opc>=0x12) {
                int64_t s=sext(a,src_esz); if(round&&shift>0) s+=1ll<<(shift-1); s>>=shift;
                r=sat_s(s,dst_esz);
            } else if (U && opc>=0x12) {
                uint64_t s=a&lane_mask(src_esz); if(round&&shift>0) s+=1ull<<(shift-1); s>>=shift;
                r=sat_u((int64_t)s,dst_esz);
            } else {
                uint64_t v=a&lane_mask(src_esz); if(round&&shift>0) v+=1ull<<(shift-1);
                r=(v>>shift)&lane_mask(dst_esz);
            }
            set_lane(out,dst_esz,Q?k+nd:k,r);
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; if(!Q)c->v[i->rd][1]=0; return 0;
    }
    int shift;
    if (opc<=0x03 || opc==0x08) shift=2*esz-(int)immhb;
    else if (opc==0x0A||opc==0x0C||opc==0x0E) shift=(int)immhb-esz;
    else shift=2*esz-(int)immhb;
    if (shift<0) shift=0;
    for (int k=0;k<n;k++) {
        uint64_t a=get_lane(c->v[i->rn],esz,k), d=get_lane(c->v[i->rd],esz,k), r;
        int sh=shift; if(sh>esz) sh=esz;
        switch (opc) {
        case 0x00: r=U?((sh>=esz)?0:((a&lane_mask(esz))>>sh)):((uint64_t)(sext(a,esz)>>sh)&lane_mask(esz)); break;
        case 0x01: {
            uint64_t add=U?((sh>=esz)?0:((a&lane_mask(esz))>>sh)):((uint64_t)(sext(a,esz)>>sh));
            r=(d+add)&lane_mask(esz); break; }
        case 0x02: case 0x03: {
            if (U){ uint64_t v=a&lane_mask(esz); if(sh>0&&sh<=esz)v+=1ull<<(sh-1); r=(sh>=esz)?0:(v>>sh); }
            else { int64_t s=sext(a,esz); if(sh>0)s+=1ll<<(sh-1); r=(uint64_t)(s>>sh)&lane_mask(esz); }
            if (opc==0x03) r=(d+r)&lane_mask(esz); break; }
        case 0x08: { /* SRI */
            uint64_t v=(sh>=esz)?0:((a&lane_mask(esz))>>sh);
            uint64_t mask=(sh>=esz)?0:(lane_mask(esz)>>sh);
            r=(d&~mask)|v; break; }
        case 0x0A: /* SHL/SLI */
            if (U){ uint64_t v=(a<<sh)&lane_mask(esz); uint64_t mask=(sh>=esz)?lane_mask(esz):((lane_mask(esz)<<sh)&lane_mask(esz)); r=(d&~mask)|v; }
            else r=(a<<sh)&lane_mask(esz); break;
        case 0x0C: r=sat_u((int64_t)(sext(a,esz)<<sh),esz); break;
        case 0x0E: r=U?sat_u((int64_t)((a&lane_mask(esz))<<sh),esz):sat_s(sext(a,esz)<<sh,esz); break;
        default: r=(a<<sh)&lane_mask(esz); break;
        }
        set_lane(out,esz,k,r);
    }
    c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0; return 0;
}

static int exec_byelem(jet_cpu *c, const jet_insn *i) {
    int U=i->N, size=i->size, opc=i->opc, Q=i->regsize;
    int esz=esize_bits(size); uint32_t w=i->raw; int idx, rm;
    if (size==2){ idx=(bit(w,11)<<1)|bit(w,21); rm=bits(w,19,16); }
    else if (size==1){ idx=(bit(w,11)<<2)|(bit(w,21)<<1)|bit(w,20); rm=bits(w,18,16); }
    else { idx=bit(w,11); rm=bits(w,19,16); if(size==0){/*D via sz*/} }
    /* Long by-element */
    if (opc==0x2||opc==0xA||opc==0x6||opc==0xE) {
        int src_esz=esz, dst_esz=esz*2, nsrc=64/src_esz, high=Q;
        uint64_t elem=get_lane(c->v[rm],src_esz,idx), out[2]={0,0};
        for (int k=0;k<nsrc;k++) {
            uint64_t a=get_lane(c->v[i->rn],src_esz,high?k+nsrc:k);
            uint64_t d=get_lane(c->v[i->rd],dst_esz,k);
            uint64_t aa=U?(a&lane_mask(src_esz)):(uint64_t)sext(a,src_esz);
            uint64_t bb=U?(elem&lane_mask(src_esz)):(uint64_t)sext(elem,src_esz), r;
            if (opc==0x2) r=d+aa*bb; else if (opc==0x6) r=d-aa*bb; else r=aa*bb;
            set_lane(out,dst_esz,k,r&lane_mask(dst_esz));
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=out[1]; return 0;
    }
    /* FP by-element */
    if (opc==0x1||opc==0x5||opc==0x9) {
        int fp_esz=esz, fn=Q?(128/fp_esz):(64/fp_esz);
        uint64_t elem=get_lane(c->v[rm],fp_esz,idx), out[2]={0,0};
        for (int k=0;k<fn;k++) {
            uint64_t a=get_lane(c->v[i->rn],fp_esz,k), d=get_lane(c->v[i->rd],fp_esz,k), r;
            if (fp_esz==32) {
                float x=f32_of(a),y=f32_of(elem),z=f32_of(d),o;
                o=(opc==0x1)?(z+x*y):((opc==0x5)?(z-x*y):(x*y)); r=f32_bits(o);
            } else {
                double x=f64_of(a),y=f64_of(elem),z=f64_of(d),o;
                o=(opc==0x1)?(z+x*y):((opc==0x5)?(z-x*y):(x*y)); r=f64_bits(o);
            }
            set_lane(out,fp_esz,k,r);
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0; return 0;
    }
    int n=Q?(128/esz):(64/esz);
    uint64_t elem=get_lane(c->v[rm],esz,idx), out[2]={0,0};
    for (int k=0;k<n;k++) {
        uint64_t a=get_lane(c->v[i->rn],esz,k), d=get_lane(c->v[i->rd],esz,k);
        uint64_t prod=(a*elem)&lane_mask(esz), r;
        switch (opc) {
        case 0x0: r=(d+prod)&lane_mask(esz); break;
        case 0x4: r=(d-prod)&lane_mask(esz); break;
        case 0x8: r=prod; break;
        case 0xC: r=sat_s((sext(a,esz)*sext(elem,esz))>>(esz-1),esz); break;
        case 0xD: r=sat_s((sext(a,esz)*sext(elem,esz)+(1ll<<(esz-2)))>>(esz-1),esz); break;
        default: r=prod; break;
        }
        set_lane(out,esz,k,r);
    }
    c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0; return 0;
}

static uint64_t ldN(uint64_t addr, int nbytes) {
    if (nbytes==1) return ld1(addr);
    if (nbytes==2) return ld2(addr);
    if (nbytes==4) return ld4(addr);
    return ld8(addr);
}
static void stN(uint64_t addr, uint64_t val, int nbytes) {
    if (nbytes==1) st1(addr,(uint8_t)val);
    else if (nbytes==2) st2(addr,(uint16_t)val);
    else if (nbytes==4) st4(addr,(uint32_t)val);
    else st8(addr,val);
}

static int exec_ld1r(jet_cpu *c, const jet_insn *i) {
    int esz=esize_bits(i->size), nregs=(int)i->opc+1, Q=i->regsize;
    int n=(Q?128:64)/esz, nbytes=esz/8;
    uint64_t addr=(i->rn==31)?c->sp:c->x[i->rn];
    for (int r=0;r<nregs;r++) {
        uint64_t val=ldN(addr+(uint64_t)r*nbytes, nbytes), out[2]={0,0};
        for (int k=0;k<n;k++) set_lane(out,esz,k,val);
        int vd=(i->rd+r)&31; c->v[vd][0]=out[0]; c->v[vd][1]=Q?out[1]:0;
    }
    if (i->uimm) {
        int inc=nbytes*nregs;
        if (i->rn==31){ if(i->rm==31) c->sp+=inc; else c->sp+=c->x[i->rm]; }
        else { if(i->rm==31) c->x[i->rn]+=inc; else c->x[i->rn]+=c->x[i->rm]; }
    }
    return 0;
}

static int exec_lane_ldst(jet_cpu *c, const jet_insn *i, int is_load) {
    uint32_t w=i->raw; int esz=8, idx=0, nregs=1;
    int opc=bits(w,15,13);
    if (opc==0x4) {
        if (bits(w,11,10)==0){ esz=32; idx=(bit(w,30)<<1)|bit(w,12); }
        else if (bits(w,11,10)==1 && bit(w,12)==0){ esz=64; idx=bit(w,30); }
    } else if (opc==0x2){ esz=16; idx=(bit(w,30)<<2)|(bit(w,12)<<1)|bit(w,11); }
    else if (opc==0x0){ esz=8; idx=(bit(w,30)<<3)|(bit(w,12)<<2)|bits(w,11,10); }
    else { esz=8<<((int)i->size); idx=0; }
    int nbytes=esz/8;
    uint64_t addr=(i->rn==31)?c->sp:c->x[i->rn];
    for (int r=0;r<nregs;r++) {
        int vd=(i->rd+r)&31;
        if (is_load) set_lane(c->v[vd],esz,idx,ldN(addr+(uint64_t)r*nbytes,nbytes));
        else stN(addr+(uint64_t)r*nbytes, get_lane(c->v[vd],esz,idx), nbytes);
    }
    if (i->uimm) {
        int inc=nbytes*nregs;
        if (i->rn==31){ if(i->rm==31) c->sp+=inc; else c->sp+=c->x[i->rm]; }
        else { if(i->rm==31) c->x[i->rn]+=inc; else c->x[i->rn]+=c->x[i->rm]; }
    }
    return 0;
}

int jet_asimd_exec(jet_cpu *c, jet_ctx *ctx, const jet_insn *i) {
    (void)ctx;
    switch (i->op) {
    case OP_ZIP1: do_permute(c,i,0); return 0;
    case OP_ZIP2: do_permute(c,i,1); return 0;
    case OP_UZP1: do_permute(c,i,2); return 0;
    case OP_UZP2: do_permute(c,i,3); return 0;
    case OP_TRN1: do_permute(c,i,4); return 0;
    case OP_TRN2: do_permute(c,i,5); return 0;
    case OP_ASIMD_3SAME: return exec_3same(c,i);
    case OP_ASIMD_3DIFF: return exec_3diff(c,i);
    case OP_ASIMD_2MISC: return exec_2misc(c,i);
    case OP_ASIMD_ACROSS: return exec_across(c,i);
    case OP_ASIMD_SHIFT: return exec_shift(c,i);
    case OP_ASIMD_BYELEM: return exec_byelem(c,i);
    case OP_LD1R: return exec_ld1r(c,i);
    case OP_LD1_LANE: return exec_lane_ldst(c,i,1);
    case OP_ST1_LANE: return exec_lane_ldst(c,i,0);
    case OP_SSHLL: case OP_USHLL: {
        jet_insn t=*i; t.op=OP_ASIMD_SHIFT; t.opc=0x14; t.N=(i->op==OP_USHLL); return exec_shift(c,&t);
    }
    case OP_XTN: {
        jet_insn t=*i; t.op=OP_ASIMD_2MISC; t.opc=0x12; t.N=0; t.uimm=0x12; return exec_2misc(c,&t);
    }
    case OP_CNT: {
        jet_insn t=*i; t.op=OP_ASIMD_2MISC; t.opc=0x05; t.N=0; t.size=0; t.uimm=0x05; return exec_2misc(c,&t);
    }
    case OP_ADDV: case OP_UADDLV: {
        jet_insn t=*i; t.op=OP_ASIMD_ACROSS; t.opc=(i->op==OP_ADDV)?0x1B:0x03;
        t.N=(i->op==OP_UADDLV); return exec_across(c,&t);
    }
    case OP_ADDP_V: {
        jet_insn t=*i; t.op=OP_ASIMD_3SAME; t.opc=0x17; t.N=0; return exec_3same(c,&t);
    }
    case OP_MUL_V: {
        jet_insn t=*i; t.op=OP_ASIMD_3SAME; t.opc=0x13; t.N=0; return exec_3same(c,&t);
    }
    case OP_CMEQ: case OP_CMGT: case OP_CMGE: case OP_CMHI: case OP_CMHS: case OP_CMTST: {
        jet_insn t=*i; t.op=OP_ASIMD_3SAME;
        if(i->op==OP_CMEQ){t.opc=0x11;t.N=1;}
        else if(i->op==OP_CMTST){t.opc=0x11;t.N=0;}
        else if(i->op==OP_CMGT){t.opc=0x06;t.N=0;}
        else if(i->op==OP_CMHI){t.opc=0x06;t.N=1;}
        else if(i->op==OP_CMGE){t.opc=0x07;t.N=0;}
        else {t.opc=0x07;t.N=1;}
        return exec_3same(c,&t);
    }
    case OP_REV64_V: case OP_REV32_V: case OP_REV16_V: {
        jet_insn t=*i; t.op=OP_ASIMD_2MISC;
        t.opc=(i->op==OP_REV16_V)?0x01:((i->op==OP_REV32_V)?0x02:0x00);
        t.N=(i->op==OP_REV32_V); t.uimm=t.opc; return exec_2misc(c,&t);
    }
    case OP_ADD_V: case OP_SUB_V: case OP_AND_V: case OP_ORR_V:
    case OP_EOR_V: case OP_BIC_V: {
        jet_insn t=*i; t.op=OP_ASIMD_3SAME;
        if (i->op==OP_ADD_V){t.opc=0x10;t.N=0;}
        else if(i->op==OP_SUB_V){t.opc=0x10;t.N=1;}
        else if(i->op==OP_AND_V){t.opc=0x03;t.N=0;t.size=0;}
        else if(i->op==OP_BIC_V){t.opc=0x03;t.N=0;t.size=1;}
        else if(i->op==OP_ORR_V){t.opc=0x03;t.N=0;t.size=2;}
        else {t.opc=0x03;t.N=1;t.size=0;} /* EOR */
        return exec_3same(c,&t);
    }
    case OP_USHR: case OP_SSHR: case OP_SHL: {
        jet_insn t=*i; t.op=OP_ASIMD_SHIFT;
        if (i->op==OP_SHL){t.opc=0x0A;t.N=0;}
        else {t.opc=0x00;t.N=(i->op==OP_USHR);}
        return exec_shift(c,&t);
    }

    case OP_UDOT: case OP_SDOT: case OP_USDOT: {
        /* Acc 4s += sum of 4x (8-bit * 8-bit) from corresponding 16b/8b lanes */
        int Q = i->regsize;
        int n = Q ? 4 : 2; /* 4S or 2S */
        uint64_t out[2] = {c->v[i->rd][0], c->v[i->rd][1]};
        for (int k = 0; k < n; k++) {
            int32_t acc = (int32_t)get_lane(out, 32, k);
            for (int j = 0; j < 4; j++) {
                int idx = k*4 + j;
                uint8_t va = (uint8_t)get_lane(c->v[i->rn], 8, idx);
                uint8_t vb = (uint8_t)get_lane(c->v[i->rm], 8, idx);
                if (i->op == OP_UDOT) acc += (int32_t)va * (int32_t)vb;
                else if (i->op == OP_SDOT) acc += (int32_t)(int8_t)va * (int32_t)(int8_t)vb;
                else /* USDOT: unsigned * signed */ acc += (int32_t)va * (int32_t)(int8_t)vb;
            }
            set_lane(out, 32, k, (uint32_t)acc);
        }
        c->v[i->rd][0]=out[0]; c->v[i->rd][1]=Q?out[1]:0;
        return 0;
    }
    default: return -1;
    }
}
