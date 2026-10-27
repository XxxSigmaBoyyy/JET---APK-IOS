/* Tiny freestanding AArch64 guest shared library for unit tests.
 * Compiled with: clang --target=aarch64-linux-gnu -nostdlib -shared -fPIC
 */
typedef unsigned long long u64;
typedef long long i64;
typedef int i32;

/* Imported from host trap table */
extern void *__errno_location_like(void);
extern int host_add(int a, int b);
extern void host_log(const char *s);

/* Simple exports */
int guest_add(int a, int b) { return a + b; }
int guest_mul_add(int a, int b, int c) { return a * b + c; }

u64 guest_sum_array(const int *p, int n) {
    u64 s = 0;
    for (int i = 0; i < n; i++) s += (u64)p[i];
    return s;
}

/* Calls a host import */
int guest_call_host(int a, int b) {
    return host_add(a, b);
}

/* setjmp / longjmp exercise — use our host traps by calling them as symbols */
extern int setjmp(void *jb);
extern void longjmp(void *jb, int v);

int guest_setjmp_test(void) {
    unsigned char buf[1024];
    for (int i = 0; i < 1024; i++) buf[i] = 0;
    int r = setjmp(buf);
    if (r == 0) {
        longjmp(buf, 42);
        return -1; /* unreachable */
    }
    return r;
}

/* Recursive fibonacci — interpreter speed benchmark */
int guest_fib(int n) {
    if (n < 2) return n;
    return guest_fib(n - 1) + guest_fib(n - 2);
}

/* Callback target for trampoline test */
int guest_cmp(const int *a, const int *b) {
    return *a - *b;
}

/* JNI-like OnLoad that calls RegisterNatives via a fake path is too heavy
 * without a full JNIEnv; keep a simple exported symbol instead. */
i32 JNI_OnLoad(void *vm, void *reserved) {
    (void)vm; (void)reserved;
    return 0x00010006;
}

int Java_com_example_Foo_bar(void *env, void *obj, int x) {
    (void)env; (void)obj;
    return x * 2 + 7;
}

void guest_entry(void) {}

/* Regression: LDR D must zero Vd high; STR D must write exactly 8 bytes.
 * Mirrors ConnectionsManager ctor: ldr d0 from (lo, 1000.0) pair then
 * str d0 at object+0 — a 16-byte store would clobber +8 with 1000.0. */
u64 guest_fp_d_store_probe(u64 *obj_two_slots) {
    /* pool: [0]=arbitrary D bits, [1]=1000.0. LDR D must NOT pull [1] into V high
     * in a way that a buggy STR D then writes to obj[1]. */
    static const unsigned long long pool[2] = {
        0x0000000500000000ull, /* same low pattern TG uses */
        0x408f400000000000ull  /* 1000.0 */
    };
    unsigned long long sentinel = 0xDEADBEEFCAFEBABEull;
    obj_two_slots[0] = 0;
    obj_two_slots[1] = sentinel;
    /* Force LDR D / STR D via asm so the compiler cannot widen to Q. */
    __asm__ __volatile__(
        "ldr d0, [%1]\n\t"
        "str d0, [%0]\n\t"
        :
        : "r"(obj_two_slots), "r"(pool)
        : "d0", "memory"
    );
    return obj_two_slots[1];
}

/* Indirect-result (x8) smoke: write a marker through x8 and return. */
struct guest_big { u64 a, b, c; }; /* 24 bytes > 16 → returned via x8 */
struct guest_big guest_return_big(u64 a, u64 b, u64 c) {
    struct guest_big r;
    r.a = a; r.b = b; r.c = c;
    return r;
}
