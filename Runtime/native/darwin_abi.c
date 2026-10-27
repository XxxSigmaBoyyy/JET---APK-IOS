/* darwin_abi.c — portable guest(Linux aarch64) <-> Darwin arm64 translation.
 * Pure mapping code; unit-tested on Linux, static_assert-verified on __APPLE__. */
#include "darwin_abi.h"
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>

#if defined(__APPLE__)
#  include <sys/stat.h>
#  include <sys/mount.h>
#  include <sys/event.h>
#  include <sys/socket.h>
#  include <sys/mman.h>
#  include <sys/resource.h>
#  include <netinet/in.h>
#  include <netinet/tcp.h>
#  include <poll.h>
#  include <termios.h>
#  include <time.h>
/* Mirror layouts must match the SDK exactly. */
_Static_assert(sizeof(struct stat) == sizeof(jet_darwin_stat), "stat mirror");
_Static_assert(offsetof(struct stat, st_ino) == offsetof(jet_darwin_stat, st_ino), "st_ino");
_Static_assert(offsetof(struct stat, st_atimespec) == offsetof(jet_darwin_stat, st_atimespec), "st_atimespec");
_Static_assert(offsetof(struct stat, st_size) == offsetof(jet_darwin_stat, st_size), "st_size");
_Static_assert(offsetof(struct stat, st_blksize) == offsetof(jet_darwin_stat, st_blksize), "st_blksize");
_Static_assert(sizeof(struct statfs) == sizeof(jet_darwin_statfs), "statfs mirror");
_Static_assert(offsetof(struct statfs, f_blocks) == offsetof(jet_darwin_statfs, f_blocks), "f_blocks");
_Static_assert(offsetof(struct statfs, f_fstypename) == offsetof(jet_darwin_statfs, f_fstypename), "f_fstypename");
_Static_assert(sizeof(struct termios) == sizeof(jet_darwin_termios), "termios mirror");
_Static_assert(offsetof(struct termios, c_ispeed) == offsetof(jet_darwin_termios, c_ispeed), "c_ispeed");
_Static_assert(NCCS == 20, "darwin NCCS");
_Static_assert(sizeof(struct flock) == sizeof(jet_darwin_flock), "flock mirror");
_Static_assert(offsetof(struct flock, l_type) == offsetof(jet_darwin_flock, l_type), "l_type");
_Static_assert(sizeof(struct kevent) == sizeof(jet_kevent), "kevent mirror");
_Static_assert(offsetof(struct kevent, udata) == offsetof(jet_kevent, udata), "udata");
_Static_assert(sizeof(struct cmsghdr) == sizeof(jet_darwin_cmsghdr), "cmsghdr mirror");
_Static_assert(CMSG_LEN(0) == 12, "darwin CMSG_LEN");
_Static_assert(sizeof(struct timeval) == sizeof(jet_darwin_timeval), "timeval mirror");
_Static_assert(AF_INET6 == JD_AF_INET6 && SOL_SOCKET == JD_SOL_SOCKET, "AF/SOL");
_Static_assert(EVFILT_READ == JD_EVFILT_READ && EVFILT_WRITE == JD_EVFILT_WRITE, "EVFILT");
_Static_assert(EV_ADD == JD_EV_ADD && EV_DELETE == JD_EV_DELETE && EV_ONESHOT == JD_EV_ONESHOT &&
               EV_CLEAR == JD_EV_CLEAR && EV_EOF == JD_EV_EOF && EV_ERROR == JD_EV_ERROR &&
               EV_ENABLE == JD_EV_ENABLE && EV_DISABLE == JD_EV_DISABLE, "EV_*");
_Static_assert(O_NONBLOCK == 0x4 && O_APPEND == 0x8 && O_CREAT == 0x200 && O_TRUNC == 0x400 &&
               O_EXCL == 0x800 && O_NOFOLLOW == 0x100 && O_DIRECTORY == 0x100000 &&
               O_CLOEXEC == 0x1000000 && O_NOCTTY == 0x20000, "O_*");
