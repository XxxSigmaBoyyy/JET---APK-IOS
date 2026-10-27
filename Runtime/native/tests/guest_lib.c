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

/* ---- v10.1: signal / statvfs / SVC canaries (bionic LP64 layouts) ---- */
extern int sigaction(int sig, const void *act, void *oact);
extern int sigemptyset(void *set);
extern int sigaddset(void *set, int sig);
extern int sigismember(const void *set, int sig);
extern int sigprocmask(int how, const void *set, void *old);
extern int statvfs(const char *path, void *buf);

static i64 raw_svc(u64 nr, u64 a0, u64 a1, u64 a2, u64 a3) {
    register u64 x8 __asm__("x8") = nr;
    register u64 x0 __asm__("x0") = a0;
    register u64 x1 __asm__("x1") = a1;
    register u64 x2 __asm__("x2") = a2;
    register u64 x3 __asm__("x3") = a3;
    __asm__ __volatile__("svc #0" : "+r"(x0) : "r"(x8), "r"(x1), "r"(x2), "r"(x3) : "memory");
    return (i64)x0;
}
static int bytes_are(const unsigned char *p, int from, int to, unsigned char v) {
    for (int i = from; i < to; i++) if (p[i] != v) return i;
    return -1;
}
struct g_sa  { int flags; int pad; u64 handler; u64 mask; u64 restorer; };   /* 32 */
struct g_ksa { u64 handler; u64 flags; u64 restorer; u64 mask; };           /* 32 */

/* returns 0 ok, else a failure code */
int guest_sigaction_no_smash(void) {
    struct g_sa act = { 0x04000004, 0, 0x1234, 1ull << 9, 0x5678 };
    unsigned char oact[48];
    for (int i = 0; i < 48; i++) oact[i] = 0xA5;
    if (sigaction(10, &act, 0) != 0) return 1;
    if (sigaction(10, 0, oact) != 0) return 2;
    if (bytes_are(oact, 32, 48, 0xA5) >= 0) return 3;              /* overrun */
    struct g_sa *o = (struct g_sa *)oact;
    if (o->flags != 0x04000004 || o->handler != 0x1234 || o->mask != (1ull << 9)
        || o->restorer != 0x5678) return 4;                        /* field order */
    return 0;
}
int guest_sigset_no_smash(void) {
    unsigned char set[16], old[16];
    for (int i = 0; i < 16; i++) { set[i] = 0xA5; old[i] = 0xA5; }
    if (sigemptyset(set) != 0) return 1;
    if (bytes_are(set, 0, 8, 0) >= 0) return 2;
    if (bytes_are(set, 8, 16, 0xA5) >= 0) return 3;                /* 16-byte write */
    if (sigaddset(set, 64) != 0 || set[7] != 0x80) return 4;
    if (!sigismember(set, 64) || sigismember(set, 1)) return 5;
    if (sigprocmask(0, set, old) != 0) return 6;
    if (bytes_are(old, 8, 16, 0xA5) >= 0) return 7;
    if (sigprocmask(2, 0, old) != 0 || old[7] != 0x80) return 8;   /* blocked 64 stuck */
    sigemptyset(set); sigprocmask(2, set, 0);
    return 0;
}
int guest_svc_rt_sigaction(void) {
    struct g_ksa k = { 0xabc, 0x4000000, 0xdef, 1ull << 1 };
    unsigned char ok[40];
    for (int i = 0; i < 40; i++) ok[i] = 0xA5;
    if (raw_svc(134, 12, (u64)&k, 0, 16) != -22) return 1;         /* sigsetsize != 8 */
    if (raw_svc(134, 12, (u64)&k, 0, 8) != 0) return 2;
    if (raw_svc(134, 12, 0, (u64)ok, 8) != 0) return 3;
    if (bytes_are(ok, 32, 40, 0xA5) >= 0) return 4;
    struct g_ksa *o = (struct g_ksa *)ok;
    if (o->handler != 0xabc || o->flags != 0x4000000 || o->restorer != 0xdef || o->mask != 2) return 5;
    struct g_sa u; if (sigaction(12, 0, &u) || u.handler != 0xabc || u.mask != 2) return 6; /* shared */
    return 0;
}
i64 guest_svc_errno(int which) {
    if (which == 0) return raw_svc(999, 0, 0, 0, 0);                              /* -ENOSYS 38 */
    if (which == 1) return raw_svc(56, (u64)-100, (u64)"/nonexistent/jet", 0, 0);   /* -ENOENT 2 */
    if (which == 2) return raw_svc(57, 1000000, 0, 0, 0);                         /* close: -EBADF 9 */
    return 0;
}
int guest_statvfs_no_smash(void) {
    unsigned char b[144];
    for (int i = 0; i < 144; i++) b[i] = 0xA5;
    if (statvfs("/", b) != 0) return 1;
    if (bytes_are(b, 112, 144, 0xA5) >= 0) return 2;
    if (*(u64 *)b == 0) return 3;                                  /* f_bsize */
    return 0;
}

