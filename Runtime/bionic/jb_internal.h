// JET generic bionic bridge — shared definitions. Generic: implements the Android arm64 (bionic) ABI on Darwin.
#pragma once
#include <stdint.h>
#include <stddef.h>
#include <stdarg.h>
#include <stdio.h>
#define JBX __attribute__((visibility("default"), used))
typedef struct { uint64_t x[8]; uint8_t v[8][16]; uint64_t *stk; uint64_t pad; } jb_regs;
_Static_assert(sizeof(jb_regs) == 208, "jb_regs layout");
// AAPCS64 va_list (passed by reference: 32-byte struct)
typedef struct { void *stack; void *gr_top; void *vr_top; int32_t gr_offs; int32_t vr_offs; } jb_aapcs_va;
// unified argument cursor over either saved registers or an AAPCS va_list
typedef struct { jb_regs *r; int ngr, nvr; uint8_t *stk; jb_aapcs_va *va; } jb_cur;
void jb_cur_regs(jb_cur *c, jb_regs *r, int fixed_gp, int fixed_fp);
void jb_cur_va(jb_cur *c, jb_aapcs_va *va);
uint64_t jb_next_gp(jb_cur *c);
double jb_next_fp(jb_cur *c);         // double (also converts binary128 when long_double)
double jb_next_ld(jb_cur *c);         // Android long double = IEEE binary128 -> double
// format-driven marshalling into a Darwin va_list (array of 8-byte slots)
int jb_marshal_printf(const char *fmt, jb_cur *c, uint64_t *slots, int max);
int jb_marshal_wprintf(const wchar_t *fmt, jb_cur *c, uint64_t *slots, int max);
int jb_marshal_scanf(const char *fmt, jb_cur *c, uint64_t *slots, int max);
int jb_marshal_wscanf(const wchar_t *fmt, jb_cur *c, uint64_t *slots, int max);
#define JB_DVA(slots) ((va_list)(char *)(slots))   /* Darwin arm64: va_list is char*, 8-byte slots */
FILE *jb_file(void *bionic_fp);       // map bionic __sF[0..2] to Darwin stdio
int jb_errno_to_linux(int e);
int jb_errno_from_linux(int e);
void jb_set_errno_linux(int e);
void jb_log(int prio, const char *tag, const char *msg);
// translators
int jb_sig_from_linux(int s);
int jb_sig_to_linux(int s);
