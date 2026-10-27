/* JNI bridge + getcwd unit tests */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdint.h>

static int g_fail;
#define CHECK(cond, fmt, ...) do { \
    if (!(cond)) { fprintf(stderr, "FAIL: " fmt "\n", ##__VA_ARGS__); g_fail++; } \
    else fprintf(stderr, "OK:   " fmt "\n", ##__VA_ARGS__); \
} while (0)

/* JNI indices */
enum {
    IDX_Throw = 13, IDX_ThrowNew = 14, IDX_ExceptionOccurred = 15,
    IDX_ExceptionDescribe = 16, IDX_ExceptionClear = 17, IDX_ExceptionCheck = 228,
    IDX_FindClass = 6, IDX_GetMethodID = 33,
    IDX_CallIntMethod = 49, IDX_CallIntMethodV = 50, IDX_CallIntMethodA = 51,
    IDX_NewStringUTF = 167
};

typedef struct {
    void *stack; void *gr_top; void *vr_top; int gr_offs; int vr_offs;
} guest_va_list;

typedef union {
    uint8_t z; int8_t b; uint16_t c; int16_t s;
    int32_t i; int64_t j; float f; double d; void *l;
} jvalue;

static void test_throw_new(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    CHECK(ctx != NULL, "create ctx");
    jet_args a = {0};
    /* FindClass */
    char clsname[] = "org/telegram/SQLite/SQLiteException";
    a.x[1] = (uint64_t)(uintptr_t)clsname;
    uint64_t cls = jet_jni_call_env(ctx, IDX_FindClass, &a);
    CHECK(cls != 0, "FindClass returns non-null");
    /* ThrowNew */
    char msg[] = "unable to open database file";
    memset(&a, 0, sizeof a);
    a.x[1] = cls; a.x[2] = (uint64_t)(uintptr_t)msg;
    uint64_t rc = jet_jni_call_env(ctx, IDX_ThrowNew, &a);
    CHECK(rc == 0, "ThrowNew returns 0");
    /* ExceptionCheck */
    memset(&a, 0, sizeof a);
    CHECK(jet_jni_call_env(ctx, IDX_ExceptionCheck, &a) == 1, "ExceptionCheck after ThrowNew");
    /* ExceptionOccurred */
    uint64_t thr = jet_jni_call_env(ctx, IDX_ExceptionOccurred, &a);
    CHECK(thr != 0, "ExceptionOccurred non-null");
    /* ExceptionDescribe (no crash) */
    jet_jni_call_env(ctx, IDX_ExceptionDescribe, &a);
    /* Clear */
    jet_jni_call_env(ctx, IDX_ExceptionClear, &a);
    CHECK(jet_jni_call_env(ctx, IDX_ExceptionCheck, &a) == 0, "ExceptionCheck after Clear");
    jet_native_destroy(ctx);
}

static void test_no_silent_stubs(void) {
    /* Known unimplemented in fake mode: DefineClass=5 */
    CHECK(strcmp(jet_jni_slot_name(14), "ThrowNew") == 0, "slot 14 name ThrowNew");
    CHECK(strcmp(jet_jni_slot_name(228), "ExceptionCheck") == 0, "slot 228 name");
    pid_t pid = fork();
    if (pid == 0) {
        jet_config cfg = {0};
        jet_ctx *ctx = jet_native_create(&cfg);
        jet_args a = {0};
        /* DefineClass is not implemented in fake → must abort */
        jet_jni_call_env(ctx, 5, &a);
        _exit(0); /* should not reach */
    }
    int st = 0; waitpid(pid, &st, 0);
    CHECK(WIFSIGNALED(st) && (WTERMSIG(st) == SIGABRT || WTERMSIG(st) == SIGSEGV),
          "unimplemented slot aborts (sig=%d)", WIFSIGNALED(st)?WTERMSIG(st):-1);
}

static void test_callint_va(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    jet_args a = {0};
    /* GetMethodID with mixed sig */
    char name[] = "mix"; char sig[] = "(IDLjava/lang/Object;)I";
    char cls_s[] = "T";
    a.x[1] = jet_jni_call_env(ctx, IDX_FindClass, &(jet_args){ .x={0,(uint64_t)(uintptr_t)cls_s}});
    a.x[2] = (uint64_t)(uintptr_t)name;
    a.x[3] = (uint64_t)(uintptr_t)sig;
    uint64_t mid = jet_jni_call_env(ctx, IDX_GetMethodID, &a);
    CHECK(mid != 0, "GetMethodID");

    /* CallIntMethodA with jvalue[3] = {42, 3.5, obj} */
    jvalue jv[3]; memset(jv,0,sizeof jv);
    jv[0].i = 42; jv[1].d = 3.5; jv[2].l = (void*)(uintptr_t)0x1;
    memset(&a, 0, sizeof a);
    a.x[1] = 0x1111; a.x[2] = mid; a.x[3] = (uint64_t)(uintptr_t)jv;
    uint64_t r = jet_jni_call_env(ctx, IDX_CallIntMethodA, &a);
    CHECK(r == (uint64_t)(42 + 3 + 1), "CallIntMethodA mixed args => %llu", (unsigned long long)r);

    /* CallIntMethodV via guest va_list in GR area */
    uint64_t gr[8];
    gr[0] = 7;                 /* int */
    /* double in VR */
    double dv = 2.0;
    uint8_t vr[16*8]; memset(vr,0,sizeof vr);
    memcpy(vr, &dv, 8);
    uint64_t obj = 0x55;
    /* After consuming int from GR, next int/ref also from GR */
    gr[1] = obj; /* but wait — double comes from VR, then object from GR */
    /* Rebuild: first int GR, then double VR, then object GR */
    guest_va_list vl;
    memset(&vl, 0, sizeof vl);
    vl.gr_top = (uint8_t*)gr + 64; /* top */
    vl.gr_offs = -64;              /* start at first slot */
    /* Put int at gr_top-64, object will be at gr_top-56 after int consumed —
       but we need object AFTER double. So: gr[0]=int, gr[1]=obj is correct
       if double comes from VR. */
    gr[0] = 7; gr[1] = obj;
    vl.vr_top = vr + 16*8;
    vl.vr_offs = -16*8;
    memset(&a, 0, sizeof a);
    a.x[1] = 0x2222; a.x[2] = mid; a.x[3] = (uint64_t)(uintptr_t)&vl;
    r = jet_jni_call_env(ctx, IDX_CallIntMethodV, &a);
    CHECK(r == (uint64_t)(7 + 2 + 1), "CallIntMethodV mixed => %llu", (unsigned long long)r);
    jet_native_destroy(ctx);
}

static void test_getcwd(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    CHECK(jet_native_set_cwd(ctx, "/data/data/org.telegram.messenger") == 0, "set_cwd");
    CHECK(strcmp(jet_native_get_cwd(ctx), "/data/data/org.telegram.messenger") == 0, "get_cwd");

    char buf[128];
    jet_args a = {0};
    uint64_t ret = 0;
    a.x[0] = (uint64_t)(uintptr_t)buf;
    a.x[1] = sizeof buf;
    CHECK(jet_native_call_host(ctx, "getcwd", &a, &ret) == 0, "call getcwd trap");
    CHECK(ret == a.x[0], "getcwd returns buf");
    CHECK(strcmp(buf, "/data/data/org.telegram.messenger") == 0, "getcwd buf content");

    a.x[0] = 0; a.x[1] = 0; ret = 0;
    CHECK(jet_native_call_host(ctx, "getcwd", &a, &ret) == 0, "getcwd NULL/0");
    CHECK(ret != 0, "getcwd allocated");
    CHECK(strcmp((char*)(uintptr_t)ret, "/data/data/org.telegram.messenger") == 0, "alloc content");
    free((void*)(uintptr_t)ret);

    char tiny[4];
    a.x[0] = (uint64_t)(uintptr_t)tiny; a.x[1] = sizeof tiny; ret = 1;
    CHECK(jet_native_call_host(ctx, "getcwd", &a, &ret) == 0, "getcwd tiny call");
    CHECK(ret == 0, "getcwd ERANGE returns NULL");

    jet_native_destroy(ctx);
}

int main(void) {
    test_throw_new();
    test_no_silent_stubs();
    test_callint_va();
    test_getcwd();
    if (g_fail) { fprintf(stderr, "\n%d FAIL\n", g_fail); return 1; }
    fprintf(stderr, "\nALL PASS\n");
    return 0;
}
