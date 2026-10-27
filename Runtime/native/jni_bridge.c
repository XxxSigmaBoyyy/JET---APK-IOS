/* jni_bridge.c - guest JNIEnv/JavaVM → host JNIEnv with full JNI 1.6 coverage.
 * Variadic (.../V) paths marshal Linux AAPCS64 va_list → jvalue[] → host *A.
 * Unimplemented slots abort loudly (name+index); never silent 0.
 */
#include "jet_internal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <limits.h>

#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

/* Minimal JNI types (avoid hard jni.h dependency). Layout matches HotSpot. */
typedef void *jobject;
typedef jobject jclass, jthrowable, jstring, jarray, jweak;
typedef jarray jbooleanArray, jbyteArray, jcharArray, jshortArray;
typedef jarray jintArray, jlongArray, jfloatArray, jdoubleArray, jobjectArray;
typedef uint8_t  jboolean;
typedef int8_t   jbyte;
typedef uint16_t jchar;
typedef int16_t  jshort;
typedef int32_t  jint;
typedef int64_t  jlong;
typedef float    jfloat;
typedef double   jdouble;
typedef jint     jsize;
typedef void    *jmethodID;
typedef void    *jfieldID;
typedef union jvalue {
    jboolean z; jbyte b; jchar c; jshort s;
    jint i; jlong j; jfloat f; jdouble d; jobject l;
} jvalue;
typedef enum { JNIInvalidRefType=0, JNILocalRefType=1, JNIGlobalRefType=2, JNIWeakGlobalRefType=3 } jobjectRefType;

#define JNI_TABLE_SIZE 233
#define JNI_VERSION_1_6 0x00010006
#define JNI_OK 0
#define JNI_COMMIT 1
#define JNI_ABORT  2

#include "jni_names.inc"

/* ---- Linux AArch64 va_list (guest) ---- */
typedef struct {
    void *stack;
    void *gr_top;
    void *vr_top;
    int   gr_offs;
    int   vr_offs;
} guest_va_list;

/* ---- methodID → signature cache (for V/... → A) ---- */
#define MID_CACHE 256
typedef struct {
    jmethodID mid;
    char sig[384];
    char name[128];
    int used;
} mid_ent;
static mid_ent g_mids[MID_CACHE];
static int g_nmids;

static void mid_cache_put(jmethodID mid, const char *name, const char *sig) {
    if (!mid || !sig) return;
    for (int i = 0; i < g_nmids; i++)
        if (g_mids[i].mid == mid) {
            snprintf(g_mids[i].sig, sizeof g_mids[i].sig, "%s", sig);
            if (name) snprintf(g_mids[i].name, sizeof g_mids[i].name, "%s", name);
            return;
        }
    if (g_nmids >= MID_CACHE) return;
    mid_ent *e = &g_mids[g_nmids++];
    e->mid = mid; e->used = 1;
    snprintf(e->sig, sizeof e->sig, "%s", sig);
    snprintf(e->name, sizeof e->name, "%s", name ? name : "");
}
static const char *mid_cache_sig(jmethodID mid) {
    for (int i = 0; i < g_nmids; i++)
        if (g_mids[i].mid == mid) return g_mids[i].sig;
    return NULL;
}

/* Advance one arg from guest va_list; is_fp selects VR vs GR/stack. */
static uint64_t va_next_int(guest_va_list *vl) {
    uint64_t v;
    if (vl->gr_offs < 0 && vl->gr_top) {
        v = *(uint64_t *)((uint8_t *)vl->gr_top + vl->gr_offs);
        vl->gr_offs += 8;
    } else {
        v = *(uint64_t *)vl->stack;
        vl->stack = (uint8_t *)vl->stack + 8;
    }
    return v;
}
static double va_next_fp(guest_va_list *vl) {
    double v;
    if (vl->vr_offs < 0 && vl->vr_top) {
        /* each VR save slot is 16 bytes; low 8 hold the double/float */
        memcpy(&v, (uint8_t *)vl->vr_top + vl->vr_offs, sizeof v);
        vl->vr_offs += 16;
    } else {
        memcpy(&v, vl->stack, sizeof v);
        vl->stack = (uint8_t *)vl->stack + 8;
    }
    return v;
}

