/* guest_abi.c — host↔guest (bionic aarch64 LP64) struct converters. */
#include "guest_abi.h"
#include <sys/un.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/un.h>

#if defined(__APPLE__)
#  include <sys/mount.h>
#endif

/* ---- stat ---- */
void jet_host_stat_to_guest(const struct stat *h, jet_guest_stat *g) {
    memset(g, 0, sizeof *g);
    g->st_dev = (uint64_t)h->st_dev;
    g->st_ino = (uint64_t)h->st_ino;
    g->st_mode = (uint32_t)h->st_mode;
    g->st_nlink = (uint32_t)h->st_nlink;
    g->st_uid = (uint32_t)h->st_uid;
    g->st_gid = (uint32_t)h->st_gid;
    g->st_rdev = (uint64_t)h->st_rdev;
    g->st_size = (int64_t)h->st_size;
    g->st_blksize = (int32_t)h->st_blksize;
    g->st_blocks = (int64_t)h->st_blocks;
#if defined(__APPLE__)
    g->st_atim.tv_sec = (int64_t)h->st_atimespec.tv_sec;
    g->st_atim.tv_nsec = (int64_t)h->st_atimespec.tv_nsec;
    g->st_mtim.tv_sec = (int64_t)h->st_mtimespec.tv_sec;
    g->st_mtim.tv_nsec = (int64_t)h->st_mtimespec.tv_nsec;
    g->st_ctim.tv_sec = (int64_t)h->st_ctimespec.tv_sec;
    g->st_ctim.tv_nsec = (int64_t)h->st_ctimespec.tv_nsec;
#elif defined(__linux__)
    g->st_atim.tv_sec = (int64_t)h->st_atim.tv_sec;
    g->st_atim.tv_nsec = (int64_t)h->st_atim.tv_nsec;
    g->st_mtim.tv_sec = (int64_t)h->st_mtim.tv_sec;
    g->st_mtim.tv_nsec = (int64_t)h->st_mtim.tv_nsec;
    g->st_ctim.tv_sec = (int64_t)h->st_ctim.tv_sec;
    g->st_ctim.tv_nsec = (int64_t)h->st_ctim.tv_nsec;
#endif
}

int jet_write_guest_stat(void *guest_buf, const struct stat *h) {
    if (!guest_buf || !h) { errno = EFAULT; return -1; }
    jet_guest_stat gs;
    jet_host_stat_to_guest(h, &gs);
    memcpy(guest_buf, &gs, sizeof gs);
    return 0;
}

/* ---- statfs / statvfs ---- */
#if defined(__linux__)
void jet_host_statfs_to_guest(const struct statfs *h, jet_guest_statfs *g) {
    memset(g, 0, sizeof *g);
    g->f_type = (uint64_t)h->f_type;
    g->f_bsize = (uint64_t)h->f_bsize;
    g->f_blocks = (uint64_t)h->f_blocks;
    g->f_bfree = (uint64_t)h->f_bfree;
    g->f_bavail = (uint64_t)h->f_bavail;
    g->f_files = (uint64_t)h->f_files;
    g->f_ffree = (uint64_t)h->f_ffree;
    memcpy(g->f_fsid, &h->f_fsid, sizeof g->f_fsid);
    g->f_namelen = (uint64_t)h->f_namelen;
    g->f_frsize = (uint64_t)h->f_frsize;
    g->f_flags = (uint64_t)h->f_flags;
}
void jet_host_statvfs_to_guest(const struct statvfs *h, jet_guest_statvfs *g) {
    memset(g, 0, sizeof *g);
    g->f_bsize = (uint64_t)h->f_bsize;
    g->f_frsize = (uint64_t)h->f_frsize;
    g->f_blocks = (uint64_t)h->f_blocks;
    g->f_bfree = (uint64_t)h->f_bfree;
    g->f_bavail = (uint64_t)h->f_bavail;
    g->f_files = (uint64_t)h->f_files;
    g->f_ffree = (uint64_t)h->f_ffree;
    g->f_favail = (uint64_t)h->f_favail;
    g->f_fsid = (uint64_t)h->f_fsid;
    g->f_flag = (uint64_t)h->f_flag;
    g->f_namemax = (uint64_t)h->f_namemax;
}
#endif

