/* Build as aarch64 ELF, run under qemu-aarch64; compare selected FP ops. */
#include <stdio.h>
#include <stdint.h>
#include <string.h>

static double fadd(double a, double b) { return a + b; }
static double fmul(double a, double b) { return a * b; }
static double fdiv(double a, double b) { return a / b; }
static float fadds(float a, float b) { return a + b; }

int main(void) {
    double a = 1.5, b = 2.25;
    double r1 = fadd(a, b);
    double r2 = fmul(a, b);
    double r3 = fdiv(a, b);
    float  r4 = fadds(1.25f, 3.5f);
    printf("fadd=%a fmul=%a fdiv=%a fadds=%a\n", r1, r2, r3, r4);
    /* Also print bit patterns for interpreter oracle */
    uint64_t u1,u2,u3; uint32_t u4;
    memcpy(&u1,&r1,8); memcpy(&u2,&r2,8); memcpy(&u3,&r3,8); memcpy(&u4,&r4,4);
    printf("bits %llx %llx %llx %x\n",
           (unsigned long long)u1, (unsigned long long)u2,
           (unsigned long long)u3, u4);
    return 0;
}