/* Parse one JNI arg type from sig at *p; advance *p. Returns 'I','J','F','D','L','[' etc. */
static char parse_arg_type(const char **p) {
    const char *s = *p;
    if (!s || !*s) return 0;
    if (*s == '[') {
        while (*s == '[') s++;
        if (*s == 'L') { while (*s && *s != ';') s++; if (*s == ';') s++; }
        else if (*s) s++;
        *p = s;
        return 'L'; /* array = reference */
    }
    if (*s == 'L') {
        while (*s && *s != ';') s++;
        if (*s == ';') s++;
        *p = s;
        return 'L';
    }
    char t = *s++;
    *p = s;
    return t;
}

static int pack_jvalues_va(guest_va_list *vl, const char *sig, jvalue *out, int maxn) {
    if (!sig || sig[0] != '(') return -1;
    const char *p = sig + 1;
    int n = 0;
    while (*p && *p != ')' && n < maxn) {
        char t = parse_arg_type(&p);
        if (!t) break;
        memset(&out[n], 0, sizeof out[n]);
        switch (t) {
        case 'Z': case 'B': case 'C': case 'S': case 'I':
            out[n].i = (jint)(int32_t)va_next_int(vl); break;
        case 'J':
            out[n].j = (jlong)va_next_int(vl); break;
        case 'F': {
            double d = va_next_fp(vl);
            float f = (float)d;
            out[n].f = f; break;
        }
        case 'D':
            out[n].d = va_next_fp(vl); break;
        case 'L':
            out[n].l = (jobject)(uintptr_t)va_next_int(vl); break;
        default:
            out[n].j = (jlong)va_next_int(vl); break;
        }
        n++;
    }
    return n;
}

/* Pack varargs that already sit in jet_args registers/stack (the '...' form).
 * fixed_n = number of named args after env (obj/mid etc.) so first vararg is a->x[fixed_n]. */
static int pack_jvalues_args(const jet_args *a, int fixed_n, const char *sig,
                             jvalue *out, int maxn) {
    if (!sig || sig[0] != '(') return -1;
    const char *p = sig + 1;
    int n = 0;
    int xi = fixed_n;   /* next x register index for int/ptr */
    int di = 0;         /* next d register for FP — on AAPCS64 variadic FP may use VR;
                         * our trap already captured d[0..] from the call. Use them in order. */
    int si = 0;         /* stack slot after the 8 GPRs if xi would exceed 7 */
    while (*p && *p != ')' && n < maxn) {
        char t = parse_arg_type(&p);
        if (!t) break;
        memset(&out[n], 0, sizeof out[n]);
        if (t == 'F' || t == 'D') {
            double dv = (di < 8) ? a->d[di++] : (a->stack ? *(double*)&a->stack[si++] : 0.0);
            if (t == 'F') out[n].f = (float)dv; else out[n].d = dv;
        } else {
            uint64_t v;
            if (xi < 8) v = a->x[xi++];
            else v = a->stack ? a->stack[si++] : 0;
            if (t == 'J') out[n].j = (jlong)v;
            else if (t == 'L') out[n].l = (jobject)(uintptr_t)v;
            else out[n].i = (jint)(int32_t)v;
        }
        n++;
    }
    return n;
}

/* ---- fake JVM pending exception ---- */
typedef struct {
    char cls[256];
    char msg[512];
    int  pending;
} fake_throwable;

static fake_throwable *fake_pending(jet_ctx *ctx) {
    return (fake_throwable *)ctx->jni_pending; /* stored as pointer */
}

static void fake_set_pending(jet_ctx *ctx, const char *cls, const char *msg) {
    fake_throwable *t = fake_pending(ctx);
    if (!t) {
        t = calloc(1, sizeof *t);
        ctx->jni_pending = t;
    }
    t->pending = 1;
    snprintf(t->cls, sizeof t->cls, "%s", cls ? cls : "java/lang/Throwable");
    snprintf(t->msg, sizeof t->msg, "%s", msg ? msg : "");
}

/* ---- host table access ---- */
static void **host_fns(jet_ctx *ctx) {
    /* host_jnienv is JNIEnv* (= JNINativeInterface** ) */
    void **envp = (void **)ctx->host_jnienv;
    if (!envp || !*envp) return NULL;
    return (void **)*envp;
}
static void *host_env(jet_ctx *ctx) { return ctx->host_jnienv; }

