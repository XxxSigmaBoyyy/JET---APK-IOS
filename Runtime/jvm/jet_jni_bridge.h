#pragma once
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
/* After jet_jvm_boot: create jet_ctx, jet_jni_bind(vm,env), install RegisterNatives cb,
 * and JNI-register dev.jet.nativeapi.Natives so Java can load APK .so files. */
int32_t jet_jni_bridge_init(char *err, int32_t errlen);
/* Direct C entry used by tests / Swift: load a .so from a filesystem path (extracted from APK). */
int32_t jet_jni_bridge_load_path(const char *path, char *err, int32_t errlen);
#ifdef __cplusplus
}
#endif
