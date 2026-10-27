/* Linkable stub until partner Runtime/native implementation lands. */
#include "jet_native.h"
#include <stdio.h>
#include <string.h>

jet_ctx *jet_native_create(const jet_config *cfg){ (void)cfg; return NULL; }
void jet_native_destroy(jet_ctx *ctx){ (void)ctx; }
int jet_native_register_host(jet_ctx *ctx, const char *sym, jet_host_fn fn, void *user){
    (void)ctx;(void)sym;(void)fn;(void)user; return JET_NATIVE_UNIMP;
}
jet_lib *jet_native_load(jet_ctx *ctx, const char *path, char *err, size_t errlen){
    (void)ctx; snprintf(err, errlen, "jet_native_load(%s): UNIMP (partner)", path ? path : "?");
    return NULL;
}
void jet_native_unload(jet_ctx *ctx, jet_lib *lib){ (void)ctx;(void)lib; }
uint64_t jet_native_sym(jet_lib *lib, const char *name){ (void)lib;(void)name; return 0; }
int jet_native_call(jet_ctx *ctx, uint64_t guest_fn, const jet_args *args,
                    uint64_t *ret_x0, double *ret_d0){
    (void)ctx;(void)guest_fn;(void)args;(void)ret_x0;(void)ret_d0; return JET_NATIVE_UNIMP;
}
int jet_jni_bind(jet_ctx *ctx, void *host_javavm, void *host_jnienv){
    (void)ctx;(void)host_javavm;(void)host_jnienv; return JET_NATIVE_UNIMP;
}
int jet_jni_onload(jet_ctx *ctx, jet_lib *lib, int32_t *version_out){
    (void)ctx;(void)lib;(void)version_out; return JET_NATIVE_UNIMP;
}
void jet_jni_on_register_natives(jet_ctx *ctx, jet_register_cb cb, void *user){
    (void)ctx;(void)cb;(void)user;
}
