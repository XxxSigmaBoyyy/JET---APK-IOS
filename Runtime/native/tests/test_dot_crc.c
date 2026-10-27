#include "jet_internal.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
static int fails;
static void load(jet_cpu *c) {
    uint8_t buf[48];
    for (int i = 0; i < 48; i++) buf[i] = (uint8_t)(i * 17 + 3);
    memcpy(&c->v[0][0], buf, 16);
    memcpy(&c->v[1][0], buf + 16, 16);
    memcpy(&c->v[2][0], buf + 32, 16);
}
int main(void) {
    jet_ctx *ctx = calloc(1, sizeof *ctx);
    jet_cpu *c = calloc(1, sizeof *c);
    struct { uint32_t raw; const char *tag; const char *exp; } cases[] = {
        {0x6e829420, "udot", "02f2b375beafcc617a6a3fcd362543b9"},
        {0x4e829420, "sdot", "02f1e575beade8617a69a2cd362543b9"},
    };
    for (int k = 0; k < 2; k++) {
        load(c);
        jet_insn ins;
        if (jet_decode(cases[k].raw, &ins) != 0) {
            fprintf(stderr, "FAIL decode %s\n", cases[k].tag); fails++; continue;
        }
        if (jet_asimd_exec(c, ctx, &ins) != 0) {
            fprintf(stderr, "FAIL exec %s op=%d\n", cases[k].tag, (int)ins.op); fails++; continue;
        }
        char got[40];
        snprintf(got, sizeof got, "%016llx%016llx",
                 (unsigned long long)c->v[0][1], (unsigned long long)c->v[0][0]);
        if (strcmp(got, cases[k].exp)) {
            fprintf(stderr, "FAIL %s\n  got %s\n  exp %s\n", cases[k].tag, got, cases[k].exp);
            fails++;
        } else fprintf(stderr, "OK   %s\n", cases[k].tag);
    }
    /* CRC decode + poly (mirrors interp.c) */
    {
        jet_insn ins;
        jet_decode(0x1acb4108, &ins);
        if (ins.op != OP_CRC32 || ins.size != 0 || ins.N != 0) {
            fprintf(stderr, "FAIL crc32b decode op=%d sz=%d N=%d\n", (int)ins.op, ins.size, ins.N);
            fails++;
        } else fprintf(stderr, "OK   crc32b decode\n");
        jet_decode(0x9ac95d08, &ins);
        if (ins.op != OP_CRC32 || ins.size != 3 || ins.N != 1) {
            fprintf(stderr, "FAIL crc32cx decode op=%d sz=%d N=%d\n", (int)ins.op, ins.size, ins.N);
            fails++;
        } else fprintf(stderr, "OK   crc32cx decode\n");
    }
    {
        static const uint32_t poly_crc = 0xEDB88320u, poly_c = 0x82F63B78u;
        struct { uint32_t acc; uint64_t data; int nbytes; int N; uint32_t exp; const char *tag; } tc[] = {
            {0x12345678u, 0xABull, 1, 0, 0x1fc8b738u, "crc32b"},
            {0x12345678u, 0xABull, 1, 1, 0xc0912609u, "crc32cb"},
            {0x12345678u, 0x0123456789ABCDEFull, 8, 0, 0x9b62eadfu, "crc32x"},
        };
        for (int i = 0; i < 3; i++) {
            uint32_t crc = tc[i].acc, poly = tc[i].N ? poly_c : poly_crc;
            uint64_t data = tc[i].data;
            for (int b = 0; b < tc[i].nbytes; b++) {
                crc ^= (uint32_t)((data >> (8 * b)) & 0xff);
                for (int k = 0; k < 8; k++)
                    crc = (crc >> 1) ^ (poly & (uint32_t)-(int32_t)(crc & 1u));
            }
            if (crc != tc[i].exp) {
                fprintf(stderr, "FAIL %s got %08x exp %08x\n", tc[i].tag, crc, tc[i].exp);
                fails++;
            } else fprintf(stderr, "OK   %s poly\n", tc[i].tag);
        }
    }
    fprintf(stderr, "%d fails\n", fails);
    free(c); free(ctx);
    return fails ? 1 : 0;
}