static void jni_abort_unimplemented(jet_ctx *ctx, int idx, int is_vm) {
    const char *name = is_vm
        ? (idx >= 0 && idx < 8 ? jni_vm_names[idx] : "?")
        : (idx >= 0 && idx < 233 ? jni_env_names[idx] : "?");
    char buf[256];
    snprintf(buf, sizeof buf, "JNI %s slot not implemented: %s idx=%d",
             is_vm ? "JavaVM" : "JNIEnv", name, idx);
    jet_log(ctx, 0, "%s", buf);
    fprintf(stderr, "[jet:E] %s\n", buf);
    fflush(stderr);
    abort();
}

/* Guest JNINativeMethod */
typedef struct { uint64_t name, sig, fnPtr; } guest_JNINativeMethod;

typedef struct { char class_name[256]; } fake_jclass;

static uint64_t fake_find_class(jet_ctx *ctx, const char *name) {
    fake_jclass *c = calloc(1, sizeof(*c));
    snprintf(c->class_name, sizeof c->class_name, "%s", name ? name : "");
    return H2G(c);
}

/* Typed host call helpers */
typedef jint       (*fn_i0)(void*);
typedef jint       (*fn_i1p)(void*, void*);
typedef jint       (*fn_i2p)(void*, void*, void*);
typedef jint       (*fn_i1pi)(void*, void*, jint);
typedef void       (*fn_v0)(void*);
typedef void       (*fn_v1p)(void*, void*);
typedef void       (*fn_v2p)(void*, void*, void*);
typedef jobject    (*fn_l0)(void*);
typedef jobject    (*fn_l1p)(void*, void*);
typedef jobject    (*fn_l2p)(void*, void*, void*);
typedef jobject    (*fn_l3p)(void*, void*, void*, void*);
typedef jobject    (*fn_l1pi)(void*, void*, jint);
typedef jobject    (*fn_l2pi)(void*, void*, void*, jint);
typedef jboolean   (*fn_z2p)(void*, void*, void*);
typedef jboolean   (*fn_z0)(void*);
typedef jmethodID  (*fn_m3p)(void*, void*, void*, void*);
typedef jfieldID   (*fn_f3p)(void*, void*, void*, void*);
typedef jsize      (*fn_sz1p)(void*, void*);
typedef void*      (*fn_p2p)(void*, void*, void*);
typedef void*      (*fn_p1p)(void*, void*);
typedef void       (*fn_v3pi)(void*, void*, void*, jint);
typedef void       (*fn_v1p2i1p)(void*, void*, jint, jint, void*);
typedef jint       (*fn_i1p1i)(void*, void*, jint);
typedef jlong      (*fn_j2p)(void*, void*, void*);
typedef jfloat     (*fn_fl2p)(void*, void*, void*);
typedef jdouble    (*fn_d2p)(void*, void*, void*);
typedef jobject    (*fn_l1p1j)(void*, void*, jlong);
typedef jlong      (*fn_j1p)(void*, void*);
typedef jint       (*fn_i1p)(void*, void*);
typedef jobjectRefType (*fn_rt1p)(void*, void*);
typedef jint       (*fn_reg)(void*, void*, void*, jint);
typedef jobject    (*fn_callA)(void*, void*, void*, const jvalue*);
typedef jint       (*fn_callAi)(void*, void*, void*, const jvalue*);
typedef jlong      (*fn_callAj)(void*, void*, void*, const jvalue*);
typedef jfloat     (*fn_callAf)(void*, void*, void*, const jvalue*);
typedef jdouble    (*fn_callAd)(void*, void*, void*, const jvalue*);
typedef void       (*fn_callAv)(void*, void*, void*, const jvalue*);
typedef jobject    (*fn_nvA)(void*, void*, void*, void*, const jvalue*);
typedef jint       (*fn_nvAi)(void*, void*, void*, void*, const jvalue*);
typedef jlong      (*fn_nvAj)(void*, void*, void*, void*, const jvalue*);
typedef jfloat     (*fn_nvAf)(void*, void*, void*, void*, const jvalue*);
typedef jdouble    (*fn_nvAd)(void*, void*, void*, void*, const jvalue*);
typedef void       (*fn_nvAv)(void*, void*, void*, void*, const jvalue*);
typedef jobject    (*fn_newA)(void*, void*, void*, const jvalue*);
typedef void*      (*fn_crit)(void*, void*, void*);
typedef void       (*fn_relcrit)(void*, void*, void*, jint);
typedef jint       (*fn_vm_env)(void*, void**, jint);
typedef jint       (*fn_vm_att)(void*, void**, void*);
typedef jint       (*fn_vm0)(void*);

