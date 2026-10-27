#include "jet_jni_bridge.h"
#include "JetJVM.h"
#include "../native/jet_native.h"
#include "../native/jet_native_status.h"
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if __has_include(<jni.h>)
#include <jni.h>
#define HAVE_JNI 1
#else
#define HAVE_JNI 0
#endif

#if HAVE_JNI
void *jet_jvm_vm(void); /* from jetjvm.c */

static jet_ctx *g_ctx;
static JavaVM *g_vm;
static pthread_mutex_t g_mu = PTHREAD_MUTEX_INITIALIZER;
/* One guest call at a time (the interpreter context is shared); recursive: guest -> Java -> native -> guest. */
static pthread_mutex_t g_call_mu;
static pthread_once_t g_call_once = PTHREAD_ONCE_INIT;
static void init_call_mu(void) {
    pthread_mutexattr_t at; pthread_mutexattr_init(&at);
    pthread_mutexattr_settype(&at, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&g_call_mu, &at);
    pthread_mutexattr_destroy(&at);
}
enum { MAX_LIBS = 64 };
static jet_lib *g_libs[MAX_LIBS];
static char g_libnames[MAX_LIBS][256];
static int g_nlibs;

/* RegisterNatives from guest JNI_OnLoad: remembered here; the JVM side calls them through Natives.jni (app `native`
 * methods are rewritten into Java bodies by jet.dex.DexClassLoader), so no host code is generated per method. */
typedef struct { char *key; uint64_t fn; } reg_ent;
static reg_ent *g_reg; static int g_nreg, g_capreg;

static void on_register(void *user, const char *cls, const char *name, const char *sig, uint64_t guest_fn) {
    (void)user;
    if (!cls || !name || !sig || !guest_fn) return;
    size_t n = strlen(cls) + strlen(name) + strlen(sig) + 2;
    char *k = malloc(n); if (!k) return;
    snprintf(k, n, "%s.%s%s", cls, name, sig);
    pthread_mutex_lock(&g_mu);
    for (int i = 0; i < g_nreg; i++) if (!strcmp(g_reg[i].key, k)) { g_reg[i].fn = guest_fn; free(k); pthread_mutex_unlock(&g_mu); return; }
    if (g_nreg == g_capreg) {
        int nc = g_capreg ? g_capreg * 2 : 256;
        reg_ent *r = realloc(g_reg, (size_t)nc * sizeof *r);
        if (!r) { free(k); pthread_mutex_unlock(&g_mu); return; }
        g_reg = r; g_capreg = nc;
    }
    g_reg[g_nreg].key = k; g_reg[g_nreg].fn = guest_fn; g_nreg++;
    pthread_mutex_unlock(&g_mu);
}

static jet_lib *find_lib(const char *path) {
    for (int i = 0; i < g_nlibs; i++) if (!strcmp(g_libnames[i], path)) return g_libs[i];
    return NULL;
}

static jboolean JNICALL N_loadPath(JNIEnv *env, jclass cls, jstring jpath) {
    (void)cls;
    if (!g_ctx) return JNI_FALSE;
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    char err[512];
    pthread_mutex_lock(&g_mu);
    jet_lib *lib = find_lib(path);
    if (!lib) {
        lib = jet_native_load(g_ctx, path, err, sizeof err);
        if (!lib) {
            pthread_mutex_unlock(&g_mu);
            (*env)->ReleaseStringUTFChars(env, jpath, path);
            fprintf(stderr, "[jetjni] load failed: %s\n", err);
            return JNI_FALSE;
        }
        if (g_nlibs < MAX_LIBS) {
            g_libs[g_nlibs] = lib;
            snprintf(g_libnames[g_nlibs], sizeof g_libnames[0], "%s", path);
            g_nlibs++;
        }
        int32_t ver = 0;
        if (jet_jni_onload(g_ctx, lib, &ver) == JET_NATIVE_OK)
            fprintf(stderr, "[jetjni] JNI_OnLoad => 0x%x\n", (unsigned)ver);
    }
    pthread_mutex_unlock(&g_mu);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return JNI_TRUE;
}

