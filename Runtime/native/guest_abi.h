/* guest_abi.h — bionic aarch64 LP64 layouts + host↔guest converters.
 * Identity-mapped guest memory; never write Darwin/glibc-x86 structs into guest. */
#ifndef JET_GUEST_ABI_H
#define JET_GUEST_ABI_H

#include <stdint.h>
#include <stddef.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <sys/resource.h>
#include <netinet/in.h>
#include <sys/un.h>
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ---- timespec (LP64) ---- */
typedef struct { int64_t tv_sec; int64_t tv_nsec; } jet_guest_timespec;
_Static_assert(sizeof(jet_guest_timespec) == 16, "timespec");

/* ---- timeval ---- */
typedef struct { int64_t tv_sec; int64_t tv_usec; } jet_guest_timeval;
_Static_assert(sizeof(jet_guest_timeval) == 16, "timeval");

/* ---- stat (128) — already used by host_bionic ---- */
typedef struct {
    uint64_t st_dev;
    uint64_t st_ino;
    uint32_t st_mode;
    uint32_t st_nlink;
    uint32_t st_uid;
    uint32_t st_gid;
    uint64_t st_rdev;
    uint64_t __pad1;
    int64_t  st_size;
    int32_t  st_blksize;
    int32_t  __pad2;
    int64_t  st_blocks;
    jet_guest_timespec st_atim;
    jet_guest_timespec st_mtim;
    jet_guest_timespec st_ctim;
    uint32_t __unused4;
    uint32_t __unused5;
} jet_guest_stat;
_Static_assert(sizeof(jet_guest_stat) == 128, "bionic LP64 struct_stat");

void jet_host_stat_to_guest(const struct stat *h, jet_guest_stat *g);
int  jet_write_guest_stat(void *guest_buf, const struct stat *h);

/* ---- statfs (120) ---- */
typedef struct {
    uint64_t f_type;
    uint64_t f_bsize;
    uint64_t f_blocks;
    uint64_t f_bfree;
    uint64_t f_bavail;
    uint64_t f_files;
    uint64_t f_ffree;
    int32_t  f_fsid[2];
    uint64_t f_namelen;
    uint64_t f_frsize;
    uint64_t f_flags;
    uint64_t f_spare[4];
} jet_guest_statfs;
_Static_assert(sizeof(jet_guest_statfs) == 120, "bionic LP64 struct_statfs");

/* ---- statvfs (112) ---- */
typedef struct {
    uint64_t f_bsize;
    uint64_t f_frsize;
    uint64_t f_blocks;
    uint64_t f_bfree;
    uint64_t f_bavail;
    uint64_t f_files;
    uint64_t f_ffree;
    uint64_t f_favail;
    uint64_t f_fsid;
    uint64_t f_flag;
    uint64_t f_namemax;
    uint32_t __f_spare[6];
} jet_guest_statvfs;
_Static_assert(sizeof(jet_guest_statvfs) == 112, "bionic LP64 struct_statvfs");

#if defined(__linux__)
#include <sys/statfs.h>
#include <sys/statvfs.h>
void jet_host_statfs_to_guest(const struct statfs *h, jet_guest_statfs *g);
void jet_host_statvfs_to_guest(const struct statvfs *h, jet_guest_statvfs *g);
#endif
#if defined(__APPLE__)
/* Darwin: convert from host struct statfs (different) */
#include <sys/mount.h>
void jet_host_statfs_to_guest_apple(const struct statfs *h, jet_guest_statfs *g);
void jet_host_statfs_to_guest_statvfs_apple(const struct statfs *h, jet_guest_statvfs *g);
#endif

/* ---- linux_dirent64 (variable; header 19 bytes + name) ---- */
typedef struct {
    uint64_t d_ino;
    int64_t  d_off;
    uint16_t d_reclen;
    uint8_t  d_type;
    char     d_name[]; /* flexible */
} jet_guest_dirent64;
/* Fixed prefix size before d_name */
#define JET_DIRENT64_HDR 19u

/* Pack one dirent into guest buffer; returns bytes written or -1 if no room. */
int jet_pack_guest_dirent64(void *dst, size_t avail,
                            uint64_t ino, int64_t off, uint8_t type, const char *name);

/* ---- sockaddr (Linux: sa_family=u16) ---- */
typedef struct {
    uint16_t sa_family;
    char     sa_data[14];
} jet_guest_sockaddr;
_Static_assert(sizeof(jet_guest_sockaddr) == 16, "sockaddr");

typedef struct {
    uint16_t sin_family;
    uint16_t sin_port;
    uint32_t sin_addr; /* struct in_addr */
    uint8_t  sin_zero[8];
} jet_guest_sockaddr_in;
_Static_assert(sizeof(jet_guest_sockaddr_in) == 16, "sockaddr_in");