static const char *resolve_sig(jet_ctx *ctx, jmethodID mid, const char *fallback) {
    (void)ctx;
    const char *s = mid_cache_sig(mid);
    return s ? s : fallback;
}

/* Forward declarations of big dispatcher pieces */
static uint64_t jni_host_dispatch(jet_ctx *ctx, const jet_args *a, int idx);
static uint64_t jni_fake_dispatch(jet_ctx *ctx, const jet_args *a, int idx);
static uint64_t jni_vm_dispatch(jet_ctx *ctx, const jet_args *a, int vi);

static uint64_t jni_trap(jet_ctx *ctx, const jet_args *a, void *user);

int jet_jni_build_tables(jet_ctx *ctx) {
    size_t bytes = 8 + JNI_TABLE_SIZE * 8 + 8 + 8 * 8;
    uint8_t *mem = calloc(1, bytes);
    if (!mem) return -1;
    uint64_t *env_ptr = (uint64_t *)mem;
    uint64_t *table = (uint64_t *)(mem + 8);
    env_ptr[0] = H2G(table);
    ctx->guest_jnienv = env_ptr;

    uint64_t *vm_ptr = (uint64_t *)(mem + 8 + JNI_TABLE_SIZE * 8);
    uint64_t *vm_table = vm_ptr + 1;
    vm_ptr[0] = H2G(vm_table);
    ctx->guest_javavm = vm_ptr;

    /* default emulated cwd */
    if (!ctx->guest_cwd[0])
        snprintf(ctx->guest_cwd, sizeof ctx->guest_cwd, "/data/data/app");

    for (int i = 0; i < JNI_TABLE_SIZE; i++) {
        char name[64];
        snprintf(name, sizeof name, "__jni_env_%d", i);
        jet_native_register_host(ctx, name, jni_trap, (void*)(uintptr_t)i);
        int idx = -1;
        for (int t = 1; t < ctx->ntraps; t++)
            if (ctx->traps[t].name && strcmp(ctx->traps[t].name, name) == 0)
                { idx = t; break; }
        table[i] = jet_stub_addr(ctx, idx);
    }
    for (int i = 0; i < 8; i++) {
        char name[64];
        snprintf(name, sizeof name, "__jni_vm_%d", i);
        jet_native_register_host(ctx, name, jni_trap, (void*)(uintptr_t)(1000 + i));
        int idx = -1;
        for (int t = 1; t < ctx->ntraps; t++)
            if (ctx->traps[t].name && strcmp(ctx->traps[t].name, name) == 0)
                { idx = t; break; }
        vm_table[i] = jet_stub_addr(ctx, idx);
    }
    jet_log(ctx, 2, "JNI tables ready (%d env slots); guest JNIEnv at %p",
            JNI_TABLE_SIZE, (void*)env_ptr);
    return 0;
}

int jet_jni_bind(jet_ctx *ctx, void *host_javavm, void *host_jnienv) {
    ctx->host_javavm = host_javavm;
    ctx->host_jnienv = host_jnienv;
    return 0;
}

void jet_jni_on_register_natives(jet_ctx *ctx, jet_register_cb cb, void *user) {
    ctx->reg_cb = cb;
    ctx->reg_cb_user = user;
}

int jet_native_set_cwd(jet_ctx *ctx, const char *path) {
    if (!ctx || !path || !path[0]) return -1;
    snprintf(ctx->guest_cwd, sizeof ctx->guest_cwd, "%s", path);
    return 0;
}
const char *jet_native_get_cwd(jet_ctx *ctx) {
    return ctx ? ctx->guest_cwd : "";
}

const char *jet_jni_slot_name(int idx) {
    if (idx >= 0 && idx < 233) return jni_env_names[idx];
    return "?";
}

static uint64_t jni_trap(jet_ctx *ctx, const jet_args *a, void *user) {
    int idx = (int)(uintptr_t)user;
    if (idx >= 1000)
        return jni_vm_dispatch(ctx, a, idx - 1000);
    if (ctx->host_jnienv && host_fns(ctx))
        return jni_host_dispatch(ctx, a, idx);
    return jni_fake_dispatch(ctx, a, idx);
}

