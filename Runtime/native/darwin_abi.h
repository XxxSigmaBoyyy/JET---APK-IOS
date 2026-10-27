/* darwin_abi.h — guest (Linux/bionic aarch64) <-> Darwin (iOS/macOS arm64) ABI translation.
 *
 * Everything here is PURE byte/number mapping with Darwin layouts described by
 * mirror structs and hard-coded Darwin constants. It compiles and is unit-tested
 * on any host (Linux CI); on __APPLE__ the mirror layouts and constants are
 * additionally static_assert'ed against the real SDK headers (darwin_abi.c).
 *
 * Direction naming: l2d = guest Linux value -> Darwin host; d2l = Darwin -> guest.
 */
#ifndef JET_DARWIN_ABI_H
#define JET_DARWIN_ABI_H
#include <stdint.h>
#include <stddef.h>
#include "guest_abi.h"

/* ---------------- Darwin mirror layouts (arm64, 64-bit inode) ---------------- */
typedef struct { int64_t tv_sec; int32_t tv_usec; int32_t __pad; } jet_darwin_timeval; /* 16 */
typedef struct {                       /* struct stat (__DARWIN_64_BIT_INO_T) */
    int32_t  st_dev;      /* 0 */
    uint16_t st_mode;     /* 4 */
    uint16_t st_nlink;    /* 6 */
    uint64_t st_ino;      /* 8 */
    uint32_t st_uid;      /* 16 */
    uint32_t st_gid;      /* 20 */
    int32_t  st_rdev;     /* 24 */
    int32_t  __pad0;      /* 28 */
    jet_guest_timespec st_atimespec;     /* 32 */
    jet_guest_timespec st_mtimespec;     /* 48 */
    jet_guest_timespec st_ctimespec;     /* 64 */
    jet_guest_timespec st_birthtimespec; /* 80 */
    int64_t  st_size;     /* 96 */
    int64_t  st_blocks;   /* 104 */
    int32_t  st_blksize;  /* 112 */
    uint32_t st_flags;    /* 116 */
    uint32_t st_gen;      /* 120 */
    int32_t  st_lspare;   /* 124 */
    int64_t  st_qspare[2];/* 128 */
} jet_darwin_stat;
_Static_assert(sizeof(jet_darwin_stat) == 144, "darwin stat");

typedef struct {                       /* struct statfs (64-bit) */
    uint32_t f_bsize;     /* 0 */
    int32_t  f_iosize;    /* 4 */
    uint64_t f_blocks, f_bfree, f_bavail, f_files, f_ffree; /* 8..48 */
    int32_t  f_fsid[2];   /* 48 */
    uint32_t f_owner;     /* 56 */
    uint32_t f_type;      /* 60 */
    uint32_t f_flags;     /* 64 */
    uint32_t f_fssubtype; /* 68 */
    char     f_fstypename[16];   /* 72 */
    char     f_mntonname[1024];  /* 88 */
    char     f_mntfromname[1024];/* 1112 */
    uint32_t f_flags_ext;        /* 2136 */
    uint32_t f_reserved[7];      /* 2140 */
} jet_darwin_statfs;
_Static_assert(sizeof(jet_darwin_statfs) == 2168, "darwin statfs");

typedef struct {                       /* struct termios */
    uint64_t c_iflag, c_oflag, c_cflag, c_lflag; /* tcflag_t = unsigned long */
    uint8_t  c_cc[20];                           /* NCCS = 20 */
    uint8_t  __pad[4];
    uint64_t c_ispeed, c_ospeed;                 /* speed_t = unsigned long */
} jet_darwin_termios;
_Static_assert(sizeof(jet_darwin_termios) == 72, "darwin termios");

typedef struct {                       /* struct flock */
    int64_t l_start, l_len; int32_t l_pid; int16_t l_type, l_whence;
} jet_darwin_flock;
_Static_assert(sizeof(jet_darwin_flock) == 24, "darwin flock");

typedef struct {                       /* struct kevent */
    uint64_t ident; int16_t filter; uint16_t flags; uint32_t fflags; int64_t data; uint64_t udata;
} jet_kevent;
_Static_assert(sizeof(jet_kevent) == 32, "darwin kevent");

typedef struct {                       /* struct cmsghdr (Darwin) */
    uint32_t cmsg_len; int32_t cmsg_level; int32_t cmsg_type;
} jet_darwin_cmsghdr;
_Static_assert(sizeof(jet_darwin_cmsghdr) == 12, "darwin cmsghdr");
#define JET_DARWIN_CMSG_ALIGN(n) (((n) + 3u) & ~(size_t)3u)
#define JET_GUEST_CMSG_ALIGN(n)  (((n) + 7u) & ~(size_t)7u)

/* Guest flock (Linux aarch64) */
typedef struct {
    int16_t l_type, l_whence; int32_t __pad; int64_t l_start, l_len; int32_t l_pid; int32_t __pad2;
} jet_guest_flock;
_Static_assert(sizeof(jet_guest_flock) == 32, "bionic LP64 struct flock");