typedef struct {
    uint16_t sin6_family;
    uint16_t sin6_port;
    uint32_t sin6_flowinfo;
    uint8_t  sin6_addr[16];
    uint32_t sin6_scope_id;
} jet_guest_sockaddr_in6;
_Static_assert(sizeof(jet_guest_sockaddr_in6) == 28, "sockaddr_in6");

typedef struct {
    uint16_t sun_family;
    char     sun_path[108];
} jet_guest_sockaddr_un;
_Static_assert(sizeof(jet_guest_sockaddr_un) == 110, "sockaddr_un");

/* Convert guest→host sockaddr into host_buf (max host_cap). Returns host addrlen or -1. */
int jet_guest_sockaddr_to_host(const void *guest_sa, uint32_t guest_len,
                               void *host_buf, size_t host_cap);
/* Convert host→guest. Writes up to guest_cap; returns guest length or -1. */
int jet_host_sockaddr_to_guest(const void *host_sa, uint32_t host_len,
                               void *guest_buf, size_t guest_cap);

/* ---- iovec / msghdr / cmsghdr (Linux LP64) ---- */
typedef struct {
    uint64_t iov_base; /* guest ptr */
    uint64_t iov_len;
} jet_guest_iovec;
_Static_assert(sizeof(jet_guest_iovec) == 16, "iovec");

typedef struct {
    uint64_t msg_name;       /* guest ptr */
    uint32_t msg_namelen;
    uint32_t __pad0;
    uint64_t msg_iov;        /* guest ptr to jet_guest_iovec[] */
    uint64_t msg_iovlen;
    uint64_t msg_control;    /* guest ptr */
    uint64_t msg_controllen;
    int32_t  msg_flags;
    int32_t  __pad1;
} jet_guest_msghdr;
_Static_assert(sizeof(jet_guest_msghdr) == 56, "msghdr");

typedef struct {
    uint64_t cmsg_len;
    int32_t  cmsg_level;
    int32_t  cmsg_type;
} jet_guest_cmsghdr;
_Static_assert(sizeof(jet_guest_cmsghdr) == 16, "cmsghdr");

/* ---- utsname: 6 × 65 ---- */
#define JET_UTS_LEN 65
typedef struct {
    char sysname[JET_UTS_LEN];
    char nodename[JET_UTS_LEN];
    char release[JET_UTS_LEN];
    char version[JET_UTS_LEN];
    char machine[JET_UTS_LEN];
    char domainname[JET_UTS_LEN];
} jet_guest_utsname;
_Static_assert(sizeof(jet_guest_utsname) == 390, "utsname");

void jet_host_utsname_to_guest(const struct utsname *h, jet_guest_utsname *g);

/* ---- rlimit ---- */
typedef struct {
    uint64_t rlim_cur;
    uint64_t rlim_max;
} jet_guest_rlimit;
_Static_assert(sizeof(jet_guest_rlimit) == 16, "rlimit");

void jet_host_rlimit_to_guest(const struct rlimit *h, jet_guest_rlimit *g);
void jet_guest_rlimit_to_host(const jet_guest_rlimit *g, struct rlimit *h);

/* ---- rusage ---- */
typedef struct {
    jet_guest_timeval ru_utime;
    jet_guest_timeval ru_stime;
    int64_t ru_maxrss, ru_ixrss, ru_idrss, ru_isrss;
    int64_t ru_minflt, ru_majflt, ru_nswap, ru_inblock, ru_oublock;
    int64_t ru_msgsnd, ru_msgrcv, ru_nsignals, ru_nvcsw, ru_nivcsw;
} jet_guest_rusage;
_Static_assert(sizeof(jet_guest_rusage) == 144, "rusage");

void jet_host_rusage_to_guest(const struct rusage *h, jet_guest_rusage *g);

/* ---- signals (guest-emulated; NEVER written into a host sigaction) ----
 * bionic LP64 (libc/include/bits/signal_types.h __SIGACTION_BODY):
 *   int sa_flags; union{handler,sigaction}; sigset_t sa_mask; void (*sa_restorer)(void);
 * sigset_t == sigset64_t == 8 bytes on LP64 (64 signals). */