static jlong JNICALL N_resolve(JNIEnv *env, jclass cls, jstring jpath, jstring jsym) {
    (void)cls;
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    const char *sym = (*env)->GetStringUTFChars(env, jsym, NULL);
    pthread_mutex_lock(&g_mu);
    jet_lib *lib = find_lib(path);
    uint64_t a = lib ? jet_native_sym(lib, sym) : 0;
    pthread_mutex_unlock(&g_mu);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    (*env)->ReleaseStringUTFChars(env, jsym, sym);
    return (jlong)a;
}

static jlong JNICALL N_call(JNIEnv *env, jclass cls, jlong guest, jlongArray jx, jdoubleArray jd) {
    (void)cls;
    if (!g_ctx || !guest) return 0;
    jet_args a; memset(&a, 0, sizeof a);
    if (jx) {
        jsize n = (*env)->GetArrayLength(env, jx); if (n > 8) n = 8;
        jlong *px = (*env)->GetLongArrayElements(env, jx, NULL);
        for (int i = 0; i < n; i++) a.x[i] = (uint64_t)px[i];
        (*env)->ReleaseLongArrayElements(env, jx, px, JNI_ABORT);
    }
    if (jd) {
        jsize n = (*env)->GetArrayLength(env, jd); if (n > 8) n = 8;
        jdouble *pd = (*env)->GetDoubleArrayElements(env, jd, NULL);
        for (int i = 0; i < n; i++) a.d[i] = pd[i];
        (*env)->ReleaseDoubleArrayElements(env, jd, pd, JNI_ABORT);
    }
    uint64_t rx = 0; double rd = 0;
    jet_native_call(g_ctx, (uint64_t)guest, &a, &rx, &rd);
    (void)rd;
    return (jlong)rx;
}

static jlong JNICALL N_registered(JNIEnv *env, jclass cls, jstring jc, jstring jn, jstring js) {
    (void)cls;
    const char *c = (*env)->GetStringUTFChars(env, jc, NULL), *n = (*env)->GetStringUTFChars(env, jn, NULL),
               *g = (*env)->GetStringUTFChars(env, js, NULL);
    size_t cl = strlen(c), nl = strlen(n);
    uint64_t fn = 0;
    pthread_mutex_lock(&g_mu);
    for (int i = 0; i < g_nreg && !fn; i++) {
        const char *k = g_reg[i].key;
        if (!strncmp(k, c, cl) && k[cl] == '.' && !strncmp(k + cl + 1, n, nl) && !strcmp(k + cl + 1 + nl, g)) fn = g_reg[i].fn;
    }
    pthread_mutex_unlock(&g_mu);
    (*env)->ReleaseStringUTFChars(env, jc, c); (*env)->ReleaseStringUTFChars(env, jn, n); (*env)->ReleaseStringUTFChars(env, js, g);
    return (jlong)fn;
}

static jlong JNICALL N_resolveAny(JNIEnv *env, jclass cls, jstring jsym) {
    (void)cls;
    const char *sym = (*env)->GetStringUTFChars(env, jsym, NULL);
    uint64_t a = 0;
    pthread_mutex_lock(&g_mu);
    for (int i = 0; i < g_nlibs && !a; i++) a = jet_native_sym(g_libs[i], sym);
    pthread_mutex_unlock(&g_mu);
    (*env)->ReleaseStringUTFChars(env, jsym, sym);
    return (jlong)a;
}

/* Lay out a JNI call per Linux AAPCS64: x0 = guest JNIEnv, x1 = receiver, then args of `desc` in order; integer-class
 * and references take x2..x7, F/D take d0..d7 (raw bits; floats in the low 32 bits), the rest 8-byte stack slots. */