/* ===================== v10.2: filesystem + netdb guest tests ===================== */
extern int *__errno(void);
extern int mkdir(const char*, unsigned);   extern int mkdirat(int, const char*, unsigned);
extern int unlink(const char*);            extern int unlinkat(int, const char*, int);
extern int rmdir(const char*);             extern int remove(const char*);
extern int rename(const char*, const char*);
extern int renameat(int, const char*, int, const char*);
extern int renameat2(int, const char*, int, const char*, unsigned);
extern int symlink(const char*, const char*); extern int symlinkat(const char*, int, const char*);
extern long readlink(const char*, char*, u64); extern long readlinkat(int, const char*, char*, u64);
extern int link(const char*, const char*);  extern int linkat(int, const char*, int, const char*, int);
extern int access(const char*, int);        extern int faccessat(int, const char*, int, int);
extern int chmod(const char*, unsigned);    extern int fchmod(int, unsigned);
extern int fchmodat(int, const char*, unsigned, int);
extern int truncate(const char*, i64);      extern int ftruncate(int, i64);
extern int utimensat(int, const char*, const void*, int); extern int futimens(int, const void*);
extern int fsync(int);                      extern int fdatasync(int);
extern int open(const char*, int, ...);     extern int close(int);
extern long write(int, const void*, u64);
extern int fstatat(int, const char*, void*, int);

#define GE (*__errno())
#define AT_FDCWD_ (-100)
struct g_stat { u64 dev, ino; unsigned mode, nlink, uid, gid; u64 rdev, pad; i64 size; int blksize, pad2; i64 blocks;
                i64 atime, atime_ns, mtime, mtime_ns, ctime, ctime_ns; unsigned unused[2]; };   /* 128 */
struct g_ts { i64 sec, nsec; };

static void pj(char *o, const char *d, const char *n) {
    int i = 0; while (*d) o[i++] = *d++; o[i++] = '/'; while (*n) o[i++] = *n++; o[i] = 0;
}
static int gstreq(const char *a, const char *b) { while (*a && *a == *b) { a++; b++; } return *a == *b; }
static i64 raw_svc5(u64 nr, u64 a0, u64 a1, u64 a2, u64 a3, u64 a4) {
    register u64 x8 __asm__("x8") = nr;
    register u64 x0 __asm__("x0") = a0; register u64 x1 __asm__("x1") = a1;
    register u64 x2 __asm__("x2") = a2; register u64 x3 __asm__("x3") = a3; register u64 x4 __asm__("x4") = a4;
    __asm__ __volatile__("svc #0" : "+r"(x0) : "r"(x8), "r"(x1), "r"(x2), "r"(x3), "r"(x4) : "memory");
    return (i64)x0;
}
#define FAILS(n, cond) do { if (!(cond)) return (n); } while (0)
#define ERR(n, call, e) do { GE = 0; if ((call) != -1 || GE != (e)) return (n); } while (0)

