/* jni_bridge.c - guest-visible JNIEnv/JavaVM with trap stubs → host JNI. */
#include "jet_internal.h"
typedef int32_t jsize;

/* Minimal JNIEnv function table size (Android/OpenJDK ≈ 233+ pointers). */
#define JNI_TABLE_SIZE 256
#define JNI_VERSION_1_6 0x00010006

/* Indices into JNINativeInterface (subset we implement). */
enum {
    JNI_IDX_GetVersion = 4,
    JNI_IDX_FindClass = 6,
    JNI_IDX_GetSuperclass = 10,
    JNI_IDX_ExceptionCheck = 228,
    JNI_IDX_ExceptionClear = 17,
    JNI_IDX_ExceptionOccurred = 15,
    JNI_IDX_NewGlobalRef = 21,
    JNI_IDX_DeleteGlobalRef = 22,
    JNI_IDX_DeleteLocalRef = 23,
    JNI_IDX_NewLocalRef = 155,
    JNI_IDX_IsSameObject = 24,
    JNI_IDX_NewObject = 28,          /* variadic — stub */
    JNI_IDX_GetObjectClass = 31,
    JNI_IDX_IsInstanceOf = 32,
    JNI_IDX_GetMethodID = 33,
    JNI_IDX_GetFieldID = 94,
    JNI_IDX_GetStaticMethodID = 113,
    JNI_IDX_GetStaticFieldID = 144,
    JNI_IDX_NewStringUTF = 167,
    JNI_IDX_GetStringUTFChars = 169,
    JNI_IDX_ReleaseStringUTFChars = 170,
    JNI_IDX_GetStringUTFLength = 168,
    JNI_IDX_GetArrayLength = 171,
    JNI_IDX_NewByteArray = 176,
    JNI_IDX_GetByteArrayElements = 184,
    JNI_IDX_ReleaseByteArrayElements = 192,
    JNI_IDX_GetByteArrayRegion = 200,
    JNI_IDX_SetByteArrayRegion = 208,
    JNI_IDX_RegisterNatives = 215,
    JNI_IDX_UnregisterNatives = 216,
    JNI_IDX_GetJavaVM = 219,
    JNI_IDX_CallStaticObjectMethod = 114,
    JNI_IDX_CallStaticObjectMethodV = 115,
    JNI_IDX_CallStaticObjectMethodA = 116,
    JNI_IDX_NewDirectByteBuffer = 229,
    JNI_IDX_GetDirectBufferAddress = 230,
    JNI_IDX_GetDirectBufferCapacity = 231,
    JNI_IDX_GetObjectRefType = 232
};

/* Guest JNINativeMethod { name*, sig*, fnPtr } */
typedef struct { uint64_t name, sig, fnPtr; } guest_JNINativeMethod;

/* Forward: generic JNI trap. user = (void*)(uintptr_t)index */
static uint64_t jni_trap(jet_ctx *ctx, const jet_args *a, void *user);

