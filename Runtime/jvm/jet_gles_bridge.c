/* jet_gles_bridge.c - guest (arm64 .so) GLES2 / EGL imports -> the host's GLES on the calling thread.
 *
 * Partner Runtime/native binds these imports to no-op stubs. Guest GL code (e.g. Telegram's Intro renderer) runs on the
 * Java thread that owns the current context (JetEGL10 makes the host context current on that same thread), so forwarding
 * each call to the host's libGLESv2 entry point draws into the surface JetEGL10 reads back and presents.
 * Guest memory is identity-mapped, so pointer args pass through unchanged. The host GL library is found at run time
 * (dlsym/dlopen); when none is present the partner stubs are kept. JET_GLES_TRAPS=0 disables the forwarding. */
#include "../native/jet_native.h"
#include <dlfcn.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef uint64_t (*fn_i6)(uint64_t, uint64_t, uint64_t, uint64_t, uint64_t, uint64_t);
enum { R_VOID, R_U32, R_S32, R_PTR };
typedef struct { const char *name; int ret; void *fn; } gl_ent;

static void *g_gl, *g_egl;
static void *gl_sym(const char *n) {
    void *p = dlsym(RTLD_DEFAULT, n);
    if (!p && g_gl) p = dlsym(g_gl, n);
    if (!p && g_egl) p = dlsym(g_egl, n);
    return p;
}

/* Integer/pointer-only signatures (all args in x0..x5). */
static gl_ent g_int[] = {
    {"glActiveTexture", R_VOID}, {"glAttachShader", R_VOID}, {"glBindBuffer", R_VOID}, {"glBindTexture", R_VOID},
    {"glBlendFunc", R_VOID}, {"glBufferData", R_VOID}, {"glBufferSubData", R_VOID}, {"glClear", R_VOID},
    {"glColorMask", R_VOID}, {"glCompileShader", R_VOID}, {"glCreateProgram", R_U32}, {"glCreateShader", R_U32},
    {"glDepthFunc", R_VOID}, {"glDepthMask", R_VOID}, {"glDisable", R_VOID}, {"glDrawArrays", R_VOID},
    {"glEnable", R_VOID}, {"glEnableVertexAttribArray", R_VOID}, {"glGenBuffers", R_VOID},
    {"glGetAttribLocation", R_S32}, {"glGetProgramiv", R_VOID}, {"glGetShaderiv", R_VOID},
    {"glGetUniformLocation", R_S32}, {"glLinkProgram", R_VOID}, {"glShaderSource", R_VOID}, {"glUniform1i", R_VOID},
    {"glUniform4fv", R_VOID}, {"glUniformMatrix4fv", R_VOID}, {"glUseProgram", R_VOID},
    {"glVertexAttribPointer", R_VOID}, {"glViewport", R_VOID},
    {"glBindFramebuffer", R_VOID}, {"glDeleteBuffers", R_VOID}, {"glDeleteProgram", R_VOID}, {"glDeleteShader", R_VOID},
    {"glDeleteTextures", R_VOID}, {"glDisableVertexAttribArray", R_VOID}, {"glDrawElements", R_VOID},
    {"glGenTextures", R_VOID}, {"glGetError", R_U32}, {"glPixelStorei", R_VOID}, {"glScissor", R_VOID},
    {"glTexImage2D", R_VOID}, {"glTexSubImage2D", R_VOID}, {"glTexParameteri", R_VOID}, {"glUniform2fv", R_VOID},
    {"glUniform3fv", R_VOID}, {"glUniform1fv", R_VOID}, {"glUniform1iv", R_VOID}, {"glUniformMatrix3fv", R_VOID},
    {"glGetShaderInfoLog", R_VOID}, {"glGetProgramInfoLog", R_VOID}, {"glFinish", R_VOID}, {"glFlush", R_VOID},
    {"glBindAttribLocation", R_VOID}, {"glGenFramebuffers", R_VOID}, {"glFramebufferTexture2D", R_VOID},
    {"glCheckFramebufferStatus", R_U32}, {"glDeleteFramebuffers", R_VOID}, {"glGetIntegerv", R_VOID},
    {"glBlendFuncSeparate", R_VOID}, {"glGenerateMipmap", R_VOID},
    {"eglGetCurrentContext", R_PTR}, {"eglGetCurrentDisplay", R_PTR}, {"eglGetCurrentSurface", R_PTR},
};

