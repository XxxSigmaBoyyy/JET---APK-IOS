/* v10.1 Darwin ABI: pure host<->guest translation (runs on Linux), epoll-on-kqueue logic
 * against a mock kqueue, eventfd pipe emulation, and guest-level signal/SVC canaries. */
#include "jet_native.h"
#include "jet_internal.h"
#include "guest_abi.h"
#include "darwin_abi.h"
#include "epoll_kq.h"
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int fails, oks;
#define CHECK(c, ...) do { if (!(c)) { fprintf(stderr, "FAIL %s:%d: ", __func__, __LINE__); \
    fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); fails++; } else oks++; } while (0)
#define CAN 0xA5
static int tail_ok(const void *p, size_t from, size_t to) {
    const uint8_t *b = p; for (size_t i = from; i < to; i++) if (b[i] != CAN) return 0; return 1;
}

static void t_sizes(void) {
    CHECK(sizeof(jet_darwin_stat) == 144 && sizeof(jet_darwin_statfs) == 2168, "stat/statfs");
    CHECK(sizeof(jet_darwin_termios) == 72 && sizeof(jet_darwin_flock) == 24, "termios/flock");
    CHECK(sizeof(jet_kevent) == 32 && sizeof(jet_darwin_cmsghdr) == 12, "kevent/cmsghdr");
    CHECK(sizeof(jet_guest_flock) == 32 && sizeof(jet_guest_sigaction) == 32, "guest flock/sigaction");
    CHECK(sizeof(jet_guest_kernel_sigaction) == 32 && sizeof(jet_guest_siginfo) == 128, "ksa/siginfo");
    CHECK(sizeof(jet_guest_sigset) == 8, "sigset");
}