int jet_jni_build_tables(jet_ctx *ctx) {
    /* Allocate guest JNIEnv: pointer to function table, then table itself. */
    /* Layout: [JNIEnv*][JNIEnv table x256][JavaVM*][JavaVM table x8] */
    size_t bytes = 8 + JNI_TABLE_SIZE * 8 + 8 + 8 * 8;
    uint8_t *mem = calloc(1, bytes);
    if (!mem) return -1;
    uint64_t *env_ptr = (uint64_t *)mem;               /* JNIEnv (= &table) */
    uint64_t *table = (uint64_t *)(mem + 8);
    env_ptr[0] = H2G(table);
    ctx->guest_jnienv = env_ptr; /* guest sees &table via *env */

    uint64_t *vm_ptr = (uint64_t *)(mem + 8 + JNI_TABLE_SIZE * 8);
    uint64_t *vm_table = vm_ptr + 1;
    vm_ptr[0] = H2G(vm_table);
    ctx->guest_javavm = vm_ptr;

    /* Register one trap per interesting JNI slot; fill rest with stub0-like. */
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
    /* JavaVM functions: DestroyJavaVM=3, AttachCurrentThread=4, Detach=5, GetEnv=6 */
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
    jet_log(ctx, 2, "JNI tables ready; guest JNIEnv at %p", (void*)env_ptr);
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

/* Fake host JNIEnv for standalone tests (when no real JVM). */
typedef struct {
    char class_name[256];
} fake_jclass;

static uint64_t fake_find_class(jet_ctx *ctx, const char *name) {
    fake_jclass *c = calloc(1, sizeof(*c));
    snprintf(c->class_name, sizeof c->class_name, "%s", name);
    jet_log(ctx, 2, "FindClass(%s) -> %p", name, (void*)c);
    return H2G(c);
}

static uint64_t jni_trap(jet_ctx *ctx, const jet_args *a, void *user) {
    int idx = (int)(uintptr_t)user;
    /* a->x[0] is JNIEnv* (guest) for env calls */

    if (idx >= 1000) {
        /* JavaVM */
        int vi = idx - 1000;
        if (vi == 6) { /* GetEnv */
            /* jint GetEnv(JavaVM*, void**, jint) */
            uint64_t *out = (uint64_t*)G2H(a->x[1]);
            if (out) *out = H2G(ctx->guest_jnienv);
            return 0; /* JNI_OK */
        }
        if (vi == 4) { /* AttachCurrentThread */
            uint64_t *out = (uint64_t*)G2H(a->x[1]);
            if (out) *out = H2G(ctx->guest_jnienv);
            return 0;
        }
        return 0;
    }

    switch (idx) {
    case JNI_IDX_GetVersion:
        return JNI_VERSION_1_6;
    case JNI_IDX_FindClass: {
        const char *name = (const char*)G2H(a->x[1]);
        if (ctx->host_jnienv) {
            /* Would call host — for now also fake */
        }
        return fake_find_class(ctx, name ? name : "");
    }
    case JNI_IDX_ExceptionCheck:
        return 0;
    case JNI_IDX_ExceptionClear:
    case JNI_IDX_ExceptionOccurred:
        return 0;
    case JNI_IDX_NewGlobalRef:
    case JNI_IDX_NewLocalRef:
        return a->x[1];
    case JNI_IDX_DeleteGlobalRef:
    case JNI_IDX_DeleteLocalRef:
        return 0;
    case JNI_IDX_IsSameObject:
        return a->x[1] == a->x[2];
    case JNI_IDX_GetObjectClass:
        return fake_find_class(ctx, "java/lang/Object");
    case JNI_IDX_IsInstanceOf:
        return 1;
    case JNI_IDX_GetMethodID:
    case JNI_IDX_GetStaticMethodID:
    case JNI_IDX_GetFieldID:
    case JNI_IDX_GetStaticFieldID: {
        /* Return a non-null fake ID */
        static uint64_t id_gen = 0x1000;
        return ++id_gen;
    }
    case JNI_IDX_NewStringUTF: {
        const char *utf = (const char*)G2H(a->x[1]);
        size_t n = utf ? strlen(utf) : 0;
        char *s = malloc(n + 1);
        if (utf) memcpy(s, utf, n + 1); else s[0]=0;
        return H2G(s);
    }
    case JNI_IDX_GetStringUTFChars: {
        /* Our "jstring" is just a char* */
        char *s = (char*)G2H(a->x[1]);
        if (a->x[2]) *(uint8_t*)G2H(a->x[2]) = 0;
        return H2G(s);
    }
    case JNI_IDX_ReleaseStringUTFChars:
        return 0;
    case JNI_IDX_GetStringUTFLength:
        return (uint64_t)strlen((char*)G2H(a->x[1]));
    case JNI_IDX_GetArrayLength: {
        /* Our NewByteArray layout: int64 length, then bytes. */
        if (!a->x[1]) return 0;
        int64_t *p = (int64_t*)G2H(a->x[1]);
        return (uint64_t)(int64_t)p[0];
    }
    case JNI_IDX_NewByteArray: {
        jsize n = (jsize)a->x[1];
        /* layout: int64 len, then bytes */
        uint8_t *p = calloc(1, 8 + (size_t)n);
        *(int64_t*)p = n;
        return H2G(p);
    }
    case JNI_IDX_GetByteArrayElements: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        if (a->x[2]) *(uint8_t*)G2H(a->x[2]) = 0;
        return H2G(p + 8);
    }
    case JNI_IDX_ReleaseByteArrayElements:
        return 0;
    case JNI_IDX_GetByteArrayRegion: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        jsize start = (jsize)a->x[2], len = (jsize)a->x[3];
        memcpy(G2H(a->x[4]), p + 8 + start, (size_t)len);
        return 0;
    }
    case JNI_IDX_SetByteArrayRegion: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        jsize start = (jsize)a->x[2], len = (jsize)a->x[3];
        memcpy(p + 8 + start, G2H(a->x[4]), (size_t)len);
        return 0;
    }
    case JNI_IDX_RegisterNatives: {
        /* jint RegisterNatives(JNIEnv*, jclass, const JNINativeMethod*, jint n) */
        fake_jclass *cls = (fake_jclass*)G2H(a->x[1]);
        const guest_JNINativeMethod *methods = (const guest_JNINativeMethod*)G2H(a->x[2]);
        int n = (int)a->x[3];
        const char *cname = cls ? cls->class_name : "?";
        if (!methods || n < 0 || n > 4096) return -1;
        for (int i = 0; i < n; i++) {
            uint64_t naddr = methods[i].name, saddr = methods[i].sig;
            if (!naddr || !saddr) continue; /* skip corrupt entries */
            const char *name = (const char*)G2H(naddr);
            const char *sig  = (const char*)G2H(saddr);
            uint64_t fn = methods[i].fnPtr;
            if (!fn) continue;
            jet_log(ctx, 2, "RegisterNatives %s.%s%s -> 0x%llx",
                    cname, name ? name : "?", sig ? sig : "?",
                    (unsigned long long)fn);
            if (ctx->nreg_log < 1024) {
                int k = ctx->nreg_log++;
                snprintf(ctx->reg_log[k].cls, sizeof ctx->reg_log[k].cls, "%s", cname);
                snprintf(ctx->reg_log[k].name, sizeof ctx->reg_log[k].name, "%s", name ? name : "");
                snprintf(ctx->reg_log[k].sig, sizeof ctx->reg_log[k].sig, "%s", sig ? sig : "");
                ctx->reg_log[k].fn = fn;
                ctx->reg_log[k].used = 1;
            }
            if (ctx->reg_cb)
                ctx->reg_cb(ctx->reg_cb_user, cname, name, sig, fn);
        }
        return 0; /* JNI_OK */
    }
    case JNI_IDX_UnregisterNatives:
        return 0;
    case JNI_IDX_GetJavaVM: {
        uint64_t *out = (uint64_t*)G2H(a->x[1]);
        if (out) *out = H2G(ctx->guest_javavm);
        return 0;
    }
    case JNI_IDX_CallStaticObjectMethod:
    case JNI_IDX_CallStaticObjectMethodV:
    case JNI_IDX_CallStaticObjectMethodA: {
        /* Common path: ByteBuffer.allocateDirect(I)Ljava/nio/ByteBuffer;.
         * Cap comes from the first vararg: for the non-V entry that's x3;
         * for V, AAPCS64 va_list.__stack (or __gr_top+__gr_offs) holds it. */
        int64_t cap = 4096;
        if (idx == JNI_IDX_CallStaticObjectMethod) {
            cap = (int64_t)a->x[3];
        } else if (idx == JNI_IDX_CallStaticObjectMethodV && a->x[3]) {
            /* typedef struct { void *stack; void *gr_top; void *vr_top; int gr_offs, vr_offs; } */
            struct { void *stack; void *gr_top; void *vr_top; int gr_offs; int vr_offs; } *vl =
                (void*)G2H(a->x[3]);
            if (vl) {
                if (vl->gr_offs < 0 && vl->gr_top) {
                    /* still in reg save area */
                    int64_t *slot = (int64_t*)((uint8_t*)vl->gr_top + vl->gr_offs);
                    cap = slot[0];
                } else if (vl->stack) {
                    cap = *(int64_t*)vl->stack;
                }
            }
        } else if (idx == JNI_IDX_CallStaticObjectMethodA && a->x[3]) {
            /* jvalue* */
            cap = *(int64_t*)G2H(a->x[3]);
        }
        if (cap <= 0 || cap > (1<<26)) cap = 4096;
        void *buf = calloc(1, (size_t)cap);
        void *wrap = calloc(1, 32);
        if (wrap) {
            ((uint64_t*)wrap)[0] = H2G(buf);
            ((int64_t*)wrap)[1] = cap;
        }
        jet_log(ctx, 2, "CallStaticObjectMethod* mid=%llx allocateDirect-ish cap=%lld wrap=%p buf=%p",
                (unsigned long long)a->x[2], (long long)cap, wrap, buf);
        return H2G(wrap);
    }
    case JNI_IDX_NewDirectByteBuffer: {
        /* NewDirectByteBuffer(env, address, capacity) */
        void *wrap = calloc(1, 32);
        /* store address/capacity at start so GetDirectBufferAddress can recover */
        if (wrap) {
            ((uint64_t*)wrap)[0] = a->x[1];
            ((int64_t*)wrap)[1] = (int64_t)a->x[2];
        }
        jet_log(ctx, 2, "NewDirectByteBuffer addr=%llx cap=%lld -> %p",
                (unsigned long long)a->x[1], (long long)a->x[2], wrap);
        return H2G(wrap);
    }
    case JNI_IDX_GetDirectBufferAddress: {
        uint64_t *wrap = a->x[1] ? (uint64_t*)G2H(a->x[1]) : NULL;
        return wrap ? wrap[0] : 0;
    }
    case JNI_IDX_GetDirectBufferCapacity: {
        int64_t *wrap = a->x[1] ? (int64_t*)G2H(a->x[1]) : NULL;
        return wrap ? (uint64_t)wrap[1] : (uint64_t)-1;
    }
    default:
        jet_log(ctx, 1, "JNI stub idx=%d x0=%llx x1=%llx x2=%llx x3=%llx", idx,
                (unsigned long long)a->x[0], (unsigned long long)a->x[1],
                (unsigned long long)a->x[2], (unsigned long long)a->x[3]);
        return 0;
    }
}

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
    a.x[1] = 0; /* reserved */
    uint64_t ver = 0;
    int rc = jet_native_call(ctx, sym, &a, &ver, NULL);
    if (rc != 0) {
        /* Helper after RegisterNatives may hit unimplemented edge cases.
         * If natives were captured, treat as success with JNI_VERSION_1_6. */
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
