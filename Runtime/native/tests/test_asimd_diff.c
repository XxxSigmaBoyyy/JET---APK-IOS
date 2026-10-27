#include "jet_internal.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static int fails;

static void load_inputs(jet_cpu *c) {
    uint8_t buf[64];
    for (int i = 0; i < 64; i++) buf[i] = (uint8_t)(i * 17 + 3);
    memcpy(&c->v[0][0], buf + 0, 16);
    memcpy(&c->v[1][0], buf + 16, 16);
    memcpy(&c->v[2][0], buf + 32, 16);
    memcpy(&c->v[3][0], buf + 48, 16);
}

static void check(const char *tag, jet_cpu *c, const char *exp) {
    char got[40];
    snprintf(got, sizeof got, "%016llx%016llx",
             (unsigned long long)c->v[0][1], (unsigned long long)c->v[0][0]);
    if (strcmp(got, exp) != 0) {
        fprintf(stderr, "FAIL %s\n  got %s\n  exp %s\n", tag, got, exp);
        fails++;
    } else {
        fprintf(stderr, "OK   %s\n", tag);
    }
}

int main(void) {
    jet_ctx *ctx = calloc(1, sizeof *ctx);
    jet_cpu *c = calloc(1, sizeof *c);
    struct { uint32_t raw; const char *tag; const char *exp; } cases[] = {
        {0x4e228420, "add_16b",  "3412f0ceac8a68462402e0be9c7a5836"},
        {0x4ea29c20, "mul_4s",   "6c9ee03105931f89cc50c3017fb6ca99"},
        {0x4ea29420, "mla_4s",   "6f90c100c440bc1446ba1b48b5dbde9c"},
        {0x6ea28c20, "cmeq_4s",  "00000000000000000000000000000000"},
        {0x2e22c020, "umull_8h", "530440c130c0230117840e4907500299"},
        {0x0e22c020, "smull_8h", "2f04c7c130c0230117840e4907500299"},
        {0x2f08a420, "uxtl_8h",  "008a0079006800570046003500240013"},
        {0x0f08a420, "sxtl_8h",  "ff8a0079006800570046003500240013"},
        {0x0e212820, "xtn_8b",   "000000000000000001dfbd9b79573513"},
        {0x2f0ba420, "ushll_8h", "045003c8034002b8023001a801200098"},
        {0x4f3d0420, "sshr_4s",  "02403e1bf9d7b593f14f2d0a08c6a482"},
        {0x6f3d0420, "ushr_4s",  "02403e1b19d7b593114f2d0a08c6a482"},
        {0x0f0d8420, "shrn_8b",  "0000000000000000401bd7934f0ac682"},
        {0x4e823820, "zip1_4s",  "9a8978678a7968575645342346352413"},
        {0x4e821820, "uzp1_4s",  "decdbcab56453423cebdac9b46352413"},
        {0x4e822820, "trn1_4s",  "decdbcabcebdac9b5645342346352413"},
        {0x4e205820, "cnt_16b",  "02010407050604050305030503040203"},
        {0x6ea0b820, "neg_4s",   "edfe0f2131425365758697a9b9cadbed"},
        {0x4ea0b820, "abs_4s",   "1201f0df31425365758697a946352413"},
        {0x4eb1b820, "addv_4s",  "000000000000000000000000b16e29e4"},
        {0x6e621c20, "bsl_16b",  "2201e0efceedacab8ae9686746652423"},
        {0x4e22d420, "fadd_4s",  "221100efdecdbcab9a89786756453423"},
        {0x6e22dc20, "fmul_4s",  "000000016e186f02000000005d0b89a7"},
        {0x4e22cc20, "fmla_4s",  "02f1e0cf6e186f027a6958475d0b89a7"},
    };
    int n = (int)(sizeof cases / sizeof cases[0]);
    for (int i = 0; i < n; i++) {
        load_inputs(c);
        jet_insn ins;
        if (jet_decode(cases[i].raw, &ins) != 0) {
            fprintf(stderr, "FAIL %s decode\n", cases[i].tag);
            fails++; continue;
        }
        if (jet_asimd_exec(c, ctx, &ins) != 0) {
            fprintf(stderr, "FAIL %s exec op=%d\n", cases[i].tag, (int)ins.op);
            fails++; continue;
        }
        check(cases[i].tag, c, cases[i].exp);
    }
    fprintf(stderr, "\n%d / %d failed\n", fails, n);
    free(c); free(ctx);
    return fails ? 1 : 0;
}
