/* Unit tests for jet_native. */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

static int g_fail = 0;
#define CHECK(cond, fmt, ...) do { \
    if (!(cond)) { fprintf(stderr, "FAIL: " fmt "\n", ##__VA_ARGS__); g_fail++; } \
    else fprintf(stderr, "OK:   " fmt "\n", ##__VA_ARGS__); \
} while (0)

static void log_cb(void *u, int level, const char *msg) {
    (void)u;
    if (level <= 1) fprintf(stderr, "  [L%d] %s\n", level, msg);
}

static uint64_t host_add(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; (void)u;
    return (uint64_t)(int64_t)((int)a->x[0] + (int)a->x[1]);
}

static double now_ms(void) {
    struct timeval tv; gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
}

int main(void) {
    jet_config cfg = {0};
    cfg.log = log_cb;
    jet_ctx *ctx = jet_native_create(&cfg);
    CHECK(ctx, "create ctx");

    jet_native_register_host(ctx, "host_add", host_add, NULL);

    char err[256];
    jet_lib *lib = jet_native_load(ctx, "tests/guest_lib.so", err, sizeof err);
    CHECK(lib, "load guest_lib.so (%s)", err);

    /* Resolve exports */
    uint64_t add = jet_native_sym(lib, "guest_add");
    CHECK(add != 0, "sym guest_add = 0x%llx", (unsigned long long)add);

    jet_args a = {0}; uint64_t r = 0;
    a.x[0] = 20; a.x[1] = 22;
    CHECK(jet_native_call(ctx, add, &a, &r, NULL) == 0, "call guest_add");
    CHECK(r == 42, "guest_add => %llu", (unsigned long long)r);

    uint64_t mul = jet_native_sym(lib, "guest_mul_add");
    a.x[0]=3; a.x[1]=5; a.x[2]=7; r=0;
    CHECK(jet_native_call(ctx, mul, &a, &r, NULL) == 0 && r == 22,
          "guest_mul_add => %llu", (unsigned long long)r);

    int arr[5] = {1,2,3,4,5};
    uint64_t sum = jet_native_sym(lib, "guest_sum_array");
    a.x[0] = (uint64_t)(uintptr_t)arr; a.x[1]=5; r=0;
    CHECK(jet_native_call(ctx, sum, &a, &r, NULL) == 0 && r == 15,
          "guest_sum_array => %llu", (unsigned long long)r);

    uint64_t ch = jet_native_sym(lib, "guest_call_host");
    a.x[0]=10; a.x[1]=32; r=0;
    CHECK(jet_native_call(ctx, ch, &a, &r, NULL) == 0 && r == 42,
          "guest_call_host => %llu", (unsigned long long)r);

    uint64_t sj = jet_native_sym(lib, "guest_setjmp_test");
    a.x[0]=0; r=0;
    int sjrc = jet_native_call(ctx, sj, &a, &r, NULL);
    CHECK(sjrc == 0 && r == 42, "guest_setjmp_test => rc=%d r=%llu err=%s",
          sjrc, (unsigned long long)r, jet_native_last_error(ctx));

    uint64_t fib = jet_native_sym(lib, "guest_fib");
    a.x[0]=20; r=0;
    double t0 = now_ms();
    CHECK(jet_native_call(ctx, fib, &a, &r, NULL) == 0 && r == 6765,
          "guest_fib(20) => %llu", (unsigned long long)r);
    double t1 = now_ms();
    /* fib(20) ≈ 21891 calls; rough insn count ~ few hundred thousand */
    fprintf(stderr, "INFO: fib(20) took %.2f ms; insn_count not directly exposed\n", t1-t0);

    /* JNI_OnLoad + Java_* lookup */
    int32_t ver = 0;
    CHECK(jet_jni_onload(ctx, lib, &ver) == 0 && ver == 0x00010006,
          "JNI_OnLoad => 0x%x", ver);
    uint64_t jbar = jet_native_sym(lib, "Java_com_example_Foo_bar");
    a.x[0]=0; a.x[1]=0; a.x[2]=10; r=0;
    CHECK(jet_native_call(ctx, jbar, &a, &r, NULL) == 0 && r == 27,
          "Java_com_example_Foo_bar(10) => %llu", (unsigned long long)r);

    /* Trampoline */
    uint64_t cmp = jet_native_sym(lib, "guest_cmp");
    jet_trampoline tr = jet_native_trampoline(ctx, cmp);
    CHECK(tr != NULL, "trampoline");
    int x=3, y=5;
    typedef int (*cmp_fn)(const int*, const int*);
    int cr = ((cmp_fn)tr)(&x, &y);
    CHECK(cr == -2, "tramp cmp => %d", cr);

    /* Loop benchmark: call guest_add many times */
    a.x[0]=1; a.x[1]=1;
    t0 = now_ms();
    const int N = 100000;
    for (int i = 0; i < N; i++) jet_native_call(ctx, add, &a, &r, NULL);
    t1 = now_ms();
    double secs = (t1 - t0) / 1000.0;
    /* guest_add is ~4-8 insns; estimate MIPS */
    double mips = (N * 6.0 / secs) / 1e6;
    fprintf(stderr, "INFO: %d calls of guest_add in %.2f ms (~%.1f MIPS est.)\n",
            N, t1-t0, mips);

    jet_native_destroy(ctx);
    if (g_fail) { fprintf(stderr, "\n%d FAILURES\n", g_fail); return 1; }
    fprintf(stderr, "\nALL PASS\n");
    return 0;
}
