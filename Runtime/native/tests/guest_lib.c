/* Tiny freestanding AArch64 guest shared library for unit tests.
 * Compiled with: clang --target=aarch64-linux-gnu -nostdlib -shared -fPIC
 */
typedef unsigned long long u64;
typedef long long i64;
typedef int i32;

/* Imported from host trap table */
extern void *__errno_location_like(void);
extern int host_add(int a, int b);
extern int host_null_trap(void);
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

/* Guest → host trap that will NULL-deref on the host (soft-fault must forward). */
int guest_call_host_null(void) {
    return host_null_trap();
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

/* ---- Hypotheses: REV / ROR / EXTR / w-zero-extend ---- */
u64 guest_rev64(u64 x) {
    u64 r;
    __asm__ __volatile__("rev %0, %1" : "=r"(r) : "r"(x));
    return r;
}
u64 guest_ror64(u64 x, int n) {
    /* Variable ROR via RORV */
    u64 r;
    __asm__ __volatile__("ror %0, %1, %2" : "=r"(r) : "r"(x), "r"((u64)(n & 63)));
    return r;
}
u64 guest_extr_ror64(u64 x) {
    u64 r;
    __asm__ __volatile__("extr %0, %1, %1, #14" : "=r"(r) : "r"(x));
    return r;
}
u64 guest_rev32_w(u64 x) {
    u64 r;
    __asm__ __volatile__("rev %w0, %w1" : "=r"(r) : "r"(x));
    return r;
}
/* Put garbage in upper 32, then ADD W — upper must become 0. */
u64 guest_w_add_zero_upper(u64 x) {
    u64 r;
    __asm__ __volatile__("add %w0, %w1, #1" : "=r"(r) : "r"(x));
    return r;
}
u64 guest_eor_ror23(u64 x) {
    u64 r;
    __asm__ __volatile__("eor %0, %1, %1, ror #23" : "=r"(r) : "r"(x));
    return r;
}

/* MOVI Vd.16B + EOR — HMAC ipad/opad path (cmode=0xE op=0 must byte-replicate). */
u64 guest_movi_eor_ipad(u64 lo, u64 hi) {
    u64 out_lo, out_hi;
    __asm__ __volatile__(
        "mov v0.d[0], %2\n\t"
        "mov v0.d[1], %3\n\t"
        "movi v2.16b, #0x36\n\t"
        "eor v0.16b, v0.16b, v2.16b\n\t"
        "mov %0, v0.d[0]\n\t"
        "mov %1, v0.d[1]\n\t"
        : "=r"(out_lo), "=r"(out_hi)
        : "r"(lo), "r"(hi)
        : "v0", "v2"
    );
    /* Pack: return lo; caller also checks via side buffer if needed.
       Encode both in return by xoring hi into a known check via returning lo only
       and writing hi through a pointer would need another arg — return lo,
       and put hi in a static for the test... Better: use two outputs via pointer. */
    (void)out_hi;
    return out_lo;
}
/* Write 16-byte ipad result to dst. */
void guest_hmac_ipad16(void *dst, const void *src) {
    __asm__ __volatile__(
        "ldr q0, [%1]\n\t"
        "movi v2.16b, #0x36\n\t"
        "eor v0.16b, v0.16b, v2.16b\n\t"
        "str q0, [%0]\n\t"
        :
        : "r"(dst), "r"(src)
        : "v0", "v2", "memory"
    );
}
void guest_hmac_opad16(void *dst, const void *src) {
    __asm__ __volatile__(
        "ldr q0, [%1]\n\t"
        "movi v2.16b, #0x5c\n\t"
        "eor v0.16b, v0.16b, v2.16b\n\t"
        "str q0, [%0]\n\t"
        :
        : "r"(dst), "r"(src)
        : "v0", "v2", "memory"
    );
}

/* Deliberate NULL deref for soft-fault unit tests. */
int guest_null_deref(void) {
    volatile int *p = 0;
    return *p;
}

/* FMOV scalar immediate + barrier smoke tests */
double guest_fmov_d10(void) {
    double d;
    __asm__ volatile("fmov %d0, #10.0" : "=w"(d));
    return d;
}
float guest_fmov_s1(void) {
    float f;
    __asm__ volatile("fmov %s0, #1.0" : "=w"(f));
    return f;
}
/* DMB ISH then return 7 — must not undef */
int guest_dmb_ish(void) {
    __asm__ volatile("dmb ish" ::: "memory");
    return 7;
}
int guest_clrex(void) {
    __asm__ volatile("clrex" ::: "memory");
    return 9;
}

u64 guest_fmov_d1_lane(u64 hi) {
    u64 out;
    __asm__ volatile(
        "fmov d0, xzr\n"
        "fmov v0.d[1], %1\n"
        "fmov %0, v0.d[1]\n"
        : "=r"(out) : "r"(hi) : "v0"
    );
    return out;
}


/* C++-style throw/catch across frames via setjmp/longjmp (guest uses its own
 * control flow; host provides setjmp/longjmp traps). Verifies multi-frame
 * unwinding of the guest stack under the interpreter — same requirement as
 * real __cxa_throw once libstdc++ PAC/CFA path is fully green. */
static int guest_throw_deep(void *jb, int n) {
    if (n <= 0) {
        longjmp(jb, 99);
        return -1;
    }
    return guest_throw_deep(jb, n - 1) + 1;
}
int guest_throw_catch(int depth) {
    unsigned char buf[1024];
    for (int i = 0; i < 1024; i++) buf[i] = 0;
    int r = setjmp(buf);
    if (r == 0) {
        guest_throw_deep(buf, depth);
        return -1; /* unreachable if throw works */
    }
    return r; /* expect 99 */
}

/* ---- v10: trap ABI + guest struct stat size ---- */
extern int open(const char *path, int flags, ...);
extern int close(int fd);
extern int fstat(int fd, void *st);
extern void *malloc(unsigned long n);
extern void free(void *p);

/* Known sentinel values in callee-saved regs; call host traps; return 0 if all
 * preserved, else a bitmask of which x19+i / d8+i failed. */
int guest_trap_abi_check(void) {
    unsigned long long want[10];
    unsigned long long want_d[8];
    for (int i = 0; i < 10; i++) want[i] = 0xA190000000000000ull + (unsigned long long)i;
    for (int i = 0; i < 8; i++) want_d[i] = 0xD800000000000000ull + (unsigned long long)i;

    register unsigned long long x19 __asm__("x19") = want[0];
    register unsigned long long x20 __asm__("x20") = want[1];
    register unsigned long long x21 __asm__("x21") = want[2];
    register unsigned long long x22 __asm__("x22") = want[3];
    register unsigned long long x23 __asm__("x23") = want[4];
    register unsigned long long x24 __asm__("x24") = want[5];
    register unsigned long long x25 __asm__("x25") = want[6];
    register unsigned long long x26 __asm__("x26") = want[7];
    register unsigned long long x27 __asm__("x27") = want[8];
    register unsigned long long x28 __asm__("x28") = want[9];
    /* Keep them live across calls */
    __asm__ volatile("" : "+r"(x19),"+r"(x20),"+r"(x21),"+r"(x22),"+r"(x23),
                           "+r"(x24),"+r"(x25),"+r"(x26),"+r"(x27),"+r"(x28));

    __asm__ volatile(
        "fmov d8, %0\n"
        "fmov d9, %1\n"
        "fmov d10, %2\n"
        "fmov d11, %3\n"
        "fmov d12, %4\n"
        "fmov d13, %5\n"
        "fmov d14, %6\n"
        "fmov d15, %7\n"
        :
        : "r"(want_d[0]),"r"(want_d[1]),"r"(want_d[2]),"r"(want_d[3]),
          "r"(want_d[4]),"r"(want_d[5]),"r"(want_d[6]),"r"(want_d[7])
        : "d8","d9","d10","d11","d12","d13","d14","d15"
    );

    /* Host traps that previously risked clobber / stack smash */
    unsigned char st[128];
    for (int i = 0; i < 128; i++) st[i] = 0xCC;
    int fd = open("/dev/null", 0);
    if (fd >= 0) {
        fstat(fd, st);
        close(fd);
    }
    void *p = malloc(64);
    if (p) free(p);

    __asm__ volatile("" : "+r"(x19),"+r"(x20),"+r"(x21),"+r"(x22),"+r"(x23),
                           "+r"(x24),"+r"(x25),"+r"(x26),"+r"(x27),"+r"(x28));

    unsigned long long got[10] = {x19,x20,x21,x22,x23,x24,x25,x26,x27,x28};
    unsigned long long got_d[8];
    __asm__ volatile(
        "fmov %0, d8\n"
        "fmov %1, d9\n"
        "fmov %2, d10\n"
        "fmov %3, d11\n"
        "fmov %4, d12\n"
        "fmov %5, d13\n"
        "fmov %6, d14\n"
        "fmov %7, d15\n"
        : "=r"(got_d[0]),"=r"(got_d[1]),"=r"(got_d[2]),"=r"(got_d[3]),
          "=r"(got_d[4]),"=r"(got_d[5]),"=r"(got_d[6]),"=r"(got_d[7])
        :
        : 
    );

    int bad = 0;
    for (int i = 0; i < 10; i++) if (got[i] != want[i]) bad |= (1 << i);
    for (int i = 0; i < 8; i++) if (got_d[i] != want_d[i]) bad |= (1 << (16 + i));
    /* Also verify fstat only wrote 128 bytes (no smash past buffer) —
     * we can't see past st here; return bad bitmask. */
    (void)st;
    return bad;
}

/* Return sizeof buffer guest expects for struct stat (compile-time 128). */
int guest_stat_size(void) { return 128; }

/* Fill a 160-byte region with 0xA5, call fstat into offset 0 with 128-byte
 * expectation; return first byte past 128 that was overwritten (or 0). */
int guest_fstat_no_smash(void) {
    unsigned char buf[160];
    for (int i = 0; i < 160; i++) buf[i] = 0xA5;
    int fd = open("/dev/null", 0);
    if (fd < 0) return -1;
    fstat(fd, buf);
    close(fd);
    for (int i = 128; i < 160; i++) {
        if (buf[i] != 0xA5) return i; /* smashed */
    }
    return 0; /* clean */
}