static void t_errno(void) {
    CHECK(jet_xl_errno_d2l(35) == 11, "EAGAIN d2l"); CHECK(jet_xl_errno_d2l(36) == 115, "EINPROGRESS");
    CHECK(jet_xl_errno_d2l(54) == 104, "ECONNRESET"); CHECK(jet_xl_errno_d2l(60) == 110, "ETIMEDOUT");
    CHECK(jet_xl_errno_d2l(61) == 111, "ECONNREFUSED"); CHECK(jet_xl_errno_d2l(78) == 38, "ENOSYS");
    CHECK(jet_xl_errno_d2l(11) == 35, "Darwin EDEADLK -> 35"); CHECK(jet_xl_errno_d2l(2) == 2, "ENOENT");
    /* Linux errnos that exist on Darwin must round-trip l2d -> d2l (Darwin-only codes are many-to-one). */
    static const int lin[] = { 1,2,4,5,9,11,12,13,17,20,21,22,24,28,32,35,36,38,39,40,61,62,71,75,84,88,89,90,
        91,92,93,94,95,97,98,99,100,101,102,103,104,105,106,107,110,111,112,113,114,115,116,122,125 };
    for (size_t i = 0; i < sizeof lin / sizeof *lin; i++)
        CHECK(jet_xl_errno_d2l(jet_xl_errno_l2d(lin[i])) == lin[i], "roundtrip linux %d (d=%d)", lin[i], jet_xl_errno_l2d(lin[i]));
    CHECK(jet_xl_errno_l2d(11) == 35 && jet_xl_errno_l2d(110) == 60 && jet_xl_errno_l2d(38) == 78, "l2d samples");
}
static void t_consts(void) {
    CHECK(jet_xl_af_l2d(10) == 30 && jet_xl_af_d2l(30) == 10 && jet_xl_af_l2d(2) == 2, "AF");
    int lo = 01 | 0100 | 01000 | 02000 | 04000 | 02000000;   /* WRONLY|CREAT|TRUNC|APPEND|NONBLOCK|CLOEXEC */
    int dO = jet_xl_oflags_l2d(lo);
    CHECK(dO == (0x1 | 0x200 | 0x400 | 0x8 | 0x4 | 0x1000000), "oflags l2d %#x", dO);
    CHECK(jet_xl_oflags_d2l(dO) == lo, "oflags d2l");
    CHECK(jet_xl_oflags_l2d(040000) == 0x100000 && jet_xl_oflags_l2d(0100000) == 0x100 && jet_xl_oflags_l2d(0x101000) == 0x400080 && jet_xl_oflags_d2l(0x400080) == 0x101000, "arm64 DIRECTORY/NOFOLLOW/SYNC(=SYNC|DSYNC)");
    CHECK(jet_xl_oflags_l2d(020000000 | 0200000) == -1, "O_TMPFILE rejected");
    CHECK(jet_xl_atfd_l2d(-100) == -2 && jet_xl_atfd_l2d(5) == 5, "AT_FDCWD");
    CHECK(jet_xl_atflags_l2d(0x100, 0) == 0x20 && jet_xl_atflags_l2d(0x200, 1) == 0x10, "AT flags");
    CHECK(jet_xl_fcntl_cmd_l2d(5) == 7 && jet_xl_fcntl_cmd_l2d(6) == 8 && jet_xl_fcntl_cmd_l2d(7) == 9, "F_*LK");
    CHECK(jet_xl_fcntl_cmd_l2d(1030) == 67 && jet_xl_fcntl_cmd_l2d(1024) == -1, "DUPFD_CLOEXEC / SETLEASE");
    jet_guest_flock gf = { 1 /*F_WRLCK*/, 1, 0, 100, 50, 4242, 0 }; jet_darwin_flock df;
    CHECK(jet_xl_flock_l2d(&gf, &df) == 0 && df.l_type == 3 && df.l_whence == 1 && df.l_start == 100 && df.l_len == 50, "flock l2d");
    df.l_type = 2; jet_guest_flock gb; memset(&gb, CAN, sizeof gb); jet_xl_flock_d2l(&df, &gb);
    CHECK(gb.l_type == 2 /*F_UNLCK*/ && gb.l_pid == 4242 && gb.l_start == 100, "flock d2l");
    CHECK(jet_xl_clock_l2d(0) == 0 && jet_xl_clock_l2d(1) == 8 && jet_xl_clock_l2d(4) == 4 && jet_xl_clock_l2d(7) == 6, "clocks");
    CHECK(jet_xl_clock_l2d(11) == 0 && jet_xl_clock_l2d(99) == -1, "TAI->REALTIME, unknown rejected");
    CHECK(jet_xl_msgflags_l2d(0x40 | 0x2 | 0x4000) == (0x80 | 0x2), "MSG DONTWAIT|PEEK, drop NOSIGNAL: %#x", jet_xl_msgflags_l2d(0x40|0x2|0x4000));
    CHECK(jet_xl_msgflags_d2l(0x10 | 0x20) == (0x20 | 0x8), "MSG TRUNC|CTRUNC d2l");
    int nb = 0, ce = 0;
    CHECK(jet_xl_socktype_l2d(1 | 04000 | 02000000, &nb, &ce) == 1 && nb && ce, "SOCK_NONBLOCK|CLOEXEC");
    int dl, dopt;
    CHECK(jet_xl_sockopt_l2d(1, 2, &dl, &dopt) == 0 && dl == 0xffff && dopt == 4, "SO_REUSEADDR");
    CHECK(jet_xl_sockopt_l2d(1, 20, &dl, &dopt) == 1 && dopt == 0x1006, "SO_RCVTIMEO timeval");
    CHECK(jet_xl_sockopt_l2d(1, 4, &dl, &dopt) == 2 && dopt == 0x1007, "SO_ERROR");
    CHECK(jet_xl_sockopt_l2d(6, 4, &dl, &dopt) == 0 && dl == 6 && dopt == 0x10, "TCP_KEEPIDLE");
    CHECK(jet_xl_sockopt_l2d(41, 26, &dl, &dopt) == 0 && dopt == 27, "IPV6_V6ONLY");
    CHECK(jet_xl_sockopt_l2d(1, 12, &dl, &dopt) == -1, "SO_PRIORITY unsupported");
    CHECK(jet_xl_mmap_flags_l2d(0x02 | 0x20) == (0x02 | 0x1000), "MAP_PRIVATE|ANON");
    CHECK(jet_xl_mmap_flags_l2d(0x10) == 0x10, "MAP_FIXED");
    CHECK(jet_xl_madvise_l2d(8) == 5 && jet_xl_madvise_l2d(4) == 4 && jet_xl_madvise_l2d(12) < 0, "madvise");
    CHECK(jet_xl_poll_l2d(0x1 | 0x4 | 0x200) == (0x1 | 0x4 | 0x100), "POLLWRBAND l2d %#x", jet_xl_poll_l2d(0x205));
    CHECK(jet_xl_poll_d2l(0x8 | 0x10 | 0x100) == (0x8 | 0x10 | 0x200), "poll d2l");
    CHECK(jet_xl_rlimit_res_l2d(7) == 8 && jet_xl_rlimit_res_l2d(6) == 7 && jet_xl_rlimit_res_l2d(8) == 6 && jet_xl_rlimit_res_l2d(9) == 5, "RLIMIT NOFILE/NPROC/MEMLOCK/AS");
    CHECK(jet_xl_rlimit_res_l2d(13) == -1, "RLIMIT_NICE unsupported");
    CHECK(jet_xl_rlim_l2d(~0ULL) == 0x7fffffffffffffffULL && jet_xl_rlim_d2l(0x7fffffffffffffffULL) == ~0ULL, "RLIM_INFINITY");
    CHECK(jet_xl_ioctl_l2d(0x541B) == 0x4004667FUL && jet_xl_ioctl_l2d(0x5421) == 0x8004667EUL, "FIONREAD/FIONBIO");
    CHECK(jet_xl_ioctl_l2d(0x5413) == 0x40087468UL && jet_xl_ioctl_l2d(0x1234) == (unsigned long)-1, "TIOCGWINSZ/unknown");
}

