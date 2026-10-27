/* v11: Rn=31 is XZR (not SP) in shifted-reg CMP/CSINV; per-CPU exclusive monitor. */
#include "jet_native.h"
#include "jet_internal.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <stdint.h>

static int g_fail;
#define CHECK(c, ...) do { if (!(c)) { fprintf(stderr,"FAIL: " __VA_ARGS__); fprintf(stderr,"\n"); g_fail++; } \
    else { fprintf(stderr,"OK:   " __VA_ARGS__); fprintf(stderr,"\n"); } } while (0)

/* Encode helpers (A64) */
static uint32_t enc_subs_shift(int sf, int rd, int rn, int rm, int shift, int imm6) {
    /* sf op=1 S=1 01011 shift 0 Rm imm6 Rn Rd */
    return ((uint32_t)sf << 31) | (1u << 30) | (1u << 29) | (0x0Bu << 24) |
           ((uint32_t)shift << 22) | ((uint32_t)rm << 16) |
           ((uint32_t)imm6 << 10) | ((uint32_t)rn << 5) | (uint32_t)rd;
}
static uint32_t enc_csinv(int sf, int rd, int rn, int rm, int cond) {
    /* sf 1 0 11010100 Rm cond 0 0 Rn Rd */
    return ((uint32_t)sf << 31) | (1u << 30) | (0xD4u << 21) |
           ((uint32_t)rm << 16) | ((uint32_t)cond << 12) |
           ((uint32_t)rn << 5) | (uint32_t)rd;
}
static uint32_t enc_orr_mov(int sf, int rd, int rm) {
    /* MOV Xd, Xm alias of ORR Xd, XZR, Xm */
    return ((uint32_t)sf << 31) | (0x2Au << 24) | ((uint32_t)rm << 16) | (31u << 5) | (uint32_t)rd;
}
static uint32_t enc_ret(void) { return 0xD65F03C0u; }

static void test_cmp_xzr_csinv(jet_ctx *ctx) {
    /* Reproduce Lottie overflow check:
     *   sbfiz-equivalent: x8 = size (0x9600)
     *   x9 = sext of related value with high bits clear
     *   cmp xzr, x9, lsr #62   → must set Z when x9>>62 == 0
     *   csinv x0, x8, xzr, eq  → x0 = x8 if Z else ~0
     */
    static uint32_t code_lottie[8];
    uint32_t *code = code_lottie;
    int n = 0;
    /* mov x8, x1  (arg size) — use ORR x8, xzr, x1 */
    code[n++] = enc_orr_mov(1, 8, 1);
    /* mov x9, x2 */
    code[n++] = enc_orr_mov(1, 9, 2);
    /* cmp xzr, x9, lsr #62  == SUBS XZR, XZR, X9, LSR #62 */
    code[n++] = enc_subs_shift(1, 31, 31, 9, 1 /*LSR */, 62);
    /* csinv x0, x8, xzr, eq (cond=0) */
    code[n++] = enc_csinv(1, 0, 8, 31, 0);
    code[n++] = enc_ret();

    uint64_t fn = (uint64_t)(uintptr_t)code;
    jet_args a = {0};
    a.x[1] = 0x9600;          /* size */
    a.x[2] = 0x4B00;          /* value whose >>62 is 0 */
    uint64_t ret = 0;
    int rc = jet_native_call(ctx, fn, &a, &ret, NULL);
    CHECK(rc == 0, "cmp/csinv call rc=%d err=%s", rc, jet_native_last_error(ctx));
    CHECK(ret == 0x9600, "malloc size must be 0x9600 not -1 (got 0x%llx)",
          (unsigned long long)ret);

    /* Negative: x9 with bit 62 set → overflow → ~0 */
    a.x[1] = 0x9600;
    a.x[2] = (1ull << 62);
    ret = 0;
    rc = jet_native_call(ctx, fn, &a, &ret, NULL);
    CHECK(rc == 0, "overflow call");
    CHECK(ret == ~0ull, "overflow must yield ~0 (got 0x%llx)", (unsigned long long)ret);
}

static void test_csel_xzr(jet_ctx *ctx) {
    /* Distinct static buffers — block cache keys by PC; stack reuse aliases. */
    static uint32_t code_cset[6];
    static uint32_t code_csinv[6];
    int n = 0;
    code_cset[n++] = enc_subs_shift(1, 31, 31, 31, 0, 0); /* cmp xzr,xzr → Z=1 */
    code_cset[n++] = (1u<<31) | (0xD4u<<21) | (31u<<16) | (1u<<12) | (1u<<10) | (31u<<5) | 0u;
    code_cset[n++] = enc_ret();
    uint64_t ret = 0;
    CHECK(jet_native_call(ctx, (uint64_t)(uintptr_t)code_cset, NULL, &ret, NULL) == 0, "cset");
    CHECK(ret == 1, "CSET EQ (CSINC NE) after cmp xzr,xzr => 1 (got %llu)", (unsigned long long)ret);

    n = 0;
    code_csinv[n++] = enc_orr_mov(1, 0, 1); /* mov x0, x1 */
    code_csinv[n++] = enc_subs_shift(1, 31, 0, 0, 0, 0); /* cmp x0, x0 */
    code_csinv[n++] = enc_csinv(1, 0, 2, 31, 1); /* csinv x0, x2, xzr, ne */
    code_csinv[n++] = enc_ret();
    jet_args a = {0};
    a.x[1] = 1; a.x[2] = 0x42;
    ret = 0;
    CHECK(jet_native_call(ctx, (uint64_t)(uintptr_t)code_csinv, &a, &ret, NULL) == 0, "csinv xzr");
    CHECK(ret == ~0ull, "CSINV Rm=XZR when cond fails => ~0 (got 0x%llx)", (unsigned long long)ret);
}

