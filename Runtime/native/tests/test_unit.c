/* Unit tests for jet_native. */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <setjmp.h>
#include <signal.h>

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


/* Soft-fault test helpers (file scope — no nested fns). */
static volatile int g_sf_prior_hits;
static sigjmp_buf g_sf_prior_jmp;
static void softfault_prior_handler(int sig, siginfo_t *si, void *uctx) {
    (void)sig; (void)si; (void)uctx;
    g_sf_prior_hits++;
    siglongjmp(g_sf_prior_jmp, 1);
}
void softfault_test_reset_hits(void) { g_sf_prior_hits = 0; }
int softfault_test_prior_hits(void) { return g_sf_prior_hits; }
void softfault_test_install_prior(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof sa);
    sa.sa_sigaction = softfault_prior_handler;
    sa.sa_flags = SA_SIGINFO | SA_NODEFER;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
}
int softfault_test_raise_and_catch(void) {
    if (sigsetjmp(g_sf_prior_jmp, 1) == 0) {
        raise(SIGSEGV);
        return 0; /* not caught */
    }
    return 1;
}

/* Host trap that dereferences NULL. Soft-fault must forward to prior while
 * jet_fault_active is cleared around trap dispatch. setjmp here so prior's
 * longjmp resumes inside the trap and guest returns cleanly. */
static uint64_t host_null_trap(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; (void)a; (void)u;
    if (sigsetjmp(g_sf_prior_jmp, 1) == 0) {
        volatile int *p = 0;
        return (uint64_t)(int64_t)(*p);
    }
    return 1; /* prior handler caught it */
}

