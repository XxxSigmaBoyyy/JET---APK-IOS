// See JET/Bridge/JetJVM.h.
#include "JetJVM.h"
#include <dlfcn.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#if __has_include(<jni.h>)
#include <jni.h>
#define JET_HAVE_JNI 1
#else
#define JET_HAVE_JNI 0
#endif

int32_t jet_jvm_compiled(void) { return JET_HAVE_JNI; }

#ifdef __APPLE__
#include <sys/sysctl.h>
#include <sys/time.h>
#include <unistd.h>
double jet_process_age_ms(void) {
    struct kinfo_proc kp; size_t len = sizeof kp;
    int mib[4] = { CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid() };
    if (sysctl(mib, 4, &kp, &len, NULL, 0) != 0 || len == 0) return -1;
    struct timeval now; gettimeofday(&now, NULL);
    struct timeval st = kp.kp_proc.p_starttime;
    return (double)(now.tv_sec - st.tv_sec) * 1e3 + (double)(now.tv_usec - st.tv_usec) / 1e3;
}
#else
double jet_process_age_ms(void) { return -1; }
#endif

static double g_boot_ms = -1;
double jet_jvm_boot_ms(void) { return g_boot_ms; }

#if JET_HAVE_JNI
static JavaVM *g_vm;
void *jet_jvm_vm(void) { return (void *)g_vm; }
static FILE *g_log;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

static double now_ms(void) {
    struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t);
    return t.tv_sec * 1e3 + t.tv_nsec / 1e6;
}

static jint JNICALL jvm_vfprintf(FILE *fp, const char *fmt, va_list ap) {
    (void)fp;
    FILE *o = g_log ? g_log : stderr;
    int n = vfprintf(o, fmt, ap);
    fflush(o);
    return n;
}
static void JNICALL jvm_exit(jint code) {
    if (g_log) { fprintf(g_log, "[jetjvm] JVM exit(%d) hook\n", (int)code); fflush(g_log); }
}
static void JNICALL jvm_abort(void) {
    if (g_log) { fprintf(g_log, "[jetjvm] JVM abort hook\n"); fflush(g_log); }
}

typedef jint (JNICALL *CreateVM_t)(JavaVM **, void **, void *);

struct boot_args {
    const char *home, *cp, *extra;
    char *err; int errlen; int rc;
};

static void *boot_thread(void *p) {
    struct boot_args *a = p;
    char path[4096];
    void *h = NULL;
    const char *variants[] = { "zero", "server", "client", NULL };
    for (int i = 0; variants[i] && !h; i++) {
        snprintf(path, sizeof path, "%s/lib/%s/libjvm.dylib", a->home, variants[i]);
        h = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
        if (!h && g_log) { fprintf(g_log, "[jetjvm] dlopen %s: %s\n", path, dlerror()); }
    }
    if (!h) { snprintf(a->err, a->errlen, "libjvm not loadable under %s/lib (see log)", a->home); a->rc = -2; return NULL; }
    CreateVM_t create = (CreateVM_t)dlsym(h, "JNI_CreateJavaVM");
    if (!create) { snprintf(a->err, a->errlen, "JNI_CreateJavaVM missing: %s", dlerror()); a->rc = -3; return NULL; }
    if (g_log) { fprintf(g_log, "[jetjvm] loaded %s\n", path); fflush(g_log); }

    JavaVMOption opts[64]; int n = 0;
    static char cpopt[8192], homeopt[4096];
    snprintf(cpopt, sizeof cpopt, "-Djava.class.path=%s", a->cp ? a->cp : "");
    snprintf(homeopt, sizeof homeopt, "-Djava.home=%s", a->home);
    opts[n++].optionString = cpopt;
    opts[n++].optionString = homeopt;
    opts[n++].optionString = "-Xss4m";
    opts[n++].optionString = "-XX:+UseSerialGC";
    opts[n++].optionString = "-Xms64m";
    opts[n++].optionString = "-Xmx768m";
    opts[n++].optionString = "-Djava.awt.headless=true";
    opts[n++].optionString = "-Dfile.encoding=UTF-8";
    opts[n].optionString = "vfprintf"; opts[n++].extraInfo = (void *)jvm_vfprintf;
    opts[n].optionString = "exit";     opts[n++].extraInfo = (void *)jvm_exit;
    opts[n].optionString = "abort";    opts[n++].extraInfo = (void *)jvm_abort;
    char *extra = a->extra ? strdup(a->extra) : NULL;
    for (char *s = extra ? strtok(extra, "\n") : NULL; s && n < 60; s = strtok(NULL, "\n"))
        if (*s) opts[n++].optionString = s;
    if (g_log) { for (int i = 0; i < n; i++) if (opts[i].optionString[0] == '-') fprintf(g_log, "[jetjvm] opt %s\n", opts[i].optionString); fflush(g_log); }

    JavaVMInitArgs args = { .version = JNI_VERSION_10, .nOptions = n, .options = opts, .ignoreUnrecognized = JNI_FALSE };
    JNIEnv *env = NULL;
    double t0 = now_ms();
    jint rc = create(&g_vm, (void **)&env, &args);
    g_boot_ms = now_ms() - t0;
    if (rc != JNI_OK) { snprintf(a->err, a->errlen, "JNI_CreateJavaVM failed rc=%d", (int)rc); g_vm = NULL; a->rc = -4; }
    else {
        if (g_log) { fprintf(g_log, "[jetjvm] JVM up in %.1f ms\n", g_boot_ms); fflush(g_log); }
        (*g_vm)->DetachCurrentThread(g_vm);
        a->rc = 0;
    }
    return NULL;   // extra is intentionally leaked: the VM may keep option strings
}

