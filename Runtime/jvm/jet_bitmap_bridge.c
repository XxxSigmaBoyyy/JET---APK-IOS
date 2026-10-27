/* jet_bitmap_bridge.c - libjnigraphics (AndroidBitmap_*) for guest .so code, over the shim's android.graphics.Bitmap.
 *
 * Partner Runtime/native binds AndroidBitmap_getInfo/lockPixels/unlockPixels to no-op stubs, so guest code that
 * renders into a Bitmap (Telegram generateGradient, RLottie getFrame, blur, ...) got a NULL pixel pointer.
 * lockPixels copies the Bitmap's pixels (getPixels, ARGB ints) into a host RGBA_8888 premultiplied buffer (Android's
 * layout); unlockPixels converts back and stores them with setPixels. Guest memory is identity-mapped, so the buffer
 * pointer is handed to the guest as is. The bitmap jobject arriving from the guest is a host reference (refs pass
 * through the forwarded JNIEnv); the host JNIEnv is this thread's (guest code runs on the calling Java thread). */
#include "../native/jet_native.h"
#include "JetJVM.h"
#include <jni.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct { uint32_t width, height, stride; int32_t format; uint32_t flags; } jet_abm_info;   /* AndroidBitmapInfo */
#define LOCKS 128
static struct { jobject bmp; uint32_t *buf; int w, h; } g_lk[LOCKS];
static pthread_mutex_t g_lk_mu = PTHREAD_MUTEX_INITIALIZER;
static jmethodID m_w, m_h, m_get, m_set;

