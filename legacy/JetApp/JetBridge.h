// C ABI between the Swift/UIKit host and the AOT-compiled Android code + JET runtime.
// In the real build, jet_runtime_main() is provided by the AOT output (RoboVM/MobiVM or our own
// DEX->LLVM backend) linked as a static library. In this skeleton it is a stub (JetBridgeStub.c).
#pragma once
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
/// Returns 0 when the translated Activity started, <0 when the runtime is not linked.
int32_t jet_runtime_main(const char *bundle_path, const char *activity_class, void *ui_view);
/// Touch input from UIKit in Android pixel coordinates. action: 0=down 1=up 2=move 3=cancel.
void jet_runtime_touch(int32_t pointer_id, int32_t action, float x, float y);
const char *jet_runtime_status(void);
#ifdef __cplusplus
}
#endif
