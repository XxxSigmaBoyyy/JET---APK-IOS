/* JET native kernel: the native (AOT) counterpart of the Java shim for the members Heading Calculator uses.
 * java.lang subset + a headless android.view/widget core (inflation from generated layout tables, text, tags,
 * listeners). Same ABI as generated code, so app classes subclass these directly. Portable C: the same file
 * compiles for arm64-apple-ios, where the view core would be backed by UIKit/CoreGraphics instead of stdout. */
#include "jetrt.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#define F(name) static jv name(jv *A)
#define O(i) ((JObj *) A[i].l)
#define RI(x) return (jv){.i = (x)}
#define RL(x) return (jv){.l = (x)}
#define RD(x) return (jv){.d = (x)}
#define RV return (jv){0}
#define S(o, k) (((JObj *) (o))->slot[k])
#define KCLASS(var, nm, sup, ...) static const JMeth var##_m[] = {__VA_ARGS__, {NULL, NULL}}; \
    JClass var = {nm, sup, NULL, 0, NULL, NULL, JET_KSLOTS, sizeof(var##_m) / sizeof(JMeth) - 1, var##_m, NULL, NULL, 1, 0};
#define KIFACE(var, nm, ...) static const char *var##_i[] = {__VA_ARGS__}; static const JMeth var##_m[] = {{NULL, NULL}}; \
    JClass var = {nm, "java.lang.Object", NULL, sizeof(var##_i) / sizeof(char *), var##_i, NULL, JET_KSLOTS, 0, var##_m, NULL, NULL, 1, 0};

/* ---------- java.lang.String: slot0 = UTF-8 bytes, slot1 = byte length ---------- */
extern JClass K_String;
void *rt_str(const char *u) { JObj *s = rt_new(&K_String); size_t n = strlen(u); char *b = malloc(n + 1); memcpy(b, u, n + 1); S(s, 0).l = b; S(s, 1).i = (int32_t) n; return s; }
const char *rt_cstr(void *s) { if (!s) return "null"; JObj *o = s; if (o->cls == &K_String) return o->slot[0].l;
    JFn ts = rt_vfind(o->cls, "java.lang.String toString()"); jv a[1] = {{.l = s}}; void *r = ts(a).l; return r ? ((JObj *) r)->slot[0].l : "null"; }
F(str_equals) { void *o = A[1].l; RI(o && ((JObj *) o)->cls == &K_String && !strcmp(rt_cstr(A[0].l), rt_cstr(o))); }
F(str_starts) { const char *a = rt_cstr(A[0].l), *p = rt_cstr(RT_NN(A[1].l)); RI(!strncmp(a, p, strlen(p))); }
F(str_concat) { const char *a = rt_cstr(A[0].l), *b = rt_cstr(RT_NN(A[1].l)); char *r = malloc(strlen(a) + strlen(b) + 1); strcpy(r, a); strcat(r, b); void *s = rt_str(r); free(r); RL(s); }
F(str_substr) { const char *a = rt_cstr(A[0].l); int i = A[1].i; if (i < 0 || i > (int) strlen(a)) rt_fatal("StringIndexOutOfBoundsException", a); RL(rt_str(a + i)); } /* ASCII-exact */
F(str_tostring) { RL(A[0].l); }
F(str_length) { RI(S(A[0].l, 1).i); }
F(str_hash) { int32_t h = 0; for (const unsigned char *p = (const unsigned char *) rt_cstr(A[0].l); *p; p++) h = 31 * h + *p; RI(h); }
extern JClass K_Integer;
F(str_format) { /* String.format(Locale, fmt, Object[]): %d %s %% */
    const char *f = rt_cstr(RT_NN(A[1].l)); JArr *args = A[2].l; char out[1024]; size_t o = 0; int ai = 0;
    for (const char *p = f; *p && o < sizeof out - 64; p++) {
        if (*p != '%') { out[o++] = *p; continue; }
        p++; if (*p == '%') { out[o++] = '%'; continue; }
        void *x = args && ai < args->len ? args->d[ai++].l : NULL;
        if (*p == 'd') o += snprintf(out + o, sizeof out - o, "%d", x ? S(x, 0).i : 0);
        else if (*p == 's') o += snprintf(out + o, sizeof out - o, "%s", rt_cstr(x));
        else rt_fatal("AOT-MISSING String.format conversion", f);
    }
    out[o] = 0; RL(rt_str(out));
}
KCLASS(K_String, "java.lang.String", "java.lang.Object",
    {"boolean equals(java.lang.Object)", str_equals}, {"boolean startsWith(java.lang.String)", str_starts},
    {"java.lang.String concat(java.lang.String)", str_concat}, {"java.lang.String substring(int)", str_substr},
    {"java.lang.String toString()", str_tostring}, {"int length()", str_length}, {"int hashCode()", str_hash},
    {"java.lang.String format(java.util.Locale,java.lang.String,java.lang.Object[])", str_format})

/* ---------- Object / Class ---------- */
extern JClass K_Class;
void *jet_classobj(const char *n) { JObj *c = rt_new(&K_Class); S(c, 0).l = rt_class(n); return c; }
F(obj_init) { RV; }
F(obj_getclass) { JObj *c = rt_new(&K_Class); S(c, 0).l = O(0)->cls; RL(c); }
F(obj_tostring) { char b[256]; snprintf(b, sizeof b, "%s@%lx", O(0)->cls->name, (unsigned long) (uintptr_t) A[0].l); RL(rt_str(b)); }
F(obj_hash) { RI((int32_t) (uintptr_t) A[0].l); }
F(obj_equals) { RI(A[0].l == A[1].l); }
KCLASS(K_Object, "java.lang.Object", NULL, {"void <init>()", obj_init}, {"java.lang.Class getClass()", obj_getclass},
    {"java.lang.String toString()", obj_tostring}, {"int hashCode()", obj_hash}, {"boolean equals(java.lang.Object)", obj_equals})
JClass *rt_find(const char *n);
F(cls_getname) { RL(rt_str(((JClass *) S(A[0].l, 0).l)->name)); }
F(cls_forname) { const char *n = rt_cstr(RT_NN(A[0].l)); JClass *c = rt_find(n);
    if (!c || !c->meths) { rt_throw_new("java.lang.ClassNotFoundException", n); RV; } JObj *k = rt_new(&K_Class); S(k, 0).l = c; RL(k); }
F(cls_newinst) { JClass *c = S(A[0].l, 0).l; void *o = rt_new(c); jv a[1] = {{.l = o}}; rt_vfind(c, "void <init>()")(a); RL(o); }
F(cls_genif) { RL(rt_newarray(0)); }
KCLASS(K_Class, "java.lang.Class", "java.lang.Object", {"java.lang.String getName()", cls_getname},
    {"java.lang.Class forName(java.lang.String)", cls_forname}, {"java.lang.Object newInstance()", cls_newinst},
    {"java.lang.reflect.Type[] getGenericInterfaces()", cls_genif})

/* ---------- Throwable family: slot0 = message ---------- */
void *rt_throw_new(const char *cls, const char *msg) { JObj *e = rt_new(rt_class_strict(cls)); S(e, 0).l = msg ? rt_str(msg) : NULL; rt_exc = e; return e; }
F(thr_init0) { RV; }
F(thr_init1) { S(A[0].l, 0).l = A[1].l; RV; }
F(thr_getst) { RL(rt_newarray(0)); }
F(thr_setst) { RV; }
F(thr_msg) { RL(S(A[0].l, 0).l); }
KCLASS(K_Throwable, "java.lang.Throwable", "java.lang.Object", {"void <init>()", thr_init0}, {"void <init>(java.lang.String)", thr_init1},
    {"java.lang.StackTraceElement[] getStackTrace()", thr_getst}, {"void setStackTrace(java.lang.StackTraceElement[])", thr_setst},
    {"java.lang.String getMessage()", thr_msg})
KCLASS(K_Exception, "java.lang.Exception", "java.lang.Throwable", {"void <init>()", thr_init0})
KCLASS(K_RuntimeException, "java.lang.RuntimeException", "java.lang.Exception", {"void <init>()", thr_init0})
KCLASS(K_NPE, "java.lang.NullPointerException", "java.lang.RuntimeException", {"void <init>()", thr_init0})
KCLASS(K_IAE, "java.lang.IllegalArgumentException", "java.lang.RuntimeException", {"void <init>()", thr_init0})
KCLASS(K_IOOBE, "java.lang.IndexOutOfBoundsException", "java.lang.RuntimeException", {"void <init>()", thr_init0})
KCLASS(K_AIOOBE, "java.lang.ArrayIndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException", {"void <init>()", thr_init0})
KCLASS(K_ROE, "java.lang.ReflectiveOperationException", "java.lang.Exception", {"void <init>()", thr_init0})
KCLASS(K_CNFE, "java.lang.ClassNotFoundException", "java.lang.ReflectiveOperationException", {"void <init>()", thr_init0})
KCLASS(K_IE, "java.lang.InstantiationException", "java.lang.ReflectiveOperationException", {"void <init>()", thr_init0})
KCLASS(K_IAccE, "java.lang.IllegalAccessException", "java.lang.ReflectiveOperationException", {"void <init>()", thr_init0})
KCLASS(K_CCE, "java.lang.ClassCastException", "java.lang.RuntimeException", {"void <init>()", thr_init0})
F(th_cur) { static JObj *t; if (!t) t = rt_new(rt_class_strict("java.lang.Thread")); RL(t); }
KCLASS(K_Thread, "java.lang.Thread", "java.lang.Object", {"java.lang.Thread currentThread()", th_cur}, {"java.lang.StackTraceElement[] getStackTrace()", thr_getst})
F(ste_name) { RL(rt_str("?")); }
KCLASS(K_STE, "java.lang.StackTraceElement", "java.lang.Object", {"java.lang.String getClassName()", ste_name}, {"java.lang.String getMethodName()", ste_name})

/* ---------- boxes, enum, math, arrays, locale ---------- */
F(int_valueof) { JObj *o = rt_new(&K_Integer); S(o, 0).i = A[0].i; RL(o); }
F(int_value) { RI(S(A[0].l, 0).i); }
F(int_tostring) { char b[16]; snprintf(b, sizeof b, "%d", S(A[0].l, 0).i); RL(rt_str(b)); }
KCLASS(K_Number, "java.lang.Number", "java.lang.Object", {"void <init>()", obj_init})
KCLASS(K_Integer, "java.lang.Integer", "java.lang.Number", {"java.lang.Integer valueOf(int)", int_valueof}, {"int intValue()", int_value}, {"java.lang.String toString()", int_tostring})
F(dbl_isnan) { RI(isnan(A[0].d) ? 1 : 0); }
KCLASS(K_Double, "java.lang.Double", "java.lang.Number", {"boolean isNaN(double)", dbl_isnan})
F(enum_init) { S(A[0].l, 0).l = A[1].l; S(A[0].l, 1).i = A[2].i; RV; }
F(enum_ord) { RI(S(A[0].l, 1).i); }
F(enum_name) { RL(S(A[0].l, 0).l); }
KCLASS(K_Enum, "java.lang.Enum", "java.lang.Object", {"void <init>(java.lang.String,int)", enum_init}, {"int ordinal()", enum_ord},
    {"java.lang.String name()", enum_name}, {"java.lang.String toString()", enum_name})
F(m_torad) { RD(A[0].d / 180.0 * M_PI); }
F(m_todeg) { RD(A[0].d * 180.0 / M_PI); }
F(m_sin) { RD(sin(A[0].d)); } F(m_cos) { RD(cos(A[0].d)); } F(m_sqrt) { RD(sqrt(A[0].d)); }
F(m_floor) { RD(floor(A[0].d)); } F(m_rint) { RD(rint(A[0].d)); } F(m_atan2) { RD(atan2(A[0].d, A[1].d)); }
KCLASS(K_Math, "java.lang.Math", "java.lang.Object", {"double toRadians(double)", m_torad}, {"double toDegrees(double)", m_todeg},
    {"double sin(double)", m_sin}, {"double cos(double)", m_cos}, {"double sqrt(double)", m_sqrt}, {"double floor(double)", m_floor},
    {"double rint(double)", m_rint}, {"double atan2(double,double)", m_atan2})
static void *copyrange(JArr *a, int from, int to) { JArr *r = rt_newarray(to - from); for (int i = from; i < to; i++) if (i < a->len) r->d[i - from] = a->d[i]; return r; }
F(arr_copyof) { RL(copyrange(RT_NN(A[0].l), 0, A[1].i)); }
F(arr_copyofr) { RL(copyrange(RT_NN(A[0].l), A[1].i, A[2].i)); }
KCLASS(K_Arrays, "java.util.Arrays", "java.lang.Object", {"java.lang.Object[] copyOf(java.lang.Object[],int)", arr_copyof},
    {"java.lang.Object[] copyOfRange(java.lang.Object[],int,int)", arr_copyofr})
KCLASS(K_Locale, "java.util.Locale", "java.lang.Object", {"void <init>()", obj_init})
/* StringBuilder: slot0 = buffer, slot1 = len, slot2 = cap */
static void sb_put(JObj *s, const char *p, size_t n) { size_t l = s->slot[1].i, c = s->slot[2].i; if (l + n + 1 > c) { c = (l + n + 1) * 2; s->slot[0].l = realloc(s->slot[0].l, c); s->slot[2].i = (int32_t) c; } memcpy((char *) s->slot[0].l + l, p, n); s->slot[1].i = (int32_t) (l + n); ((char *) s->slot[0].l)[l + n] = 0; }
F(sb_init) { sb_put(O(0), "", 0); RV; }
F(sb_apps) { const char *p = rt_cstr(A[1].l); sb_put(O(0), p, strlen(p)); RL(A[0].l); }
F(sb_appc) { char b[4]; int c = A[1].i, n; if (c < 0x80) { b[0] = (char) c; n = 1; } else if (c < 0x800) { b[0] = (char) (0xc0 | c >> 6); b[1] = (char) (0x80 | (c & 63)); n = 2; } else { b[0] = (char) (0xe0 | c >> 12); b[1] = (char) (0x80 | ((c >> 6) & 63)); b[2] = (char) (0x80 | (c & 63)); n = 3; } sb_put(O(0), b, n); RL(A[0].l); }
F(sb_appi) { char b[16]; int n = snprintf(b, sizeof b, "%d", A[1].i); sb_put(O(0), b, n); RL(A[0].l); }
F(sb_tostr) { RL(rt_str(O(0)->slot[0].l)); }
KCLASS(K_StringBuilder, "java.lang.StringBuilder", "java.lang.Object", {"void <init>()", sb_init}, {"java.lang.StringBuilder append(java.lang.String)", sb_apps},
    {"java.lang.StringBuilder append(char)", sb_appc}, {"java.lang.StringBuilder append(int)", sb_appi}, {"java.lang.String toString()", sb_tostr})
KCLASS(K_CharSequence, "java.lang.CharSequence", "java.lang.Object", {"void <init>()", obj_init})
jv *jet_kstatic(const char *cls, const char *name) {
    static jv us;
    if (!strcmp(cls, "java.util.Locale") && !strcmp(name, "US")) { if (!us.l) us.l = rt_new(&K_Locale); return &us; }
    char b[256]; snprintf(b, sizeof b, "%s.%s", cls, name); rt_fatal("AOT-MISSING static field", b);
}

/* ---------- android: native view engine ----------
 * Native counterpart of the JET Java shim's android.view/android.widget core for AOT builds: views are inflated from
 * the generated layout tables (styles flattened by tools/aot/gen_res.py), measured and laid out with the AOSP
 * algorithms (MeasureSpec, LinearLayout weights, TableLayout column stretch/shrink, TableRow cells), drawn into a
 * display list (JetDrawOp, see JetBridge.h) that the iOS host renders with CoreGraphics, and receive touches
 * (pressed state, click, long click). No UIKit dependency: the host supplies text metrics through a callback.
 * View slots: 0 text (String), 1 id, 2 children (JArr*), 3 tag, 4 KV* (native state), 5 OnClickListener,
 * 6 OnLongClickListener, 7 context. */
#include "JetBridge.h"
#include <time.h>
enum { KV_VIEW, KV_GROUP, KV_LINEAR, KV_TABLE, KV_ROW, KV_TEXT };
#define MATCH (-1)
#define WRAP (-2)
#define ST_PRESSED 1
#define ST_SELECTED 2
#define ST_FOCUSED 4
#define ST_ENABLED 8
#define ST_WINFOCUS 32
typedef struct KV {
    JObj *self; struct KV *parent; int kind, button;
    int lw, lh; float weight; int ml, mt, mr, mb, lgrav;          /* layout params (px) */
    int pl, pt, pr, pb, gravity, orient, baseline;                 /* view attributes */
    float tsize; uint32_t tcolor; int bold, single;
    int32_t bg; uint32_t bgcolor; int hasbgcolor;
    int clickable, longclickable, pressed, selected, enabled, vis;
    unsigned stretch, shrink; int stretch_all, shrink_all;          /* TableLayout column sets (columns 0..31) */
    int colw[32], ncol;                                             /* TableLayout: constrained column widths */
    int mw, mh, x, y, offset, nextoff;                              /* measured size, frame origin, TableRow offsets */
    const char *desc;
} KV;
static float k_density = 3.0f;
static jet_text_measure_fn k_measure;
static void k_default_measure(const char *s, float px, int32_t bold, float *w, float *asc, float *desc) {
    int n = 0; for (const unsigned char *p = (const unsigned char *) s; *p; p++) if ((*p & 0xc0) != 0x80) n++;
    *w = n * px * (bold ? 0.60f : 0.56f); *asc = px * 0.93f; *desc = px * 0.24f;   /* headless fallback only */
}
void kv_set_measurer(jet_text_measure_fn fn) { k_measure = fn; }
void kv_set_density(float d) { if (d > 0) k_density = d; }
static void tmeasure(const char *s, float px, int bold, float *w, float *asc, float *desc) { (k_measure ? k_measure : k_default_measure)(s, px, bold, w, asc, desc); }
static KV *kv(void *o) { return o ? (KV *) ((JObj *) o)->slot[4].l : NULL; }
static double now_ms(void) { struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t); return t.tv_sec * 1000.0 + t.tv_nsec / 1e6; }

/* ---- attribute parsing (values are literals produced by gen_res.py) ---- */
static int dim(const char *v) {           /* "-1" "-2" "12.0dip" "14sp" "3px" -> px */
    if (!v) return 0; char *e; double d = strtod(v, &e);
    if (!strcmp(v, "match_parent") || !strcmp(v, "fill_parent")) return MATCH;
    if (!strcmp(v, "wrap_content")) return WRAP;
    if (e == v) return 0;
    if (!*e) return (int) d;              /* raw -1 / -2 / px */
    if (!strncmp(e, "dip", 3) || !strncmp(e, "dp", 2) || !strncmp(e, "sp", 2)) return (int) (d * k_density + (d >= 0 ? 0.5 : -0.5));
    if (!strncmp(e, "pt", 2)) return (int) (d * k_density * 160.0 / 72.0 + 0.5);
    return (int) d;
}
static float kfdim(const char *v) { char *e; double d = strtod(v, &e); if (!strncmp(e, "px", 2)) return (float) d; return (float) (d * k_density); }
static uint32_t colorv(const char *v) { return v && v[0] == '#' ? (uint32_t) strtoul(v + 1, NULL, 16) : 0xff000000u; }
static int intv(const char *v) { return (int) strtol(v, NULL, 0); }
static int boolv(const char *v) { return v && !strcmp(v, "true"); }
static unsigned colset(const char *v, int *all) {
    unsigned m = 0; if (!v) return 0;
    for (const char *p = v; *p; ) { while (*p == ' ' || *p == ',') p++; if (*p == '*') { *all = 1; p++; continue; } if (!*p) break;
        char *e; long c = strtol(p, &e, 10); if (e == p) break; if (c >= 0 && c < 32) m |= 1u << c; p = e; }
    return m;
}
static int32_t drawref(const char *v) { return v && !strncmp(v, "@d:", 3) ? (int32_t) strtoul(v + 3, NULL, 16) : 0; }
static void apply_attrs(KV *k, const char *const *a) {
    int m = -100000, pad = -100000;
    for (; a && a[0]; a += 2) {
        const char *n = a[0], *v = a[1];
        if (!strcmp(n, "layout_width")) k->lw = dim(v);
        else if (!strcmp(n, "layout_height")) k->lh = dim(v);
        else if (!strcmp(n, "layout_weight")) k->weight = (float) strtod(v, NULL);
        else if (!strcmp(n, "layout_margin")) m = dim(v);
        else if (!strcmp(n, "layout_gravity")) k->lgrav = intv(v);
        else if (!strcmp(n, "padding")) pad = dim(v);
        else if (!strcmp(n, "gravity")) k->gravity = intv(v);
        else if (!strcmp(n, "orientation")) k->orient = intv(v);
        else if (!strcmp(n, "baselineAligned")) k->baseline = boolv(v);
        else if (!strcmp(n, "textSize")) k->tsize = kfdim(v);
        else if (!strcmp(n, "textColor")) k->tcolor = colorv(v);
        else if (!strcmp(n, "textStyle")) k->bold = intv(v) & 1;
        else if (!strcmp(n, "singleLine")) k->single = boolv(v);
        else if (!strcmp(n, "background")) { if (v[0] == '#') { k->bgcolor = colorv(v); k->hasbgcolor = 1; k->bg = 0; } else { k->bg = drawref(v); k->hasbgcolor = 0; } }
        else if (!strcmp(n, "clickable")) k->clickable = boolv(v);
        else if (!strcmp(n, "longClickable")) k->longclickable = boolv(v);
        else if (!strcmp(n, "enabled")) k->enabled = boolv(v);
        else if (!strcmp(n, "visibility")) k->vis = intv(v) == 2 ? 8 : intv(v) == 1 ? 4 : intv(v);
        else if (!strcmp(n, "stretchColumns")) k->stretch = colset(v, &k->stretch_all);
        else if (!strcmp(n, "shrinkColumns")) k->shrink = colset(v, &k->shrink_all);
    }
    if (m != -100000) k->ml = k->mt = k->mr = k->mb = m;
    if (pad != -100000) k->pl = k->pt = k->pr = k->pb = pad;
}
static void apply_attrs2(KV *k, const char *const *a) {   /* side-specific values override the shorthand ones */
    for (; a && a[0]; a += 2) {
        const char *n = a[0], *v = a[1];
        if (!strcmp(n, "layout_marginLeft") || !strcmp(n, "layout_marginStart")) k->ml = dim(v);
        else if (!strcmp(n, "layout_marginTop")) k->mt = dim(v);
        else if (!strcmp(n, "layout_marginRight") || !strcmp(n, "layout_marginEnd")) k->mr = dim(v);
        else if (!strcmp(n, "layout_marginBottom")) k->mb = dim(v);
        else if (!strcmp(n, "layout_marginHorizontal")) k->ml = k->mr = dim(v);
        else if (!strcmp(n, "layout_marginVertical")) k->mt = k->mb = dim(v);
        else if (!strcmp(n, "paddingLeft") || !strcmp(n, "paddingStart")) k->pl = dim(v);
        else if (!strcmp(n, "paddingTop")) k->pt = dim(v);
        else if (!strcmp(n, "paddingRight") || !strcmp(n, "paddingEnd")) k->pr = dim(v);
        else if (!strcmp(n, "paddingBottom")) k->pb = dim(v);
        else if (!strcmp(n, "paddingHorizontal")) k->pl = k->pr = dim(v);
        else if (!strcmp(n, "paddingVertical")) k->pt = k->pb = dim(v);
    }
}

typedef struct { JClass *cls; const JResNode *node; } KAttrs;
extern JClass K_View, K_ViewGroup, K_LinearLayout, K_TableLayout, K_TableRow, K_TextView, K_Button, K_Resources, K_LayoutInflater, K_Toast;
static const char *res_string(int id) { for (const JResString *s = jet_strings; s->s; s++) if (s->id == id) return s->s; return NULL; }
static void addchild(void *parent, void *child) {
    JArr *c = S(parent, 2).l; int n = c ? c->len : 0; JArr *r = rt_newarray(n + 1);
    for (int i = 0; i < n; i++) r->d[i] = c->d[i]; r->d[n].l = child; S(parent, 2).l = r;
    KV *ck = kv(child), *pk = kv(parent); if (ck) ck->parent = pk;
    if (ck && pk && pk->kind == KV_TABLE && ck->kind == KV_ROW) ck->lw = MATCH;    /* TableLayout.LayoutParams */
}
static const JResNode *node_of(void *attrs) { return attrs ? ((KAttrs *) attrs)->node : NULL; }
static void view_setup(void *o, void *ctx, const JResNode *n) {
    JObj *v = o; KV *k = kv(v);
    if (!k) { k = calloc(1, sizeof *k); S(v, 4).l = k; }
    else { KV *p = k->parent; memset(k, 0, sizeof *k); k->parent = p; }
    k->self = v; S(v, 7).l = ctx;
    k->kind = rt_instanceof(v, &K_TableRow) ? KV_ROW : rt_instanceof(v, &K_TableLayout) ? KV_TABLE : rt_instanceof(v, &K_LinearLayout) ? KV_LINEAR
            : rt_instanceof(v, &K_ViewGroup) ? KV_GROUP : rt_instanceof(v, &K_TextView) ? KV_TEXT : KV_VIEW;
    k->button = rt_instanceof(v, &K_Button) != 0;
    k->lw = k->lh = WRAP; k->enabled = 1; k->baseline = 1; k->gravity = 0x33; /* TOP|START */
    k->tsize = 14.0f * k_density; k->tcolor = jet_theme_text_color;
    if (k->kind == KV_TABLE) k->orient = 1;
    if (k->kind == KV_ROW) k->lw = MATCH;
    if (k->button) { k->gravity = 0x11; k->clickable = 1; k->tcolor = 0xff000000u; k->hasbgcolor = 1; k->bgcolor = 0xffd6d7d7u; }
    if (n) {
        S(v, 1).i = n->id; if (n->text) S(v, 0).l = rt_str(n->text); k->desc = n->desc;
        apply_attrs(k, n->attrs); apply_attrs2(k, n->attrs);
    }
}
F(view_init) { view_setup(A[0].l, A[1].l, node_of(A[2].l)); RV; }
F(view_init1) { view_setup(A[0].l, A[1].l, NULL); RV; }
static void *findview(void *v, int id) {
    if (S(v, 1).i == id) return v; JArr *c = S(v, 2).l;
    for (int i = 0; c && i < c->len; i++) { void *r = findview(c->d[i].l, id); if (r) return r; }
    return NULL;
}
F(view_find) { RL(findview(A[0].l, A[1].i)); }
F(view_res) { RL(rt_new(&K_Resources)); }
F(view_gettag) { RL(S(A[0].l, 3).l); }
F(view_settag) { S(A[0].l, 3).l = A[1].l; RV; }
F(view_setclick) { S(A[0].l, 5).l = A[1].l; if (A[1].l && kv(A[0].l)) kv(A[0].l)->clickable = 1; RV; }
F(view_setlong) { S(A[0].l, 6).l = A[1].l; if (A[1].l && kv(A[0].l)) kv(A[0].l)->longclickable = 1; RV; }
F(view_ctx) { RL(S(A[0].l, 7).l); }
F(view_desc) { KV *k = kv(A[0].l); RL(k && k->desc ? rt_str(k->desc) : NULL); }
static void setsel(void *v, int s) { KV *k = kv(v); if (k) k->selected = s; JArr *c = S(v, 2).l; for (int i = 0; c && i < c->len; i++) setsel(c->d[i].l, s); }
F(view_setsel) { setsel(A[0].l, A[1].i != 0); RV; }       /* AOSP dispatchSetSelected: children follow */
F(view_issel) { KV *k = kv(A[0].l); RI(k && k->selected); }
F(view_editmode) { RI(0); }
F(view_setvis) { KV *k = kv(A[0].l); if (k) k->vis = A[1].i; RV; }
F(view_getvis) { KV *k = kv(A[0].l); RI(k ? k->vis : 0); }
F(view_setenabled) { KV *k = kv(A[0].l); if (k) k->enabled = A[1].i != 0; RV; }
F(view_setbgcolor) { KV *k = kv(A[0].l); if (k) { k->hasbgcolor = 1; k->bgcolor = (uint32_t) A[1].i; k->bg = 0; } RV; }
F(view_setbgres) { KV *k = kv(A[0].l); if (k) { k->bg = A[1].i; k->hasbgcolor = 0; } RV; }
F(view_invalidate) { RV; }
F(vg_add) { addchild(A[0].l, RT_NN(A[1].l)); RV; }
F(vg_count) { JArr *c = S(A[0].l, 2).l; RI(c ? c->len : 0); }
F(vg_childat) { JArr *c = S(A[0].l, 2).l; int i = A[1].i; RL(c && i >= 0 && i < c->len ? c->d[i].l : NULL); }
#define VIEW_METHODS {"void <init>(android.content.Context,android.util.AttributeSet)", view_init}, {"void <init>(android.content.Context)", view_init1}
KCLASS(K_View, "android.view.View", "java.lang.Object", VIEW_METHODS,
    {"android.view.View findViewById(int)", view_find}, {"android.content.res.Resources getResources()", view_res},
    {"java.lang.Object getTag()", view_gettag}, {"void setTag(java.lang.Object)", view_settag},
    {"void setOnClickListener(android.view.View$OnClickListener)", view_setclick}, {"void setOnLongClickListener(android.view.View$OnLongClickListener)", view_setlong},
    {"android.content.Context getContext()", view_ctx}, {"java.lang.CharSequence getContentDescription()", view_desc},
    {"void setSelected(boolean)", view_setsel}, {"boolean isSelected()", view_issel}, {"boolean isInEditMode()", view_editmode},
    {"void setVisibility(int)", view_setvis}, {"int getVisibility()", view_getvis}, {"void setEnabled(boolean)", view_setenabled},
    {"void setBackgroundColor(int)", view_setbgcolor}, {"void setBackgroundResource(int)", view_setbgres}, {"void invalidate()", view_invalidate},
    {"void requestLayout()", view_invalidate})
KCLASS(K_ViewGroup, "android.view.ViewGroup", "android.view.View", VIEW_METHODS, {"void addView(android.view.View)", vg_add},
    {"int getChildCount()", vg_count}, {"android.view.View getChildAt(int)", vg_childat})
F(ll_setorient) { KV *k = kv(A[0].l); if (k) k->orient = A[1].i; RV; }
KCLASS(K_LinearLayout, "android.widget.LinearLayout", "android.view.ViewGroup", VIEW_METHODS, {"void setOrientation(int)", ll_setorient})
KCLASS(K_TableLayout, "android.widget.TableLayout", "android.widget.LinearLayout", VIEW_METHODS)
KCLASS(K_TableRow, "android.widget.TableRow", "android.widget.LinearLayout", VIEW_METHODS)
F(tv_settext) { void *t = A[1].l; S(A[0].l, 0).l = t ? rt_str(rt_cstr(t)) : NULL; RV; }
F(tv_settextid) { const char *s = res_string(A[1].i); if (!s) rt_fatal("Resources$NotFoundException", "string"); S(A[0].l, 0).l = rt_str(s); RV; }
F(tv_gettext) { RL(S(A[0].l, 0).l ? S(A[0].l, 0).l : rt_str("")); }
F(tv_settextcolor) { KV *k = kv(A[0].l); if (k) k->tcolor = (uint32_t) A[1].i; RV; }
F(tv_settextsize) { KV *k = kv(A[0].l); if (k) k->tsize = A[1].f * k_density; RV; }
F(tv_setgravity) { KV *k = kv(A[0].l); if (k) k->gravity = A[1].i; RV; }
KCLASS(K_TextView, "android.widget.TextView", "android.view.View", VIEW_METHODS,
    {"void setText(java.lang.CharSequence)", tv_settext}, {"void setText(int)", tv_settextid}, {"java.lang.CharSequence getText()", tv_gettext},
    {"void setTextColor(int)", tv_settextcolor}, {"void setTextSize(float)", tv_settextsize}, {"void setGravity(int)", tv_setgravity})
KCLASS(K_Button, "android.widget.Button", "android.widget.TextView", VIEW_METHODS)
F(res_name) { char b[64]; snprintf(b, sizeof b, "id/0x%08x", (unsigned) A[1].i); RL(rt_str(b)); }
F(res_getstring) { const char *s = res_string(A[1].i); if (!s) rt_fatal("Resources$NotFoundException", "string"); RL(rt_str(s)); }
KCLASS(K_Resources, "android.content.res.Resources", "java.lang.Object", {"java.lang.String getResourceName(int)", res_name}, {"java.lang.String getString(int)", res_getstring})

/* LayoutInflater: slot0 = context. Builds each node through its class's (Context,AttributeSet) constructor (kernel
 * widget or translated app class), so app subclasses run their own constructors exactly as on Android. */
static void *construct(const char *tag, void *ctx, const JResNode *n) {
    char nm[256];
    if (strchr(tag, '.')) snprintf(nm, sizeof nm, "%s", tag);
    else if (!strcmp(tag, "View") || !strcmp(tag, "ViewGroup")) snprintf(nm, sizeof nm, "android.view.%s", tag);
    else snprintf(nm, sizeof nm, "android.widget.%s", tag);
    JClass *c = rt_class_strict(nm); void *o = rt_new(c); KAttrs at = {NULL, n};
    jv a[3] = {{.l = o}, {.l = ctx}, {.l = &at}};
    rt_vfind(c, "void <init>(android.content.Context,android.util.AttributeSet)")(a);
    if (rt_exc) rt_fatal("InflateException", nm);
    if (!kv(o)) view_setup(o, ctx, n);      /* app view whose constructor did not reach View(Context,AttributeSet) */
    return o;
}
static void *inflate(void *ctx, int id, void *root, int attach) {
    const JResLayout *L = NULL; for (const JResLayout *l = jet_layouts; l->nodes; l++) if (l->id == id) L = l;
    if (!L) rt_fatal("Resources$NotFoundException", "layout");
    void *stack[64]; void *top = NULL;
    for (const JResNode *n = L->nodes; n->depth >= 0; n++) {
        void *v = construct(n->cls, ctx, n);
        if (n->depth == 0) top = v; else addchild(stack[n->depth - 1], v);
        stack[n->depth] = v;
    }
    if (root && attach) { addchild(root, top); return root; }
    return top;
}
F(li_from) { JObj *l = rt_new(&K_LayoutInflater); S(l, 0).l = A[0].l; RL(l); }
F(li_inflate2) { RL(inflate(S(A[0].l, 0).l, A[1].i, A[2].l, A[2].l != NULL)); }
F(li_inflate3) { RL(inflate(S(A[0].l, 0).l, A[1].i, A[2].l, A[3].i)); }
KCLASS(K_LayoutInflater, "android.view.LayoutInflater", "java.lang.Object", {"android.view.LayoutInflater 'from'(android.content.Context)", li_from},
    {"android.view.View inflate(int,android.view.ViewGroup)", li_inflate2}, {"android.view.View inflate(int,android.view.ViewGroup,boolean)", li_inflate3})

/* Toast: shown by the view engine as an overlay for LENGTH_SHORT 2 s / LENGTH_LONG 3.5 s */
static char k_toast[512]; static double k_toast_until;
F(toast_make) { JObj *t = rt_new(&K_Toast); S(t, 0).l = A[1].l; S(t, 1).i = A[2].i; RL(t); }
F(toast_make_id) { JObj *t = rt_new(&K_Toast); const char *s = res_string(A[1].i); S(t, 0).l = rt_str(s ? s : ""); S(t, 1).i = A[2].i; RL(t); }
F(toast_show) { snprintf(k_toast, sizeof k_toast, "%s", rt_cstr(S(A[0].l, 0).l)); k_toast_until = now_ms() + (S(A[0].l, 1).i == 1 ? 3500 : 2000); printf("[toast] %s\n", k_toast); RV; }
KCLASS(K_Toast, "android.widget.Toast", "java.lang.Object", {"android.widget.Toast makeText(android.content.Context,java.lang.CharSequence,int)", toast_make},
    {"android.widget.Toast makeText(android.content.Context,int,int)", toast_make_id}, {"void show()", toast_show})
/* Bundle: tiny int map in a JArr of (String,Integer) pairs at slot0 */
F(b_putint) { JArr *m = S(A[0].l, 0).l; int n = m ? m->len : 0; JArr *r = rt_newarray(n + 2); for (int i = 0; i < n; i++) r->d[i] = m->d[i]; r->d[n].l = A[1].l; r->d[n + 1].i = A[2].i; S(A[0].l, 0).l = r; RV; }
F(b_getint) { JArr *m = S(A[0].l, 0).l; const char *k = rt_cstr(A[1].l); int v = 0; for (int i = 0; m && i < m->len; i += 2) if (!strcmp(rt_cstr(m->d[i].l), k)) v = m->d[i + 1].i; RI(v); }
KCLASS(K_Bundle, "android.os.Bundle", "java.lang.Object", {"void putInt(java.lang.String,int)", b_putint}, {"int getInt(java.lang.String)", b_getint})
/* Context / Activity: slot0 = content view */
F(li_ctx) { JObj *l = rt_new(&K_LayoutInflater); S(l, 0).l = A[0].l; RL(l); }
F(act_setcv) { S(A[0].l, 0).l = A[1].l; KV *k = kv(A[1].l); if (k) { k->lw = k->lh = MATCH; k->parent = NULL; } RV; }
F(act_setcvid) { void *v = inflate(A[0].l, A[1].i, NULL, 0); S(A[0].l, 0).l = v; KV *k = kv(v); if (k) { k->lw = k->lh = MATCH; } RV; }
F(act_find) { RL(S(A[0].l, 0).l ? findview(S(A[0].l, 0).l, A[1].i) : NULL); }
F(act_noop1) { RV; }
F(ctx_res) { RL(rt_new(&K_Resources)); }
F(ctx_string) { const char *s = res_string(A[1].i); if (!s) rt_fatal("Resources$NotFoundException", "string"); RL(rt_str(s)); }
KCLASS(K_Context, "android.content.Context", "java.lang.Object", {"void <init>()", obj_init}, {"android.content.res.Resources getResources()", ctx_res},
    {"java.lang.String getString(int)", ctx_string})
KCLASS(K_ContextWrapper, "android.content.ContextWrapper", "android.content.Context", {"void <init>()", obj_init})
KCLASS(K_Activity, "android.app.Activity", "android.content.ContextWrapper", {"void <init>()", obj_init},
    {"void onCreate(android.os.Bundle)", act_noop1}, {"void onSaveInstanceState(android.os.Bundle)", act_noop1},
    {"void onStart()", act_noop1}, {"void onResume()", act_noop1}, {"void onPause()", act_noop1}, {"void onStop()", act_noop1}, {"void onDestroy()", act_noop1},
    {"void setContentView(android.view.View)", act_setcv}, {"void setContentView(int)", act_setcvid}, {"android.view.View findViewById(int)", act_find},
    {"android.view.LayoutInflater getLayoutInflater()", li_ctx})

/* ---- measure / layout (AOSP semantics, integer px) ---- */
enum { MS_UNSPEC = 0, MS_EXACT = 1, MS_ATMOST = 2 };
typedef struct { int mode, size; } Spec;
static Spec spec(int mode, int size) { Spec s = {mode, size < 0 ? 0 : size}; return s; }
static int resolve_size(int want, Spec s) { return s.mode == MS_EXACT ? s.size : s.mode == MS_ATMOST ? (want < s.size ? want : s.size) : want; }
static Spec child_spec(Spec p, int padding, int dimv) {        /* ViewGroup.getChildMeasureSpec */
    int size = p.size - padding; if (size < 0) size = 0;
    if (dimv >= 0) return spec(MS_EXACT, dimv);
    if (p.mode == MS_EXACT) return dimv == MATCH ? spec(MS_EXACT, size) : spec(MS_ATMOST, size);
    if (p.mode == MS_ATMOST) return spec(MS_ATMOST, size);
    return spec(MS_UNSPEC, 0);
}
static JArr *kids(KV *k) { return S(k->self, 2).l; }
#define FOR_KIDS(k, c) for (int _i = 0; kids(k) && _i < kids(k)->len; _i++) for (KV *c = kv(kids(k)->d[_i].l); c; c = NULL)
static void measure(KV *k, Spec w, Spec h);
/* TextView includeFontPadding (default true): Android lines use the font's top/bottom extents rather than ascent/
 * descent; for Roboto that adds 0.128 em above and 0.027 em below. Host fonts report ascent/descent only. */
#define FONTPAD_TOP 0.128f
#define FONTPAD_BOTTOM 0.027f
static void text_box(KV *k, float *tw, float *th) {
    const char *t = S(k->self, 0).l ? rt_cstr(S(k->self, 0).l) : "";
    float w = 0, a = 0, d = 0, maxw = 0; int lines = 0; char line[1024];
    for (const char *p = t; ; ) {          /* explicit line breaks only (single-line and short texts) */
        const char *e = strchr(p, '\n'); size_t n = e ? (size_t) (e - p) : strlen(p); if (n >= sizeof line) n = sizeof line - 1;
        memcpy(line, p, n); line[n] = 0; tmeasure(line, k->tsize, k->bold, &w, &a, &d);
        if (w > maxw) maxw = w; lines++;
        if (!e || k->single) break; p = e + 1;
    }
    tmeasure("", k->tsize, k->bold, &w, &a, &d);
    *tw = maxw; *th = (a + d) * lines + k->tsize * (FONTPAD_TOP + FONTPAD_BOTTOM);
}
static void measure_linear(KV *k, Spec w, Spec h) {
    int vert = k->orient == 1;
    Spec main = vert ? h : w, cross = vert ? w : h;
    int padM = vert ? k->pt + k->pb : k->pl + k->pr, padC = vert ? k->pl + k->pr : k->pt + k->pb;
    int total = 0, maxc = 0; float wsum = 0;
    FOR_KIDS(k, c) {
        if (c->vis == 8) continue;
        int cm = vert ? c->mt + c->mb : c->ml + c->mr, cc = vert ? c->ml + c->mr : c->mt + c->mb;
        int cdim = vert ? c->lh : c->lw, ccross = vert ? c->lw : c->lh;
        wsum += c->weight;
        if (k->kind == KV_ROW && k->ncol) {                 /* TableRow.measureChildBeforeLayout */
            int col = _i < k->ncol ? k->colw[_i] : 0, hg = c->lgrav & 7;
            int mode = (hg == 3 || hg == 5 || hg == 1) ? MS_ATMOST : MS_EXACT;
            measure(c, spec(mode, col - c->ml - c->mr), child_spec(cross, padC + cc, c->lh));
            c->nextoff = mode == MS_ATMOST ? col - c->mw : 0;
            c->offset = mode == MS_ATMOST ? (hg == 5 ? c->nextoff : hg == 1 ? c->nextoff / 2 : 0) : 0;
            total += c->mw + cm + c->nextoff;
        } else if (main.mode == MS_EXACT && cdim == 0 && c->weight > 0) {
            total += cm;                                    /* measured in the weight pass */
        } else {
            Spec ms = child_spec(main, padM + cm + (wsum == 0 ? total : 0), cdim == 0 && c->weight > 0 ? WRAP : cdim);
            Spec cs = child_spec(cross, padC + cc, ccross);
            if (vert) measure(c, cs, ms); else measure(c, ms, cs);
            total += (vert ? c->mh : c->mw) + cm;
        }
        int ext = (vert ? c->mw : c->mh) + cc; if (ext > maxc) maxc = ext;
    }
    total += padM;
    int msize = resolve_size(total, main);
    int remain = msize - total;
    if (remain != 0 && wsum > 0) {
        total = 0; float wleft = wsum;
        FOR_KIDS(k, c) {
            if (c->vis == 8) continue;
            int cm = vert ? c->mt + c->mb : c->ml + c->mr, cc = vert ? c->ml + c->mr : c->mt + c->mb;
            int cdim = vert ? c->lh : c->lw, ccross = vert ? c->lw : c->lh;
            if (c->weight > 0) {
                int share = (int) (c->weight * remain / wleft); remain -= share; wleft -= c->weight;
                int was = (main.mode == MS_EXACT && cdim == 0) ? 0 : (vert ? c->mh : c->mw);
                int sz = was + share; if (sz < 0) sz = 0;
                Spec cs = child_spec(cross, padC + cc, ccross);
                if (vert) measure(c, cs, spec(MS_EXACT, sz)); else measure(c, spec(MS_EXACT, sz), cs);
            }
            total += (vert ? c->mh : c->mw) + cm + c->nextoff;
            int ext = (vert ? c->mw : c->mh) + cc; if (ext > maxc) maxc = ext;
        }
        total += padM;
    }
    int csize = resolve_size(maxc + padC, cross);
    if (vert) { k->mw = csize; k->mh = msize; } else { k->mw = msize; k->mh = csize; }
    /* match_parent children in a non-exact cross dimension: re-measure to the final size (forceUniformWidth/Height) */
    if (cross.mode != MS_EXACT) FOR_KIDS(k, c) {
        if (c->vis == 8 || (vert ? c->lw : c->lh) != MATCH) continue;
        int cc = vert ? c->ml + c->mr : c->mt + c->mb;
        if (vert) measure(c, spec(MS_EXACT, csize - padC - cc), spec(MS_EXACT, c->mh));
        else measure(c, spec(MS_EXACT, c->mw), spec(MS_EXACT, csize - padC - cc));
    }
}
static void measure_table(KV *k, Spec w, Spec h) {
    int maxw[32] = {0}, ncol = 0;
    FOR_KIDS(k, r) {                                         /* TableRow.getColumnsWidths */
        if (r->vis == 8 || r->kind != KV_ROW) continue;
        int i = 0;
        FOR_KIDS(r, c) {
            if (i >= 32) break;
            int width = 0;
            if (c->vis != 8) {
                Spec s = c->lw == WRAP ? child_spec(w, 0, WRAP) : c->lw == MATCH ? spec(MS_UNSPEC, 0) : spec(MS_EXACT, c->lw);
                measure(c, s, s); width = c->mw + c->ml + c->mr;
            }
            if (width > maxw[i]) maxw[i] = width;
            i++;
        }
        if (i > ncol) ncol = i;
    }
    int total = 0; for (int i = 0; i < ncol; i++) total += maxw[i];
    int size = w.size - k->pl - k->pr;
    unsigned set = 0; int all = 0, cnt = 0;
    if (total > size && (k->shrink_all || k->shrink)) { set = k->shrink; all = k->shrink_all; }
    else if (total < size && (k->stretch_all || k->stretch)) { set = k->stretch; all = k->stretch_all; }
    if (all || set) {                                        /* TableLayout.mutateColumnsWidth */
        for (int i = 0; i < ncol; i++) if (all || (set >> i & 1)) cnt++;
        if (cnt) {
            int extra = (size - total) / cnt;
            for (int i = 0; i < ncol; i++) if (all || (set >> i & 1)) { maxw[i] += extra; if (maxw[i] < 0) maxw[i] = 0; }
        }
    }
    FOR_KIDS(k, r) if (r->kind == KV_ROW) { r->ncol = ncol; memcpy(r->colw, maxw, sizeof maxw); }
    measure_linear(k, w, h);
}
static void measure(KV *k, Spec w, Spec h) {
    if (k->vis == 8) { k->mw = k->mh = 0; return; }
    switch (k->kind) {
    case KV_TEXT: {
        float tw, th; text_box(k, &tw, &th);
        k->mw = resolve_size((int) (tw + 0.999f) + k->pl + k->pr, w);
        k->mh = resolve_size((int) (th + 0.999f) + k->pt + k->pb, h);
        break; }
    case KV_LINEAR: case KV_ROW: measure_linear(k, w, h); break;
    case KV_TABLE: measure_table(k, w, h); break;
    default: {                                               /* FrameLayout-like */
        int mw = 0, mh = 0;
        FOR_KIDS(k, c) { measure(c, child_spec(w, k->pl + k->pr + c->ml + c->mr, c->lw), child_spec(h, k->pt + k->pb + c->mt + c->mb, c->lh));
            if (c->mw + c->ml + c->mr > mw) mw = c->mw + c->ml + c->mr; if (c->mh + c->mt + c->mb > mh) mh = c->mh + c->mt + c->mb; }
        k->mw = resolve_size(mw + k->pl + k->pr, w); k->mh = resolve_size(mh + k->pt + k->pb, h);
        break; }
    }
}
static void layout(KV *k) {
    if (k->kind == KV_LINEAR || k->kind == KV_TABLE || k->kind == KV_ROW) {
        int vert = k->orient == 1, pos = vert ? k->pt : k->pl;
        FOR_KIDS(k, c) {
            if (c->vis == 8) continue;
            if (vert) {
                int hg = c->lgrav & 7, room = k->mw - k->pl - k->pr - c->mw - c->ml - c->mr;
                c->x = k->pl + c->ml + (hg == 5 ? room : hg == 1 ? room / 2 : 0);
                pos += c->mt; c->y = pos; pos += c->mh + c->mb;
            } else {
                int vg = c->lgrav & 0x70, room = k->mh - k->pt - k->pb - c->mh - c->mt - c->mb;
                c->y = k->pt + c->mt + (vg == 0x50 ? room : vg == 0x10 ? room / 2 : 0);
                pos += c->ml; c->x = pos + c->offset; pos += c->mw + c->mr + c->nextoff;
            }
            layout(c);
        }
    } else {
        FOR_KIDS(k, c) {
            int hg = c->lgrav & 7, vg = c->lgrav & 0x70;
            int rw = k->mw - k->pl - k->pr - c->mw - c->ml - c->mr, rh = k->mh - k->pt - k->pb - c->mh - c->mt - c->mb;
            c->x = k->pl + c->ml + (hg == 5 ? rw : hg == 1 ? rw / 2 : 0); c->y = k->pt + c->mt + (vg == 0x50 ? rh : vg == 0x10 ? rh / 2 : 0);
            layout(c);
        }
    }
}

/* ---- drawing: display list ---- */
static JetDrawOp *k_ops; static int k_nops, k_cap;
static JetDrawOp *op(void) {
    if (k_nops == k_cap) { k_cap = k_cap ? k_cap * 2 : 256; k_ops = realloc(k_ops, (size_t) k_cap * sizeof *k_ops); }
    JetDrawOp *o = &k_ops[k_nops++]; memset(o, 0, sizeof *o); return o;
}
static int state_of(KV *k) { return (k->pressed ? ST_PRESSED : 0) | (k->selected ? ST_SELECTED : 0) | (k->enabled ? ST_ENABLED : 0) | ST_WINFOCUS; }
static int drawable_color(int32_t id, int state, uint32_t *out, int depth) {
    if (!id || depth > 8) return 0;
    for (const JResDrawable *d = jet_drawables; d->id; d++) {
        if (d->id != id) continue;
        if (d->kind == 1 || d->kind == 3) { *out = d->color; return 1; }
        if (d->kind == 2) for (const int32_t *s = d->states; s && s[0] != -1; s += 3) if ((state & s[0]) == s[1]) return drawable_color(s[2], state, out, depth + 1);
        return 0;
    }
    return 0;
}
static void draw(KV *k, float ox, float oy, float cx0, float cy0, float cx1, float cy1) {
    if (k->vis != 0) return;
    float x = ox + k->x, y = oy + k->y;
    if (x > cx0) cx0 = x; if (y > cy0) cy0 = y; if (x + k->mw < cx1) cx1 = x + k->mw; if (y + k->mh < cy1) cy1 = y + k->mh;   /* clipChildren */
    if (cx1 <= cx0 || cy1 <= cy0) return;
    uint32_t c;
    if ((k->hasbgcolor && (c = k->bgcolor, 1)) || drawable_color(k->bg, state_of(k), &c, 0)) {
        if (c >> 24) { JetDrawOp *o = op(); o->op = JET_OP_RECT; o->x = x; o->y = y; o->w = (float) k->mw; o->h = (float) k->mh; o->argb = c;
            o->clip_x = cx0; o->clip_y = cy0; o->clip_w = cx1 - cx0; o->clip_h = cy1 - cy0; }
    }
    if (k->kind == KV_TEXT && S(k->self, 0).l) {
        const char *t = rt_cstr(S(k->self, 0).l);
        if (*t) { JetDrawOp *o = op(); o->op = JET_OP_TEXT; o->x = x + k->pl; o->w = (float) (k->mw - k->pl - k->pr);
            o->y = y + k->pt + k->tsize * FONTPAD_TOP; o->h = (float) (k->mh - k->pt - k->pb) - k->tsize * (FONTPAD_TOP + FONTPAD_BOTTOM);
            o->argb = k->enabled ? k->tcolor : (k->tcolor & 0x00ffffffu) | 0x80000000u; o->size = k->tsize; o->flags = k->bold ? JET_TEXT_BOLD : 0;
            o->gravity = k->gravity; o->text = t; o->clip_x = cx0; o->clip_y = cy0; o->clip_w = cx1 - cx0; o->clip_h = cy1 - cy0; }
    }
    FOR_KIDS(k, ch) draw(ch, x, y, cx0, cy0, cx1, cy1);
}
static int k_vw = 1080, k_vh = 1920;
void kv_set_viewport(int w, int h) { if (w > 0) k_vw = w; if (h > 0) k_vh = h; }
static void relayout(void *root) {
    KV *k = kv(root); if (!k) return;
    k->x = k->y = 0;
    measure(k, spec(MS_EXACT, k_vw), spec(MS_EXACT, k_vh));
    layout(k);
}
int kv_frame(void *root, const JetDrawOp **ops, uint32_t *window_bg) {
    k_nops = 0;
    uint32_t bg = 0xff000000u; drawable_color(jet_theme_window_bg, ST_ENABLED | ST_WINFOCUS, &bg, 0);
    if (window_bg) *window_bg = bg;
    if (root && kv(root)) { relayout(root); draw(kv(root), 0, 0, 0, 0, (float) k_vw, (float) k_vh); }
    if (k_toast[0] && now_ms() < k_toast_until) {     /* platform Toast: dark rounded bubble, bottom centre */
        float w, a, d, ts = 14.0f * k_density, pad = 16.0f * k_density; tmeasure(k_toast, ts, 0, &w, &a, &d);
        if (w > k_vw - 4 * pad) w = k_vw - 4 * pad;
        float bw = w + 2 * pad, bh = a + d + pad, bx = (k_vw - bw) / 2, by = k_vh - 64.0f * k_density - bh;
        JetDrawOp *o = op(); o->op = JET_OP_RRECT; o->x = bx; o->y = by; o->w = bw; o->h = bh; o->argb = 0xe6323232u; o->size = bh / 2;
        o->clip_w = (float) k_vw; o->clip_h = (float) k_vh;
        o = op(); o->op = JET_OP_TEXT; o->x = bx + pad; o->y = by; o->w = bw - 2 * pad; o->h = bh; o->argb = 0xffffffffu; o->size = ts; o->gravity = 0x11; o->text = k_toast;
        o->clip_w = (float) k_vw; o->clip_h = (float) k_vh;
    }
    if (ops) *ops = k_ops;
    return k_nops;
}
int32_t kv_next_wakeup_ms(void) { double t = now_ms(); return k_toast[0] && t < k_toast_until ? (int32_t) (k_toast_until - t) + 1 : -1; }

/* ---- touch: AOSP View.onTouchEvent essentials (pressed state, click on up, long click after the timeout) ---- */
static KV *k_target; static int k_longdone; static float k_downx, k_downy;
static KV *hit(KV *k, float px, float py, float ox, float oy) {
    if (k->vis != 0) return NULL;
    float x = ox + k->x, y = oy + k->y;
    if (px < x || py < y || px >= x + k->mw || py >= y + k->mh) return NULL;
    JArr *c = kids(k);
    for (int i = c ? c->len - 1 : -1; i >= 0; i--) { KV *r = hit(kv(c->d[i].l), px, py, x, y); if (r) return r; }
    return (k->clickable || k->longclickable) && k->enabled ? k : NULL;
}
static int inside(KV *k, float px, float py) {
    float x = 0, y = 0; for (KV *p = k; p; p = p->parent) { x += p->x; y += p->y; }
    float slop = 8.0f * k_density;
    return px >= x - slop && py >= y - slop && px < x + k->mw + slop && py < y + k->mh + slop;
}
static int perform_click(KV *k) {
    void *l = S(k->self, 5).l; if (!l) return 0;
    jv c[2] = {{.l = l}, {.l = k->self}};
    rt_vfind(((JObj *) l)->cls, "void onClick(android.view.View)")(c);
    return 1;
}
static int perform_long_click(KV *k) {
    void *l = S(k->self, 6).l; if (!l) return 0;
    jv c[2] = {{.l = l}, {.l = k->self}};
    return rt_vfind(((JObj *) l)->cls, "boolean onLongClick(android.view.View)")(c).i != 0;
}
/* action: 0 down, 1 up, 2 move, 3 cancel, 4 long-press timeout (host timer). Returns 1 when a redraw is needed. */
int kv_touch(void *root, int action, float px, float py) {
    KV *r = kv(root); if (!r) return 0;
    if (action == 0) {
        if (!kids(r) && !r->clickable) return 0;
        relayout(root);
        k_target = hit(r, px, py, 0, 0); k_longdone = 0; k_downx = px; k_downy = py;
        if (k_target) { k_target->pressed = 1; return 1; }
        return 0;
    }
    KV *t = k_target; if (!t) return 0;
    if (action == 2) { if (t->pressed && !inside(t, px, py)) { t->pressed = 0; return 1; } return 0; }
    if (action == 3) { t->pressed = 0; k_target = NULL; return 1; }
    if (action == 4) { if (t->pressed && !k_longdone && t->longclickable) { k_longdone = perform_long_click(t); if (k_longdone) t->pressed = 0; return 1; } return 0; }
    if (action == 1) {
        int was = t->pressed; t->pressed = 0; k_target = NULL;
        if (was && !k_longdone && t->clickable) perform_click(t);
        return 1;
    }
    return 0;
}
/* test/automation helper: centre of the first visible view whose text equals label (pixels) */
static KV *findlabel(KV *k, const char *label, int buttons, float ox, float oy, float *cx, float *cy) {
    if (!k || k->vis != 0) return NULL;
    float x = ox + k->x, y = oy + k->y;
    if (k->kind == KV_TEXT && S(k->self, 0).l && !strcmp(rt_cstr(S(k->self, 0).l), label) && (buttons ? k->button && k->clickable : k->clickable || k->longclickable)) {
        *cx = x + k->mw / 2.0f; *cy = y + k->mh / 2.0f; return k; }
    FOR_KIDS(k, c) { KV *r = findlabel(c, label, buttons, x, y, cx, cy); if (r) return r; }
    return NULL;
}
/* buttons first, then any clickable / long-clickable text */
int kv_find_label(void *root, const char *label, float *cx, float *cy) {
    relayout(root);
    return findlabel(kv(root), label, 1, 0, 0, cx, cy) || findlabel(kv(root), label, 0, 0, 0, cx, cy);
}
int kv_is_selected(void *v) { KV *k = kv(v); return k && k->selected; }


static JClass *kall[] = {&K_Object, &K_String, &K_Class, &K_Throwable, &K_Exception, &K_RuntimeException, &K_NPE, &K_IAE, &K_IOOBE, &K_AIOOBE,
    &K_ROE, &K_CNFE, &K_IE, &K_IAccE, &K_CCE, &K_Thread, &K_STE, &K_Number, &K_Integer, &K_Double, &K_Enum, &K_Math, &K_Arrays, &K_Locale,
    &K_StringBuilder, &K_CharSequence, &K_View, &K_ViewGroup, &K_LinearLayout, &K_TableLayout, &K_TableRow, &K_TextView, &K_Button,
    &K_Resources, &K_LayoutInflater, &K_Toast, &K_Bundle, &K_Context, &K_ContextWrapper, &K_Activity};
void jet_kernel_register(void) { for (size_t i = 0; i < sizeof kall / sizeof *kall; i++) rt_register(kall[i]); }
int jet_kernel_classes(void) { return (int) (sizeof kall / sizeof *kall); }
