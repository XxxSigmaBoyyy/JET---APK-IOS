
#include "jet_native.h"
#include <stdio.h>
#include <string.h>
static void log_cb(void *u, int level, const char *msg) {
    (void)u; if (level <= 2) fprintf(stderr, "[L%d] %s\n", level, msg);
}
int main(void) {
    const char *path = "/workspace/jet/proto/harness/out/tgfoss_p33n/natives/arm64-v8a/libtmessages.49.so";
    jet_config cfg = {0}; cfg.log = log_cb; cfg.hwcap = 3;
    jet_ctx *ctx = jet_native_create(&cfg);
    char err[512];
    /* Load WITHOUT running init - need a way. Use jet_elf_load path via load then check. */
    jet_lib *lib = jet_native_load(ctx, path, err, sizeof err);
    printf("load lib=%p err=%s last=%s\n", (void*)lib, err, jet_native_last_error(ctx));
    jet_native_destroy(ctx);
    return 0;
}
