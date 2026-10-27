/* JET native AOT core runtime: class registry, dispatch, allocation, type checks. Portable C. */
#include "jetrt.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
void *rt_exc;
JClass rt_array_class = { "[java.lang.Object", "java.lang.Object", NULL, 0, NULL, NULL, 0, 0, NULL, NULL, NULL, 1, 1 };
static JClass *reg[1024]; static int nreg;
void rt_register(JClass *c) { if (nreg < 1024) reg[nreg++] = c; }
JClass *rt_find(const char *n) { for (int i = 0; i < nreg; i++) if (!strcmp(reg[i]->name, n)) return reg[i]; return NULL; }
JClass *rt_class(const char *n) {
    JClass *c = rt_find(n); if (c) return c;
    c = calloc(1, sizeof *c); c->name = strdup(n); c->supername = "java.lang.Object"; c->nslots = JET_KSLOTS; c->initd = 1;
    c->super = rt_find("java.lang.Object"); rt_register(c); return c;   /* phantom: interfaces/markers only */
}
JClass *rt_class_strict(const char *n) { JClass *c = rt_find(n); if (!c) rt_fatal("AOT-MISSING class", n); return c; }
void rt_link(void) {
    for (int i = 0; i < nreg; i++) {
        JClass *c = reg[i];
        if (c->supername && !c->super) c->super = rt_class_strict(c->supername);
        if (c->nifaces && !c->ifaces) { c->ifaces = calloc(c->nifaces, sizeof(JClass *)); for (int k = 0; k < c->nifaces; k++) c->ifaces[k] = rt_class(c->ifacenames[k]); }
    }
    rt_array_class.super = rt_find("java.lang.Object");
}
void rt_initclass(JClass *c) {
    if (c->initd) return; c->initd = 1;
    if (c->super) rt_initclass(c->super);
    if (c->clinit) { jv a[1] = {{0}}; c->clinit(a); if (rt_exc) rt_fatal("ExceptionInInitializerError", c->name); }
}
JFn rt_vfind(JClass *c, const char *sub) {
    for (JClass *k = c; k; k = k->super) for (int i = 0; i < k->nmeth; i++) if (k->meths[i].sub && !strcmp(k->meths[i].sub, sub)) return k->meths[i].fn;
    char b[512]; snprintf(b, sizeof b, "%s.%s", c ? c->name : "?", sub); rt_fatal("AOT-MISSING method", b);
}
JFn rt_sfind(const char *cls, const char *sub) { return rt_vfind(rt_class_strict(cls), sub); }
void *rt_new(JClass *c) {
    if (!c->initd) rt_initclass(c);
    int n = c->nslots < JET_KSLOTS ? JET_KSLOTS : c->nslots;
    JObj *o = calloc(1, sizeof(JObj) + n * sizeof(jv)); o->cls = c; return o;
}
void *rt_newarray(int32_t n) {
    if (n < 0) rt_fatal("NegativeArraySizeException", "");
    JArr *a = calloc(1, sizeof(JArr) + (size_t) n * sizeof(jv)); a->cls = &rt_array_class; a->len = n; return a;
}
static int isa(JClass *k, JClass *c) {
    for (; k; k = k->super) { if (k == c) return 1; for (int i = 0; i < k->nifaces; i++) if (k->ifaces && isa(k->ifaces[i], c)) return 1; }
    return 0;
}
int32_t rt_instanceof(void *o, JClass *c) { return o && isa(((JObj *) o)->cls, c); }
void *rt_checkcast(void *o, JClass *c) { if (o && !isa(((JObj *) o)->cls, c)) { char b[512]; snprintf(b, sizeof b, "%s -> %s", ((JObj *) o)->cls->name, c->name); rt_fatal("ClassCastException", b); } return o; }
void (*rt_fatal_hook)(const char *what, const char *detail);
_Noreturn void rt_fatal(const char *what, const char *detail) { if (rt_fatal_hook) rt_fatal_hook(what, detail); /* embedders longjmp out */ fflush(stdout); fprintf(stderr, "AOT-FATAL %s: %s\n", what, detail); exit(70); }
int32_t rt_idiv(int32_t a, int32_t b) { if (!b) rt_fatal("ArithmeticException", "/ by zero"); if (a == INT32_MIN && b == -1) return a; return a / b; }
int32_t rt_irem(int32_t a, int32_t b) { if (!b) rt_fatal("ArithmeticException", "% by zero"); if (b == -1) return 0; return a % b; }
int64_t rt_ldiv(int64_t a, int64_t b) { if (!b) rt_fatal("ArithmeticException", "/ by zero"); if (a == INT64_MIN && b == -1) return a; return a / b; }
int64_t rt_lrem(int64_t a, int64_t b) { if (!b) rt_fatal("ArithmeticException", "% by zero"); if (b == -1) return 0; return a % b; }
int32_t rt_d2i(double d) { if (isnan(d)) return 0; if (d >= 2147483647.0) return INT32_MAX; if (d <= -2147483648.0) return INT32_MIN; return (int32_t) d; }
int64_t rt_d2l(double d) { if (isnan(d)) return 0; if (d >= 9223372036854775807.0) return INT64_MAX; if (d <= -9223372036854775808.0) return INT64_MIN; return (int64_t) d; }