static int run_big(void *(*fn)(void *), void *arg) {
    pthread_attr_t at; pthread_attr_init(&at);
    pthread_attr_setstacksize(&at, 16u << 20);
    pthread_t t;
    if (pthread_create(&t, &at, fn, arg) != 0) return -1;
    pthread_join(t, NULL);
    pthread_attr_destroy(&at);
    return 0;
}

int32_t jet_jvm_boot(const char *java_home, const char *classpath, const char *extra_opts, const char *log_path,
                     char *err, int32_t errlen) {
    pthread_mutex_lock(&g_lock);
    if (g_vm) { pthread_mutex_unlock(&g_lock); return 0; }
    if (log_path && !g_log) g_log = fopen(log_path, "a");
    setenv("JAVA_HOME", java_home, 1);   // iOS port's AWT font fallback looks in $JAVA_HOME/lib/fonts
    struct boot_args a = { java_home, classpath, extra_opts, err, errlen, -1 };
    if (err && errlen) err[0] = 0;
    if (run_big(boot_thread, &a) != 0) { snprintf(err, errlen, "pthread_create failed"); a.rc = -1; }
    pthread_mutex_unlock(&g_lock);
    return a.rc;
}

struct call_args { const char *cls, *method, *arg; char *out; int outlen; int rc; };

static void describe_exc(JNIEnv *env, char *out, int outlen) {
    jthrowable ex = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    jclass oc = (*env)->FindClass(env, "java/lang/Object");
    jmethodID ts = (*env)->GetMethodID(env, oc, "toString", "()Ljava/lang/String;");
    jstring s = ex ? (jstring)(*env)->CallObjectMethod(env, ex, ts) : NULL;
    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
    const char *c = s ? (*env)->GetStringUTFChars(env, s, NULL) : NULL;
    snprintf(out, outlen, "EXCEPTION %s", c ? c : "?");
    if (c) (*env)->ReleaseStringUTFChars(env, s, c);
}

static void *call_thread(void *p) {
    struct call_args *a = p;
    JNIEnv *env;
    if ((*g_vm)->AttachCurrentThread(g_vm, (void **)&env, NULL) != JNI_OK) { snprintf(a->out, a->outlen, "attach failed"); a->rc = -2; return NULL; }
    jclass c = (*env)->FindClass(env, a->cls);
    jmethodID m = c ? (*env)->GetStaticMethodID(env, c, a->method, "(Ljava/lang/String;)Ljava/lang/String;") : NULL;
    if (!m) { describe_exc(env, a->out, a->outlen); a->rc = -3; goto done; }
    jstring in = (*env)->NewStringUTF(env, a->arg ? a->arg : "");
    jstring r = (jstring)(*env)->CallStaticObjectMethod(env, c, m, in);
    if ((*env)->ExceptionCheck(env)) { describe_exc(env, a->out, a->outlen); a->rc = -4; goto done; }
    const char *s = r ? (*env)->GetStringUTFChars(env, r, NULL) : NULL;
    snprintf(a->out, a->outlen, "%s", s ? s : "null");
    if (s) (*env)->ReleaseStringUTFChars(env, r, s);
    a->rc = 0;
done:
    (*g_vm)->DetachCurrentThread(g_vm);
    return NULL;
}

int32_t jet_jvm_call_string(const char *cls, const char *method, const char *arg, char *out, int32_t outlen) {
    if (!g_vm) { snprintf(out, outlen, "JVM not booted"); return -1; }
    struct call_args a = { cls, method, arg, out, outlen, -1 };
    if (run_big(call_thread, &a) != 0) return -1;
    return a.rc;
}

