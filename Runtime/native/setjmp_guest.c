/* setjmp_guest.c - in-guest setjmp/longjmp saving jet_cpu state. */
#include "jet_internal.h"

#define JET_JMP_MAGIC 0x4A45544A4D502101ull
/* Keep under 384 bytes so small guest buffers work. AAPCS64 saves GP + d8-d15. */
typedef struct {
    uint64_t magic;
    uint64_t x[31];
    uint64_t sp, pc, nzcv, tpidr;
    uint64_t d8_15[8];
} jet_jmp_buf;

static uint64_t h_setjmp(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)u;
    jet_cpu *c = jet_cpu_current(ctx);
    jet_jmp_buf *jb = (jet_jmp_buf *)G2H(a->x[0]);
    memset(jb, 0, sizeof(*jb));
    jb->magic = JET_JMP_MAGIC;
    memcpy(jb->x, c->x, sizeof c->x);
    jb->sp = c->sp;
    jb->pc = c->x[30]; /* resume at LR */
    jb->nzcv = c->nzcv;
    jb->tpidr = c->tpidr_el0;
    for (int i = 0; i < 8; i++) jb->d8_15[i] = c->v[8 + i][0];
    return 0;
}

static uint64_t h_longjmp(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)u;
    jet_cpu *c = jet_cpu_current(ctx);
    jet_jmp_buf *jb = (jet_jmp_buf *)G2H(a->x[0]);
    uint64_t v = a->x[1];
    if (jb->magic != JET_JMP_MAGIC) {
        jet_set_error(ctx, "longjmp: bad jmp_buf magic");
        c->err_code = 1; c->stop = 1;
        return 0;
    }
    memcpy(c->x, jb->x, sizeof c->x);
    c->sp = jb->sp;
    c->nzcv = (uint32_t)jb->nzcv;
    c->tpidr_el0 = jb->tpidr;
    for (int i = 0; i < 8; i++) {
        c->v[8 + i][0] = jb->d8_15[i];
        c->v[8 + i][1] = 0;
    }
    c->x[0] = v ? v : 1;
    c->x[30] = jb->pc; /* trap return uses LR */
    c->pc = jb->pc;
    c->jumping = 1;
    return c->x[0];
}

int jet_host_install_setjmp(jet_ctx *ctx) {
    jet_native_register_host(ctx, "setjmp", h_setjmp, NULL);
    jet_native_register_host(ctx, "_setjmp", h_setjmp, NULL);
    jet_native_register_host(ctx, "sigsetjmp", h_setjmp, NULL);
    jet_native_register_host(ctx, "__sigsetjmp", h_setjmp, NULL);
    jet_native_register_host(ctx, "longjmp", h_longjmp, NULL);
    jet_native_register_host(ctx, "_longjmp", h_longjmp, NULL);
    jet_native_register_host(ctx, "siglongjmp", h_longjmp, NULL);
    return 0;
}