/* Two threads LDAXR/STLXR on DIFFERENT addresses must both succeed (no global steal). */
static jet_ctx *g_ctx;
static volatile int g_go, g_done[2];
static uint64_t g_cell[2] __attribute__((aligned(8)));
static uint64_t g_fn_lock;

static uint32_t enc_ldaxr_x(int rt, int rn) {
    /* LDAXR Xt, [Xn] : 1100100001011111111111 Rn Rt */
    return 0xC85FFC00u | ((uint32_t)rn << 5) | (uint32_t)rt;
}
static uint32_t enc_stlxr_x(int rs, int rt, int rn) {
    /* STLXR Ws, Xt, [Xn] */
    return 0xC820FC00u | ((uint32_t)rs << 16) | ((uint32_t)rn << 5) | (uint32_t)rt;
}
static uint32_t enc_cbnz_w(int rt, int imm19) {
    /* CBNZ Wt, #imm */
    return 0x35000000u | (((uint32_t)imm19 & 0x7FFFF) << 5) | (uint32_t)rt;
}
static uint32_t enc_add_imm(int rd, int rn, int imm) {
    return (1u<<31) | (0x11u<<24) | ((uint32_t)imm << 10) | ((uint32_t)rn << 5) | (uint32_t)rd;
}

static void *thr_excl(void *arg) {
    int id = (int)(intptr_t)arg;
    jet_cpu *cpu = jet_cpu_create(g_ctx);
    pthread_setspecific(g_ctx->cpu_key, cpu);
    while (!g_go) { }
    jet_args a = {0};
    a.x[0] = (uint64_t)(uintptr_t)&g_cell[id];
    a.x[1] = 0x11110000ull + (uint64_t)id;
    uint64_t ret = 0;
    int rc = jet_native_call(g_ctx, g_fn_lock, &a, &ret, NULL);
    CHECK(rc == 0, "thr%d call", id);
    CHECK(ret == 0, "thr%d stlxr status (0=ok) got %llu", id, (unsigned long long)ret);
    CHECK(g_cell[id] == 0x11110000ull + (uint64_t)id, "thr%d cell", id);
    g_done[id] = 1;
    return NULL;
}

static void test_excl_parallel(jet_ctx *ctx) {
    /* loop: ldaxr x2,[x0]; stlxr w3,x1,[x0]; cbnz w3, loop; mov x0,x3; ret
     * x0=addr x1=newval → returns 0 on success */
    static uint32_t code_lock[16];
    uint32_t *code = code_lock;
    int n = 0;
    int loop = n;
    code[n++] = enc_ldaxr_x(2, 0);
    code[n++] = enc_stlxr_x(3, 1, 0);
    /* cbnz w3, loop — backward */
    int imm19 = loop - n; /* PC-relative in instructions */
    code[n++] = enc_cbnz_w(3, imm19 & 0x7FFFF);
    code[n++] = enc_orr_mov(1, 0, 3); /* mov x0, x3 */
    code[n++] = enc_ret();
    g_fn_lock = (uint64_t)(uintptr_t)code;
    g_ctx = ctx;
    g_cell[0] = g_cell[1] = 0;
    g_go = 0; g_done[0] = g_done[1] = 0;
    pthread_t t0, t1;
    pthread_create(&t0, NULL, thr_excl, (void*)(intptr_t)0);
    pthread_create(&t1, NULL, thr_excl, (void*)(intptr_t)1);
    g_go = 1;
    pthread_join(t0, NULL);
    pthread_join(t1, NULL);
    CHECK(g_done[0] && g_done[1], "both threads finished");
}

static void test_stats_named(jet_ctx *ctx) {
    setenv("JET_NATIVE_STATS", "1", 1);
    ctx->stats_enabled = 1;
    /* clear stats */
    memset(ctx->native_stats, 0, sizeof ctx->native_stats);
    static uint32_t code_ret[2] = { 0xD65F03C0u, 0 }; /* ret */
    uint32_t *code = code_ret;
    uint64_t ret = 0;
    CHECK(jet_native_call_named(ctx, "Test.method", "()V",
          (uint64_t)(uintptr_t)code, NULL, &ret, NULL) == 0, "call_named");
    int found = 0;
    for (int i = 0; i < JET_STATS_POOL; i++) {
        if (ctx->native_stats[i].used && strstr(ctx->native_stats[i].key, "Test.method")) {
            found = 1;
            CHECK(ctx->native_stats[i].calls >= 1, "named calls");
            break;
        }
    }
    CHECK(found, "stats row for Test.method()V");
}

int main(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    CHECK(ctx, "create");
    test_cmp_xzr_csinv(ctx);
    test_csel_xzr(ctx);
    test_excl_parallel(ctx);
    test_stats_named(ctx);
    if (g_fail) { fprintf(stderr, "%d failures\n", g_fail); return 1; }
    fprintf(stderr, "ALL PASS\n");
    return 0;
}
