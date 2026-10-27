/* SBFIZ / LDP / LDRSW / LDUR / UXTW — unit + qemu golden for SBFIZ. */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

static int g_fail;
#define CHECK(c, fmt, ...) do { \
    if (!(c)) { fprintf(stderr, "FAIL: " fmt "\n", ##__VA_ARGS__); g_fail++; } \
    else fprintf(stderr, "OK:   " fmt "\n", ##__VA_ARGS__); \
} while (0)

static int write_and_build(void) {
    static const char asm_src[] =
        ".text\n"
        ".globl guest_sbfiz32\nguest_sbfiz32:\n  sbfiz x0, x0, #5, #32\n  ret\n"
        ".globl guest_sbfiz_neg\nguest_sbfiz_neg:\n  mov w0, #0xffffffff\n  sbfiz x0, x0, #5, #32\n  ret\n"
        ".globl guest_ldpsw\nguest_ldpsw:\n  ldpsw x0, x1, [x0]\n  add x0, x0, x1\n  ret\n"
        ".globl guest_ldp_wb_pre\nguest_ldp_wb_pre:\n"
        "  mov x2, x0\n  ldp x0, x1, [x2, #16]!\n  add x0, x0, x1\n  add x0, x0, x2\n  ret\n"
        ".globl guest_ldur_neg\nguest_ldur_neg:\n  add x0, x0, #8\n  ldur x0, [x0, #-8]\n  ret\n"
        ".globl guest_ldrsw\nguest_ldrsw:\n  ldrsw x0, [x0]\n  ret\n"
        ".globl guest_ldrb_uxtw\nguest_ldrb_uxtw:\n  ldrb w0, [x0, w1, uxtw]\n  ret\n"
        ".globl guest_ldp_w_zext\nguest_ldp_w_zext:\n"
        "  movk x0, #0xdead, lsl #48\n  movk x1, #0xbeef, lsl #48\n"
        "  ldp w0, w1, [x2]\n  orr x0, x0, x1\n  ret\n"
        ".globl guest_stp_ldp_post\nguest_stp_ldp_post:\n"
        "  mov x3, x0\n  mov x0, #0x1111\n  mov x1, #0x2222\n"
        "  stp x0, x1, [x3], #16\n  ldp x0, x1, [x3, #-16]\n  add x0, x0, x1\n  ret\n";
    FILE *f = fopen("/tmp/guest_memops.S", "w");
    if (!f) return -1;
    fputs(asm_src, f); fclose(f);
    return system("clang --target=aarch64-linux-gnu -nostdlib -shared -fPIC "
                  "-o /tmp/guest_memops.so /tmp/guest_memops.S 2>/tmp/memops_build.err");
}

static uint64_t call1(jet_ctx *ctx, jet_lib *lib, const char *sym, uint64_t a0) {
    uint64_t fn = jet_native_sym(lib, sym);
    jet_args a = {0}; a.x[0] = a0; uint64_t r = 0;
    if (jet_native_call(ctx, fn, &a, &r, NULL) != 0) return (uint64_t)-1;
    return r;
}

int main(void) {
    if (write_and_build() != 0) {
        fprintf(stderr, "SKIP: build failed:\n");
        system("cat /tmp/memops_build.err");
        return 0;
    }
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    char err[256];
    jet_lib *lib = jet_native_load(ctx, "/tmp/guest_memops.so", err, sizeof err);
    CHECK(lib != NULL, "load: %s", err);

    CHECK(call1(ctx, lib, "guest_sbfiz32", 7) == 224, "sbfiz 7 => 224");
    {
        uint64_t r = call1(ctx, lib, "guest_sbfiz_neg", 0);
        CHECK(r == ~0x1full, "sbfiz -1 => ~0x1f (0x%llx)", (unsigned long long)r);
    }
    {
        int32_t buf[2] = { -1, 2 };
        uint64_t fn = jet_native_sym(lib, "guest_ldpsw");
        jet_args a = {0}; a.x[0] = (uint64_t)(uintptr_t)buf; uint64_t r = 0;
        jet_native_call(ctx, fn, &a, &r, NULL);
        CHECK(r == 1, "ldpsw => 1 (got %lld)", (long long)r);
    }
    {
        int32_t v = -5;
        CHECK(call1(ctx, lib, "guest_ldrsw", (uint64_t)(uintptr_t)&v) == (uint64_t)-5, "ldrsw");
    }
    {
        uint64_t slot[2] = { 0x1111222233334444ull, 0 };
        CHECK(call1(ctx, lib, "guest_ldur_neg", (uint64_t)(uintptr_t)slot) == slot[0], "ldur");
    }
    {
        uint32_t w[2] = { 0x11, 0x22 };
        uint64_t fn = jet_native_sym(lib, "guest_ldp_w_zext");
        jet_args a = {0}; a.x[2] = (uint64_t)(uintptr_t)w; uint64_t r = 0;
        jet_native_call(ctx, fn, &a, &r, NULL);
        CHECK(r == 0x33, "ldp w zext (0x%llx)", (unsigned long long)r);
    }
    {
        uint8_t bytes[8] = {0,1,2,3,4,5,6,7};
        uint64_t fn = jet_native_sym(lib, "guest_ldrb_uxtw");
        jet_args a = {0};
        a.x[0] = (uint64_t)(uintptr_t)bytes;
        a.x[1] = 0xdeadbeef00000003ull;
        uint64_t r = 0;
        jet_native_call(ctx, fn, &a, &r, NULL);
        CHECK(r == 3, "ldrb uxtw => 3");
    }
    {
        uint64_t buf[4] = {0};
        uint64_t fn = jet_native_sym(lib, "guest_stp_ldp_post");
        jet_args a = {0}; a.x[0] = (uint64_t)(uintptr_t)buf; uint64_t r = 0;
        jet_native_call(ctx, fn, &a, &r, NULL);
        CHECK(r == 0x3333, "stp/ldp post => 0x3333 (0x%llx)", (unsigned long long)r);
    }
    /* LDP pre writeback: buf={0,0,10,20} at offsets 0,8,16,24 — load from +16 */
    {
        uint64_t buf[4] = {0, 0, 10, 20};
        uint64_t fn = jet_native_sym(lib, "guest_ldp_wb_pre");
        jet_args a = {0}; a.x[0] = (uint64_t)(uintptr_t)buf; uint64_t r = 0;
        jet_native_call(ctx, fn, &a, &r, NULL);
        /* x0+x1+x2 = 10+20+(buf+16) */
        uint64_t expect = 30 + ((uint64_t)(uintptr_t)buf + 16);
        CHECK(r == expect, "ldp pre wb (got 0x%llx want 0x%llx)",
              (unsigned long long)r, (unsigned long long)expect);
    }

    /* qemu golden */
    system("printf '%s\\n' "
           "'#include <stdio.h>' '#include <stdint.h>' "
           "'int main(){ uint64_t x=7; asm volatile(\"sbfiz %0,%0,#5,#32\":\"+r\"(x));'"
           "'printf(\"%llu\\\\n\",(unsigned long long)x);'"
           "'uint64_t y; uint32_t w=0xffffffffu;'"
           "'asm volatile(\"sbfiz %0,%1,#5,#32\":\"=r\"(y):\"r\"((uint64_t)w));'"
           "'printf(\"%llu\\\\n\",(unsigned long long)y); return 0;}'"
           " > /tmp/q_sbfiz.c");
    if (system("clang --target=aarch64-linux-gnu -static -O0 -o /tmp/q_sbfiz /tmp/q_sbfiz.c 2>/dev/null") == 0) {
        FILE *q = popen("qemu-aarch64 /tmp/q_sbfiz", "r");
        unsigned long long a = 0, b = 0;
        if (q && fscanf(q, "%llu%llu", &a, &b) == 2) {
            CHECK(a == 224, "qemu sbfiz 7");
            CHECK(b == ~0x1full, "qemu sbfiz -1");
        }
        if (q) pclose(q);
    }

    jet_native_destroy(ctx);
    if (g_fail) { fprintf(stderr, "%d FAIL\n", g_fail); return 1; }
    fprintf(stderr, "ALL PASS\n");
    return 0;
}