_Static_assert(AT_FDCWD == JD_AT_FDCWD && AT_SYMLINK_NOFOLLOW == 0x20 && AT_REMOVEDIR == 0x80, "AT_*");
_Static_assert(F_GETLK == 7 && F_SETLK == 8 && F_SETLKW == 9 && F_DUPFD_CLOEXEC == 67, "F_*");
_Static_assert(F_RDLCK == 1 && F_UNLCK == 2 && F_WRLCK == 3, "lock types");
_Static_assert(MAP_ANON == 0x1000 && MAP_NORESERVE == 0x40, "MAP_*");
_Static_assert(MSG_DONTWAIT == 0x80 && MSG_WAITALL == 0x40 && MSG_TRUNC == 0x10 && MSG_CTRUNC == 0x20, "MSG_*");
_Static_assert(SO_REUSEADDR == 4 && SO_KEEPALIVE == 8 && SO_SNDBUF == 0x1001 && SO_RCVBUF == 0x1002 &&
               SO_RCVTIMEO == 0x1006 && SO_SNDTIMEO == 0x1005 && SO_ERROR == 0x1007 &&
               SO_NOSIGPIPE == JD_SO_NOSIGPIPE && SO_REUSEPORT == 0x200, "SO_*");
_Static_assert(TCP_KEEPALIVE == 0x10 && TCP_KEEPINTVL == 0x101 && TCP_KEEPCNT == 0x102, "TCP_*");
_Static_assert(IPV6_V6ONLY == 27, "IPV6_V6ONLY");
_Static_assert(RLIMIT_NOFILE == 8 && RLIMIT_NPROC == 7 && RLIMIT_MEMLOCK == 6 && RLIMIT_AS == 5, "RLIMIT_*");
_Static_assert(CLOCK_MONOTONIC == 6 && CLOCK_UPTIME_RAW == 8 && CLOCK_MONOTONIC_RAW == 4, "CLOCK_*");
_Static_assert(POLLWRNORM == 0x4 && POLLWRBAND == 0x100, "POLL*");
_Static_assert(EAGAIN == 35 && EINPROGRESS == 36 && ECONNREFUSED == 61 && ETIMEDOUT == 60, "errno");
_Static_assert(FIONREAD == 0x4004667f && FIONBIO == 0x8004667e, "FIO*");
#endif

/* ---------------- helpers ---------------- */
typedef struct { uint32_t l, d; } bitmap_t;
static uint64_t map_bits(uint64_t v, const bitmap_t *t, size_t n, int l2d) {
    uint64_t r = 0;
    for (size_t i = 0; i < n; i++) {
        uint32_t from = l2d ? t[i].l : t[i].d, to = l2d ? t[i].d : t[i].l;
        if (from && (v & from) == from) r |= to;
    }
    return r;
}
#define N(a) (sizeof(a)/sizeof((a)[0]))

/* ---------------- errno ---------------- */
int jet_xl_errno_d2l(int e) {
    if (e <= 0) return e;
    if (e == 11) return 35;                 /* EDEADLK */
    if (e <= 34) return e;                  /* 1..34 identical except 11 */
    static const short t[] = {
        /*35*/11,115,114,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,
        /*55*/105,106,107,108,109,110,111,40,36,112,113,39,11,87,122,116,66,22,22,22,
        /*75*/22,22,37,38,22,22,22,22,5,75,8,8,8,8,125,43,42,84,61,74,
        /*95*/72,61,67,63,60,71,62,95,22,131,130,105 };
    if (e >= 35 && e < 35 + (int)N(t)) return t[e - 35];
    return 22;
}

int jet_xl_errno_l2d(int le) {
    if (le <= 0) return le;
    for (int d = 1; d <= 106; d++) if (jet_xl_errno_d2l(d) == le) return d;
    return 22;
}

/* ---------------- sockets ---------------- */
int jet_xl_af_l2d(int af) {
    switch (af) { case 0: case 1: case 2: return af; case 10: return JD_AF_INET6; default: return -1; }
}
int jet_xl_af_d2l(int af) {
    switch (af) { case JD_AF_INET6: return 10; case 18: return 17; default: return af; }
}
int jet_xl_socktype_l2d(int t, int *nb, int *cx) {
    if (nb) *nb = !!(t & 0x800);
    if (cx) *cx = !!(t & 0x80000);
    return t & ~(0x800 | 0x80000);
}
static const bitmap_t k_msg[] = {
    {0x1,0x1},{0x2,0x2},{0x4,0x4},{0x8,0x20},{0x20,0x10},{0x40,0x80},{0x80,0x8},{0x100,0x40} };
int jet_xl_msgflags_l2d(int f) { return (int)map_bits((uint32_t)f, k_msg, N(k_msg), 1); }
int jet_xl_msgflags_d2l(int f) { return (int)map_bits((uint32_t)f, k_msg, N(k_msg), 0); }

