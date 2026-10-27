/* JET native AOT runtime ABI (portable C11 + GNU statement-expressions; builds with clang for
 * x86_64-linux and arm64-apple-ios). Generated code (jet.aot.JimpleToC) targets only this header. */
#ifndef JETRT_H
#define JETRT_H
#include <stdint.h>
#include <stddef.h>
typedef union jv { int32_t i; int64_t j; float f; double d; void *l; } jv;
typedef jv (*JFn)(jv *a);
typedef struct { const char *sub; JFn fn; } JMeth;
typedef struct JClass JClass;
struct JClass {
    const char *name; const char *supername; JClass *super;
    int nifaces; const char **ifacenames; JClass **ifaces;
    int nslots; int nmeth; const JMeth *meths;
    jv *statics; JFn clinit; int initd; int isarray;
};
#define JET_KSLOTS 8            /* slots reserved for kernel (native shim) classes in every object */
typedef struct JObj { JClass *cls; jv slot[]; } JObj;
typedef struct JArr { JClass *cls; int32_t len; int32_t pad; jv d[]; } JArr;
typedef struct { int depth; const char *cls; int id; const char *text; const char *desc; const char *const *attrs; } JResNode;
/* drawable table (tools/aot/gen_res.py): kind 0 none/unsupported, 1 solid color, 2 selector, 3 inline color item.
 * selector rows are (required-state mask, required-state values, child drawable id) triplets ending with -1 */
typedef struct { int32_t id; int32_t kind; uint32_t color; const int32_t *states; } JResDrawable;
extern const JResDrawable jet_drawables[]; extern const int32_t jet_theme_window_bg; extern const uint32_t jet_theme_text_color;
typedef struct { int id; const JResNode *nodes; } JResLayout;
typedef struct { int id; const char *s; } JResString;
extern const JResLayout jet_layouts[]; extern const JResString jet_strings[];

extern void *rt_exc;                         /* pending exception (ParparVM-style, no setjmp) */
extern JClass rt_array_class;
void rt_register(JClass *c);                 /* app + kernel class registry */
JClass *rt_class(const char *name);          /* by name; phantom class for unknown interfaces */
JClass *rt_class_strict(const char *name);   /* aborts with AOT-MISSING if absent */
void rt_link(void);                          /* resolve supers/interfaces */
void rt_initclass(JClass *c);
JFn rt_vfind(JClass *c, const char *sub);    /* virtual/interface dispatch */
JFn rt_sfind(const char *cls, const char *sub); /* special/static dispatch into kernel or app */
void *rt_new(JClass *c);
void *rt_newarray(int32_t n);
int32_t rt_instanceof(void *o, JClass *c);
void *rt_checkcast(void *o, JClass *c);
void *rt_str(const char *utf8);              /* java.lang.String from UTF-8 */
const char *rt_cstr(void *s);                /* UTF-8 view of a java.lang.String (NULL -> "null") */
void *rt_throw_new(const char *cls, const char *msg); /* sets rt_exc, returns it */
_Noreturn void rt_fatal(const char *what, const char *detail);
extern void (*rt_fatal_hook)(const char *what, const char *detail); /* if set and it returns, rt_fatal exits as before */
int32_t rt_idiv(int32_t a, int32_t b); int32_t rt_irem(int32_t a, int32_t b);
int64_t rt_ldiv(int64_t a, int64_t b); int64_t rt_lrem(int64_t a, int64_t b);
int32_t rt_d2i(double d); int64_t rt_d2l(double d);
jv *jet_kstatic(const char *cls, const char *name);   /* kernel static field */
void *jet_classobj(const char *name);                  /* java.lang.Class object */
void jet_kernel_register(void); void jet_app_register(void);
#define RT_INIT(c) do { if (!(c).initd) rt_initclass(&(c)); } while (0)
#define RT_NN(o) ({ void *_n = (o); if (!_n) rt_fatal("NullPointerException", "null receiver/field base"); _n; })
#define RT_F(o, k) (((JObj *) RT_NN(o))->slot[k])
#define RT_A(a, i) (((JArr *) rt_aidx((a), (i)))->d[(i)])
static inline void *rt_aidx(void *a, int32_t i) { JArr *x = (JArr *) a; if (!x) rt_fatal("NullPointerException", "array"); if (i < 0 || i >= x->len) rt_fatal("ArrayIndexOutOfBoundsException", "index"); return x; }
#define RT_LEN(a) (((JArr *) RT_NN(a))->len)
#define RT_LCMP(a, b) ((a) > (b) ? 1 : (a) == (b) ? 0 : -1)
#define RT_CMPL(a, b) ((a) > (b) ? 1 : (a) == (b) ? 0 : -1)
#define RT_CMPG(a, b) ((a) < (b) ? -1 : (a) == (b) ? 0 : 1)
#endif