#if defined(__APPLE__)
void jet_host_statfs_to_guest_apple(const struct statfs *h, jet_guest_statfs *g) {
    memset(g, 0, sizeof *g);
    /* Darwin struct statfs: f_type short, f_bsize, etc. Map best-effort. */
    g->f_type = (uint64_t)h->f_type;
    g->f_bsize = (uint64_t)h->f_bsize;
    g->f_blocks = (uint64_t)h->f_blocks;
    g->f_bfree = (uint64_t)h->f_bfree;
    g->f_bavail = (uint64_t)h->f_bavail;
    g->f_files = (uint64_t)h->f_files;
    g->f_ffree = (uint64_t)h->f_ffree;
    memcpy(g->f_fsid, &h->f_fsid, sizeof g->f_fsid > sizeof h->f_fsid ? sizeof h->f_fsid : sizeof g->f_fsid);
    g->f_namelen = 255;
    g->f_frsize = (uint64_t)h->f_bsize;
    g->f_flags = (uint64_t)h->f_flags;
}
#endif

/* ---- dirent64 pack ---- */
int jet_pack_guest_dirent64(void *dst, size_t avail,
                            uint64_t ino, int64_t off, uint8_t type, const char *name) {
    if (!dst || !name) return -1;
    size_t nlen = strlen(name) + 1;
    size_t reclen = (JET_DIRENT64_HDR + nlen + 7u) & ~7u; /* 8-byte align */
    if (reclen > avail) return -1;
    uint8_t *p = (uint8_t *)dst;
    memset(p, 0, reclen);
    memcpy(p + 0, &ino, 8);
    memcpy(p + 8, &off, 8);
    uint16_t rl = (uint16_t)reclen;
    memcpy(p + 16, &rl, 2);
    p[18] = type;
    memcpy(p + 19, name, nlen);
    return (int)reclen;
}

/* ---- sockaddr ---- */
#ifndef AF_UNIX
#define AF_UNIX 1
#endif

int jet_guest_sockaddr_to_host(const void *guest_sa, uint32_t guest_len,
                               void *host_buf, size_t host_cap) {
    if (!guest_sa || !host_buf || guest_len < 2) return -1;
    const uint8_t *g = (const uint8_t *)guest_sa;
    uint16_t family;
    memcpy(&family, g, 2);

#if defined(__APPLE__)
    /* Host wants sa_len + sa_family=u8 */
    if (family == AF_INET && guest_len >= sizeof(jet_guest_sockaddr_in) &&
        host_cap >= sizeof(struct sockaddr_in)) {
        const jet_guest_sockaddr_in *gin = (const jet_guest_sockaddr_in *)guest_sa;
        struct sockaddr_in hin;
        memset(&hin, 0, sizeof hin);
        hin.sin_len = sizeof hin;
        hin.sin_family = AF_INET;
        hin.sin_port = gin->sin_port;
        memcpy(&hin.sin_addr, &gin->sin_addr, 4);
        memcpy(host_buf, &hin, sizeof hin);
        return (int)sizeof hin;
    }
    if (family == AF_INET6 && guest_len >= sizeof(jet_guest_sockaddr_in6) &&
        host_cap >= sizeof(struct sockaddr_in6)) {
        const jet_guest_sockaddr_in6 *gin = (const jet_guest_sockaddr_in6 *)guest_sa;
        struct sockaddr_in6 hin;
        memset(&hin, 0, sizeof hin);
        hin.sin6_len = sizeof hin;
        hin.sin6_family = AF_INET6;
        hin.sin6_port = gin->sin6_port;
        hin.sin6_flowinfo = gin->sin6_flowinfo;
        memcpy(&hin.sin6_addr, gin->sin6_addr, 16);
        hin.sin6_scope_id = gin->sin6_scope_id;
        memcpy(host_buf, &hin, sizeof hin);
        return (int)sizeof hin;
    }
    if (family == AF_UNIX && host_cap >= sizeof(struct sockaddr_un)) {
        const jet_guest_sockaddr_un *gun = (const jet_guest_sockaddr_un *)guest_sa;
        struct sockaddr_un hun;
        memset(&hun, 0, sizeof hun);
        hun.sun_len = sizeof hun;
        hun.sun_family = AF_UNIX;
        size_t plen = guest_len > 2 ? guest_len - 2 : 0;
        if (plen > sizeof hun.sun_path) plen = sizeof hun.sun_path;
        memcpy(hun.sun_path, gun->sun_path, plen);
        memcpy(host_buf, &hun, sizeof hun);
        return (int)sizeof hun;
    }
    return -1;
#else
    /* Linux host: guest layout matches for sockaddr_in/in6/un */
    size_t n = guest_len;
    if (n > host_cap) n = host_cap;
    memcpy(host_buf, guest_sa, n);
    return (int)n;
#endif
}