int jet_xl_sockopt_l2d(int level, int opt, int *dl, int *dopt) {
    *dl = level; *dopt = opt;
    if (level == 1) { /* SOL_SOCKET */
        *dl = JD_SOL_SOCKET;
        switch (opt) {
        case 1: *dopt = 1; return 0;        case 2: *dopt = 4; return 0;
        case 3: *dopt = 0x1008; return 0;   case 4: *dopt = 0x1007; return 2;
        case 5: *dopt = 0x10; return 0;     case 6: *dopt = 0x20; return 0;
        case 7: *dopt = 0x1001; return 0;   case 8: *dopt = 0x1002; return 0;
        case 9: *dopt = 8; return 0;        case 10: *dopt = 0x100; return 0;
        case 13: *dopt = 0x1080; return 0;  /* SO_LINGER_SEC: seconds like Linux */
        case 15: *dopt = 0x200; return 0;   case 18: *dopt = 0x1004; return 0;
        case 19: *dopt = 0x1003; return 0;  case 20: *dopt = 0x1006; return 1;
        case 21: *dopt = 0x1005; return 1;  case 29: *dopt = 0x400; return 0;
        case 30: *dopt = 2; return 0;
        default: return -1;
        }
    }
    if (level == 6) { /* IPPROTO_TCP */
        switch (opt) {
        case 1: case 2: return 0;
        case 3: *dopt = 4; return 0;        /* TCP_CORK -> TCP_NOPUSH */
        case 4: *dopt = 0x10; return 0;     case 5: *dopt = 0x101; return 0;
        case 6: *dopt = 0x102; return 0;    case 23: *dopt = 0x105; return 0;
        default: return -1;
        }
    }
    if (level == 0) { /* IPPROTO_IP */
        switch (opt) {
        case 1: *dopt = 3; return 0;  case 2: *dopt = 4; return 0;  case 3: *dopt = 2; return 0;
        case 8: *dopt = 26; return 0; case 32: *dopt = 9; return 0; case 33: *dopt = 10; return 0;
        case 34: *dopt = 11; return 0; case 35: *dopt = 12; return 0; case 36: *dopt = 13; return 0;
        default: return -1;
        }
    }
    if (level == 41) { /* IPPROTO_IPV6 */
        switch (opt) {
        case 16: *dopt = 4; return 0;  case 17: *dopt = 9; return 0;  case 18: *dopt = 10; return 0;
        case 19: *dopt = 11; return 0; case 20: *dopt = 12; return 0; case 21: *dopt = 13; return 0;
        case 26: *dopt = 27; return 0; case 49: *dopt = 61; return 0; case 50: *dopt = 46; return 0;
        case 67: *dopt = 36; return 0;
        default: return -1;
        }
    }
    return 0;
}

/* ---------------- files ---------------- */
static const bitmap_t k_oflags[] = {
    {0x40,0x200},{0x80,0x800},{0x100,0x20000},{0x200,0x400},{0x400,0x8},{0x800,0x4},
    {0x1000,0x400000},{0x2000,0x40},{0x4000,0x100000},{0x8000,0x100},{0x80000,0x1000000},
    {0x101000,0x80} };
int jet_xl_oflags_l2d(int f) {
    if (f & 0x400000) return -1;                      /* O_TMPFILE: unsupported */
    return (f & 3) | (int)map_bits((uint32_t)f, k_oflags, N(k_oflags), 1);
}
int jet_xl_oflags_d2l(int f) {
    return (f & 3) | (int)map_bits((uint32_t)f, k_oflags, N(k_oflags), 0);
}
int jet_xl_atfd_l2d(int fd) { return fd == -100 ? JD_AT_FDCWD : fd; }
int jet_xl_atflags_l2d(int f, int for_access) {
    int r = 0;
    if (f & 0x100) r |= 0x20;
    if (f & 0x200) r |= for_access ? 0x10 : 0x80;
    if (f & 0x400) r |= 0x40;
    return r;                                          /* AT_EMPTY_PATH dropped */
}
int jet_xl_fcntl_cmd_l2d(int c) {
    switch (c) {
    case 0: case 1: case 2: case 3: case 4: return c;
    case 5: case 36: return 7;
    case 6: case 37: return 8;
    case 7: case 38: return 9;
    case 8: return 6;
    case 9: return 5;
    case 1030: return 67;
    default: return -1;
    }
}
int jet_xl_flock_l2d(const jet_guest_flock *g, jet_darwin_flock *d) {
    static const int16_t t[3] = { 1, 3, 2 };          /* RD, WR, UN */
    if (g->l_type < 0 || g->l_type > 2) return -1;
    memset(d, 0, sizeof *d);
    d->l_type = t[g->l_type]; d->l_whence = g->l_whence;
    d->l_start = g->l_start; d->l_len = g->l_len; d->l_pid = g->l_pid;
    return 0;
}
void jet_xl_flock_d2l(const jet_darwin_flock *d, jet_guest_flock *g) {
    memset(g, 0, sizeof *g);
    g->l_type = d->l_type == 1 ? 0 : d->l_type == 3 ? 1 : 2;
    g->l_whence = d->l_whence; g->l_start = d->l_start; g->l_len = d->l_len; g->l_pid = d->l_pid;
}

