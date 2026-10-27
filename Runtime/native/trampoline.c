/* trampoline.c - auto-generated host stubs that re-enter the interpreter. */
#include "jet_internal.h"

static jet_ctx *g_active_ctx;
void jet_tramp_set_active(jet_ctx *ctx) { g_active_ctx = ctx; }

static uint64_t tramp_0(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[0].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[0].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_1(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[1].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[1].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_2(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[2].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[2].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_3(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[3].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[3].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_4(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[4].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[4].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_5(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[5].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[5].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_6(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[6].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[6].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_7(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[7].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[7].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_8(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[8].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[8].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_9(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[9].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[9].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_10(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[10].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[10].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_11(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[11].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[11].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_12(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[12].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[12].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_13(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[13].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[13].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_14(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[14].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[14].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_15(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[15].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[15].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_16(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[16].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[16].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_17(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[17].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[17].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_18(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[18].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[18].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_19(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[19].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[19].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_20(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[20].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[20].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_21(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[21].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[21].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_22(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[22].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[22].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_23(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[23].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[23].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_24(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[24].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[24].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_25(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[25].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[25].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_26(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[26].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[26].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_27(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[27].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[27].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_28(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[28].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[28].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_29(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[29].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[29].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_30(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[30].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[30].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_31(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[31].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[31].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_32(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[32].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[32].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_33(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[33].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[33].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_34(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[34].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[34].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_35(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[35].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[35].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_36(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[36].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[36].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_37(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[37].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[37].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_38(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[38].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[38].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_39(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[39].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[39].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_40(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[40].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[40].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_41(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[41].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[41].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_42(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[42].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[42].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_43(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[43].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[43].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_44(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[44].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[44].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_45(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[45].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[45].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_46(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[46].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[46].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_47(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[47].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[47].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_48(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[48].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[48].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_49(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[49].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[49].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_50(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[50].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[50].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_51(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[51].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[51].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_52(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[52].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[52].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_53(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[53].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[53].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_54(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[54].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[54].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_55(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[55].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[55].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_56(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[56].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[56].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_57(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[57].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[57].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_58(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[58].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[58].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_59(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[59].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[59].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_60(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[60].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[60].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_61(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[61].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[61].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_62(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[62].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[62].guest_fn, &a, &r, NULL);
    return r;
}
static uint64_t tramp_63(uint64_t a0, uint64_t a1, uint64_t a2, uint64_t a3,
                          uint64_t a4, uint64_t a5, uint64_t a6, uint64_t a7) {
    jet_ctx *c = g_active_ctx;
    if (!c || !c->tramps[63].used) return 0;
    jet_args a = {0};
    a.x[0]=a0; a.x[1]=a1; a.x[2]=a2; a.x[3]=a3;
    a.x[4]=a4; a.x[5]=a5; a.x[6]=a6; a.x[7]=a7;
    uint64_t r=0; jet_native_call(c, c->tramps[63].guest_fn, &a, &r, NULL);
    return r;
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

jet_trampoline jet_native_trampoline(jet_ctx *ctx, uint64_t guest_fn) {
    init_once();
    g_active_ctx = ctx;
    for (int i = 0; i < 64; i++) {
        if (ctx->tramps[i].used && ctx->tramps[i].guest_fn == guest_fn)
            return (jet_trampoline)table[i];
    }
    for (int i = 0; i < 64; i++) {
        if (!ctx->tramps[i].used) {
            ctx->tramps[i].used = 1;
            ctx->tramps[i].ctx = ctx;
            ctx->tramps[i].guest_fn = guest_fn;
            return (jet_trampoline)table[i];
        }
    }
    jet_set_error(ctx, "trampoline pool exhausted");
    return NULL;
}