static JNIEnv *env_now(void) {
    JavaVM *vm = (JavaVM *)jet_jvm_vm(); JNIEnv *e = NULL;
    if (!vm || (*vm)->GetEnv(vm, (void **)&e, JNI_VERSION_1_6) != JNI_OK) return NULL;
    return e;
}
static int ids(JNIEnv *e, jobject b) {
    if (m_set) return 1;
    jclass c = (*e)->GetObjectClass(e, b); if (!c) return 0;
    m_w = (*e)->GetMethodID(e, c, "getWidth", "()I"); m_h = (*e)->GetMethodID(e, c, "getHeight", "()I");
    m_get = (*e)->GetMethodID(e, c, "getPixels", "([IIIIIII)V");
    jmethodID s = (*e)->GetMethodID(e, c, "setPixels", "([IIIIIII)V");
    if ((*e)->ExceptionCheck(e)) { (*e)->ExceptionClear(e); return 0; }
    m_set = s; return m_set != NULL;
}
static uint64_t h_getInfo(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; (void)u; JNIEnv *e = env_now(); jobject b = (jobject)(uintptr_t)a->x[1]; jet_abm_info *o = (jet_abm_info *)(uintptr_t)a->x[2];
    if (!e || !b || !ids(e, b)) return (uint64_t)(int64_t)-1;   /* ANDROID_BITMAP_RESULT_BAD_PARAMETER */
    int w = (*e)->CallIntMethod(e, b, m_w), h = (*e)->CallIntMethod(e, b, m_h);
    if (o) { o->width = (uint32_t)w; o->height = (uint32_t)h; o->stride = (uint32_t)w * 4; o->format = 1 /* RGBA_8888 */; o->flags = 0; }
    return 0;
}
static uint64_t h_lock(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; (void)u; JNIEnv *e = env_now(); jobject b = (jobject)(uintptr_t)a->x[1]; void **out = (void **)(uintptr_t)a->x[2];
    if (!e || !b || !ids(e, b)) return (uint64_t)(int64_t)-1;
    int w = (*e)->CallIntMethod(e, b, m_w), h = (*e)->CallIntMethod(e, b, m_h);
    if (w <= 0 || h <= 0) return (uint64_t)(int64_t)-1;
    jintArray arr = (*e)->NewIntArray(e, w * h); if (!arr) { (*e)->ExceptionClear(e); return (uint64_t)(int64_t)-3; }
    (*e)->CallVoidMethod(e, b, m_get, arr, 0, w, 0, 0, w, h);
    uint32_t *buf = malloc((size_t)w * h * 4);
    if (!buf) { (*e)->DeleteLocalRef(e, arr); return (uint64_t)(int64_t)-3; }
    jint *px = (*e)->GetIntArrayElements(e, arr, NULL);
    for (int i = 0; i < w * h; i++) {   /* ARGB int (straight) -> RGBA bytes premultiplied */
        uint32_t p = (uint32_t)px[i], A = p >> 24, R = (p >> 16) & 255, G = (p >> 8) & 255, B = p & 255;
        if (A != 255) { R = R * A / 255; G = G * A / 255; B = B * A / 255; }
        buf[i] = R | (G << 8) | (B << 16) | (A << 24);
    }
    (*e)->ReleaseIntArrayElements(e, arr, px, JNI_ABORT); (*e)->DeleteLocalRef(e, arr);
    pthread_mutex_lock(&g_lk_mu);
    int slot = -1;
    for (int i = 0; i < LOCKS; i++) if (!g_lk[i].bmp) { slot = i; break; }
    if (slot >= 0) { g_lk[slot].bmp = (*e)->NewGlobalRef(e, b); g_lk[slot].buf = buf; g_lk[slot].w = w; g_lk[slot].h = h; }
    pthread_mutex_unlock(&g_lk_mu);
    if (slot < 0) { free(buf); return (uint64_t)(int64_t)-3; }
    if (out) *out = buf;
    return 0;
}
static uint64_t h_unlock(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; (void)u; JNIEnv *e = env_now(); jobject b = (jobject)(uintptr_t)a->x[1];
    if (!e || !b) return (uint64_t)(int64_t)-1;
    pthread_mutex_lock(&g_lk_mu);
    int slot = -1;
    for (int i = LOCKS - 1; i >= 0; i--) if (g_lk[i].bmp && (*e)->IsSameObject(e, g_lk[i].bmp, b)) { slot = i; break; }
    jobject g = NULL; uint32_t *buf = NULL; int w = 0, h = 0;
    if (slot >= 0) { g = g_lk[slot].bmp; buf = g_lk[slot].buf; w = g_lk[slot].w; h = g_lk[slot].h; g_lk[slot].bmp = NULL; g_lk[slot].buf = NULL; }
    pthread_mutex_unlock(&g_lk_mu);
    if (slot < 0) return (uint64_t)(int64_t)-1;
    jintArray arr = (*e)->NewIntArray(e, w * h);
    if (arr) {
        jint *px = (*e)->GetIntArrayElements(e, arr, NULL);
        for (int i = 0; i < w * h; i++) {   /* RGBA premultiplied -> ARGB straight */
            uint32_t p = buf[i], R = p & 255, G = (p >> 8) & 255, B = (p >> 16) & 255, A = p >> 24;
            if (A && A != 255) { R = R * 255 / A; G = G * 255 / A; B = B * 255 / A; if (R > 255) R = 255; if (G > 255) G = 255; if (B > 255) B = 255; }
            px[i] = (jint)((A << 24) | (R << 16) | (G << 8) | B);
        }
        (*e)->ReleaseIntArrayElements(e, arr, px, 0);
        (*e)->CallVoidMethod(e, b, m_set, arr, 0, w, 0, 0, w, h);
        if ((*e)->ExceptionCheck(e)) (*e)->ExceptionClear(e);
        (*e)->DeleteLocalRef(e, arr);
    } else (*e)->ExceptionClear(e);
    (*e)->DeleteGlobalRef(e, g); free(buf);
    return 0;
}
int jet_bitmap_bridge_install(jet_ctx *ctx) {
    const char *off = getenv("JET_BITMAP_TRAPS");
    if (!ctx || (off && *off == '0')) return 0;
    int n = 0;
    n += jet_native_register_host(ctx, "AndroidBitmap_getInfo", h_getInfo, NULL) == 0;
    n += jet_native_register_host(ctx, "AndroidBitmap_lockPixels", h_lock, NULL) == 0;
    n += jet_native_register_host(ctx, "AndroidBitmap_unlockPixels", h_unlock, NULL) == 0;
    return n;
}