static void t_structs(void) {
    jet_darwin_stat ds; memset(&ds, 0, sizeof ds);
    ds.st_dev = 0x1000004; ds.st_mode = 0100644; ds.st_nlink = 3; ds.st_ino = 0x123456789ULL;
    ds.st_uid = 501; ds.st_gid = 20; ds.st_size = 0x10000000001LL; ds.st_blocks = 9; ds.st_blksize = 4096;
    ds.st_mtimespec.tv_sec = 1700000000; ds.st_mtimespec.tv_nsec = 7;
    uint8_t gbuf[160]; memset(gbuf, CAN, sizeof gbuf);
    jet_guest_stat gs; jet_xl_stat_d2l(&ds, &gs); memcpy(gbuf, &gs, sizeof gs);
    jet_guest_stat *g = (jet_guest_stat *)gbuf;
    CHECK(g->st_ino == 0x123456789ULL && g->st_mode == 0100644 && g->st_nlink == 3 && g->st_uid == 501, "stat ids");
    CHECK(g->st_size == 0x10000000001LL && g->st_blksize == 4096 && g->st_mtim.tv_nsec == 7, "stat size/time");
    CHECK(tail_ok(gbuf, 128, 160), "stat canary");

    jet_darwin_statfs df; memset(&df, 0, sizeof df);
    df.f_bsize = 4096; df.f_blocks = 1ULL << 33; df.f_bfree = 5; df.f_bavail = 4; df.f_files = 77; df.f_ffree = 66;
    df.f_fsid[0] = 0x11; df.f_fsid[1] = 0x22; df.f_flags = 0x1 | 0x8; strcpy(df.f_fstypename, "apfs");
    uint8_t fb[160]; memset(fb, CAN, sizeof fb);
    jet_guest_statfs gf; jet_xl_statfs_d2l(&df, &gf); memcpy(fb, &gf, sizeof gf);
    CHECK(gf.f_type == 0xEF53 && gf.f_blocks == (1ULL << 33) && gf.f_bsize == 4096 && gf.f_namelen == 255, "statfs");
    CHECK((gf.f_flags & 3) == 3 && tail_ok(fb, 120, 160), "statfs flags/canary");
    uint8_t vb[160]; memset(vb, CAN, sizeof vb);
    jet_guest_statvfs gv; jet_xl_statvfs_d2l(&df, &gv); memcpy(vb, &gv, sizeof gv);
    CHECK(gv.f_fsid == 0x2200000011ULL && gv.f_flag == 3 && gv.f_favail == 66 && gv.f_namemax == 255, "statvfs");
    CHECK(tail_ok(vb, 112, 160), "statvfs canary (112)");

    jet_guest_timeval tv = { 12, 345678 }; jet_darwin_timeval dtv; jet_xl_timeval_l2d(&tv, &dtv);
    CHECK(dtv.tv_sec == 12 && dtv.tv_usec == 345678 && dtv.__pad == 0, "timeval l2d");
    jet_guest_timeval tv2; jet_xl_timeval_d2l(&dtv, &tv2); CHECK(tv2.tv_usec == 345678, "timeval d2l");

    /* termios: raw-ish 8N1 115200, ICANON|ECHO|ISIG|IEXTEN|ECHOCTL|ECHOKE, VMIN=1 VTIME=0 */
    jet_guest_termios t; memset(&t, 0, sizeof t);
    t.c_iflag = 0x100 | 0x400 | 0x4000;           /* ICRNL IXON IUTF8 */
    t.c_oflag = 0x1 | 0x4;                        /* OPOST ONLCR */
    t.c_cflag = 0x30 | 0x80 | 0x1002;             /* CS8 CREAD B115200 */
    t.c_lflag = 0x1 | 0x2 | 0x8 | 0x8000 | 0x200 | 0x800;
    for (int i = 0; i < 17; i++) t.c_cc[i] = (uint8_t)(0x40 + i);
    t.c_cc[7] = 0;                                /* VSWTC: no Darwin slot */
    jet_darwin_termios d; jet_xl_termios_l2d(&t, &d);
    CHECK(d.c_iflag == (0x100 | 0x200 | 0x4000), "iflag %#llx", (unsigned long long)d.c_iflag);
    CHECK(d.c_oflag == (0x1 | 0x2), "oflag");
    CHECK(d.c_cflag == (0x300 | 0x800), "cflag %#llx", (unsigned long long)d.c_cflag);
    CHECK(d.c_lflag == (0x80 | 0x100 | 0x8 | 0x400 | 0x40 | 0x1), "lflag %#llx", (unsigned long long)d.c_lflag);
    CHECK(d.c_ispeed == 115200 && d.c_ospeed == 115200, "speed");
    CHECK(d.c_cc[8] == 0x40 && d.c_cc[0] == 0x44 && d.c_cc[16] == 0x46 && d.c_cc[17] == 0x45, "cc map");
    jet_guest_termios t2; jet_xl_termios_d2l(&d, &t2);
    CHECK(!memcmp(&t, &t2, sizeof t), "termios roundtrip");
}

