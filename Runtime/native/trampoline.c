/* trampoline.c - auto-generated host stubs that re-enter the interpreter. */
#include "jet_internal.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <stdlib.h>

static jet_ctx *g_active_ctx;
void jet_tramp_set_active(jet_ctx *ctx) { g_active_ctx = ctx; }

/* Capture host x8 (AAPCS64 IPRIN) before the C prologue can clobber it.
 * On Apple the host ABI reserves x18, but guest x18 is interpreter state and
 * is restored by jet_native_call_ex's full register snapshot — do not alias. */
static inline uint64_t tramp_read_x8(void) {
    uint64_t v;
#if defined(__aarch64__)
    __asm__ volatile("mov %0, x8" : "=r"(v));
#else
    v = 0; /* non-a64 host (qemu-user builds run guest a64 under interpreter) */
#endif
    return v;
}
static inline void tramp_fill_args(jet_args *a,
        uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
        uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    a->x[0]=a0; a->x[1]=a1; a->x[2]=a2; a->x[3]=a3;
    a->x[4]=a4; a->x[5]=a5; a->x[6]=a6; a->x[7]=a7;
    a->x8 = tramp_read_x8();
}

static int stats_wanted(jet_ctx *c) {
    if (c->stats_enabled) return 1;
    static int once, en;
    if (!once) {
        const char *e = getenv("JET_NATIVE_STATS");
        en = (e && e[0] == '1');
        once = 1;
    }
    if (en) c->stats_enabled = 1;
    return en;
}

static uint64_t tramp_enter(int idx,
        uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
        uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || idx < 0 || idx >= JET_TRAMP_POOL || !c->tramps[idx].used) return 0;
    jet_loader_push(c, c->tramps[idx].class_loader);
    jet_args a = {0};
    tramp_fill_args(&a, a0, a1, a2, a3, a4, a5, a6, a7);
    uint64_t r = 0;
    int do_stats = stats_wanted(c);
    uint64_t insn0 = 0;
    struct timespec t0, t1;
    if (do_stats) {
        insn0 = c->insn_count;
        clock_gettime(CLOCK_MONOTONIC, &t0);
    }
    jet_native_call(c, c->tramps[idx].guest_fn, &a, &r, NULL);
    if (do_stats) {
        clock_gettime(CLOCK_MONOTONIC, &t1);
        uint64_t ns = (uint64_t)(t1.tv_sec - t0.tv_sec) * 1000000000ull
                    + (uint64_t)(t1.tv_nsec - t0.tv_nsec);
        uint64_t di = c->insn_count - insn0;
        if (!c->native_stats[idx].used) {
            c->native_stats[idx].used = 1;
            snprintf(c->native_stats[idx].key, sizeof c->native_stats[idx].key,
                     "%s.%s%s",
                     c->tramps[idx].class_name[0] ? c->tramps[idx].class_name : "?",
                     c->tramps[idx].method_name[0] ? c->tramps[idx].method_name : "?",
                     c->tramps[idx].method_sig);
        }
        c->native_stats[idx].calls++;
        c->native_stats[idx].insn += di;
        c->native_stats[idx].wall_ns += ns;
    }
    jet_loader_pop(c);
    return r;
}