/* ---------------- time / memory / limits ---------------- */
int jet_xl_clock_l2d(int c) {
    switch (c) {
    case 0: case 5: case 8: case 11: return 0;        /* REALTIME(+COARSE/ALARM/TAI) */
    case 1: case 6: return 8;                         /* MONOTONIC -> UPTIME_RAW (no suspend) */
    case 7: case 9: return 6;                         /* BOOTTIME -> MONOTONIC (counts sleep) */
    case 2: return 12;
    case 3: return 16;
    case 4: return 4;
    default: return -1;
    }
}
int jet_xl_mmap_flags_l2d(int f) {
    int r = f & 0x13;                                 /* SHARED|PRIVATE|FIXED */
    if ((f & 3) == 3) r = (r & ~3) | 1;               /* MAP_SHARED_VALIDATE */
    if (f & 0x20) r |= 0x1000;
    if (f & 0x4000) r |= 0x40;
    return r;                                          /* drop EXECUTABLE/POPULATE/STACK/... */
}
int jet_xl_madvise_l2d(int a) { return a <= 4 ? a : a == 8 ? 5 : -1; }
static const bitmap_t k_poll[] = {
    {1,1},{2,2},{4,4},{8,8},{0x10,0x10},{0x20,0x20},{0x40,0x40},{0x80,0x80},{0x100,0x4},{0x200,0x100} };
int16_t jet_xl_poll_l2d(int16_t e) { return (int16_t)map_bits((uint16_t)e, k_poll, N(k_poll), 1); }
int16_t jet_xl_poll_d2l(int16_t e) {
    uint64_t r = 0; uint16_t v = (uint16_t)e;
    for (size_t i = 0; i < N(k_poll); i++) if (k_poll[i].d != 4 || k_poll[i].l == 4)
        if (v & k_poll[i].d) r |= k_poll[i].l;      /* POLLOUT never reported as WRNORM */
    return (int16_t)r;
}
int jet_xl_rlimit_res_l2d(int r) {
    switch (r) {
    case 0: case 1: case 2: case 3: case 4: case 5: return r;
    case 6: return 7; case 7: return 8; case 8: return 6; case 9: return 5;
    default: return -1;
    }
}
#define JD_RLIM_INF 0x7fffffffffffffffULL
uint64_t jet_xl_rlim_l2d(uint64_t v) { return v == ~0ULL || v > JD_RLIM_INF ? JD_RLIM_INF : v; }
uint64_t jet_xl_rlim_d2l(uint64_t v) { return v >= JD_RLIM_INF ? ~0ULL : v; }
unsigned long jet_xl_ioctl_l2d(unsigned long q) {
    switch (q) {
    case 0x541B: return 0x4004667fUL;   /* FIONREAD */
    case 0x5421: return 0x8004667eUL;   /* FIONBIO */
    case 0x5452: return 0x8004667dUL;   /* FIOASYNC */
    case 0x5451: return 0x20006601UL;   /* FIOCLEX */
    case 0x5450: return 0x20006602UL;   /* FIONCLEX */
    case 0x5413: return 0x40087468UL;   /* TIOCGWINSZ */
    case 0x5414: return 0x80087467UL;   /* TIOCSWINSZ */
    default: return (unsigned long)-1;
    }
}

