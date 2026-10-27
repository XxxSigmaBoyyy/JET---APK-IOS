// JET generic bionic bridge — AAPCS64 variadic entry trampolines (Darwin arm64 assembler).
// Android (AAPCS64) passes variadic args in x0-x7 / q0-q7 then the stack; Darwin arm64 passes every variadic
// arg on the stack. Each _jb_<fn> saves the full argument register file plus the caller's stack-args pointer
// into a jb_regs block and calls the C marshaller _jbv_<fn>(jb_regs*), which knows the fixed-arg count.
// Layout (must match jb_internal.h): x[8] @0, v[8][16] @64, stk @192, size 208.
    .text
.macro JB_VA name
    .globl _jb_\name
    .p2align 2
_jb_\name:
    stp     x29, x30, [sp, #-16]!
    mov     x29, sp
    sub     sp, sp, #208
    stp     x0, x1, [sp, #0]
    stp     x2, x3, [sp, #16]
    stp     x4, x5, [sp, #32]
    stp     x6, x7, [sp, #48]
    stp     q0, q1, [sp, #64]
    stp     q2, q3, [sp, #96]
    stp     q4, q5, [sp, #128]
    stp     q6, q7, [sp, #160]
    add     x9, x29, #16
    str     x9, [sp, #192]
    mov     x0, sp
    bl      _jbv_\name
    mov     sp, x29
    ldp     x29, x30, [sp], #16
    ret
.endm
    JB_VA printf
    JB_VA fprintf
    JB_VA sprintf
    JB_VA snprintf
    JB_VA swprintf
    JB_VA asprintf
    JB_VA dprintf
    JB_VA sscanf
    JB_VA fscanf
    JB_VA scanf
    JB_VA swscanf
    JB_VA syslog
    JB_VA __android_log_print
    JB_VA __android_log_assert
    JB_VA __android_log_buf_print
    JB_VA __snprintf_chk
    JB_VA __sprintf_chk