int jet_host_sockaddr_to_guest(const void *host_sa, uint32_t host_len,
                               void *guest_buf, size_t guest_cap) {
    if (!host_sa || !guest_buf || host_len < 2) return -1;
#if defined(__APPLE__)
    const struct sockaddr *hsa = (const struct sockaddr *)host_sa;
    uint8_t fam = hsa->sa_family;
    if (fam == AF_INET && host_len >= sizeof(struct sockaddr_in) &&
        guest_cap >= sizeof(jet_guest_sockaddr_in)) {
        const struct sockaddr_in *hin = (const struct sockaddr_in *)host_sa;
        jet_guest_sockaddr_in gin;
        memset(&gin, 0, sizeof gin);
        gin.sin_family = AF_INET;
        gin.sin_port = hin->sin_port;
        memcpy(&gin.sin_addr, &hin->sin_addr, 4);
        memcpy(guest_buf, &gin, sizeof gin);
        return (int)sizeof gin;
    }
    if (fam == AF_INET6 && host_len >= sizeof(struct sockaddr_in6) &&
        guest_cap >= sizeof(jet_guest_sockaddr_in6)) {
        const struct sockaddr_in6 *hin = (const struct sockaddr_in6 *)host_sa;
        jet_guest_sockaddr_in6 gin;
        memset(&gin, 0, sizeof gin);
        gin.sin6_family = AF_INET6;
        gin.sin6_port = hin->sin6_port;
        gin.sin6_flowinfo = hin->sin6_flowinfo;
        memcpy(gin.sin6_addr, &hin->sin6_addr, 16);
        gin.sin6_scope_id = hin->sin6_scope_id;
        memcpy(guest_buf, &gin, sizeof gin);
        return (int)sizeof gin;
    }
    if (fam == AF_UNIX && guest_cap >= sizeof(jet_guest_sockaddr_un)) {
        const struct sockaddr_un *hun = (const struct sockaddr_un *)host_sa;
        jet_guest_sockaddr_un gun;
        memset(&gun, 0, sizeof gun);
        gun.sun_family = AF_UNIX;
        memcpy(gun.sun_path, hun->sun_path, sizeof gun.sun_path);
        memcpy(guest_buf, &gun, sizeof gun);
        return (int)sizeof gun;
    }
    return -1;
#else
    size_t n = host_len;
    if (n > guest_cap) n = guest_cap;
    memcpy(guest_buf, host_sa, n);
    return (int)n;
#endif
}

/* ---- utsname ---- */
static void copy_uts_field(char *dst, size_t dstlen, const char *src) {
    memset(dst, 0, dstlen);
    if (!src) return;
    size_t n = strlen(src);
    if (n >= dstlen) n = dstlen - 1;
    memcpy(dst, src, n);
}

void jet_host_utsname_to_guest(const struct utsname *h, jet_guest_utsname *g) {
    memset(g, 0, sizeof *g);
    copy_uts_field(g->sysname, JET_UTS_LEN, h->sysname);
    copy_uts_field(g->nodename, JET_UTS_LEN, h->nodename);
    copy_uts_field(g->release, JET_UTS_LEN, h->release);
    copy_uts_field(g->version, JET_UTS_LEN, h->version);
    copy_uts_field(g->machine, JET_UTS_LEN, h->machine);
#if defined(__linux__)
    copy_uts_field(g->domainname, JET_UTS_LEN, h->domainname);
#else
    copy_uts_field(g->domainname, JET_UTS_LEN, "");
#endif
    /* Guest always reports aarch64 machine for identity. */
    copy_uts_field(g->machine, JET_UTS_LEN, "aarch64");
}