static int guest_jni_call(JNIEnv *env, jlong fn, jobject recv, jstring jdesc, jlongArray jp, jlongArray jf,
                          jobjectArray jr, uint64_t *rx, uint64_t *rdbits) {
    if (!g_ctx || !fn) return -1;
    const char *desc = (*env)->GetStringUTFChars(env, jdesc, NULL);
    jsize np = jp ? (*env)->GetArrayLength(env, jp) : 0, nf = jf ? (*env)->GetArrayLength(env, jf) : 0;
    jlong *p = np ? (*env)->GetLongArrayElements(env, jp, NULL) : NULL, *f = nf ? (*env)->GetLongArrayElements(env, jf, NULL) : NULL;
    jet_args a; memset(&a, 0, sizeof a);
    uint64_t stack[64]; uint32_t ns = 0;
    int nx = 0, nd = 0, pi = 0, fi = 0, ri = 0;
    a.x[nx++] = jet_jni_guest_env(g_ctx);
    a.x[nx++] = (uint64_t)(uintptr_t)recv;
    const char *d = strchr(desc, '(');
    for (d = d ? d + 1 : desc; *d && *d != ')' && ns < 64; ) {
        uint64_t v; int fp = 0;
        if (*d == 'F' || *d == 'D') { v = (uint64_t)(fi < nf ? f[fi] : 0); fi++; fp = 1; d++; }
        else if (*d == 'L' || *d == '[') {
            jobject o = (*env)->GetObjectArrayElement(env, jr, ri++);   /* local ref, alive until we return to Java */
            v = (uint64_t)(uintptr_t)o;
            while (*d == '[') d++;
            if (*d == 'L') { const char *e = strchr(d, ';'); d = e ? e + 1 : d + strlen(d); } else d++;
        } else { v = (uint64_t)(pi < np ? p[pi] : 0); pi++; d++; }
        if (fp) { if (nd < 8) memcpy(&a.d[nd++], &v, 8); else stack[ns++] = v; }
        else    { if (nx < 8) a.x[nx++] = v; else stack[ns++] = v; }
    }
    if (p) (*env)->ReleaseLongArrayElements(env, jp, p, JNI_ABORT);
    if (f) (*env)->ReleaseLongArrayElements(env, jf, f, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, jdesc, desc);
    a.stack = stack;
    pthread_once(&g_call_once, init_call_mu);
    pthread_mutex_lock(&g_call_mu);
    jet_jni_bind(g_ctx, g_vm, env);   /* the guest's JNI calls go to this thread's env */
    uint64_t x0 = 0; double d0 = 0;
    int rc = jet_native_call_ex(g_ctx, (uint64_t)fn, &a, ns, &x0, &d0);
    pthread_mutex_unlock(&g_call_mu);
    *rx = x0; memcpy(rdbits, &d0, 8);
    return rc;
}

static void throw_link(JNIEnv *env, int rc) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass c = (*env)->FindClass(env, "java/lang/UnsatisfiedLinkError");
    char m[256]; snprintf(m, sizeof m, "guest JNI call failed rc=%d: %s", rc, g_ctx ? jet_native_last_error(g_ctx) : "no native ctx");
    if (c) (*env)->ThrowNew(env, c, m);
}

static jlong JNICALL N_invoke0(JNIEnv *env, jclass cls, jlong fn, jobject recv, jstring desc, jlongArray p, jlongArray f, jobjectArray r) {
    (void)cls;
    uint64_t x0 = 0, d0 = 0;
    int rc = guest_jni_call(env, fn, recv, desc, p, f, r, &x0, &d0);
    if (rc != JET_NATIVE_OK) { throw_link(env, rc); return 0; }
    const char *s = (*env)->GetStringUTFChars(env, desc, NULL);
    const char *ret = strchr(s, ')'); char rt = ret ? ret[1] : 'V';
    (*env)->ReleaseStringUTFChars(env, desc, s);
    return (jlong)((rt == 'F' || rt == 'D') ? d0 : x0);
}