/* dir: an existing empty temp directory (host path; identity-mapped). Returns 0 or the failing step. */
int guest_fs_ops(const char *dir) {
    char a[512], b[512], f[512], f2[512], l[512], hl[512], hl2[512], x[512], buf[64];
    struct g_stat st;
    pj(a, dir, "a"); pj(b, dir, "b"); pj(x, dir, "nope");
    FAILS(1, mkdir(a, 0755) == 0);
    ERR(2, mkdir(a, 0755), 17);                                     /* EEXIST */
    FAILS(3, mkdirat(AT_FDCWD_, b, 0700) == 0);
    pj(f, a, "f"); pj(f2, a, "f2"); pj(l, a, "l"); pj(hl, a, "hl"); pj(hl2, a, "hl2");
    int fd = open(f, 0101 /*O_WRONLY|O_CREAT*/, 0644);
    FAILS(4, fd >= 0);
    FAILS(5, write(fd, "hello", 5) == 5);
    FAILS(6, fsync(fd) == 0 && fdatasync(fd) == 0);
    FAILS(7, ftruncate(fd, 3) == 0);
    ERR(8, ftruncate(fd, -1), 22);
    struct g_ts now2[2] = { { 0, (1 << 30) - 1 }, { 0, (1 << 30) - 1 } };   /* UTIME_NOW both */
    FAILS(9, futimens(fd, now2) == 0);
    FAILS(10, fchmod(fd, 0600) == 0);
    close(fd);
    ERR(11, fsync(fd), 9);                                          /* EBADF after close */
    FAILS(12, truncate(f, 2) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0 && st.size == 2);
    FAILS(13, (st.mode & 0777) == 0600);
    ERR(14, truncate(x, 0), 2);                                     /* ENOENT */
    /* symlink / readlink */
    FAILS(15, symlink("f", l) == 0);
    for (int i = 0; i < 64; i++) buf[i] = 'Z';
    FAILS(16, readlink(l, buf, 64) == 1 && buf[0] == 'f' && buf[1] == 'Z');
    FAILS(17, readlinkat(AT_FDCWD_, l, buf, 64) == 1);
    ERR(18, readlinkat(AT_FDCWD_, l, buf, 0), 22);
    ERR(19, readlink(f, buf, 64), 22);                              /* not a link */
    ERR(20, symlink("f", l), 17);
    FAILS(21, symlinkat("f2", AT_FDCWD_, x) == 0 && unlink(x) == 0);
    /* link: Linux semantics = hard link to the symlink itself */
    FAILS(22, link(l, hl) == 0 && fstatat(AT_FDCWD_, hl, &st, 0x100) == 0 && (st.mode & 0170000) == 0120000);
    FAILS(23, linkat(AT_FDCWD_, l, AT_FDCWD_, hl2, 0x400) == 0 && fstatat(AT_FDCWD_, hl2, &st, 0x100) == 0
              && (st.mode & 0170000) == 0100000 && st.nlink == 2);
    ERR(24, linkat(AT_FDCWD_, f, AT_FDCWD_, x, 0x8), 22);
    ERR(25, link(f, hl2), 17);
    /* access */
    FAILS(26, access(f, 4) == 0 && access(f, 0) == 0);
    ERR(27, access(f, 8), 22);
    ERR(28, access(x, 0), 2);
    FAILS(29, faccessat(AT_FDCWD_, f, 4, 0x200) == 0 && faccessat(AT_FDCWD_, l, 0, 0x100) == 0);
    ERR(30, faccessat(AT_FDCWD_, f, 0, 0x1), 22);
    /* chmod */
    FAILS(31, chmod(f, 0640) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0 && (st.mode & 0777) == 0640);
    FAILS(32, fchmodat(AT_FDCWD_, f, 0604, 0) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0 && (st.mode & 0777) == 0604);
    ERR(33, fchmodat(AT_FDCWD_, f, 0600, 0x2), 22);
    ERR(34, chmod(x, 0600), 2);
    /* utimensat: set atime, OMIT mtime */
    FAILS(35, fstatat(AT_FDCWD_, f, &st, 0) == 0);
    i64 m0 = st.mtime, m0n = st.mtime_ns;
    struct g_ts ts[2] = { { 1000000000, 5000 }, { 1234567890, (1 << 30) - 2 } };
    FAILS(36, utimensat(AT_FDCWD_, f, ts, 0) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0);
    FAILS(37, st.atime == 1000000000 && st.atime_ns == 5000 && st.mtime == m0 && st.mtime_ns == m0n);
    struct g_ts ts2[2] = { { 0, (1 << 30) - 2 }, { 1500000000, 0 } };
    FAILS(38, utimensat(AT_FDCWD_, f, ts2, 0) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0
              && st.atime == 1000000000 && st.mtime == 1500000000);
    FAILS(39, utimensat(AT_FDCWD_, f, 0, 0) == 0 && fstatat(AT_FDCWD_, f, &st, 0) == 0 && st.mtime > 1500000000);
    ERR(40, utimensat(AT_FDCWD_, f, 0, 0x1), 22);
    struct g_ts bad[2] = { { 0, 1000000000 }, { 0, 0 } };
    ERR(41, utimensat(AT_FDCWD_, f, bad, 0), 22);
    FAILS(42, utimensat(AT_FDCWD_, l, ts, 0x100) == 0);             /* NOFOLLOW on symlink */
    /* rename family */
    FAILS(43, rename(f, f2) == 0 && access(f, 0) == -1 && access(f2, 0) == 0);
    ERR(44, renameat2(AT_FDCWD_, f2, AT_FDCWD_, hl2, 1), 17);       /* NOREPLACE -> EEXIST */
    ERR(45, renameat2(AT_FDCWD_, f2, AT_FDCWD_, f, 2), 22);         /* EXCHANGE -> EINVAL */
    ERR(46, renameat2(AT_FDCWD_, f2, AT_FDCWD_, f, 4), 22);         /* WHITEOUT -> EINVAL */
    FAILS(47, renameat2(AT_FDCWD_, f2, AT_FDCWD_, f, 1) == 0 && access(f, 0) == 0);
    FAILS(48, renameat2(AT_FDCWD_, f, AT_FDCWD_, f2, 0) == 0 && renameat(AT_FDCWD_, f2, AT_FDCWD_, f) == 0
              && access(f, 0) == 0);
    ERR(49, rename(x, f2), 2);
    /* unlink / rmdir */
    ERR(50, unlink(a), 21);                                         /* EISDIR (Darwin EPERM fixed up) */
    ERR(51, unlinkat(AT_FDCWD_, a, 0), 21);
    ERR(52, unlinkat(AT_FDCWD_, a, 0x1), 22);
    ERR(53, rmdir(a), 39);                                          /* ENOTEMPTY */
    ERR(54, unlinkat(AT_FDCWD_, a, 0x200), 39);
    ERR(55, rmdir(f), 20);                                          /* ENOTDIR */
    FAILS(56, unlinkat(AT_FDCWD_, b, 0x200) == 0 && access(b, 0) == -1);
    FAILS(57, unlink(l) == 0 && unlinkat(AT_FDCWD_, hl, 0) == 0 && remove(hl2) == 0 && remove(f) == 0);
    ERR(58, remove(x), 2);
    ERR(59, unlink(f), 2);
    /* raw SVC path (kernel ABI, -errno returns) */
    FAILS(60, raw_svc5(34, (u64)-100, (u64)b, 0700, 0, 0) == 0);    /* mkdirat */
    FAILS(61, raw_svc5(34, (u64)-100, (u64)b, 0700, 0, 0) == -17);
    FAILS(62, raw_svc5(48, (u64)-100, (u64)b, 0, 0x12345, 0) == 0); /* faccessat: no flags arg */
    FAILS(63, raw_svc5(439, (u64)-100, (u64)b, 0, 0x1, 0) == -22);  /* faccessat2 bad flags */
    FAILS(64, raw_svc5(35, (u64)-100, (u64)b, 0, 0, 0) == -21);     /* unlinkat dir w/o REMOVEDIR */
    FAILS(65, raw_svc5(276, (u64)-100, (u64)b, (u64)-100, (u64)x, 2) == -22); /* renameat2 EXCHANGE */
    FAILS(66, raw_svc5(276, (u64)-100, (u64)b, (u64)-100, (u64)x, 1) == 0);   /* NOREPLACE ok */
    FAILS(67, raw_svc5(36, (u64)"nope", (u64)-100, (u64)b, 0, 0) == 0);       /* symlinkat */
    FAILS(68, raw_svc5(78, (u64)-100, (u64)b, (u64)buf, 64, 0) == 4);         /* readlinkat */
    FAILS(69, raw_svc5(35, (u64)-100, (u64)b, 0, 0, 0) == 0);
    FAILS(70, raw_svc5(35, (u64)-100, (u64)x, 0x200, 0, 0) == 0);
    FAILS(71, raw_svc5(35, (u64)-100, (u64)a, 0x200, 0, 0) == 0);
    FAILS(72, raw_svc5(35, (u64)-100, (u64)a, 0x200, 0, 0) == -2);
    FAILS(73, raw_svc5(82, 9999, 0, 0, 0, 0) == -9);                 /* fsync EBADF */
    return 0;
}

