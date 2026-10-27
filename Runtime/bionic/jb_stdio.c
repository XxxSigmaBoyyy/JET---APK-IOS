#include "jb_internal.h"
#include <stdio.h>
#include <string.h>
#include <wchar.h>
#include <stdlib.h>
// bionic FILE is opaque to callers that only pass FILE*; the only address comparison we see is against
// the three slots of __sF. We publish a 3*152-byte array and map those addresses to Darwin's stdio.
enum { BIONIC_FILE_SZ = 152 };
JBX char __sF[3 * BIONIC_FILE_SZ];
FILE *jb_file(void *p){
    if (!p) return NULL;
    if ((char*)p >= __sF && (char*)p < __sF + 3*BIONIC_FILE_SZ)
        return (&stdin)[((char*)p - __sF) / BIONIC_FILE_SZ];
    return (FILE*)p; // files opened through the bridge are Darwin FILE*
}
void jb_cur_regs(jb_cur *c, jb_regs *r, int fixed_gp, int fixed_fp){
    c->r=r; c->ngr=fixed_gp; c->nvr=fixed_fp; c->stk=(uint8_t*)r->stk; c->va=NULL;
}
void jb_cur_va(jb_cur *c, jb_aapcs_va *va){ c->r=NULL; c->ngr=c->nvr=0; c->stk=NULL; c->va=va; }
uint64_t jb_next_gp(jb_cur *c){
    if (c->va){
        if (c->va->gr_offs <= -8){ uint64_t v = *(uint64_t*)((char*)c->va->gr_top + c->va->gr_offs); c->va->gr_offs += 8; return v; }
        uint64_t v = *(uint64_t*)c->va->stack; c->va->stack = (char*)c->va->stack + 8; return v;
    }
    if (c->ngr < 8) return c->r->x[c->ngr++];
    uint64_t v = *(uint64_t*)c->stk; c->stk += 8; return v;
}
double jb_next_fp(jb_cur *c){
    if (c->va){
        if (c->va->vr_offs <= -16){ double v = *(double*)((char*)c->va->vr_top + c->va->vr_offs); c->va->vr_offs += 16; return v; }
        double v = *(double*)c->va->stack; c->va->stack = (char*)c->va->stack + 8; return v;
    }
    if (c->nvr < 8){ double v; memcpy(&v, c->r->v[c->nvr++], 8); return v; }
    double v = *(double*)c->stk; c->stk += 8; return v;
}
static double jb_bin128_to_d(unsigned __int128 u){
    uint64_t hi = (uint64_t)(u >> 64), lo = (uint64_t)u;
    int sign = (int)(hi >> 63);
    int exp = (int)((hi >> 48) & 0x7fff) - 16383 + 1023;
    uint64_t mant = ((hi & 0xffffffffffffULL) << 4) | (lo >> 60);
    if ((hi & 0x7fff000000000000ULL) == 0) return sign ? -0.0 : 0.0;
    if ((hi & 0x7fff000000000000ULL) == 0x7fff000000000000ULL){
        uint64_t bits = ((uint64_t)sign<<63) | (0x7ffULL<<52) | (mant?1:0); double d; memcpy(&d,&bits,8); return d; }
    if (exp <= 0) return sign ? -0.0 : 0.0;
    if (exp >= 0x7ff){ uint64_t bits = ((uint64_t)sign<<63) | (0x7ffULL<<52); double d; memcpy(&d,&bits,8); return d; }
    uint64_t bits = ((uint64_t)sign<<63) | ((uint64_t)exp<<52) | (mant & ((1ULL<<52)-1));
    double d; memcpy(&d,&bits,8); return d;
}
double jb_next_ld(jb_cur *c){ // AAPCS long double = IEEE binary128
    unsigned __int128 u = 0;
    if (c->va){
        if (c->va->vr_offs <= -16){ memcpy(&u, (char*)c->va->vr_top + c->va->vr_offs, 16); c->va->vr_offs += 16; }
        else { memcpy(&u, c->va->stack, 16); c->va->stack = (char*)c->va->stack + 16; }
    } else if (c->nvr < 8){ memcpy(&u, c->r->v[c->nvr++], 16); }
    else { memcpy(&u, c->stk, 16); c->stk += 16; }
    return jb_bin128_to_d(u);
}