static void t_sockaddr(void) {
    uint8_t g6[28] = {0}; uint16_t fam = 10; memcpy(g6, &fam, 2);
    g6[2] = 0x1f; g6[3] = 0x90; g6[4] = 0xaa; g6[8] = 0xfe; g6[9] = 0x80; g6[23] = 1; g6[24] = 3;
    uint8_t b[128]; memset(b, CAN, sizeof b);
    int n = jet_xl_sockaddr_l2bsd(g6, 28, b, sizeof b);
    CHECK(n == 28 && b[0] == 28 && b[1] == 30 && b[2] == 0x1f && b[4] == 0xaa && b[24] == 3, "in6 l2bsd");
    CHECK(tail_ok(b, 28, 128), "in6 l2bsd canary");
    uint8_t g[128]; memset(g, CAN, sizeof g);
    n = jet_xl_sockaddr_bsd2l(b, 28, g, sizeof g);
    CHECK(n == 28 && !memcmp(g, g6, 28) && tail_ok(g, 28, 128), "in6 bsd2l");
    memset(g, CAN, sizeof g);
    n = jet_xl_sockaddr_bsd2l(b, 28, g, 16);       /* truncated */
    CHECK(n == 28 && g[0] == 10 && g[1] == 0 && tail_ok(g, 16, 128), "in6 truncated reports full len");
    uint8_t g4[16] = {2, 0, 0, 80, 127, 0, 0, 1};
    memset(b, CAN, sizeof b);
    CHECK(jet_xl_sockaddr_l2bsd(g4, 16, b, sizeof b) == 16 && b[0] == 16 && b[1] == 2 && b[7] == 1 && tail_ok(b, 16, 128), "in4");
    uint8_t gu[110] = {1, 0}; strcpy((char *)gu + 2, "/tmp/jet.sock");
    memset(b, CAN, sizeof b);
    n = jet_xl_sockaddr_l2bsd(gu, 2 + 14, b, sizeof b);
    CHECK(n == 16 && b[1] == 1 && !strcmp((char *)b + 2, "/tmp/jet.sock") && tail_ok(b, 16, 128), "unix l2bsd n=%d", n);
    memset(g, CAN, sizeof g);
    n = jet_xl_sockaddr_bsd2l(b, (uint32_t)n, g, 110);
    CHECK(n == 16 && g[0] == 1 && g[1] == 0 && !strcmp((char *)g + 2, "/tmp/jet.sock"), "unix bsd2l");
    uint8_t ab[8] = {1, 0, 0, 'x', 'y'};
    CHECK(jet_xl_sockaddr_l2bsd(ab, 5, b, sizeof b) == -1, "abstract unix rejected");
    uint8_t lp[110] = {1, 0}; memset(lp + 2, 'a', 104);
    CHECK(jet_xl_sockaddr_l2bsd(lp, 110, b, sizeof b) == -1, "path > 103 rejected");
    CHECK(jet_xl_sockaddr_l2bsd(g6, 28, b, 20) == -1, "cap too small");
}