/* ---------------- structs ---------------- */
void jet_xl_stat_d2l(const jet_darwin_stat *d, jet_guest_stat *g) {
    memset(g, 0, sizeof *g);
    g->st_dev = (uint64_t)(uint32_t)d->st_dev; g->st_ino = d->st_ino;
    g->st_mode = d->st_mode; g->st_nlink = d->st_nlink;
    g->st_uid = d->st_uid; g->st_gid = d->st_gid; g->st_rdev = (uint64_t)(uint32_t)d->st_rdev;
    g->st_size = d->st_size; g->st_blksize = d->st_blksize; g->st_blocks = d->st_blocks;
    g->st_atim = d->st_atimespec; g->st_mtim = d->st_mtimespec; g->st_ctim = d->st_ctimespec;
}
static uint64_t mnt_to_st_flags(uint32_t m) {
    uint64_t f = 0;
    if (m & 0x1) f |= 1;          /* MNT_RDONLY -> ST_RDONLY */
    if (m & 0x8) f |= 2;          /* MNT_NOSUID -> ST_NOSUID */
    if (m & 0x10) f |= 4;         /* MNT_NODEV */
    if (m & 0x4) f |= 8;          /* MNT_NOEXEC */
    if (m & 0x2) f |= 16;         /* MNT_SYNCHRONOUS */
    if (m & 0x10000000) f |= 1024;/* MNT_NOATIME */
    return f;
}
void jet_xl_statfs_d2l(const jet_darwin_statfs *d, jet_guest_statfs *g) {
    memset(g, 0, sizeof *g);
    char tn[17]; memcpy(tn, d->f_fstypename, 16); tn[16] = 0;
    g->f_type = !strcmp(tn, "msdos") ? 0x4d44 : !strcmp(tn, "devfs") ? 0x1373 :
                !strcmp(tn, "nfs") ? 0x6969 : 0xEF53;
    g->f_bsize = d->f_bsize; g->f_frsize = d->f_bsize;
    g->f_blocks = d->f_blocks; g->f_bfree = d->f_bfree; g->f_bavail = d->f_bavail;
    g->f_files = d->f_files; g->f_ffree = d->f_ffree;
    g->f_fsid[0] = d->f_fsid[0]; g->f_fsid[1] = d->f_fsid[1];
    g->f_namelen = 255;
    g->f_flags = mnt_to_st_flags(d->f_flags) | 0x20; /* ST_VALID */
}
void jet_xl_statvfs_d2l(const jet_darwin_statfs *d, jet_guest_statvfs *g) {
    memset(g, 0, sizeof *g);
    g->f_bsize = d->f_bsize; g->f_frsize = d->f_bsize;
    g->f_blocks = d->f_blocks; g->f_bfree = d->f_bfree; g->f_bavail = d->f_bavail;
    g->f_files = d->f_files; g->f_ffree = d->f_ffree; g->f_favail = d->f_ffree;
    g->f_fsid = (uint64_t)(uint32_t)d->f_fsid[0] | ((uint64_t)(uint32_t)d->f_fsid[1] << 32);
    g->f_flag = mnt_to_st_flags(d->f_flags);
    g->f_namemax = 255;
}
void jet_xl_timeval_d2l(const jet_darwin_timeval *d, jet_guest_timeval *g) {
    g->tv_sec = d->tv_sec; g->tv_usec = d->tv_usec;
}
void jet_xl_timeval_l2d(const jet_guest_timeval *g, jet_darwin_timeval *d) {
    memset(d, 0, sizeof *d); d->tv_sec = g->tv_sec; d->tv_usec = (int32_t)g->tv_usec;
}

/* termios */
static const bitmap_t k_iflag[] = { {1,1},{2,2},{4,4},{8,8},{0x10,0x10},{0x20,0x20},{0x40,0x40},
    {0x80,0x80},{0x100,0x100},{0x400,0x200},{0x800,0x800},{0x1000,0x400},{0x2000,0x2000},{0x4000,0x4000} };
static const bitmap_t k_oflag[] = { {1,1},{4,2},{8,0x10},{0x10,0x20},{0x20,0x40} };
static const bitmap_t k_cflag[] = { {0x10,0x100},{0x20,0x200},{0x40,0x400},{0x80,0x800},{0x100,0x1000},
    {0x200,0x2000},{0x400,0x4000},{0x800,0x8000},{0x80000000u,0x30000} };
static const bitmap_t k_lflag[] = { {1,0x80},{2,0x100},{8,8},{0x10,2},{0x20,4},{0x40,0x10},
    {0x80,0x80000000u},{0x100,0x400000},{0x200,0x40},{0x400,0x20},{0x800,1},{0x1000,0x800000},
    {0x4000,0x20000000},{0x8000,0x400} };
static const signed char k_cc[17] = { 8,9,3,5,0,17,16,-1,12,13,10,1,6,15,4,14,2 };
static const struct { uint32_t code, baud; } k_baud[] = {
    {0,0},{1,50},{2,75},{3,110},{4,134},{5,150},{6,200},{7,300},{8,600},{9,1200},{0xa,1800},
    {0xb,2400},{0xc,4800},{0xd,9600},{0xe,19200},{0xf,38400},{0x1001,57600},{0x1002,115200},
    {0x1003,230400} };