static jobject JNICALL N_invokeRef0(JNIEnv *env, jclass cls, jlong fn, jobject recv, jstring desc, jlongArray p, jlongArray f, jobjectArray r) {
    (void)cls;
    uint64_t x0 = 0, d0 = 0;
    int rc = guest_jni_call(env, fn, recv, desc, p, f, r, &x0, &d0);
    if (rc != JET_NATIVE_OK) { throw_link(env, rc); return NULL; }
    return (jobject)(uintptr_t)x0;   /* a host reference the guest obtained through its (forwarded) JNIEnv */
}

static int register_natives_class(JNIEnv *env) {
    jclass c = (*env)->FindClass(env, "dev/jet/nativeapi/Natives");
    if (!c) { (*env)->ExceptionClear(env); return -1; }
    JNINativeMethod m[] = {
        { "loadPath", "(Ljava/lang/String;)Z", (void *)N_loadPath },
        { "resolve",  "(Ljava/lang/String;Ljava/lang/String;)J", (void *)N_resolve },
        { "call",     "(J[J[D)J", (void *)N_call },
        { "registered", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J", (void *)N_registered },
        { "resolveAny", "(Ljava/lang/String;)J", (void *)N_resolveAny },
        { "invoke0", "(JLjava/lang/Object;Ljava/lang/String;[J[J[Ljava/lang/Object;)J", (void *)N_invoke0 },
        { "invokeRef0", "(JLjava/lang/Object;Ljava/lang/String;[J[J[Ljava/lang/Object;)Ljava/lang/Object;", (void *)N_invokeRef0 },
    };
    return (*env)->RegisterNatives(env, c, m, (jint)(sizeof m / sizeof m[0]));
}

int32_t jet_jni_bridge_init(char *err, int32_t errlen) {
    /* libjvm is dlopen'd by jetjvm.c — do not link JNI_GetCreatedJavaVMs. */
    JavaVM *vm = (JavaVM *)jet_jvm_vm();
    if (!vm) {
        snprintf(err, errlen, "no JavaVM (boot JVM first)"); return -1;
    }
    g_vm = vm;
    g_ctx = jet_native_create(NULL);
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*vm)->AttachCurrentThread(vm, (void **)&env, NULL) != JNI_OK) {
            snprintf(err, errlen, "AttachCurrentThread failed"); return -2;
        }
    }
    if (g_ctx) {
        jet_jni_bind(g_ctx, vm, env);
        jet_jni_on_register_natives(g_ctx, on_register, NULL);
    }
    if (register_natives_class(env) != 0) {
        snprintf(err, errlen, "Natives class not on classpath yet (soft)");
        return 0;
    }
    return 0;
}

int32_t jet_jni_bridge_load_path(const char *path, char *err, int32_t errlen) {
    if (!g_ctx) { snprintf(err, errlen, "bridge not init (native create UNIMP/NULL)"); return JET_NATIVE_UNIMP; }
    char e2[512];
    jet_lib *lib = jet_native_load(g_ctx, path, e2, sizeof e2);
    if (!lib) { snprintf(err, errlen, "%s", e2); return JET_NATIVE_ERR; }
    int32_t ver = 0; jet_jni_onload(g_ctx, lib, &ver);
    pthread_mutex_lock(&g_mu);
    if (g_nlibs < MAX_LIBS) {
        g_libs[g_nlibs] = lib;
        snprintf(g_libnames[g_nlibs], sizeof g_libnames[0], "%s", path);
        g_nlibs++;
    }
    pthread_mutex_unlock(&g_mu);
    return JET_NATIVE_OK;
}
#else
int32_t jet_jni_bridge_init(char *err, int32_t errlen) { snprintf(err, errlen, "no JNI"); return -10; }
int32_t jet_jni_bridge_load_path(const char *path, char *err, int32_t errlen) {
    (void)path; snprintf(err, errlen, "no JNI"); return -10;
}
#endif
