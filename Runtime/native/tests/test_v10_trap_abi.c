/* v10: trap ABI callee-saved, guest struct stat size, ClassLoader GlobalRef. */
#include "jet_native.h"
#include "jet_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

static int g_fail;
#define CHECK(c, ...) do { if (!(c)) { fprintf(stderr,"FAIL: " __VA_ARGS__); fprintf(stderr,"\n"); g_fail++; } \
    else { fprintf(stderr,"OK:   " __VA_ARGS__); fprintf(stderr,"\n"); } } while (0)

int main(void) {
    setenv("JET_TRAP_ABI_CHECK", "1", 1);

    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    CHECK(ctx, "create");

    char err[256];
    jet_lib *lib = jet_native_load(ctx, "tests/guest_lib.so", err, sizeof err);
    CHECK(lib, "load: %s", err);

    uint64_t sym = jet_native_sym(lib, "guest_trap_abi_check");
    CHECK(sym, "guest_trap_abi_check sym");
    uint64_t ret = 0;
    int rc = jet_native_call(ctx, sym, NULL, &ret, NULL);
    CHECK(rc == 0, "abi_check call rc=%d err=%s", rc, jet_native_last_error(ctx));
    CHECK(ret == 0, "callee-saved preserved across fstat/open/malloc (bad=0x%llx)",
          (unsigned long long)ret);

    sym = jet_native_sym(lib, "guest_stat_size");
    ret = 0;
    CHECK(jet_native_call(ctx, sym, NULL, &ret, NULL) == 0, "stat_size call");
    CHECK(ret == 128, "guest_stat_size=%llu", (unsigned long long)ret);

    sym = jet_native_sym(lib, "guest_fstat_no_smash");
    ret = 0;
    CHECK(jet_native_call(ctx, sym, NULL, &ret, NULL) == 0, "fstat_no_smash call");
    CHECK(ret == 0, "fstat must not write past 128-byte guest struct stat (hit=%llu)",
          (unsigned long long)ret);

    /* ClassLoader GlobalRef promotion */
    int before = jet_native_global_ref_count(ctx);
    void *fake = (void *)(uintptr_t)0xBEEF;
    CHECK(jet_native_set_class_loader(ctx, fake) == 0, "set_class_loader");
    CHECK(jet_native_global_ref_count(ctx) == before + 1, "global ref count bumped");
    CHECK(jet_native_get_class_loader(ctx) == fake, "get_loader identity (fake JNI)");

    /* RegisterNatives stores loader; with fake getClassLoader path may not fire,
     * but set_class_loader promote is the API contract under test. */
    jet_args a = {0};
    char cname[] = "com/example/V10";
    a.x[1] = (uint64_t)(uintptr_t)cname;
    uint64_t cls = jet_jni_call_env(ctx, 6, &a);
    CHECK(cls != 0, "FindClass");
    struct { uint64_t name, sig, fn; } meth[1];
    char n1[] = "nativeTick"; char s1[] = "()V";
    meth[0].name = (uint64_t)(uintptr_t)n1;
    meth[0].sig  = (uint64_t)(uintptr_t)s1;
    meth[0].fn   = jet_native_sym(lib, "guest_add");
    memset(&a, 0, sizeof a);
    a.x[1] = cls; a.x[2] = (uint64_t)(uintptr_t)meth; a.x[3] = 1;
    CHECK(jet_jni_call_env(ctx, 215, &a) == 0, "RegisterNatives");

    setenv("JET_NATIVE_STATS", "1", 1);
    ctx->stats_enabled = 1;
    /* Invoke via trampoline if one was installed */
    for (int i = 0; i < 64; i++) {
        if (ctx->tramps[i].used && ctx->tramps[i].guest_fn == meth[0].fn) {
            jet_trampoline tr = jet_native_trampoline(ctx, meth[0].fn);
            CHECK(tr != NULL, "trampoline");
            if (tr) {
                typedef uint64_t (*fn8)(uint64_t,uint64_t,uint64_t,uint64_t,
                                       uint64_t,uint64_t,uint64_t,uint64_t);
                ((fn8)tr)(0, 0, 3, 4, 0, 0, 0, 0); /* guest_add(3,4) via x2/x3? args in x0,x1 */
                ((fn8)tr)(3, 4, 0, 0, 0, 0, 0, 0);
            }
            CHECK(ctx->native_stats[i].used, "stats recorded for tramp %d", i);
            CHECK(ctx->native_stats[i].calls >= 1, "stats calls=%llu",
                  (unsigned long long)ctx->native_stats[i].calls);
            break;
        }
    }
    jet_native_dump_stats(ctx);

    jet_native_destroy(ctx);
    if (g_fail) { fprintf(stderr, "%d FAIL\n", g_fail); return 1; }
    fprintf(stderr, "ALL OK\n");
    return 0;
}