/* ========== JavaVM invoke interface ========== */
static uint64_t jni_vm_dispatch(jet_ctx *ctx, const jet_args *a, int vi) {
    void *vm = ctx->host_javavm;
    if (vm) {
        void **fns = *(void ***)vm; /* JavaVM* → JNIInvokeInterface* */
        void *hvm = vm;
        switch (vi) {
        case 3: /* DestroyJavaVM */
            return (uint64_t)(int64_t)((fn_vm0)fns[3])(hvm);
        case 4: { /* AttachCurrentThread */
            void **penv = a->x[1] ? (void**)G2H(a->x[1]) : NULL;
            void *args = a->x[2] ? G2H(a->x[2]) : NULL;
            jint rc = ((fn_vm_att)fns[4])(hvm, penv, args);
            if (rc == JNI_OK && penv) {
                /* Replace host JNIEnv* with guest JNIEnv so subsequent guest
                 * calls stay in the trap table. Keep host env in ctx. */
                ctx->host_jnienv = *penv;
                *penv = (void*)(uintptr_t)H2G(ctx->guest_jnienv);
            }
            return (uint64_t)(int64_t)rc;
        }
        case 5: /* DetachCurrentThread */
            return (uint64_t)(int64_t)((fn_vm0)fns[5])(hvm);
        case 6: { /* GetEnv */
            void **penv = a->x[1] ? (void**)G2H(a->x[1]) : NULL;
            jint ver = (jint)a->x[2];
            void *tmp = NULL;
            jint rc = ((fn_vm_env)fns[6])(hvm, &tmp, ver);
            if (rc == JNI_OK) {
                ctx->host_jnienv = tmp;
                if (penv) *penv = (void*)(uintptr_t)H2G(ctx->guest_jnienv);
            }
            return (uint64_t)(int64_t)rc;
        }
        case 7: { /* AttachCurrentThreadAsDaemon */
            void **penv = a->x[1] ? (void**)G2H(a->x[1]) : NULL;
            void *args = a->x[2] ? G2H(a->x[2]) : NULL;
            jint rc = ((fn_vm_att)fns[7])(hvm, penv, args);
            if (rc == JNI_OK && penv) {
                ctx->host_jnienv = *penv;
                *penv = (void*)(uintptr_t)H2G(ctx->guest_jnienv);
            }
            return (uint64_t)(int64_t)rc;
        }
        default:
            jni_abort_unimplemented(ctx, vi, 1);
        }
    }
    /* Fake VM */
    switch (vi) {
    case 3: return 0; /* Destroy */
    case 4: case 7: {
        uint64_t *out = a->x[1] ? (uint64_t*)G2H(a->x[1]) : NULL;
        if (out) *out = H2G(ctx->guest_jnienv);
        return 0;
    }
    case 5: return 0;
    case 6: {
        uint64_t *out = a->x[1] ? (uint64_t*)G2H(a->x[1]) : NULL;
        if (out) *out = H2G(ctx->guest_jnienv);
        return 0;
    }
    default:
        jni_abort_unimplemented(ctx, vi, 1);
    }
    return 0;
}