/* ---- rlimit / rusage ---- */
void jet_host_rlimit_to_guest(const struct rlimit *h, jet_guest_rlimit *g) {
    g->rlim_cur = (uint64_t)h->rlim_cur;
    g->rlim_max = (uint64_t)h->rlim_max;
}
void jet_guest_rlimit_to_host(const jet_guest_rlimit *g, struct rlimit *h) {
    h->rlim_cur = (rlim_t)g->rlim_cur;
    h->rlim_max = (rlim_t)g->rlim_max;
}

void jet_host_rusage_to_guest(const struct rusage *h, jet_guest_rusage *g) {
    memset(g, 0, sizeof *g);
    g->ru_utime.tv_sec = (int64_t)h->ru_utime.tv_sec;
    g->ru_utime.tv_usec = (int64_t)h->ru_utime.tv_usec;
    g->ru_stime.tv_sec = (int64_t)h->ru_stime.tv_sec;
    g->ru_stime.tv_usec = (int64_t)h->ru_stime.tv_usec;
    g->ru_maxrss = (int64_t)h->ru_maxrss;
    g->ru_ixrss = (int64_t)h->ru_ixrss;
    g->ru_idrss = (int64_t)h->ru_idrss;
    g->ru_isrss = (int64_t)h->ru_isrss;
    g->ru_minflt = (int64_t)h->ru_minflt;
    g->ru_majflt = (int64_t)h->ru_majflt;
    g->ru_nswap = (int64_t)h->ru_nswap;
    g->ru_inblock = (int64_t)h->ru_inblock;
    g->ru_oublock = (int64_t)h->ru_oublock;
    g->ru_msgsnd = (int64_t)h->ru_msgsnd;
    g->ru_msgrcv = (int64_t)h->ru_msgrcv;
    g->ru_nsignals = (int64_t)h->ru_nsignals;
    g->ru_nvcsw = (int64_t)h->ru_nvcsw;
    g->ru_nivcsw = (int64_t)h->ru_nivcsw;
}

/* ---- termios ---- */
#if defined(__linux__) || defined(__APPLE__)
void jet_host_termios_to_guest(const struct termios *h, jet_guest_termios *g) {
    memset(g, 0, sizeof *g);
    g->c_iflag = (uint32_t)h->c_iflag;
    g->c_oflag = (uint32_t)h->c_oflag;
    g->c_cflag = (uint32_t)h->c_cflag;
    g->c_lflag = (uint32_t)h->c_lflag;
#if defined(__linux__)
    g->c_line = (uint8_t)h->c_line;
#endif
    size_t n = sizeof g->c_cc;
#ifdef NCCS
    if ((size_t)NCCS < n) n = (size_t)NCCS;
#endif
    memcpy(g->c_cc, h->c_cc, n);
}
void jet_guest_termios_to_host(const jet_guest_termios *g, struct termios *h) {
    memset(h, 0, sizeof *h);
    h->c_iflag = g->c_iflag;
    h->c_oflag = g->c_oflag;
    h->c_cflag = g->c_cflag;
    h->c_lflag = g->c_lflag;
#if defined(__linux__)
    h->c_line = g->c_line;
#endif
    size_t n = sizeof g->c_cc;
#ifdef NCCS
    if ((size_t)NCCS < n) n = (size_t)NCCS;
#endif
    memcpy(h->c_cc, g->c_cc, n);
}
#endif

/* ---- epoll ---- */
#if defined(__linux__)
void jet_host_epoll_to_guest(const struct epoll_event *h, jet_guest_epoll_event *g) {
    memset(g, 0, sizeof *g);
    g->events = h->events;
    /* Host x86_64: data at +4 (packed 12). Guest aarch64: data at +8.
     * Copy via union to be safe regardless of host packing. */
    g->data.u64 = h->data.u64;
}
void jet_guest_epoll_to_host(const jet_guest_epoll_event *g, struct epoll_event *h) {
    memset(h, 0, sizeof *h);
    h->events = g->events;
    h->data.u64 = g->data.u64;
}
#endif
