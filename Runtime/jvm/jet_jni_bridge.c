#include "jet_jni_bridge.h"
#include "JetJVM.h"
#include "../native/jet_native.h"
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
enum { MAX_LIBS = 64 };
static jet_lib *g_libs[MAX_LIBS];
static char g_libnames[MAX_LIBS][256];
static int g_nlibs;

static void on_register(void *user, const char *cls, const char *name, const char *sig, uint64_t guest_fn) {
    (void)user;
    fprintf(stderr, "[jetjni] RegisterNatives %s.%s%s -> 0x%llx\n",
            cls ? cls : "?", name ? name : "?", sig ? sig : "?", (unsigned long long)guest_fn);
    /* Full host RegisterNatives trampoline: partner + JVM method cookies, next milestone. */
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

static int register_natives_class(JNIEnv *env) {
    jclass c = (*env)->FindClass(env, "dev/jet/nativeapi/Natives");
    if (!c) { (*env)->ExceptionClear(env); return -1; }
    JNINativeMethod m[] = {
        { "loadPath", "(Ljava/lang/String;)Z", (void *)N_loadPath },
        { "resolve",  "(Ljava/lang/String;Ljava/lang/String;)J", (void *)N_resolve },
        { "call",     "(J[J[D)J", (void *)N_call },
    };
    return (*env)->RegisterNatives(env, c, m, 3);
}

int32_t jet_jni_bridge_init(char *err, int32_t errlen) {
    JavaVM *vm = (JavaVM *)jet_jvm_vm();
    if (!vm) {
        jsize n = 0; JavaVM *vms[1];
        if (JNI_GetCreatedJavaVMs(vms, 1, &n) != JNI_OK || n < 1) {
            snprintf(err, errlen, "no JavaVM"); return -1;
        }
        vm = vms[0];
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
