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
