// Placeholder until the AOT-compiled runtime library is linked (PLAN.md, section 9).
#include "JetBridge.h"
int32_t jet_runtime_main(const char *bundle_path, const char *activity_class, void *ui_view) { (void)bundle_path; (void)activity_class; (void)ui_view; return -1; }
void jet_runtime_touch(int32_t pointer_id, int32_t action, float x, float y) { (void)pointer_id; (void)action; (void)x; (void)y; }
const char *jet_runtime_status(void) { return "JET runtime not linked (skeleton build)"; }
