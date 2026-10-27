// Stub implementation of JetBridge.h (ABI v1), compiled only when the JET runtime library is not linked.
// A runtime build defines JET_RUNTIME_LINKED (project.runtime.yml) and links Vendor/JetRuntime/lib/libjetruntime.a.
#ifndef JET_RUNTIME_LINKED
#include "JetBridge.h"
#include <stdio.h>
#include <string.h>

static const char *g_status = "JET runtime is not linked into this build (shell preview).";

int32_t jet_bridge_abi_version(void) { return JET_BRIDGE_ABI_VERSION; }
const char *jet_bridge_version(void) { return "stub-0.2"; }
int32_t jet_bridge_has_runtime(void) { return 0; }
const char *jet_bridge_modules(void) { return ""; }

int32_t jet_translate_apk(const char *apk_path, const char *bundle_dir, jet_progress_fn progress, void *ctx) {
    if (!apk_path || !bundle_dir || !*apk_path || !*bundle_dir) return JET_BRIDGE_ERR_INPUT;
    if (progress) progress(0.0f, "stub", ctx);
    char path[4096];
    if (snprintf(path, sizeof path, "%s/app.properties", bundle_dir) >= (int) sizeof path) return JET_BRIDGE_ERR_INPUT;
    FILE *f = fopen(path, "w");
    if (!f) return JET_BRIDGE_ERR_IO;
    const char *base = strrchr(apk_path, '/');
    fprintf(f, "jet.bridge=%s\njet.abi=%d\njet.translated=false\napk=%s\n", jet_bridge_version(), JET_BRIDGE_ABI_VERSION, base ? base + 1 : apk_path);
    fclose(f);
    if (progress) progress(1.0f, "stub", ctx);
    return JET_BRIDGE_STUB;
}

int32_t jet_runtime_main(const char *bundle_path, const char *activity_class, void *ui_view) {
    (void) bundle_path; (void) activity_class; (void) ui_view;
    return JET_RUNTIME_NOT_LINKED;
}

void jet_runtime_touch(int32_t pointer_id, int32_t action, float x, float y) { (void) pointer_id; (void) action; (void) x; (void) y; }
int32_t jet_runtime_ui_snapshot(char *buf, int32_t cap) { if (buf && cap > 0) buf[0] = 0; return JET_RUNTIME_NOT_LINKED; }
int32_t jet_runtime_click(const char *label) { (void) label; return JET_RUNTIME_NOT_LINKED; }
void jet_runtime_stop(void) {}
const char *jet_runtime_status(void) { return g_status; }
#endif