/* Helper: call host *A variant for instance/static/nonvirtual/new. */
static uint64_t host_call_A(jet_ctx *ctx, int idx, const jet_args *a,
                            jvalue *jv, int is_static, int is_nonvirt, int is_new) {
    void **f = host_fns(ctx); void *e = host_env(ctx);
    /* Map base CallObjectMethodA index families:
       instance: ObjectA=36, BoolA=39, ... VoidA=63  (step 3 from 34)
       nonvirt:  ObjectA=66 ... VoidA=93
       static:   ObjectA=116 ... VoidA=143
       NewObjectA=30
    */
    if (is_new) {
        return (uint64_t)(uintptr_t)((fn_newA)f[30])(e, (jclass)(uintptr_t)a->x[1],
                        (jmethodID)(uintptr_t)a->x[2], jv);
    }
    int kind; /* 0=obj 1=bool 2=byte 3=char 4=short 5=int 6=long 7=float 8=double 9=void */
    int base;
    if (is_nonvirt) {
        base = 66; kind = (idx - 64) / 3;
    } else if (is_static) {
        base = 116; kind = (idx - 114) / 3;
    } else {
        base = 36; kind = (idx - 34) / 3;
    }
    int slot = base + kind * 3; /* should be the A slot; idx may be ./V/A */
    /* Normalize: for any of ./V/A in the triple, use A slot */
    int triple = is_nonvirt ? (idx - 64) : (is_static ? (idx - 114) : (idx - 34));
    kind = triple / 3;
    slot = (is_nonvirt ? 66 : is_static ? 116 : 36) + kind * 3;

    jobject obj = (jobject)(uintptr_t)a->x[1];
    jclass cls = (jclass)(uintptr_t)a->x[is_nonvirt ? 2 : 1];
    jmethodID mid = (jmethodID)(uintptr_t)a->x[is_nonvirt ? 3 : 2];
    if (is_static) { cls = (jclass)(uintptr_t)a->x[1]; mid = (jmethodID)(uintptr_t)a->x[2]; }

    if (is_nonvirt) {
        switch (kind) {
        case 0: return (uint64_t)(uintptr_t)((fn_nvA)f[slot])(e,obj,cls,mid,jv);
        case 1: return (uint64_t)(jboolean)((fn_nvAi)f[slot])(e,obj,cls,mid,jv);
        case 2: return (uint64_t)(int64_t)(jbyte)((fn_nvAi)f[slot])(e,obj,cls,mid,jv);
        case 3: return (uint64_t)(jchar)((fn_nvAi)f[slot])(e,obj,cls,mid,jv);
        case 4: return (uint64_t)(int64_t)(jshort)((fn_nvAi)f[slot])(e,obj,cls,mid,jv);
        case 5: return (uint64_t)(int64_t)((fn_nvAi)f[slot])(e,obj,cls,mid,jv);
        case 6: return (uint64_t)((fn_nvAj)f[slot])(e,obj,cls,mid,jv);
        case 7: { jfloat r=((fn_nvAf)f[slot])(e,obj,cls,mid,jv); uint32_t u; memcpy(&u,&r,4); return u; }
        case 8: { jdouble r=((fn_nvAd)f[slot])(e,obj,cls,mid,jv); uint64_t u; memcpy(&u,&r,8); return u; }
        case 9: ((fn_nvAv)f[slot])(e,obj,cls,mid,jv); return 0;
        }
    } else if (is_static) {
        switch (kind) {
        case 0: return (uint64_t)(uintptr_t)((fn_callA)f[slot])(e,cls,mid,jv);
        case 1: return (uint64_t)(jboolean)((fn_callAi)f[slot])(e,cls,mid,jv);
        case 2: return (uint64_t)(int64_t)(jbyte)((fn_callAi)f[slot])(e,cls,mid,jv);
        case 3: return (uint64_t)(jchar)((fn_callAi)f[slot])(e,cls,mid,jv);
        case 4: return (uint64_t)(int64_t)(jshort)((fn_callAi)f[slot])(e,cls,mid,jv);
        case 5: return (uint64_t)(int64_t)((fn_callAi)f[slot])(e,cls,mid,jv);
        case 6: return (uint64_t)((fn_callAj)f[slot])(e,cls,mid,jv);
        case 7: { jfloat r=((fn_callAf)f[slot])(e,cls,mid,jv); uint32_t u; memcpy(&u,&r,4); return u; }
        case 8: { jdouble r=((fn_callAd)f[slot])(e,cls,mid,jv); uint64_t u; memcpy(&u,&r,8); return u; }
        case 9: ((fn_callAv)f[slot])(e,cls,mid,jv); return 0;
        }
    } else {
        switch (kind) {
        case 0: return (uint64_t)(uintptr_t)((fn_callA)f[slot])(e,obj,mid,jv);
        case 1: return (uint64_t)(jboolean)((fn_callAi)f[slot])(e,obj,mid,jv);
        case 2: return (uint64_t)(int64_t)(jbyte)((fn_callAi)f[slot])(e,obj,mid,jv);
        case 3: return (uint64_t)(jchar)((fn_callAi)f[slot])(e,obj,mid,jv);
        case 4: return (uint64_t)(int64_t)(jshort)((fn_callAi)f[slot])(e,obj,mid,jv);
        case 5: return (uint64_t)(int64_t)((fn_callAi)f[slot])(e,obj,mid,jv);
        case 6: return (uint64_t)((fn_callAj)f[slot])(e,obj,mid,jv);
        case 7: { jfloat r=((fn_callAf)f[slot])(e,obj,mid,jv); uint32_t u; memcpy(&u,&r,4); return u; }
        case 8: { jdouble r=((fn_callAd)f[slot])(e,obj,mid,jv); uint64_t u; memcpy(&u,&r,8); return u; }
        case 9: ((fn_callAv)f[slot])(e,obj,mid,jv); return 0;
        }
    }
    jni_abort_unimplemented(ctx, idx, 0);
    return 0;
}