static uint64_t h_int(jet_ctx *c, const jet_args *a, void *u) {
    (void)c; gl_ent *e = (gl_ent *)u;
    uint64_t r = ((fn_i6)e->fn)(a->x[0], a->x[1], a->x[2], a->x[3], a->x[4], a->x[5]);
    switch (e->ret) {
    case R_U32: return (uint32_t)r;
    case R_S32: return (uint64_t)(int64_t)(int32_t)r;
    case R_PTR: return r;
    default: return 0;
    }
}
static float f32(const jet_args *a, int i) { float f; memcpy(&f, &a->d[i], 4); return f; }
static void *p_clearColor, *p_clearDepthf, *p_uniform1f, *p_uniform2f, *p_uniform4f, *p_lineWidth;
static uint64_t h_clearColor(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(float, float, float, float))p_clearColor)(f32(a, 0), f32(a, 1), f32(a, 2), f32(a, 3)); return 0; }
static uint64_t h_clearDepthf(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(float))p_clearDepthf)(f32(a, 0)); return 0; }
static uint64_t h_uniform1f(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(int32_t, float))p_uniform1f)((int32_t)a->x[0], f32(a, 0)); return 0; }
static uint64_t h_uniform2f(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(int32_t, float, float))p_uniform2f)((int32_t)a->x[0], f32(a, 0), f32(a, 1)); return 0; }
static uint64_t h_uniform4f(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(int32_t, float, float, float, float))p_uniform4f)((int32_t)a->x[0], f32(a, 0), f32(a, 1), f32(a, 2), f32(a, 3)); return 0; }
static uint64_t h_lineWidth(jet_ctx *c, const jet_args *a, void *u) { (void)c; (void)u; ((void (*)(float))p_lineWidth)(f32(a, 0)); return 0; }

int jet_gles_bridge_install(jet_ctx *ctx) {
    const char *off = getenv("JET_GLES_TRAPS");
    if (!ctx || (off && *off == '0')) return 0;
    const char *libs[] = { getenv("JET_GLES_LIB"), "libGLESv2.so.2", "libGLESv2.so",
                           "/System/Library/Frameworks/OpenGLES.framework/OpenGLES" };
    for (int i = 0; i < 4 && !g_gl; i++) if (libs[i]) g_gl = dlopen(libs[i], RTLD_LAZY | RTLD_GLOBAL);
    g_egl = dlopen("libEGL.so.1", RTLD_LAZY | RTLD_GLOBAL);
    if (!gl_sym("glClear")) return 0;   /* no host GLES: keep the partner stubs */
    int n = 0;
    for (size_t i = 0; i < sizeof g_int / sizeof g_int[0]; i++) {
        g_int[i].fn = gl_sym(g_int[i].name);
        if (g_int[i].fn && jet_native_register_host(ctx, g_int[i].name, h_int, &g_int[i]) == 0) n++;
    }
    struct { const char *n; void **p; jet_host_fn h; } fl[] = {
        {"glClearColor", &p_clearColor, h_clearColor}, {"glClearDepthf", &p_clearDepthf, h_clearDepthf},
        {"glUniform1f", &p_uniform1f, h_uniform1f}, {"glUniform2f", &p_uniform2f, h_uniform2f},
        {"glUniform4f", &p_uniform4f, h_uniform4f}, {"glLineWidth", &p_lineWidth, h_lineWidth},
    };
    for (size_t i = 0; i < sizeof fl / sizeof fl[0]; i++)
        if ((*fl[i].p = gl_sym(fl[i].n)) && jet_native_register_host(ctx, fl[i].n, fl[i].h, NULL) == 0) n++;
    return n;
}