static void t_cmsg(void) {
    int p[2]; CHECK(pipe(p) == 0, "pipe");
    /* guest: SCM_RIGHTS(2 fds) + IP_TOS(1 int) */
    uint8_t gc[64]; memset(gc, 0, sizeof gc);
    jet_guest_cmsghdr h1 = { 16 + 8, 1, 1 }; memcpy(gc, &h1, 16); memcpy(gc + 16, p, 8);
    jet_guest_cmsghdr h2 = { 16 + 4, 0, 1 }; memcpy(gc + 24, &h2, 16); int tos = 0x10; memcpy(gc + 40, &tos, 4);
    uint8_t dc[64]; memset(dc, CAN, sizeof dc);
    long dn = jet_xl_cmsg_l2bsd(gc, 48, dc, sizeof dc);
    jet_darwin_cmsghdr d1, d2; memcpy(&d1, dc, 12); memcpy(&d2, dc + 20, 12);
    CHECK(dn == 20 + 16, "l2bsd len %ld", dn);
    CHECK(d1.cmsg_len == 20 && d1.cmsg_level == 0xffff && d1.cmsg_type == 1, "SCM_RIGHTS hdr");
    CHECK(d2.cmsg_len == 16 && d2.cmsg_level == 0 && d2.cmsg_type == 3, "IP_TOS hdr (4-aligned @20)");
    CHECK(tail_ok(dc, 36, 64), "l2bsd canary");
    uint8_t gb[64]; memset(gb, CAN, sizeof gb); int ct = -1;
    long gn = jet_xl_cmsg_bsd2l(dc, (size_t)dn, gb, sizeof gb, &ct);
    CHECK(gn == 48 && ct == 0 && !memcmp(gb, gc, 44), "bsd2l roundtrip (8-aligned) gn=%ld", gn);
    CHECK(tail_ok(gb, 48, 64), "bsd2l canary");
    /* truncation: only room for nothing -> fds closed, ctrunc */
    int q[2]; CHECK(pipe(q) == 0, "pipe2");
    uint8_t dr[20]; jet_darwin_cmsghdr dh = { 20, 0xffff, 1 }; memcpy(dr, &dh, 12); memcpy(dr + 12, q, 8);
    memset(gb, CAN, sizeof gb);
    gn = jet_xl_cmsg_bsd2l(dr, 20, gb, 16, &ct);
    CHECK(gn == 0 && ct == 1 && tail_ok(gb, 0, 64), "ctrunc");
    CHECK(fcntl(q[0], F_GETFD) == -1 && errno == EBADF && fcntl(q[1], F_GETFD) == -1, "undelivered fds closed");
    /* SCM_TIMESTAMP: Darwin timeval {i64,i32,pad} -> guest {i64,i64} */
    uint8_t dt[28]; jet_darwin_cmsghdr th = { 12 + 16, 0xffff, 2 }; memcpy(dt, &th, 12);
    jet_darwin_timeval dtv = { 99, 123456, 0x7777 }; memcpy(dt + 12, &dtv, 16);
    gn = jet_xl_cmsg_bsd2l(dt, 28, gb, sizeof gb, &ct);
    jet_guest_cmsghdr gh; memcpy(&gh, gb, 16); jet_guest_timeval gtv; memcpy(&gtv, gb + 16, 16);
    CHECK(gn == 32 && gh.cmsg_level == 1 && gh.cmsg_type == 29 && gtv.tv_sec == 99 && gtv.tv_usec == 123456, "SCM_TIMESTAMP");
    close(p[0]); close(p[1]);
}