static uint64_t host_call_family(jet_ctx *ctx, const jet_args *a, int idx,
                                 int is_static, int is_nonvirt, int is_new) {
    jvalue jv[16]; memset(jv, 0, sizeof jv);
    int form; /* 0=. 1=V 2=A */
    int base_idx;
    if (is_new) {
        form = idx - 28; /* 28,29,30 */
        jmethodID mid = (jmethodID)(uintptr_t)a->x[2];
        const char *sig = resolve_sig(ctx, mid, "()V");
        if (form == 2) {
            jvalue *src = a->x[3] ? (jvalue*)G2H(a->x[3]) : jv;
            return host_call_A(ctx, idx, a, src, 0, 0, 1);
        } else if (form == 1) {
            guest_va_list *vl = a->x[3] ? (guest_va_list*)G2H(a->x[3]) : NULL;
            guest_va_list copy = vl ? *vl : (guest_va_list){0};
            pack_jvalues_va(&copy, sig, jv, 16);
            return host_call_A(ctx, idx, a, jv, 0, 0, 1);
        } else {
            pack_jvalues_args(a, 3, sig, jv, 16);
            return host_call_A(ctx, idx, a, jv, 0, 0, 1);
        }
    }
    if (is_nonvirt) base_idx = 64;
    else if (is_static) base_idx = 114;
    else base_idx = 34;
    form = (idx - base_idx) % 3;
    int mid_xi = is_nonvirt ? 3 : 2;
    jmethodID mid = (jmethodID)(uintptr_t)a->x[mid_xi];
    const char *sig = resolve_sig(ctx, mid, "()V");
    if (form == 2) { /* A */
        jvalue *src = a->x[mid_xi+1] ? (jvalue*)G2H(a->x[mid_xi+1]) : jv;
        return host_call_A(ctx, idx, a, src, is_static, is_nonvirt, 0);
    } else if (form == 1) { /* V */
        guest_va_list *vl = a->x[mid_xi+1] ? (guest_va_list*)G2H(a->x[mid_xi+1]) : NULL;
        guest_va_list copy = vl ? *vl : (guest_va_list){0};
        pack_jvalues_va(&copy, sig, jv, 16);
        return host_call_A(ctx, idx, a, jv, is_static, is_nonvirt, 0);
    } else { /* ... */
        pack_jvalues_args(a, mid_xi+1, sig, jv, 16);
        return host_call_A(ctx, idx, a, jv, is_static, is_nonvirt, 0);
    }
}

#include "jni_host_dispatch.inc"
#include "jni_fake_dispatch.inc"

uint64_t jet_jni_guest_env(jet_ctx *ctx) {
    return ctx && ctx->guest_jnienv ? (uint64_t)(uintptr_t)ctx->guest_jnienv : 0;
}

int jet_jni_onload(jet_ctx *ctx, jet_lib *lib, int32_t *version_out) {
    uint64_t sym = jet_native_sym(lib, "JNI_OnLoad");
    if (!sym) {
        jet_set_error(ctx, "JNI_OnLoad not found");
        return -1;
    }
    jet_args a = {0};
    a.x[0] = H2G(ctx->guest_javavm);
    a.x[1] = 0;
    uint64_t ver = 0;
    int rc = jet_native_call(ctx, sym, &a, &ver, NULL);
    if (rc != 0) {
        if (ctx->nreg_log > 0) {
            ver = JNI_VERSION_1_6;
            jet_log(ctx, 1, "JNI_OnLoad soft-ok after error '%s' (%d natives)",
                    ctx->last_error, ctx->nreg_log);
            ctx->last_error[0] = 0;
            rc = 0;
        } else {
            return -1;
        }
    }
    if (version_out) *version_out = (int32_t)ver;
    jet_log(ctx, 2, "JNI_OnLoad => 0x%x (%d natives registered)",
            (unsigned)ver, ctx->nreg_log);
    return rc;
}

/* Direct call into guest JNIEnv trap table (for unit tests). */
uint64_t jet_jni_call_env(jet_ctx *ctx, int idx, const jet_args *a) {
    return jni_trap(ctx, a, (void*)(uintptr_t)idx);
}
