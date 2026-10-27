/* FindClass loader API, RegisterNatives real class name, dl_iterate_phdr. */
#include "jet_native.h"
#include "jet_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

static int g_fail;
#define CHECK(c, ...) do { if (!(c)) { fprintf(stderr,"FAIL: " __VA_ARGS__); fprintf(stderr,"\n"); g_fail++; } \
    else { fprintf(stderr,"OK:   " __VA_ARGS__); fprintf(stderr,"\n"); } } while (0)

static char g_reg_cls[256];
static void on_reg(void *u, const char *cls, const char *name, const char *sig, uint64_t fn) {
    (void)u;(void)name;(void)sig;(void)fn;
    snprintf(g_reg_cls, sizeof g_reg_cls, "%s", cls ? cls : "");
}

static int g_seen_libs, g_seen_eh, g_seen_tls;
static uint64_t h_phdr_cb(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    /* a0 = &dl_phdr_info (host/guest identity) */
    typedef struct {
        uint64_t dlpi_addr; const char *dlpi_name; const void *dlpi_phdr;
        uint16_t dlpi_phnum; uint64_t dlpi_adds, dlpi_subs;
        size_t dlpi_tls_modid; void *dlpi_tls_data;
    } info_t;
    typedef struct { uint32_t p_type, p_flags; uint64_t o,v,p,f,m,a; } phdr_t;
    info_t *info = (info_t*)(uintptr_t)a->x[0];
    if (!info) return 0;
    g_seen_libs++;
    const phdr_t *ph = (const phdr_t*)info->dlpi_phdr;
    for (int i = 0; ph && i < info->dlpi_phnum; i++) {
        if (ph[i].p_type == 0x6474e550u) g_seen_eh++;
        if (ph[i].p_type == 7) g_seen_tls++;
    }
    return 0;
}

int main(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    jet_jni_on_register_natives(ctx, on_reg, NULL);

    jet_args a = {0};
    char cname[] = "org/telegram/messenger/Utilities";
    a.x[1] = (uint64_t)(uintptr_t)cname;
    uint64_t cls = jet_jni_call_env(ctx, 6, &a);
    CHECK(cls != 0, "FindClass");

    struct { uint64_t name, sig, fn; } meth[1];
    char n1[] = "generateGradient"; char s1[] = "()V";
    meth[0].name = (uint64_t)(uintptr_t)n1;
    meth[0].sig  = (uint64_t)(uintptr_t)s1;
    meth[0].fn   = 0x2000;
    memset(&a, 0, sizeof a);
    a.x[1] = cls; a.x[2] = (uint64_t)(uintptr_t)meth; a.x[3] = 1;
    CHECK(jet_jni_call_env(ctx, 215, &a) == 0, "RegisterNatives JNI_OK");
    CHECK(strcmp(g_reg_cls, "org/telegram/messenger/Utilities") == 0,
          "reg class '%s'", g_reg_cls);

    void *fake_loader = (void*)(uintptr_t)0x1234;
    CHECK(jet_native_set_class_loader(ctx, fake_loader) == 0, "set_loader");
    CHECK(jet_native_get_class_loader(ctx) == fake_loader, "get_loader");

    char err[256];
    jet_lib *lib = jet_native_load(ctx, "tests/guest_lib.so", err, sizeof err);
    CHECK(lib, "load guest_lib: %s", err);
    CHECK(lib->phnum > 0, "phnum=%u", lib->phnum);
    CHECK(lib->phdrs != NULL, "phdrs non-null");
    jet_native_lib_set_class_loader(lib, fake_loader);

    /* Host callback as guest-callable trap, then dl_iterate_phdr */
    jet_native_register_host(ctx, "__phdr_cb", h_phdr_cb, NULL);
    /* Need guest address of the trap — look up stub */
    uint64_t cb_stub = 0;
    for (int i = 1; i < ctx->ntraps; i++) {
        if (ctx->traps[i].name && strcmp(ctx->traps[i].name, "__phdr_cb") == 0) {
            cb_stub = jet_stub_addr(ctx, i);
            break;
        }
    }
    CHECK(cb_stub != 0, "phdr_cb stub");
    jet_args da = {0}; da.x[0] = cb_stub; da.x[1] = 0;
    uint64_t dlr = 1;
    CHECK(jet_native_call_host(ctx, "dl_iterate_phdr", &da, &dlr) == 0, "iterate");
    CHECK(dlr == 0, "iterate rc");
    CHECK(g_seen_libs >= 1, "seen libs %d", g_seen_libs);
    /* guest_lib may or may not have EH_FRAME; at least phdrs walked */
    fprintf(stderr, "INFO: seen_libs=%d eh=%d tls=%d\n", g_seen_libs, g_seen_eh, g_seen_tls);

    jet_native_destroy(ctx);
    if (g_fail) { fprintf(stderr, "%d FAIL\n", g_fail); return 1; }
    fprintf(stderr, "ALL PASS\n");
    return 0;
}
