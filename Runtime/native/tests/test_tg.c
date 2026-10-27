/* Acceptance test: load stock Telegram FOSS libtmessages.49.so and run JNI_OnLoad. */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

static const char *TG_SO =
    "/workspace/jet/proto/harness/out/tgfoss_p33n/natives/arm64-v8a/libtmessages.49.so";

static void log_cb(void *u, int level, const char *msg) {
    (void)u;
    if (level <= 2) fprintf(stderr, "[tg L%d] %s\n", level, msg);
}

static uint64_t g_cm_native_init;
static void on_reg(void *user, const char *cls, const char *name,
                   const char *sig, uint64_t guest_fn) {
    int *count = user;
    (*count)++;
    fprintf(stderr, "  REG %4d  %s.%s%s  fn=0x%llx\n",
            *count, cls, name, sig, (unsigned long long)guest_fn);
    if (cls && name && strstr(cls, "ConnectionsManager") && strcmp(name, "native_init") == 0)
        g_cm_native_init = guest_fn;
}

int main(int argc, char **argv) {
    const char *path = argc > 1 ? argv[1] : TG_SO;
    jet_config cfg = {0};
    cfg.log = log_cb;
    /* Keep HWCAP to FP|ASIMD only (no AES/SHA/LSE). */
    cfg.hwcap = (1ull<<0) | (1ull<<1);

    jet_ctx *ctx = jet_native_create(&cfg);
    if (!ctx) { fprintf(stderr, "create failed\n"); return 1; }

    int reg_count = 0;
    jet_jni_on_register_natives(ctx, on_reg, &reg_count);

    char err[512];
    fprintf(stderr, "Loading %s ...\n", path);
    jet_lib *lib = jet_native_load(ctx, path, err, sizeof err);
    if (!lib) {
        fprintf(stderr, "LOAD FAILED: %s\n", err);
        return 2;
    }
    fprintf(stderr, "LOAD OK. last_error='%s'\n", jet_native_last_error(ctx));

    /* Even if init_array failed, try symbol lookup */
    uint64_t onload = jet_native_sym(lib, "JNI_OnLoad");
    fprintf(stderr, "JNI_OnLoad sym = 0x%llx\n", (unsigned long long)onload);

    /* List a few Java_* exports */
    const char *probe[] = {
        "Java_org_telegram_messenger_Utilities_aesCtrDecryption",
        "Java_org_telegram_messenger_Utilities_aesCbcEncryptionByteArray",
        "Java_org_telegram_messenger_Utilities_pbkdf2",
        "Java_org_telegram_messenger_Utilities_aesIgeEncryption",
        "Java_org_telegram_messenger_FileLog_getNativeStackTrace",
        "Java_org_telegram_messenger_Utilities_pinBitmap",
        "Java_org_telegram_messenger_Intro_setScrollOffset",
        "Java_org_telegram_SQLite_SQLitePreparedStatement_step",
        NULL
    };
    for (int i = 0; probe[i]; i++) {
        uint64_t s = jet_native_sym(lib, probe[i]);
        fprintf(stderr, "  sym %s = 0x%llx\n", probe[i], (unsigned long long)s);
    }

    int32_t ver = 0;
    int rc = jet_jni_onload(ctx, lib, &ver);
    fprintf(stderr, "JNI_OnLoad call rc=%d ver=0x%x regs=%d err='%s'\n",
            rc, (unsigned)ver, reg_count, jet_native_last_error(ctx));

    /* Try calling a couple of simple Java_* methods if OnLoad worked or even if not */
    /* Intro_setScrollOffset(F)V — void, takes float */
    uint64_t setoff = jet_native_sym(lib, "Java_org_telegram_messenger_Intro_setScrollOffset");
    if (setoff) {
        jet_args a = {0};
        a.x[0] = 0; a.x[1] = 0; /* env, obj */
        float f = 0.5f; memcpy(&a.d[0], &f, 4); /* actually float arg is in s0 = v0 */
        /* JNI: (JNIEnv*, jobject, jfloat) — jfloat in s0 / d0 per AAPCS64 */
        uint32_t bits; memcpy(&bits, &f, 4);
        a.x[2] = 0; /* unused for float — float is in V0 */
        /* Put float bits into v0 via call args d[0] */
        double d = 0; memcpy(&d, &bits, 4);
        a.d[0] = 0; memcpy(&a.d[0], &f, 4);
        uint64_t r=0;
        int crc = jet_native_call(ctx, setoff, &a, &r, NULL);
        fprintf(stderr, "call Intro_setScrollOffset rc=%d err='%s'\n",
                crc, jet_native_last_error(ctx));
    }

    uint64_t pin = jet_native_sym(lib, "Java_org_telegram_messenger_Utilities_pinBitmap");
    if (pin) {
        jet_args a = {0}; a.x[0]=0; a.x[1]=0; a.x[2]=0;
        uint64_t r=0;
        int crc = jet_native_call(ctx, pin, &a, &r, NULL);
        fprintf(stderr, "call pinBitmap rc=%d ret=%llu err='%s'\n",
                crc, (unsigned long long)r, jet_native_last_error(ctx));
    }

    /* Stretch: ConnectionsManager.native_init */
    fprintf(stderr, "native_init fn=0x%llx env=0x%llx\n",
            (unsigned long long)g_cm_native_init,
            (unsigned long long)jet_jni_guest_env(ctx));
    if (g_cm_native_init) {
        uint64_t env = jet_jni_guest_env(ctx);
        /* jstrings are char* in our fake JNI */
        const char *s_device = "JetDevice";
        const char *s_sys = "Android14";
        const char *s_app = "10.0.0";
        const char *s_lang = "en";
        const char *s_slang = "en-US";
        const char *s_cfg = "/data/data/org.telegram.messenger/files";
        const char *s_log = "/data/data/org.telegram.messenger/files/logs";
        const char *s_reg = "";
        const char *s_fp = "jet-fingerprint";
        const char *s_inst = "com.android.vending";
        const char *s_pkg = "org.telegram.messenger";
        /* Stack args after x0..x7: 9 strings + I + J + Z + Z + Z + I + I = 9+1+1+3+2 = 16 slots
         * (jlong is one 8-byte slot). */
        uint64_t stack[20];
        memset(stack, 0, sizeof stack);
        stack[0] = (uint64_t)(uintptr_t)s_app;
        stack[1] = (uint64_t)(uintptr_t)s_lang;
        stack[2] = (uint64_t)(uintptr_t)s_slang;
        stack[3] = (uint64_t)(uintptr_t)s_cfg;
        stack[4] = (uint64_t)(uintptr_t)s_log;
        stack[5] = (uint64_t)(uintptr_t)s_reg;
        stack[6] = (uint64_t)(uintptr_t)s_fp;
        stack[7] = (uint64_t)(uintptr_t)s_inst;
        stack[8] = (uint64_t)(uintptr_t)s_pkg;
        stack[9] = 0;            /* timezone offset int */
        stack[10] = 0;           /* userId jlong */
        stack[11] = 1;           /* enablePushConnection */
        stack[12] = 0;           /* enableProxyConnection */
        stack[13] = 0;           /* enableNetworkDataSaving */
        stack[14] = 0;           /* performanceClass */
        stack[15] = 0;           /* last int */
        jet_args a = {0};
        a.x[0] = env;
        a.x[1] = 0; /* jclass */
        a.x[2] = 0; /* instanceNum */
        a.x[3] = 12345; /* version */
        a.x[4] = 178; /* layer */
        a.x[5] = 4; /* apiId (test) */
        a.x[6] = (uint64_t)(uintptr_t)s_device;
        a.x[7] = (uint64_t)(uintptr_t)s_sys;
        a.stack = stack;
        uint64_t r = 0;
        int crc = jet_native_call_ex(ctx, g_cm_native_init, &a, 16, &r, NULL);
        fprintf(stderr, "call native_init rc=%d ret=%llu err='%s'\n",
                crc, (unsigned long long)r, jet_native_last_error(ctx));
    }

    fprintf(stderr, "\n=== SUMMARY ===\n");
    fprintf(stderr, "load: %s\n", lib ? "OK" : "FAIL");
    fprintf(stderr, "JNI_OnLoad: rc=%d ver=0x%x\n", rc, (unsigned)ver);
    fprintf(stderr, "RegisterNatives captured: %d\n", reg_count);
    fprintf(stderr, "last_error: %s\n", jet_native_last_error(ctx));

    jet_native_destroy(ctx);
    /* Exit 0 if load succeeded; partial OnLoad still useful */
    return lib ? 0 : 1;
}
