// JetJVM — embedded no-JIT JVM (OpenJDK 21 Zero interpreter) for running Android app code on iOS (M2, option A).
// The JRE (java.home layout) is bundled at JET.app/jre by CI (scripts/jre/build_jre_ios.sh); libjvm is dlopen'ed.
#pragma once
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif

/// 1 if this binary was compiled with JNI support (jni.h available at build time).
int32_t jet_jvm_compiled(void);

/// Boot the JVM once (idempotent). java_home: dir containing lib/zero/libjvm.dylib (or lib/server/...).
/// classpath: ':'-separated. extra_opts: '\n'-separated -D/-X options (may be NULL). log_path: JVM vfprintf output
/// (may be NULL). Runs JNI_CreateJavaVM on a dedicated 16 MB-stack pthread. Returns 0 or a negative error;
/// err receives a message. Blocking; call off the main thread.
int32_t jet_jvm_boot(const char *java_home, const char *classpath, const char *extra_opts, const char *log_path,
                     char *err, int32_t errlen);

/// Call `static String <method>(String)` on class `cls` (binary name with '/', e.g. "dev/jet/Hello") on a fresh
/// attached big-stack thread. The result (or the exception text) is copied to out. Returns 0, or <0 on error.
int32_t jet_jvm_call_string(const char *cls, const char *method, const char *arg, char *out, int32_t outlen);

/// Milliseconds since this process was created (kernel start time), for cold-start measurements; -1 if unknown.
double jet_process_age_ms(void);

/// Milliseconds spent in JNI_CreateJavaVM by jet_jvm_boot (or -1).
double jet_jvm_boot_ms(void);

/// Borrowed JavaVM* after a successful boot (NULL otherwise).
void *jet_jvm_vm(void);

/// Pixel sink of the Android shim (jet.ios.JetIosHost.fb, Java ARGB ints = little-endian BGRA bytes, i.e. CGImage
/// byteOrder32Little + noneSkipFirst). If the frame sequence differs from *seq, copies up to cap pixels into dst,
/// sets *w, *h, *seq and returns 1; returns 0 when unchanged, -7 (with *w/*h set) when cap is too small,
/// other <0 when no JVM/frame. The calling thread is attached
/// to the JVM as a daemon on first use and stays attached: call from one long-lived thread with >= 1 MB stack.
int32_t jet_jvm_frame(uint32_t *dst, int32_t cap, int32_t *w, int32_t *h, int32_t *seq);

/// Touch into the shim (MotionEvent action 0=down 1=up 2=move 3=cancel; x/y in frame pixels). Same thread rules as
/// jet_jvm_frame. Returns 0 or <0.
int32_t jet_jvm_touch(int32_t action, float x, float y);

#ifdef __cplusplus
}
#endif
