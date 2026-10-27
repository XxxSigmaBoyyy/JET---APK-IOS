/* jet_native.h - JET embeddable no-JIT AArch64 user-mode interpreter.
 * Public C11 API. Guest (arm64 .so from an APK) runs in the SAME address
 * space as the host; guest memory is mapped RW and never executed natively.
 * The interpreter fetches/decodes/executes guest instructions.
 *
 * Portable: no inline asm, no PROT_EXEC, little-endian 64-bit host assumed.
 */
#ifndef JET_NATIVE_H
#define JET_NATIVE_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct jet_ctx jet_ctx;
typedef struct jet_lib jet_lib;

/* AAPCS64 call descriptor: integer args in x[0..7], FP/SIMD in d[0..7],
 * overflow (stack) args via `stack` (8-byte slots laid out per Linux AAPCS64).
 * `x8` is the indirect result register (IPRIN): address of the hidden return
 * slot for large structs / std::string returned by value. Host shims that
 * return >16-byte aggregates must write through G2H(a->x8). All host<->guest
 * trampolines must forward x8.
 * For host trap functions `stack` points at the guest SP at the call, so the
 * host may read as many stack slots as its signature needs. When calling INTO
 * the guest, use jet_native_call_ex to say how many stack slots to copy. */
typedef struct {
    uint64_t x[8];
    uint64_t x8;              /* AAPCS64 indirect result (must be forwarded) */
    double   d[8];
    const uint64_t *stack;
} jet_args;

/* A host function exposed to the guest via the trap table. Called when the
 * guest branches to the magic stub bound to `sym`. Reads args from *a. */
typedef uint64_t (*jet_host_fn)(jet_ctx *ctx, const jet_args *a, void *user);

/* Log callback. level: 0=err 1=warn 2=info 3=debug/trace. */
typedef void (*jet_log_fn)(void *user, int level, const char *msg);

typedef struct {
    uint64_t hwcap;           /* AT_HWCAP reported to guest. 0 => base (FP|ASIMD). */
    uint64_t hwcap2;          /* AT_HWCAP2. 0 => none. */
    size_t   thread_stack_size; /* per-guest-thread stack; 0 => 8 MiB default. */
    jet_log_fn log;
    void    *log_user;
    int      trace;           /* 1 => per-instruction trace via log cb (slow). */
} jet_config;

/* ---- lifecycle ---- */
jet_ctx *jet_native_create(const jet_config *cfg);
void     jet_native_destroy(jet_ctx *ctx);

/* ---- host trap table (imports) ---- */
/* Bind an imported symbol name to a host function. Resolving an undefined
 * guest symbol of this name yields a magic stub address the interpreter traps. */
int jet_native_register_host(jet_ctx *ctx, const char *sym, jet_host_fn fn, void *user);

/* ---- loading ---- */
jet_lib *jet_native_load(jet_ctx *ctx, const char *path, char *err, size_t errlen);
void     jet_native_unload(jet_lib *lib); /* runs fini_array */

/* Resolve an exported dynsym (e.g. "JNI_OnLoad", "Java_..."). 0 if absent. */
uint64_t jet_native_sym(jet_lib *lib, const char *name);

/* ---- calling into the guest ---- */
/* Call guest function at guest_fn with AAPCS64 args from *a. Returns 0 on ok. */
int jet_native_call(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a,
                    uint64_t *ret_x0, double *ret_d0);

/* Same, copying `nstack` 8-byte stack-argument slots from a->stack. */
int jet_native_call_ex(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a,
                       uint32_t nstack, uint64_t *ret_x0, double *ret_d0);

/* Register host data object (e.g. __sF, SL_IID_ENGINE) for OBJECT imports. */
int jet_native_register_data(jet_ctx *ctx, const char *sym, void *addr);

/* Flags variant: JET_HOST_RET_F64 => returned uint64 is a double bit pattern
 * placed in d0; JET_HOST_RET_F32 => low 32 bits are a float placed in s0. */
#define JET_HOST_RET_F64 1u
#define JET_HOST_RET_F32 2u
int jet_native_register_host_ex(jet_ctx *ctx, const char *sym, jet_host_fn fn,
                                void *user, unsigned flags);

/* ---- host->guest callback trampoline ---- */
/* Turn a guest function address into a host-callable function pointer usable
 * where a C callback is expected (comparators, thread start routines, atexit,
 * signal handlers). The returned pointer re-enters the interpreter on the
 * calling host thread. Pool is finite; stubs are not freed individually. */
typedef void (*jet_trampoline)(void); /* cast to the needed signature */
jet_trampoline jet_native_trampoline(jet_ctx *ctx, uint64_t guest_fn);

/* ---- JNI bridge ---- */
/* Provide the host JavaVM* and JNIEnv*. The interpreter builds a guest-visible
 * JNIEnv whose 233 function slots are magic stubs forwarding to host JNIEnv. */
int jet_jni_bind(jet_ctx *ctx, void *host_javavm, void *host_jnienv);

/* Call the library's JNI_OnLoad(JavaVM*, void*). *version_out gets the result. */
int jet_jni_onload(jet_ctx *ctx, jet_lib *lib, int32_t *version_out);

/* Guest-visible JNIEnv* (identity-mapped pointer) for calling natives. */
uint64_t jet_jni_guest_env(jet_ctx *ctx);

/* Callback invoked for each RegisterNatives entry the guest registers. */
typedef void (*jet_register_cb)(void *user, const char *cls, const char *name,
                                const char *sig, uint64_t guest_fn);
void jet_jni_on_register_natives(jet_ctx *ctx, jet_register_cb cb, void *user);

/* Emulated guest cwd (absolute path). Used by getcwd / syscall getcwd. */
int         jet_native_set_cwd(jet_ctx *ctx, const char *path);
const char *jet_native_get_cwd(jet_ctx *ctx);

/* ClassLoader (host jobject) used by guest FindClass / JNI_OnLoad. */
int  jet_native_set_class_loader(jet_ctx *ctx, void *class_loader);
void *jet_native_get_class_loader(jet_ctx *ctx);
/* Attach a ClassLoader to a loaded library (used during its JNI_OnLoad). */
void jet_native_lib_set_class_loader(jet_lib *lib, void *class_loader);

/* Test helpers */
const char *jet_jni_slot_name(int idx);
uint64_t    jet_jni_call_env(jet_ctx *ctx, int idx, const jet_args *a);

/* Introspection: last unimplemented-opcode info (for reporting). */
const char *jet_native_last_error(jet_ctx *ctx);

/* Invoke a registered host trap by symbol name (tests / tooling). */
int jet_native_call_host(jet_ctx *ctx, const char *sym, const jet_args *a, uint64_t *ret);

#ifdef __cplusplus
}
#endif
#endif /* JET_NATIVE_H */