// ---- frame / touch (persistent daemon attachment of the caller thread) ----
static jclass g_host; static jfieldID g_fb, g_fbw, g_fbh, g_seq; static jmethodID g_touch;

static JNIEnv *attached_env(void) {
    if (!g_vm) return NULL;
    JNIEnv *env = NULL;
    if ((*g_vm)->GetEnv(g_vm, (void **)&env, JNI_VERSION_10) == JNI_OK) return env;
    if ((*g_vm)->AttachCurrentThreadAsDaemon(g_vm, (void **)&env, NULL) != JNI_OK) return NULL;
    return env;
}
static int host_ids(JNIEnv *env) {
    if (g_host) return 0;
    jclass c = (*env)->FindClass(env, "jet/ios/JetIosHost");   // app class loader: system loader (jvm-app jars)
    if (!c) { (*env)->ExceptionClear(env); return -2; }
    g_fb = (*env)->GetStaticFieldID(env, c, "fb", "[I");
    g_fbw = (*env)->GetStaticFieldID(env, c, "fbW", "I");
    g_fbh = (*env)->GetStaticFieldID(env, c, "fbH", "I");
    g_seq = (*env)->GetStaticFieldID(env, c, "fbSeq", "I");
    g_touch = (*env)->GetStaticMethodID(env, c, "touch", "(Ljava/lang/String;)Ljava/lang/String;");
    if ((*env)->ExceptionCheck(env) || !g_fb || !g_fbw || !g_fbh || !g_seq || !g_touch) { (*env)->ExceptionClear(env); return -3; }
    g_host = (jclass)(*env)->NewGlobalRef(env, c);
    return 0;
}

int32_t jet_jvm_frame(uint32_t *dst, int32_t cap, int32_t *w, int32_t *h, int32_t *seq) {
    JNIEnv *env = attached_env();
    if (!env) return -1;
    if (host_ids(env)) return -2;
    jint s = (*env)->GetStaticIntField(env, g_host, g_seq);
    if (s == *seq) return 0;
    jintArray a = (jintArray)(*env)->GetStaticObjectField(env, g_host, g_fb);
    if (!a) return -4;
    jint fw = (*env)->GetStaticIntField(env, g_host, g_fbw), fh = (*env)->GetStaticIntField(env, g_host, g_fbh);
    jsize n = (*env)->GetArrayLength(env, a);
    if (fw <= 0 || fh <= 0 || (jlong)fw * fh > n) { (*env)->DeleteLocalRef(env, a); return -5; }
    if ((jlong)fw * fh > cap) { (*env)->DeleteLocalRef(env, a); *w = fw; *h = fh; return -7; }   // caller grows dst
    (*env)->GetIntArrayRegion(env, a, 0, fw * fh, (jint *)dst);
    (*env)->DeleteLocalRef(env, a);
    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return -6; }
    *w = fw; *h = fh; *seq = s;
    return 1;
}

int32_t jet_jvm_touch(int32_t action, float x, float y) {
    JNIEnv *env = attached_env();
    if (!env) return -1;
    if (host_ids(env)) return -2;
    char buf[96]; snprintf(buf, sizeof buf, "%d\n%.1f\n%.1f", (int)action, x, y);
    jstring in = (*env)->NewStringUTF(env, buf);
    jobject r = (*env)->CallStaticObjectMethod(env, g_host, g_touch, in);
    (*env)->DeleteLocalRef(env, in);
    if (r) (*env)->DeleteLocalRef(env, r);
    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); return -3; }
    return 0;
}

#else  // no jni.h at build time
int32_t jet_jvm_frame(uint32_t *d, int32_t c, int32_t *w, int32_t *h, int32_t *s) { (void)d; (void)c; (void)w; (void)h; (void)s; return -10; }
int32_t jet_jvm_touch(int32_t a, float x, float y) { (void)a; (void)x; (void)y; return -10; }
void *jet_jvm_vm(void) { return NULL; }
int32_t jet_jvm_boot(const char *h, const char *cp, const char *x, const char *l, char *err, int32_t errlen) {
    (void)h; (void)cp; (void)x; (void)l;
    if (err && errlen) snprintf(err, errlen, "JET built without JNI headers (no JRE bundled)");
    return -10;
}
int32_t jet_jvm_call_string(const char *c, const char *m, const char *a, char *out, int32_t outlen) {
    (void)c; (void)m; (void)a; if (out && outlen) snprintf(out, outlen, "no JVM"); return -10;
}
#endif