/* ---- netdb: guest bionic addrinfo (48 bytes, BSD order) ---- */
struct g_ai { int flags, family, socktype, protocol; unsigned addrlen; int pad;
              char *canonname; unsigned char *addr; struct g_ai *next; };
extern int getaddrinfo(const char*, const char*, const struct g_ai*, struct g_ai**);
extern void freeaddrinfo(struct g_ai*);
extern int getnameinfo(const void*, unsigned, char*, u64, char*, u64, int);
extern const char *gai_strerror(int);

static void ai_zero(struct g_ai *h) { char *p = (char*)h; for (int i = 0; i < 48; i++) p[i] = 0; }

int guest_netdb(void) {
    struct g_ai h, *r = 0;
    if (sizeof(struct g_ai) != 48 || __builtin_offsetof(struct g_ai, canonname) != 24
        || __builtin_offsetof(struct g_ai, addr) != 32 || __builtin_offsetof(struct g_ai, next) != 40) return 1;
    /* IPv6 numeric */
    ai_zero(&h); h.flags = 4 | 8; h.family = 10; h.socktype = 1;
    FAILS(2, getaddrinfo("::1", "8080", &h, &r) == 0 && r);
    FAILS(3, r->family == 10 && r->socktype == 1 && r->addrlen == 28 && r->next == 0 && r->canonname == 0);
    FAILS(4, r->addr && r->addr[0] == 10 && r->addr[1] == 0 && r->addr[2] == 0x1f && r->addr[3] == 0x90);
    for (int i = 8; i < 23; i++) FAILS(5, r->addr[i] == 0);
    FAILS(6, r->addr[23] == 1);
    freeaddrinfo(r); r = 0;
    /* IPv4 numeric + canonname */
    ai_zero(&h); h.flags = 4 | 8 | 2; h.family = 2; h.socktype = 2;
    FAILS(7, getaddrinfo("127.0.0.1", "80", &h, &r) == 0 && r);
    FAILS(8, r->family == 2 && r->socktype == 2 && r->protocol == 17 && r->addrlen == 16);
    FAILS(9, r->addr[0] == 2 && r->addr[1] == 0 && r->addr[2] == 0 && r->addr[3] == 80
             && r->addr[4] == 127 && r->addr[5] == 0 && r->addr[6] == 0 && r->addr[7] == 1);
    FAILS(10, r->canonname && gstreq(r->canonname, "127.0.0.1"));
    /* getnameinfo round-trip on the guest sockaddr */
    char host[64], serv[16];
    FAILS(11, getnameinfo(r->addr, r->addrlen, host, sizeof host, serv, sizeof serv, 2 | 8) == 0
              && gstreq(host, "127.0.0.1") && gstreq(serv, "80"));
    FAILS(12, getnameinfo(r->addr, r->addrlen, host, 3, 0, 0, 2) == 14);          /* EAI_OVERFLOW */
    FAILS(13, getnameinfo(r->addr, 7, host, sizeof host, 0, 0, 2) == 5);          /* bad salen -> EAI_FAMILY (bionic) */
    freeaddrinfo(r); r = 0;
    /* null hints -> list over socktypes, walk next */
    FAILS(14, getaddrinfo("127.0.0.1", "443", 0, &r) == 0 && r);
    int n = 0;
    for (struct g_ai *p = r; p; p = p->next) {
        n++;
        FAILS(15, p->family == 2 && p->addrlen == 16 && p->addr[0] == 2 && p->addr[2] == 1 && p->addr[3] == 0xbb);
        FAILS(16, (unsigned char*)p->addr == (unsigned char*)p + 48);              /* one guest-owned block */
    }
    FAILS(17, n >= 1);
    freeaddrinfo(r); r = 0;
    /* IPv6 getnameinfo */
    unsigned char s6[28] = {0}; s6[0] = 10; s6[2] = 0x01; s6[3] = 0xbb; s6[23] = 1;
    FAILS(18, getnameinfo(s6, 28, host, sizeof host, serv, sizeof serv, 2 | 8) == 0 && gstreq(host, "::1") && gstreq(serv, "443"));
    /* errors: positive bionic EAI codes */
    ai_zero(&h); h.flags = 4;
    FAILS(19, getaddrinfo("not-an-ip", 0, &h, &r) == 8);               /* EAI_NONAME */
    FAILS(20, getaddrinfo(0, 0, 0, &r) == 8);
    ai_zero(&h); h.flags = 0x800;
    FAILS(21, getaddrinfo("127.0.0.1", 0, &h, &r) == 3);               /* EAI_BADFLAGS */
    ai_zero(&h); h.family = 1;
    FAILS(22, getaddrinfo("127.0.0.1", 0, &h, &r) == 5);               /* EAI_FAMILY */
    ai_zero(&h); h.socktype = 5;
    FAILS(23, getaddrinfo("127.0.0.1", 0, &h, &r) == 10);              /* EAI_SOCKTYPE */
    ai_zero(&h); h.next = &h;
    FAILS(24, getaddrinfo("127.0.0.1", 0, &h, &r) == 12);              /* EAI_BADHINTS */
    ai_zero(&h); h.flags = 4 | 8;
    FAILS(25, getaddrinfo("127.0.0.1", "http", &h, &r) == 8);          /* NUMERICSERV non-numeric */
    ai_zero(&h); h.flags = 4; h.family = 10;
    FAILS(26, getaddrinfo("127.0.0.1", 0, &h, &r) != 0);               /* v4 literal w/ AF_INET6 */
    FAILS(27, gstreq(gai_strerror(8), "hostname nor servname provided, or not known"));
    FAILS(28, gstreq(gai_strerror(0), "Success") && gstreq(gai_strerror(99), "Unknown error"));
    return 0;
}
