#include <stdalign.h>
/* Build: clang --target=aarch64-linux-gnu -static -O2 -o build/qemu_asimd_diff tests/qemu_asimd_diff.c
 * Run:   qemu-aarch64 ./build/qemu_asimd_diff
 * Prints hex V-reg results for a battery of ASIMD ops for jet to match. */
#include <stdio.h>
#include <stdint.h>
#include <string.h>

static void dump(const char *tag, const uint64_t v[2]) {
    printf("%s %016llx%016llx\n", tag,
           (unsigned long long)v[1], (unsigned long long)v[0]);
}

#define RUN(tag, asm_body) do { \
    uint64_t out[2]; \
    __asm__ __volatile__( \
        "ldr q0, [%1]\n\t" \
        "ldr q1, [%1, #16]\n\t" \
        "ldr q2, [%1, #32]\n\t" \
        "ldr q3, [%1, #48]\n\t" \
        asm_body "\n\t" \
        "str q0, [%0]\n\t" \
        : : "r"(out), "r"(in) : "v0","v1","v2","v3","memory"); \
    dump(tag, out); \
} while (0)

int main(void) {
    /* Input pattern: V0..V3 as 4x 16-byte lanes */
    alignas(16) uint8_t buf[64];
    for (int i = 0; i < 64; i++) buf[i] = (uint8_t)(i * 17 + 3);
    const uint64_t *in = (const uint64_t *)buf;

    RUN("add_16b",  "add v0.16b, v1.16b, v2.16b");
    RUN("mul_4s",   "mul v0.4s, v1.4s, v2.4s");
    RUN("mla_4s",   "mla v0.4s, v1.4s, v2.4s");
    RUN("cmeq_4s",  "cmeq v0.4s, v1.4s, v2.4s");
    RUN("umull_8h", "umull v0.8h, v1.8b, v2.8b");
    RUN("smull_8h", "smull v0.8h, v1.8b, v2.8b");
    RUN("uxtl_8h",  "uxtl v0.8h, v1.8b");
    RUN("sxtl_8h",  "sxtl v0.8h, v1.8b");
    RUN("xtn_8b",   "xtn v0.8b, v1.8h");
    RUN("ushll_8h", "ushll v0.8h, v1.8b, #3");
    RUN("sshr_4s",  "sshr v0.4s, v1.4s, #3");
    RUN("ushr_4s",  "ushr v0.4s, v1.4s, #3");
    RUN("shrn_8b",  "shrn v0.8b, v1.8h, #3");
    RUN("zip1_4s",  "zip1 v0.4s, v1.4s, v2.4s");
    RUN("uzp1_4s",  "uzp1 v0.4s, v1.4s, v2.4s");
    RUN("trn1_4s",  "trn1 v0.4s, v1.4s, v2.4s");
    RUN("cnt_16b",  "cnt v0.16b, v1.16b");
    RUN("neg_4s",   "neg v0.4s, v1.4s");
    RUN("abs_4s",   "abs v0.4s, v1.4s");
    RUN("addv_4s",  "addv s0, v1.4s");
    RUN("bsl_16b",  "bsl v0.16b, v1.16b, v2.16b");
    RUN("fadd_4s",  "fadd v0.4s, v1.4s, v2.4s");
    RUN("fmul_4s",  "fmul v0.4s, v1.4s, v2.4s");
    RUN("fmla_4s",  "fmla v0.4s, v1.4s, v2.4s");
    return 0;
}