void jet_xl_termios_l2d(const jet_guest_termios *g, jet_darwin_termios *d) {
    memset(d, 0, sizeof *d);
    d->c_iflag = map_bits(g->c_iflag, k_iflag, N(k_iflag), 1);
    d->c_oflag = map_bits(g->c_oflag, k_oflag, N(k_oflag), 1);
    d->c_cflag = map_bits(g->c_cflag, k_cflag, N(k_cflag), 1);
    d->c_lflag = map_bits(g->c_lflag, k_lflag, N(k_lflag), 1);
    for (int i = 0; i < 17; i++) if (k_cc[i] >= 0) d->c_cc[k_cc[i]] = g->c_cc[i];
    uint32_t code = g->c_cflag & 0x100f;
    for (size_t i = 0; i < N(k_baud); i++) if (k_baud[i].code == code)
        d->c_ispeed = d->c_ospeed = k_baud[i].baud;
}
void jet_xl_termios_d2l(const jet_darwin_termios *d, jet_guest_termios *g) {
    memset(g, 0, sizeof *g);
    g->c_iflag = (uint32_t)map_bits(d->c_iflag, k_iflag, N(k_iflag), 0);
    g->c_oflag = (uint32_t)map_bits(d->c_oflag, k_oflag, N(k_oflag), 0);
    g->c_cflag = (uint32_t)map_bits(d->c_cflag & ~0x300ULL, k_cflag, N(k_cflag), 0)
               | (uint32_t)((d->c_cflag & 0x300) >> 4);                 /* CSIZE */
    g->c_lflag = (uint32_t)map_bits(d->c_lflag, k_lflag, N(k_lflag), 0);
    for (int i = 0; i < 17; i++) if (k_cc[i] >= 0) g->c_cc[i] = d->c_cc[k_cc[i]];
    for (size_t i = 0; i < N(k_baud); i++) if (k_baud[i].baud == d->c_ospeed) {
        g->c_cflag |= k_baud[i].code; break;
    }
}

/* ---------------- sockaddr ---------------- */
int jet_xl_sockaddr_l2bsd(const void *gv, uint32_t glen, void *out, size_t cap) {
    const uint8_t *g = (const uint8_t *)gv; uint8_t *o = (uint8_t *)out;
    if (!g || !o || glen < 2) return -1;
    uint16_t fam; memcpy(&fam, g, 2);
    uint8_t tmp[128]; memset(tmp, 0, sizeof tmp);
    size_t n;
    switch (fam) {
    case 0:  n = 16; break;
    case 2:  if (glen < 16) return -1; n = 16; memcpy(tmp + 2, g + 2, 14); break;
    case 10: if (glen < 24) return -1; n = 28; memcpy(tmp + 2, g + 2, (glen < 28 ? glen : 28) - 2);
             tmp[1] = JD_AF_INET6; break;
    case 1: {
        size_t plen = glen > 110 ? 108 : glen - 2;
        if (plen && g[2] == 0) return -1;                         /* abstract ns: no Darwin equivalent */
        size_t sl = strnlen((const char *)g + 2, plen);
        if (sl > 103) return -1;                                  /* Darwin sun_path[104] */
        memcpy(tmp + 2, g + 2, sl);
        n = 2 + sl + 1; break;
    }
    default: return -1;
    }
    if (fam != 10) tmp[1] = (uint8_t)fam;
    tmp[0] = (uint8_t)n;
    if (n > cap) return -1;
    memcpy(o, tmp, n);
    return (int)n;
}
int jet_xl_sockaddr_bsd2l(const void *bv, uint32_t blen, void *gv, size_t gcap) {
    const uint8_t *b = (const uint8_t *)bv;
    if (!b || !gv || blen < 2) return -1;
    uint8_t tmp[128]; memset(tmp, 0, sizeof tmp);
    uint16_t fam = (uint16_t)jet_xl_af_d2l(b[1]);
    size_t n;
    switch (b[1]) {
    case 2:  n = 16; memcpy(tmp + 2, b + 2, (blen < 16 ? blen : 16) - 2); break;
    case JD_AF_INET6: n = 28; memcpy(tmp + 2, b + 2, (blen < 28 ? blen : 28) - 2); break;
    case 1: {
        size_t plen = blen > 106 ? 104 : blen - 2;
        size_t sl = strnlen((const char *)b + 2, plen);
        memcpy(tmp + 2, b + 2, sl);
        n = sl ? 2 + sl + 1 : 2; break;
    }
    default: n = blen > sizeof tmp ? sizeof tmp : blen; memcpy(tmp + 2, b + 2, n - 2); break;
    }
    memcpy(tmp, &fam, 2);
    memcpy(gv, tmp, n < gcap ? n : gcap);
    return (int)n;                      /* Linux: report full size even if truncated */
}