/* ---------------- Darwin constants (numeric, verified on __APPLE__) ---------------- */
#define JD_AF_INET6           30
#define JD_AT_FDCWD           (-2)
#define JD_SOL_SOCKET         0xffff
#define JD_EVFILT_READ        (-1)
#define JD_EVFILT_WRITE       (-2)
#define JD_EV_ADD             0x0001
#define JD_EV_DELETE          0x0002
#define JD_EV_ENABLE          0x0004
#define JD_EV_DISABLE         0x0008
#define JD_EV_ONESHOT         0x0010
#define JD_EV_CLEAR           0x0020
#define JD_EV_ERROR           0x4000
#define JD_EV_EOF             0x8000
#define JD_SO_NOSIGPIPE       0x1022

/* ---------------- scalar translations ---------------- */
int      jet_xl_errno_d2l(int darwin_errno);
int      jet_xl_errno_l2d(int linux_errno);   /* inverse (first match) */
int      jet_xl_af_l2d(int af);
int      jet_xl_af_d2l(int af);
int      jet_xl_oflags_l2d(int linux_flags);
int      jet_xl_oflags_d2l(int darwin_flags);
int      jet_xl_atfd_l2d(int dirfd);
int      jet_xl_atflags_l2d(int linux_flags, int for_faccessat);
int      jet_xl_fcntl_cmd_l2d(int linux_cmd);           /* -1 = unsupported */
int      jet_xl_flock_l2d(const jet_guest_flock *g, jet_darwin_flock *d); /* -1 bad type */
void     jet_xl_flock_d2l(const jet_darwin_flock *d, jet_guest_flock *g);
int      jet_xl_clock_l2d(int linux_clock);             /* -1 = unsupported */
int      jet_xl_msgflags_l2d(int linux_flags);
int      jet_xl_msgflags_d2l(int darwin_flags);
int      jet_xl_socktype_l2d(int linux_type, int *nonblock, int *cloexec);
/* Returns kind: 0=int-like passthrough, 1=timeval, 2=SO_ERROR(errno int), -1=unsupported */
int      jet_xl_sockopt_l2d(int level, int opt, int *dlevel, int *dopt);
int      jet_xl_mmap_flags_l2d(int linux_flags);
int      jet_xl_madvise_l2d(int linux_advice);
int16_t  jet_xl_poll_l2d(int16_t ev);
int16_t  jet_xl_poll_d2l(int16_t ev);
int      jet_xl_rlimit_res_l2d(int linux_res);         /* -1 = unsupported */
uint64_t jet_xl_rlim_l2d(uint64_t v);
uint64_t jet_xl_rlim_d2l(uint64_t v);
unsigned long jet_xl_ioctl_l2d(unsigned long linux_req);

/* ---------------- struct translations ---------------- */
void jet_xl_stat_d2l(const jet_darwin_stat *d, jet_guest_stat *g);
void jet_xl_statfs_d2l(const jet_darwin_statfs *d, jet_guest_statfs *g);
void jet_xl_statvfs_d2l(const jet_darwin_statfs *d, jet_guest_statvfs *g);
void jet_xl_termios_l2d(const jet_guest_termios *g, jet_darwin_termios *d);
void jet_xl_termios_d2l(const jet_darwin_termios *d, jet_guest_termios *g);
void jet_xl_timeval_d2l(const jet_darwin_timeval *d, jet_guest_timeval *g);
void jet_xl_timeval_l2d(const jet_guest_timeval *g, jet_darwin_timeval *d);

/* sockaddr: guest {u16 family} <-> BSD {u8 len, u8 family}; family numbers mapped. */
int jet_xl_sockaddr_l2bsd(const void *g, uint32_t glen, void *out, size_t cap);
int jet_xl_sockaddr_bsd2l(const void *b, uint32_t blen, void *g, size_t gcap);

/* cmsg chains: guest Linux (hdr16, align 8) <-> Darwin (hdr12, align 4).
 * l2bsd returns Darwin bytes or -1; bsd2l returns guest bytes written and sets *ctrunc
 * if the guest buffer was too small (MSG_CTRUNC). */
long jet_xl_cmsg_l2bsd(const void *g, size_t glen, void *out, size_t cap);
long jet_xl_cmsg_bsd2l(const void *b, size_t blen, void *g, size_t gcap, int *ctrunc);

/* ---------------- eventfd emulation (pipe pair) — used on Darwin ---------------- */
int  jet_efd_create(unsigned initval, int linux_flags);   /* returns read fd */
int  jet_efd_is(int fd);
long jet_efd_write(int fd, const void *buf, size_t n);    /* -1 + errno */
long jet_efd_read(int fd, void *buf, size_t n);
int  jet_efd_close(int fd);                               /* 1 if handled */

#endif