static void t_eventfd(void) {
    int fd = jet_efd_create(3, 0x800 /*EFD_NONBLOCK*/);
    CHECK(fd >= 0 && jet_efd_is(fd), "create");
    uint64_t v = 5, r = 0;
    CHECK(jet_efd_write(fd, &v, 8) == 8, "write 5");
    v = 2; CHECK(jet_efd_write(fd, &v, 8) == 8, "write 2");
    CHECK(jet_efd_read(fd, &r, 8) == 8 && r == 10, "read sum=10 got %llu", (unsigned long long)r);
    errno = 0; CHECK(jet_efd_read(fd, &r, 8) == -1 && errno == EAGAIN, "empty -> EAGAIN");
    CHECK(jet_efd_read(fd, &r, 4) == -1 && errno == EINVAL, "short buf EINVAL");
    v = ~0ULL; CHECK(jet_efd_write(fd, &v, 8) == -1 && errno == EINVAL, "write ~0 EINVAL");
    CHECK(jet_efd_close(fd) == 1 && !jet_efd_is(fd), "close");
    int s = jet_efd_create(3, 0x800 | 1 /*EFD_SEMAPHORE*/);
    int got = 0; while (jet_efd_read(s, &r, 8) == 8) { CHECK(r == 1, "sem reads 1"); got++; }
    CHECK(got == 3, "sem count 3 got %d", got);
    jet_efd_close(s);
    CHECK(jet_efd_close(0) == 0, "non-efd close not handled");
}