/* ---------------- cmsg ---------------- */
static int cmsg_type_l2d(int lvl, int t) {
    if (lvl == 1) return t == 1 ? 1 : t == 29 ? 2 : -1;          /* SCM_RIGHTS, SCM_TIMESTAMP */
    if (lvl == 0) return t == 8 ? 26 : t == 1 ? 3 : t == 2 ? 4 : -1;
    if (lvl == 41) return t == 50 ? 46 : t == 52 ? 47 : t == 67 ? 36 : -1;
    return t;
}
static int cmsg_type_d2l(int dlvl, int t) {
    if (dlvl == JD_SOL_SOCKET) return t == 1 ? 1 : t == 2 ? 29 : -1;
    if (dlvl == 0) return t == 26 ? 8 : t == 3 ? 1 : t == 4 ? 2 : t == 7 ? 8 : -1;
    if (dlvl == 41) return t == 46 ? 50 : t == 47 ? 52 : t == 36 ? 67 : -1;
    return t;
}
long jet_xl_cmsg_l2bsd(const void *gv, size_t glen, void *outv, size_t cap) {
    const uint8_t *g = (const uint8_t *)gv; uint8_t *o = (uint8_t *)outv;
    size_t off = 0, dout = 0;
    while (off + sizeof(jet_guest_cmsghdr) <= glen) {
        jet_guest_cmsghdr h; memcpy(&h, g + off, sizeof h);
        if (h.cmsg_len < sizeof h || off + h.cmsg_len > glen) return -1;
        size_t dlen = (size_t)h.cmsg_len - sizeof h;
        int dt = cmsg_type_l2d(h.cmsg_level, h.cmsg_type);
        if (dt < 0) return -1;
        size_t need = sizeof(jet_darwin_cmsghdr) + JET_DARWIN_CMSG_ALIGN(dlen);
        if (dout + need > cap) return -1;
        jet_darwin_cmsghdr dh = { (uint32_t)(sizeof dh + dlen),
                                  h.cmsg_level == 1 ? JD_SOL_SOCKET : h.cmsg_level, dt };
        memset(o + dout, 0, need);
        memcpy(o + dout, &dh, sizeof dh);
        memcpy(o + dout + sizeof dh, g + off + sizeof h, dlen);
        dout += need;
        off += JET_GUEST_CMSG_ALIGN((size_t)h.cmsg_len);
    }
    return (long)dout;
}
long jet_xl_cmsg_bsd2l(const void *bv, size_t blen, void *gv, size_t gcap, int *ctrunc) {
    const uint8_t *b = (const uint8_t *)bv; uint8_t *g = (uint8_t *)gv;
    size_t off = 0, gout = 0;
    if (ctrunc) *ctrunc = 0;
    while (off + sizeof(jet_darwin_cmsghdr) <= blen) {
        jet_darwin_cmsghdr dh; memcpy(&dh, b + off, sizeof dh);
        if (dh.cmsg_len < sizeof dh || off + dh.cmsg_len > blen) break;
        size_t dlen = dh.cmsg_len - sizeof dh;
        const uint8_t *data = b + off + sizeof dh;
        int lt = cmsg_type_d2l(dh.cmsg_level, dh.cmsg_type);
        int is_rights = dh.cmsg_level == JD_SOL_SOCKET && dh.cmsg_type == 1;
        uint8_t conv[16]; size_t clen = dlen;
        if (dh.cmsg_level == JD_SOL_SOCKET && dh.cmsg_type == 2 && dlen >= 12) { /* timeval */
            jet_darwin_timeval dt; memcpy(&dt, data, dlen < 16 ? dlen : 16);
            jet_guest_timeval gt; jet_xl_timeval_d2l(&dt, &gt);
            memcpy(conv, &gt, 16); data = conv; clen = 16;
        }
        size_t need = sizeof(jet_guest_cmsghdr) + clen;
        if (lt < 0 || gout + need > gcap) {
            if (lt >= 0 && ctrunc) *ctrunc = 1;
            if (is_rights) {                                   /* don't leak undelivered fds */
                for (size_t i = 0; i + 4 <= dlen; i += 4) { int fd; memcpy(&fd, data + i, 4); close(fd); }
            }
            off += JET_DARWIN_CMSG_ALIGN(dh.cmsg_len);
            continue;
        }
        jet_guest_cmsghdr h = { need, dh.cmsg_level == JD_SOL_SOCKET ? 1 : dh.cmsg_level, lt };
        memcpy(g + gout, &h, sizeof h);
        memcpy(g + gout + sizeof h, data, clen);
        size_t sp = JET_GUEST_CMSG_ALIGN(need);
        if (gout + sp > gcap) sp = gcap - gout;
        memset(g + gout + need, 0, sp - need);
        gout += sp;
        off += JET_DARWIN_CMSG_ALIGN(dh.cmsg_len);
    }
    return (long)gout;
}

