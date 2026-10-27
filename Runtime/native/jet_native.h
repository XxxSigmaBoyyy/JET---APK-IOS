/* JET native APK-.so API — contract owned by the native partner (Runtime/native/).
 * JVM/DEX side codes against this header; stub implementations return UNIMP until partner lands. */
#pragma once
#include <stdint.h>
#include <stddef.h>
#ifdef __cplusplus
extern "C" {
#endif

typedef struct jet_ctx jet_ctx;
typedef struct jet_lib jet_lib;
typedef struct jet_config jet_config; /* opaque / partner-defined; NULL = defaults */

typedef struct {
    uint64_t x[8];
    double   d[8];
    const uint64_t *stack; /* optional extra GP args beyond x0-x7; NULL if none */
} jet_args;

typedef uint64_t (*jet_host_fn)(jet_ctx *ctx, const jet_args *args, void *user);

typedef void (*jet_register_cb)(void *user, const char *cls, const char *name,
                                const char *sig, uint64_t guest_fn);

jet_ctx  *jet_native_create(const jet_config *cfg);
void      jet_native_destroy(jet_ctx *ctx);

int       jet_native_register_host(jet_ctx *ctx, const char *sym, jet_host_fn fn, void *user);

jet_lib  *jet_native_load(jet_ctx *ctx, const char *path, char *err, size_t errlen);
void      jet_native_unload(jet_ctx *ctx, jet_lib *lib);

uint64_t  jet_native_sym(jet_lib *lib, const char *name);

int       jet_native_call(jet_ctx *ctx, uint64_t guest_fn, const jet_args *args,
                          uint64_t *ret_x0, double *ret_d0);

/* Bind host JavaVM / JNIEnv so guest JNI_* trampolines share address space. */
int       jet_jni_bind(jet_ctx *ctx, void *host_javavm, void *host_jnienv);

/* Run guest JNI_OnLoad(JavaVM*, void*); returns JNI version in *version_out. */
int       jet_jni_onload(jet_ctx *ctx, jet_lib *lib, int32_t *version_out);

/* Guest RegisterNatives → callback so the JVM can bind cls/name/sig → guest_fn. */
void      jet_jni_on_register_natives(jet_ctx *ctx, jet_register_cb cb, void *user);

enum { JET_NATIVE_OK = 0, JET_NATIVE_UNIMP = -1, JET_NATIVE_ERR = -2 };

#ifdef __cplusplus
}
#endif