/* ---- mock kqueue ---- */
typedef struct { uint64_t ident; int16_t filter; uint16_t flags; } mk_reg;
typedef struct { mk_reg r[32]; int n; jet_kevent q[16]; int nq; int calls, maxbatch; int badfd; } mockkq;
static int mk_find(mockkq *m, uint64_t id, int16_t f) { for (int i = 0; i < m->n; i++) if (m->r[i].ident == id && m->r[i].filter == f) return i; return -1; }
static int mock_kevent(void *u, int kq, const jet_kevent *chg, int nchg, jet_kevent *ev, int nev, int t) {
    (void)kq; (void)t; mockkq *m = u; m->calls++;
    if (nchg > m->maxbatch) m->maxbatch = nchg;
    for (int i = 0; i < nchg; i++) {
        int k = mk_find(m, chg[i].ident, chg[i].filter);
        if (chg[i].flags & JD_EV_DELETE) {
            if (k < 0) { errno = 2; return -1; }
            m->r[k] = m->r[--m->n];
        } else if (chg[i].flags & JD_EV_ADD) {
            if ((int)chg[i].ident == m->badfd) { errno = 9; return -1; }
            if (chg[i].udata != chg[i].ident) { errno = 22; return -1; }
            if (k < 0) k = m->n++;
            m->r[k].ident = chg[i].ident; m->r[k].filter = chg[i].filter; m->r[k].flags = chg[i].flags;
        }
    }
    if (!nev) return 0;
    int n = m->nq < nev ? m->nq : nev;
    memcpy(ev, m->q, (size_t)n * sizeof *ev);
    m->nq = 0;
    return n;
}
static void mk_push(mockkq *m, int fd, int16_t filt, uint16_t flags, uint32_t fflags) {
    jet_kevent *k = &m->q[m->nq++]; memset(k, 0, sizeof *k);
    k->ident = (uint64_t)fd; k->filter = filt; k->flags = flags; k->fflags = fflags; k->udata = (uint64_t)fd;
}
static void t_epoll_kq(void) {
    mockkq m; memset(&m, 0, sizeof m); m.badfd = 99;
    jet_ekq e; jet_ekq_init(&e, 500, mock_kevent, &m);
    jet_guest_epoll_event out[8];
    CHECK(jet_ekq_ctl(&e, 1, 7, JET_EPOLLIN | JET_EPOLLOUT, 0x7777) == 0, "ADD 7 in|out");
    CHECK(m.n == 2 && mk_find(&m, 7, JD_EVFILT_READ) >= 0 && mk_find(&m, 7, JD_EVFILT_WRITE) >= 0, "both filters");
    CHECK(m.maxbatch == 1, "one change per kevent call");
    CHECK(jet_ekq_ctl(&e, 1, 7, JET_EPOLLIN, 0) == -17, "ADD dup -> EEXIST");
    CHECK(jet_ekq_ctl(&e, 1, 99, JET_EPOLLIN, 0) == -9 && e.n == 1, "ADD bad fd -> EBADF, rolled back");
    CHECK(jet_ekq_ctl(&e, 3, 8, JET_EPOLLIN, 0) == -2 && jet_ekq_ctl(&e, 2, 8, 0, 0) == -2, "MOD/DEL missing ENOENT");
    CHECK(jet_ekq_ctl(&e, 1, 500, JET_EPOLLIN, 0) == -22, "self EINVAL");
    CHECK(jet_ekq_ctl(&e, 3, 7, JET_EPOLLIN | JET_EPOLLET, 0x8888) == 0, "MOD 7 -> in|et");
    int k = mk_find(&m, 7, JD_EVFILT_READ);
    CHECK(mk_find(&m, 7, JD_EVFILT_WRITE) < 0 && k >= 0 && (m.r[k].flags & JD_EV_CLEAR), "MOD deleted write filter, ET->EV_CLEAR");
    CHECK(jet_ekq_ctl(&e, 3, 7, JET_EPOLLIN | JET_EPOLLOUT, 0x7777) == 0, "MOD back in|out");
    /* merge read+write into one event */
    mk_push(&m, 7, JD_EVFILT_READ, 0, 0); mk_push(&m, 7, JD_EVFILT_WRITE, 0, 0);
    int n = jet_ekq_wait(&e, out, 8, 0);
    CHECK(n == 1 && out[0].events == (JET_EPOLLIN | JET_EPOLLOUT) && out[0].data.u64 == 0x7777 && out[0].__pad == 0, "merged n=%d ev=%#x", n, n > 0 ? out[0].events : 0);
    /* EOF on read with RDHUP, fflags -> ERR; EOF on write -> HUP */
    CHECK(jet_ekq_ctl(&e, 1, 9, JET_EPOLLIN | JET_EPOLLRDHUP | JET_EPOLLOUT, 9) == 0, "ADD 9");
    mk_push(&m, 9, JD_EVFILT_READ, JD_EV_EOF, 54); mk_push(&m, 9, JD_EVFILT_WRITE, JD_EV_EOF, 0);
    n = jet_ekq_wait(&e, out, 8, 0);
    CHECK(n == 1 && out[0].events == (JET_EPOLLIN | JET_EPOLLRDHUP | JET_EPOLLERR | JET_EPOLLOUT | JET_EPOLLHUP), "EOF mapping %#x", n ? out[0].events : 0);
    mk_push(&m, 9, JD_EVFILT_READ, JD_EV_ERROR, 0);
    n = jet_ekq_wait(&e, out, 8, 0); CHECK(n == 1 && out[0].events == JET_EPOLLERR, "EV_ERROR -> ERR");
    /* udata=fd survives DEL swap: delete 7 (index 0); 9 moves; event for 9 still maps */
    CHECK(jet_ekq_ctl(&e, 2, 7, 0, 0) == 0 && mk_find(&m, 7, JD_EVFILT_READ) < 0, "DEL 7 removed filters");
    mk_push(&m, 7, JD_EVFILT_READ, 0, 0); mk_push(&m, 9, JD_EVFILT_READ, 0, 0);
    n = jet_ekq_wait(&e, out, 8, 0);
    CHECK(n == 1 && out[0].data.u64 == 9, "stale 7 skipped, 9 delivered");
    /* ONESHOT: fires once, whole fd disarmed until MOD */
    CHECK(jet_ekq_ctl(&e, 1, 11, JET_EPOLLIN | JET_EPOLLOUT | JET_EPOLLONESHOT, 11) == 0, "ADD 11 oneshot");
    k = mk_find(&m, 11, JD_EVFILT_READ); CHECK(k >= 0 && (m.r[k].flags & JD_EV_ONESHOT), "EV_ONESHOT");
    m.r[k] = m.r[--m.n];                        /* kernel auto-drops fired oneshot read */
    mk_push(&m, 11, JD_EVFILT_READ, 0, 0);
    n = jet_ekq_wait(&e, out, 8, 0); CHECK(n == 1 && out[0].events == JET_EPOLLIN, "oneshot first");
    CHECK(mk_find(&m, 11, JD_EVFILT_WRITE) < 0, "oneshot disarmed write filter too");
    mk_push(&m, 11, JD_EVFILT_WRITE, 0, 0);
    n = jet_ekq_wait(&e, out, 8, 0); CHECK(n == 0, "oneshot: nothing until MOD");
    CHECK(jet_ekq_ctl(&e, 3, 11, JET_EPOLLIN | JET_EPOLLONESHOT, 11) == 0 && mk_find(&m, 11, JD_EVFILT_READ) >= 0, "MOD rearms");
    /* maxevents cap */
    mk_push(&m, 9, JD_EVFILT_READ, 0, 0); mk_push(&m, 11, JD_EVFILT_READ, 0, 0);
    n = jet_ekq_wait(&e, out, 1, 0); CHECK(n == 1, "maxevents=1");
    CHECK(jet_ekq_wait(&e, out, 0, 0) == -22, "maxevents 0 EINVAL");
    /* forget closed fd */
    jet_ekq_forget_fd(&e, 9);
    mk_push(&m, 9, JD_EVFILT_READ, 0, 0);
    CHECK(jet_ekq_wait(&e, out, 8, 0) == 0 && jet_ekq_ctl(&e, 2, 9, 0, 0) == -2, "forgotten fd");
    jet_ekq_destroy(&e);
    /* registry */
    mockkq m2; memset(&m2, 0, sizeof m2); m2.badfd = -5;
    jet_ekq *r = jet_ekq_registry_add(601, mock_kevent, &m2);
    CHECK(r && jet_ekq_registry_get(601) == r && !jet_ekq_registry_get(602), "registry get");
    CHECK(jet_ekq_ctl(r, 1, 3, JET_EPOLLIN, 3) == 0, "reg add");
    jet_ekq_registry_forget_fd(3);
    CHECK(jet_ekq_ctl(r, 2, 3, 0, 0) == -2, "registry forget_fd");
    CHECK(jet_ekq_registry_close(601) == 1 && jet_ekq_registry_close(601) == 0 && !jet_ekq_registry_get(601), "registry close");
}