typedef uint64_t jet_guest_sigset;
_Static_assert(sizeof(jet_guest_sigset) == 8, "bionic LP64 sigset_t");
typedef struct {
    int32_t  sa_flags;    /* 0 */
    int32_t  __pad;       /* 4 */
    uint64_t handler;     /* 8  (sa_handler / sa_sigaction; glibc macro-names those) */
    uint64_t sa_mask;     /* 16 */
    uint64_t sa_restorer; /* 24 */
} jet_guest_sigaction;
_Static_assert(sizeof(jet_guest_sigaction) == 32, "bionic LP64 struct sigaction");
_Static_assert(offsetof(jet_guest_sigaction, handler) == 8, "sa_handler@8");
_Static_assert(offsetof(jet_guest_sigaction, sa_mask) == 16, "sa_mask@16");

/* arm64 kernel struct sigaction (rt_sigaction SVC; arm64 defines SA_RESTORER). */
typedef struct {
    uint64_t handler;     /* 0 */
    uint64_t sa_flags;    /* 8 */
    uint64_t sa_restorer; /* 16 */
    uint64_t sa_mask;     /* 24 */
} jet_guest_kernel_sigaction;
_Static_assert(sizeof(jet_guest_kernel_sigaction) == 32, "arm64 kernel sigaction");

/* siginfo_t: 128 bytes (SI_MAX_SIZE); signo/errno/code then 8-aligned union @16. */
typedef struct {
    int32_t si_signo;
    int32_t si_errno;
    int32_t si_code;
    int32_t __pad0;
    union {
        int32_t _pad[28];
        struct { int32_t pid; uint32_t uid; } kill;
        struct { int32_t tid; int32_t overrun; uint64_t sigval; } timer;
        struct { uint64_t addr; } fault;          /* si_addr @16 */
        struct { int64_t band; int32_t fd; } poll;
    } _sifields;
} jet_guest_siginfo;
_Static_assert(sizeof(jet_guest_siginfo) == 128, "siginfo_t");
_Static_assert(offsetof(jet_guest_siginfo, _sifields) == 16, "si_addr@16");

/* Build a guest siginfo (e.g. for a future emulated SIGSEGV delivery). */
void jet_guest_siginfo_fill(jet_guest_siginfo *si, int signo, int code, uint64_t addr);

/* ---- stack_t ---- */
typedef struct {
    uint64_t ss_sp;
    int32_t  ss_flags;
    int32_t  __pad;
    uint64_t ss_size;
} jet_guest_stack_t;
_Static_assert(sizeof(jet_guest_stack_t) == 24, "stack_t");

/* ---- termios (bionic NCCS=19 → 36 bytes; no separate ispeed/ospeed) ---- */
#define JET_NCCS 19
typedef struct {
    uint32_t c_iflag;
    uint32_t c_oflag;
    uint32_t c_cflag;
    uint32_t c_lflag;
    uint8_t  c_line;
    uint8_t  c_cc[JET_NCCS];
} jet_guest_termios;
_Static_assert(sizeof(jet_guest_termios) == 36, "bionic termios");

#if defined(__linux__) || defined(__APPLE__)
#include <termios.h>
void jet_host_termios_to_guest(const struct termios *h, jet_guest_termios *g);
void jet_guest_termios_to_host(const jet_guest_termios *g, struct termios *h);
#endif

/* ---- pollfd ---- */
typedef struct {
    int32_t fd;
    int16_t events;
    int16_t revents;
} jet_guest_pollfd;
_Static_assert(sizeof(jet_guest_pollfd) == 8, "pollfd");

/* ---- epoll_event: aarch64 bionic = 16 (data at +8); x86_64 host packed = 12 ---- */
typedef union {
    uint64_t u64;
    uint32_t u32;
    int32_t  fd;
    uint64_t ptr; /* guest ptr bits */
} jet_guest_epoll_data;

typedef struct {
    uint32_t events;
    uint32_t __pad;
    jet_guest_epoll_data data;
} jet_guest_epoll_event;
_Static_assert(sizeof(jet_guest_epoll_event) == 16, "epoll_event aarch64");
_Static_assert(offsetof(jet_guest_epoll_event, data) == 8, "epoll data @8");

#if defined(__linux__)
#include <sys/epoll.h>
void jet_host_epoll_to_guest(const struct epoll_event *h, jet_guest_epoll_event *g);
void jet_guest_epoll_to_host(const jet_guest_epoll_event *g, struct epoll_event *h);
#endif

/* ---- fd_set: Linux NFDBITS=64, FD_SETSIZE=1024 → 128 bytes ---- */
#define JET_FD_SETSIZE 1024
#define JET_NFDBITS 64
typedef struct {
    uint64_t fds_bits[JET_FD_SETSIZE / JET_NFDBITS];
} jet_guest_fd_set;
_Static_assert(sizeof(jet_guest_fd_set) == 128, "fd_set");

#ifdef __cplusplus
}
#endif
#endif /* JET_GUEST_ABI_H */