static uint64_t tramp_0(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(0, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_1(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(1, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_2(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(2, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_3(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(3, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_4(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(4, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_5(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(5, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_6(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(6, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_7(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(7, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_8(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(8, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_9(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(9, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_10(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(10, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_11(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(11, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_12(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(12, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_13(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(13, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_14(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(14, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_15(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(15, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_16(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(16, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_17(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(17, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_18(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(18, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_19(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(19, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_20(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(20, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_21(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(21, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_22(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(22, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_23(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(23, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_24(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(24, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_25(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(25, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_26(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(26, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_27(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(27, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_28(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(28, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_29(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(29, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_30(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(30, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_31(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(31, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_32(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(32, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_33(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(33, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_34(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(34, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_35(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(35, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_36(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(36, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_37(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(37, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_38(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(38, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_39(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(39, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_40(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(40, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_41(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(41, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_42(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(42, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_43(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(43, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_44(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(44, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_45(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(45, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_46(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(46, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_47(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(47, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_48(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(48, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_49(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(49, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_50(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(50, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_51(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(51, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_52(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(52, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_53(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(53, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_54(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(54, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_55(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(55, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_56(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(56, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_57(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(57, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_58(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(58, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_59(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(59, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_60(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(60, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_61(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(61, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_62(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(62, a0, a1, a2, a3, a4, a5, a6, a7);
}
static uint64_t tramp_63(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    return tramp_enter(63, a0, a1, a2, a3, a4, a5, a6, a7);
}

typedef uint64_t (*tn)(uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t,uint64_t);
static tn table[64];
static void init_once(void) {
    if (table[0]) return;
    table[0] = tramp_0;
    table[1] = tramp_1;
    table[2] = tramp_2;
    table[3] = tramp_3;
    table[4] = tramp_4;
    table[5] = tramp_5;
    table[6] = tramp_6;
    table[7] = tramp_7;
    table[8] = tramp_8;
    table[9] = tramp_9;
    table[10] = tramp_10;
    table[11] = tramp_11;
    table[12] = tramp_12;
    table[13] = tramp_13;
    table[14] = tramp_14;
    table[15] = tramp_15;
    table[16] = tramp_16;
    table[17] = tramp_17;
    table[18] = tramp_18;
    table[19] = tramp_19;
    table[20] = tramp_20;
    table[21] = tramp_21;
    table[22] = tramp_22;
    table[23] = tramp_23;
    table[24] = tramp_24;
    table[25] = tramp_25;
    table[26] = tramp_26;
    table[27] = tramp_27;
    table[28] = tramp_28;
    table[29] = tramp_29;
    table[30] = tramp_30;
    table[31] = tramp_31;
    table[32] = tramp_32;
    table[33] = tramp_33;
    table[34] = tramp_34;
    table[35] = tramp_35;
    table[36] = tramp_36;
    table[37] = tramp_37;
    table[38] = tramp_38;
    table[39] = tramp_39;
    table[40] = tramp_40;
    table[41] = tramp_41;
    table[42] = tramp_42;
    table[43] = tramp_43;
    table[44] = tramp_44;
    table[45] = tramp_45;
    table[46] = tramp_46;
    table[47] = tramp_47;
    table[48] = tramp_48;
    table[49] = tramp_49;
    table[50] = tramp_50;
    table[51] = tramp_51;
    table[52] = tramp_52;
    table[53] = tramp_53;
    table[54] = tramp_54;
    table[55] = tramp_55;
    table[56] = tramp_56;
    table[57] = tramp_57;
    table[58] = tramp_58;
    table[59] = tramp_59;
    table[60] = tramp_60;
    table[61] = tramp_61;
    table[62] = tramp_62;
    table[63] = tramp_63;
}

jet_trampoline jet_native_trampoline_ex(jet_ctx *ctx, uint64_t guest_fn,
                                        void *class_loader, const char *class_name) {
    init_once();
    g_active_ctx = ctx;
    int limit = 64; /* table[] has 64 entries */
    for (int i = 0; i < limit; i++) {
        if (ctx->tramps[i].used && ctx->tramps[i].guest_fn == guest_fn) {
            if (class_loader) ctx->tramps[i].class_loader = class_loader;
            if (class_name && class_name[0])
                snprintf(ctx->tramps[i].class_name, sizeof ctx->tramps[i].class_name, "%s", class_name);
            return (jet_trampoline)table[i];
        }
    }
    for (int i = 0; i < limit; i++) {
        if (!ctx->tramps[i].used) {
            ctx->tramps[i].used = 1;
            ctx->tramps[i].ctx = ctx;
            ctx->tramps[i].guest_fn = guest_fn;
            ctx->tramps[i].class_loader = class_loader;
            if (class_name)
                snprintf(ctx->tramps[i].class_name, sizeof ctx->tramps[i].class_name, "%s", class_name);
            else
                ctx->tramps[i].class_name[0] = 0;
            return (jet_trampoline)table[i];
        }
    }
    jet_set_error(ctx, "trampoline pool exhausted");
    return NULL;
}
jet_trampoline jet_native_trampoline(jet_ctx *ctx, uint64_t guest_fn) {
    return jet_native_trampoline_ex(ctx, guest_fn, NULL, NULL);
}
