/* v10.2: real fs traps + bionic netdb. Host-side unit checks + guest-level runs via tests/guest_lib.so. */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "jet_native.h"
#include "jet_internal.h"
#include "darwin_abi.h"
#include "netdb_abi.h"
#include <errno.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>

int jet_trap_find(jet_ctx *ctx, const char *name);   /* ctx.c */
static int fails, oks;
#define CHECK(c, ...) do { if (!(c)) { fprintf(stderr, "FAIL %s:%d: ", __func__, __LINE__); \
    fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); fails++; } else oks++; } while (0)

static void t_darwin_maps(void) {
    CHECK(jet_xl_utime_nsec_l2d((1L << 30) - 1) == -1, "UTIME_NOW l2d");
    CHECK(jet_xl_utime_nsec_l2d((1L << 30) - 2) == -2, "UTIME_OMIT l2d");
    CHECK(jet_xl_utime_nsec_l2d(5000) == 5000 && jet_xl_utime_nsec_l2d(0) == 0, "plain nsec");
    CHECK(jet_xl_renameat2_flags_l2d(0) == 0 && jet_xl_renameat2_flags_l2d(1) == 4, "NOREPLACE -> RENAME_EXCL");
    CHECK(jet_xl_renameat2_flags_l2d(2) == -1 && jet_xl_renameat2_flags_l2d(4) == -1 && jet_xl_renameat2_flags_l2d(3) == -1,
          "EXCHANGE/WHITEOUT rejected");
}
static void t_netdb_maps(void) {
    CHECK(jet_gai_err_h2g(0) == 0, "0");
    CHECK(jet_gai_err_h2g(EAI_NONAME) == 8 && jet_gai_err_h2g(EAI_AGAIN) == 2 && jet_gai_err_h2g(EAI_FAIL) == 4,
          "NONAME/AGAIN/FAIL");
    CHECK(jet_gai_err_h2g(EAI_FAMILY) == 5 && jet_gai_err_h2g(EAI_SOCKTYPE) == 10 && jet_gai_err_h2g(EAI_SERVICE) == 9,
          "FAMILY/SOCKTYPE/SERVICE");
    CHECK(jet_gai_err_h2g(EAI_MEMORY) == 6 && jet_gai_err_h2g(EAI_SYSTEM) == 11 && jet_gai_err_h2g(EAI_BADFLAGS) == 3,
          "MEMORY/SYSTEM/BADFLAGS");
#ifdef EAI_OVERFLOW
    CHECK(jet_gai_err_h2g(EAI_OVERFLOW) == 14, "OVERFLOW");
#endif
    CHECK(jet_gai_flags_g2h(8) == AI_NUMERICSERV && jet_gai_flags_g2h(4) == AI_NUMERICHOST
          && jet_gai_flags_g2h(1 | 2) == (AI_PASSIVE | AI_CANONNAME) && jet_gai_flags_g2h(0x400) == AI_ADDRCONFIG,
          "AI flags g2h");
    CHECK(jet_ni_flags_g2h(2 | 8) == (NI_NUMERICHOST | NI_NUMERICSERV) && jet_ni_flags_g2h(0x10) == NI_DGRAM
          && jet_ni_flags_g2h(4) == NI_NAMEREQD && jet_ni_flags_g2h(1) == NI_NOFQDN, "NI flags g2h");
    CHECK(!strcmp(jet_gai_strerror_guest(14), "Argument buffer overflow")
          && !strcmp(jet_gai_strerror_guest(-1), "Unknown error") && !strcmp(jet_gai_strerror_guest(15), "Unknown error"),
          "strerror table bounds");
    /* direct: layout of a result node */
    jet_guest_addrinfo h; memset(&h, 0, sizeof h); h.ai_flags = 4 | 8; h.ai_family = 2; h.ai_socktype = 1;
    uint64_t res = 0;
    CHECK(jet_getaddrinfo_guest("10.1.2.3", "65535", &h, &res) == 0 && res, "direct gai");
    if (res) {
        jet_guest_addrinfo *g = (jet_guest_addrinfo *)(uintptr_t)res;
        const uint8_t *sa = (const uint8_t *)(uintptr_t)g->ai_addr;
        CHECK(g->ai_family == 2 && g->ai_protocol == 6 && g->ai_addrlen == 16 && g->ai_flags == (4 | 8), "node fields");
        CHECK(sa == (const uint8_t *)g + 48 && sa[0] == 2 && sa[1] == 0 && sa[2] == 0xff && sa[3] == 0xff
              && sa[4] == 10 && sa[7] == 3, "guest sockaddr_in (u16 family, BE port)");
        jet_freeaddrinfo_guest(res);
    }
    jet_freeaddrinfo_guest(0);
}
static void t_guest(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    char err[256];
    jet_lib *lib = ctx ? jet_native_load(ctx, "tests/guest_lib.so", err, sizeof err) : NULL;
    CHECK(lib, "load guest_lib");
    if (!lib) return;
    char dir[] = "/tmp/jet_v102_XXXXXX";
    CHECK(mkdtemp(dir) != NULL, "mkdtemp");
    uint64_t sym = jet_native_sym(lib, "guest_fs_ops"), ret = 777;
    jet_args aa = {0}; aa.x[0] = (uint64_t)(uintptr_t)dir;
    CHECK(sym && jet_native_call(ctx, sym, &aa, &ret, NULL) == 0, "guest_fs_ops call: %s", jet_native_last_error(ctx));
    CHECK((int)ret == 0, "guest_fs_ops failed at step %d (host errno %d)", (int)ret, errno);
    CHECK(rmdir(dir) == 0, "temp dir left empty by guest (errno %d)", errno);
    sym = jet_native_sym(lib, "guest_netdb"); ret = 777;
    CHECK(sym && jet_native_call(ctx, sym, NULL, &ret, NULL) == 0, "guest_netdb call: %s", jet_native_last_error(ctx));
    CHECK((int)ret == 0, "guest_netdb failed at step %d", (int)ret);
    /* no stub left for these names: the first registered trap must be the real one */
    static const char *real[] = { "mkdir","mkdirat","unlink","unlinkat","rename","renameat","renameat2","rmdir",
        "symlink","symlinkat","readlink","readlinkat","link","linkat","access","faccessat","chmod","fchmod",
        "fchmodat","truncate","ftruncate","utimensat","futimens","fsync","fdatasync","remove",
        "getaddrinfo","freeaddrinfo","getnameinfo","gai_strerror" };
    for (size_t i = 0; i < sizeof real / sizeof *real; i++) {
        int n = 0;
        for (int k = 1; k < ctx->ntraps; k++) if (ctx->traps[k].name && !strcmp(ctx->traps[k].name, real[i])) n++;
        CHECK(jet_trap_find(ctx, real[i]) > 0 && n == 1, "trap %s registered exactly once (n=%d)", real[i], n);
    }
    jet_native_destroy(ctx);
}
int main(void) {
    t_darwin_maps(); t_netdb_maps(); t_guest();
    printf("test_v102_fs_netdb: %d ok, %d fail\n", oks, fails);
    printf(fails ? "FAILED\n" : "ALL PASS\n");
    return fails ? 1 : 0;
}