/* "Newer" handler that displaces jet — used to verify re-chain on next guest entry. */
static volatile int g_sf_mid_hits;
static sigjmp_buf g_sf_mid_jmp;
static void softfault_mid_handler(int sig, siginfo_t *si, void *uctx) {
    (void)sig; (void)si; (void)uctx;
    g_sf_mid_hits++;
    siglongjmp(g_sf_mid_jmp, 1);
}
static void softfault_test_install_mid(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof sa);
    sa.sa_sigaction = softfault_mid_handler;
    sa.sa_flags = SA_SIGINFO | SA_NODEFER;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
}
static int softfault_test_raise_mid(void) {
    if (sigsetjmp(g_sf_mid_jmp, 1) == 0) {
        raise(SIGSEGV);
        return 0;
    }
    return 1;
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
    jet_native_register_host(ctx, "host_null_trap", host_null_trap, NULL);

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

    /* FP LDR/STR D width: must not clobber adjacent u64 with 1000.0 (TG CM bug). */
    uint64_t fp_probe = jet_native_sym(lib, "guest_fp_d_store_probe");
    CHECK(fp_probe != 0, "sym guest_fp_d_store_probe");
    uint64_t slots[2] = {0, 0};
    a.x[0] = (uint64_t)(uintptr_t)slots; r = 0;
    CHECK(jet_native_call(ctx, fp_probe, &a, &r, NULL) == 0, "call fp_d_store_probe");
    CHECK(r == 0xDEADBEEFCAFEBABEull,
          "fp D-store adjacent kept sentinel (got 0x%llx)", (unsigned long long)r);
    CHECK(slots[1] == 0xDEADBEEFCAFEBABEull, "slots[1] intact");
    CHECK(slots[1] != 0x408f400000000000ull, "slots[1] is not 1000.0 bit pattern");

    /* AAPCS64 x8 indirect result: guest returns 24-byte struct via x8. */
    uint64_t ret_big = jet_native_sym(lib, "guest_return_big");
    CHECK(ret_big != 0, "sym guest_return_big");
    struct { uint64_t a,b,c; } big = {0};
    a.x[0]=11; a.x[1]=22; a.x[2]=33; a.x8 = (uint64_t)(uintptr_t)&big; r=0;
    CHECK(jet_native_call(ctx, ret_big, &a, &r, NULL) == 0, "call guest_return_big");
    CHECK(big.a==11 && big.b==22 && big.c==33,
          "x8 indirect result {%llu,%llu,%llu}",
          (unsigned long long)big.a, (unsigned long long)big.b, (unsigned long long)big.c);

    /* REV misdecode regression: was UDIV/SDIV with Rm=0 → always 0 (SHA/PBKDF2). */
    {
        uint64_t x = 0xdef09abc56781234ull;
        uint64_t rev = jet_native_sym(lib, "guest_rev64");
        a.x[0]=x; r=0;
        CHECK(jet_native_call(ctx, rev, &a, &r, NULL)==0 && r==0x34127856bc9af0deull,
              "guest_rev64 => 0x%llx", (unsigned long long)r);
        uint64_t revw = jet_native_sym(lib, "guest_rev32_w");
        a.x[0]=x; r=0;
        CHECK(jet_native_call(ctx, revw, &a, &r, NULL)==0 && r==0x34127856ull,
              "guest_rev32_w => 0x%llx", (unsigned long long)r);
        uint64_t extr = jet_native_sym(lib, "guest_extr_ror64");
        a.x[0]=x; r=0;
        CHECK(jet_native_call(ctx, extr, &a, &r, NULL)==0 && r==0x48d37bc26af159e0ull,
              "guest_extr_ror64 => 0x%llx", (unsigned long long)r);
        uint64_t eor = jet_native_sym(lib, "guest_eor_ror23");
        a.x[0]=x; r=0;
        CHECK(jet_native_call(ctx, eor, &a, &r, NULL)==0 && r==0x2ed4f301b74d6a98ull,
              "guest_eor_ror23 => 0x%llx", (unsigned long long)r);
        uint64_t wadd = jet_native_sym(lib, "guest_w_add_zero_upper");
        a.x[0]=0xbeefdeadffffffffull; r=0;
        CHECK(jet_native_call(ctx, wadd, &a, &r, NULL)==0 && r==0x0ull,
              "guest_w_add_zero_upper (ffffffff+1) => 0x%llx", (unsigned long long)r);
        a.x[0]=0xbeefdeadffff0000ull; r=0;
        CHECK(jet_native_call(ctx, wadd, &a, &r, NULL)==0 && r==0xffff0001ull,
              "guest_w_add_zero_upper upper-cleared => 0x%llx", (unsigned long long)r);
        /* MOVI.16B byte-replicate + EOR — HMAC ipad/opad (was 64-bit expand). */
        {
            uint8_t src[16] = {0}, dst[16] = {0};
            memcpy(src, "password", 8);
            uint64_t ipad = jet_native_sym(lib, "guest_hmac_ipad16");
            a.x[0]=(uint64_t)(uintptr_t)dst; a.x[1]=(uint64_t)(uintptr_t)src; r=0;
            CHECK(jet_native_call(ctx, ipad, &a, &r, NULL)==0, "call guest_hmac_ipad16");
            const uint8_t exp_i[16] = {
                0x46,0x57,0x45,0x45,0x41,0x59,0x44,0x52,
                0x36,0x36,0x36,0x36,0x36,0x36,0x36,0x36};
            CHECK(memcmp(dst, exp_i, 16)==0, "guest_hmac_ipad16 bytes");
            uint64_t opad = jet_native_sym(lib, "guest_hmac_opad16");
            memset(dst, 0, 16);
            a.x[0]=(uint64_t)(uintptr_t)dst; a.x[1]=(uint64_t)(uintptr_t)src; r=0;
            CHECK(jet_native_call(ctx, opad, &a, &r, NULL)==0, "call guest_hmac_opad16");
            const uint8_t exp_o[16] = {
                0x2c,0x3d,0x2f,0x2f,0x2b,0x33,0x2e,0x38,
                0x5c,0x5c,0x5c,0x5c,0x5c,0x5c,0x5c,0x5c};
            CHECK(memcmp(dst, exp_o, 16)==0, "guest_hmac_opad16 bytes");
        }
    }


    /* ---- Soft-fault: JVM-safe SIGSEGV chaining ---- */
    {
        /* Prior SA_SIGINFO handler simulates JVM / -XX:-UseSignalChaining peer. */
        softfault_test_reset_hits();
        softfault_test_install_prior();

        /* (1) Fault OUTSIDE guest → prior handler must run. */
        CHECK(softfault_test_raise_and_catch() == 1, "outside-guest fault caught by prior");
        CHECK(softfault_test_prior_hits() >= 1, "prior_hits after outside fault => %d",
              softfault_test_prior_hits());

        /* Enter guest once so jet installs on top and chains to prior. */
        a.x[0]=1; a.x[1]=2; r=0;
        CHECK(jet_native_call(ctx, add, &a, &r, NULL) == 0 && r == 3,
              "guest_add after prior install => %llu", (unsigned long long)r);

        /* (2) Fault INSIDE guest (NULL deref) → soft-handled, not prior. */
        {
            int hits_before = softfault_test_prior_hits();
            uint64_t nd = jet_native_sym(lib, "guest_null_deref");
            CHECK(nd != 0, "sym guest_null_deref");
            a.x[0]=0; r=0;
            int nrc = jet_native_call(ctx, nd, &a, &r, NULL);
            CHECK(nrc != 0, "inside-guest NULL deref soft-handled rc=%d err=%s",
                  nrc, jet_native_last_error(ctx));
            CHECK(softfault_test_prior_hits() == hits_before,
                  "prior NOT called for inside-guest fault (hits %d -> %d)",
                  hits_before, softfault_test_prior_hits());
        }

        /* (3) Nested install: second guest entry must not lose prior.
         * Another guest call re-checks chain; then outside fault still hits prior. */
        a.x[0]=7; a.x[1]=8; r=0;
        CHECK(jet_native_call(ctx, add, &a, &r, NULL) == 0 && r == 15,
              "nested guest entry => %llu", (unsigned long long)r);
        {
            int hits_before = softfault_test_prior_hits();
            CHECK(softfault_test_raise_and_catch() == 1, "after nested install, outside fault to prior");
            CHECK(softfault_test_prior_hits() == hits_before + 1,
                  "prior still chained after nested install (hits %d)",
                  softfault_test_prior_hits());
        }

        /* (4) Newer handler displaces jet; next guest entry re-chains without
         * losing the newer one (libjsig / late JVM install). */
        {
            g_sf_mid_hits = 0;
            softfault_test_install_mid(); /* now mid is head, jet displaced */
            a.x[0]=3; a.x[1]=4; r=0;
            CHECK(jet_native_call(ctx, add, &a, &r, NULL) == 0 && r == 7,
                  "guest entry re-chains over mid => %llu", (unsigned long long)r);
            /* Outside fault should hit mid (saved as previous), not be lost. */
            CHECK(softfault_test_raise_mid() == 1, "outside fault forwarded to mid after re-chain");
            CHECK(g_sf_mid_hits == 1, "mid_hits after re-chain forward => %d", g_sf_mid_hits);
        }

        /* (5) Guest → host trap that NULL-derefs: prior must receive it
         * (jet_fault_active cleared around trap; host PC not claimed). */
        {
            softfault_test_install_prior();
            a.x[0]=1; a.x[1]=1; r=0;
            CHECK(jet_native_call(ctx, add, &a, &r, NULL) == 0 && r == 2,
                  "re-chain prior before host-null test => %llu", (unsigned long long)r);
            int hits_before = softfault_test_prior_hits();
            uint64_t hn = jet_native_sym(lib, "guest_call_host_null");
            CHECK(hn != 0, "sym guest_call_host_null");
            a.x[0]=0; r=0;
            int hrc = jet_native_call(ctx, hn, &a, &r, NULL);
            CHECK(hrc == 0 && r == 1,
                  "host-null trap forwarded to prior, guest returned r=%llu rc=%d err=%s",
                  (unsigned long long)r, hrc, jet_native_last_error(ctx));
            CHECK(softfault_test_prior_hits() == hits_before + 1,
                  "prior received host-trap NULL (hits %d -> %d)",
                  hits_before, softfault_test_prior_hits());
        }
    }

    jet_native_destroy(ctx);
    if (g_fail) { fprintf(stderr, "\n%d FAILURES\n", g_fail); return 1; }
    fprintf(stderr, "\nALL PASS\n");
    return 0;
}