static void t_guest(void) {
    jet_config cfg = {0};
    jet_ctx *ctx = jet_native_create(&cfg);
    char err[256];
    jet_lib *lib = ctx ? jet_native_load(ctx, "tests/guest_lib.so", err, sizeof err) : NULL;
    CHECK(lib, "load guest_lib");
    if (!lib) return;
    static const char *names[] = { "guest_sigaction_no_smash", "guest_sigset_no_smash",
                                   "guest_svc_rt_sigaction", "guest_statvfs_no_smash" };
    for (int i = 0; i < 4; i++) {
        uint64_t sym = jet_native_sym(lib, names[i]), ret = 77;
        CHECK(sym && jet_native_call(ctx, sym, NULL, &ret, NULL) == 0, "%s call: %s", names[i], jet_native_last_error(ctx));
        CHECK((int)ret == 0, "%s -> fail code %d", names[i], (int)ret);
    }
    static const int64_t want[] = { -38, -2, -9 };
    uint64_t sym = jet_native_sym(lib, "guest_svc_errno");
    for (int i = 0; i < 3; i++) {
        jet_args aa = {0}; aa.x[0] = (uint64_t)i; uint64_t ret = 0;
        CHECK(jet_native_call(ctx, sym, &aa, &ret, NULL) == 0, "svc_errno call");
        CHECK((int64_t)ret == want[i], "SVC %d returns Linux -errno: want %lld got %lld", i, (long long)want[i], (long long)(int64_t)ret);
    }
    jet_native_destroy(ctx);
}

int main(void) {
    t_sizes(); t_errno(); t_consts(); t_structs(); t_sockaddr(); t_cmsg(); t_eventfd(); t_epoll_kq(); t_guest();
    printf("test_v101_darwin_abi: %d ok, %d fail\n", oks, fails);
    printf(fails ? "FAILED\n" : "ALL PASS\n");
    return fails ? 1 : 0;
}
