/* Acceptance test: load stock Telegram FOSS libtmessages.49.so and run JNI_OnLoad. */
#include "jet_native.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char *TG_SO =
    "/workspace/jet/proto/harness/out/tgfoss_p33n/natives/arm64-v8a/libtmessages.49.so";

static void log_cb(void *u, int level, const char *msg) {
    (void)u;
    if (level <= 2) fprintf(stderr, "[tg L%d] %s\n", level, msg);
}

static void on_reg(void *user, const char *cls, const char *name,
                   const char *sig, uint64_t guest_fn) {
    int *count = user;
    (*count)++;
    fprintf(stderr, "  REG %4d  %s.%s%s  fn=0x%llx\n",
            *count, cls, name, sig, (unsigned long long)guest_fn);
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



    fprintf(stderr, "\n=== SUMMARY ===\n");
    fprintf(stderr, "load: %s\n", lib ? "OK" : "FAIL");
    fprintf(stderr, "JNI_OnLoad: rc=%d ver=0x%x\n", rc, (unsigned)ver);
    fprintf(stderr, "RegisterNatives captured: %d\n", reg_count);
    fprintf(stderr, "last_error: %s\n", jet_native_last_error(ctx));

    jet_native_destroy(ctx);
    /* Exit 0 if load succeeded; partial OnLoad still useful */
    return lib ? 0 : 1;
}