/* ---------------- eventfd on a pipe pair ---------------- */
#define JET_EFD_MAX 256
static struct { int rfd, wfd, sem; } g_efd[JET_EFD_MAX];
static int g_efd_n;
static pthread_mutex_t g_efd_mu = PTHREAD_MUTEX_INITIALIZER;

static int efd_find_locked(int fd) {
    for (int i = 0; i < g_efd_n; i++) if (g_efd[i].rfd == fd) return i;
    return -1;
}
int jet_efd_create(unsigned initval, int lf) {
    int p[2];
    if (pipe(p) != 0) return -1;
    fcntl(p[1], F_SETFL, fcntl(p[1], F_GETFL) | O_NONBLOCK);
    if (lf & 0x800) fcntl(p[0], F_SETFL, fcntl(p[0], F_GETFL) | O_NONBLOCK);
    if (lf & 0x80000) { fcntl(p[0], F_SETFD, FD_CLOEXEC); fcntl(p[1], F_SETFD, FD_CLOEXEC); }
    else fcntl(p[1], F_SETFD, FD_CLOEXEC);           /* hidden end never inherited */
    pthread_mutex_lock(&g_efd_mu);
    if (g_efd_n >= JET_EFD_MAX) {
        pthread_mutex_unlock(&g_efd_mu); close(p[0]); close(p[1]); errno = EMFILE; return -1;
    }
    g_efd[g_efd_n].rfd = p[0]; g_efd[g_efd_n].wfd = p[1]; g_efd[g_efd_n].sem = lf & 1;
    g_efd_n++;
    pthread_mutex_unlock(&g_efd_mu);
    if (initval) { uint64_t v = initval; (void)!write(p[1], &v, 8); }
    return p[0];
}
static int efd_get(int fd, int *wfd, int *sem) {
    pthread_mutex_lock(&g_efd_mu);
    int i = efd_find_locked(fd);
    if (i >= 0) { if (wfd) *wfd = g_efd[i].wfd; if (sem) *sem = g_efd[i].sem; }
    pthread_mutex_unlock(&g_efd_mu);
    return i >= 0;
}
int jet_efd_is(int fd) { return efd_get(fd, NULL, NULL); }
long jet_efd_write(int fd, const void *buf, size_t n) {
    int wfd;
    if (!efd_get(fd, &wfd, NULL)) { errno = EBADF; return -1; }
    if (n < 8) { errno = EINVAL; return -1; }
    uint64_t v; memcpy(&v, buf, 8);
    if (v == ~0ULL) { errno = EINVAL; return -1; }
    if (v == 0) return 8;
    if (write(wfd, &v, 8) != 8 && errno != EAGAIN) return -1;  /* full pipe: already signalled */
    return 8;
}
long jet_efd_read(int fd, void *buf, size_t n) {
    int wfd, sem;
    if (!efd_get(fd, &wfd, &sem)) { errno = EBADF; return -1; }
    if (n < 8) { errno = EINVAL; return -1; }
    uint64_t v = 0, sum;
    ssize_t r = read(fd, &v, 8);
    if (r != 8) { if (r >= 0) errno = EIO; return -1; }
    if (sem) {
        if (v > 1) { uint64_t rest = v - 1; (void)!write(wfd, &rest, 8); }
        sum = 1;
    } else {
        sum = v;
        int avail = 0;
        while (ioctl(fd, FIONREAD, &avail) == 0 && avail >= 8) {
            if (read(fd, &v, 8) != 8) break;
            sum += v;
        }
    }
    memcpy(buf, &sum, 8);
    return 8;
}
int jet_efd_close(int fd) {
    pthread_mutex_lock(&g_efd_mu);
    int i = efd_find_locked(fd);
    if (i < 0) { pthread_mutex_unlock(&g_efd_mu); return 0; }
    int wfd = g_efd[i].wfd;
    g_efd[i] = g_efd[--g_efd_n];
    pthread_mutex_unlock(&g_efd_mu);
    close(wfd); close(fd);
    return 1;
}
