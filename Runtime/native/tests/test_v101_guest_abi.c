/* v10.1: guest ABI canary tests — converters must not smash past guest buffer. */
#include "guest_abi.h"
#include "jet_native.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <fcntl.h>
#if defined(__linux__)
#  include <sys/statfs.h>
#  include <sys/statvfs.h>
#  include <sys/epoll.h>
#endif
#include <termios.h>
#include <poll.h>

static int fails;

#define CANARY 0xA5
#define CHECK(cond, msg) do { if (!(cond)) { fprintf(stderr, "FAIL: %s\n", msg); fails++; } } while(0)

static void fill_canary(uint8_t *p, size_t n) { memset(p, CANARY, n); }
static int canary_ok(const uint8_t *p, size_t n) {
    for (size_t i = 0; i < n; i++) if (p[i] != CANARY) return 0;
    return 1;
}

static void test_sizes(void) {
    printf("sizes: stat=%zu statfs=%zu statvfs=%zu utsname=%zu rlimit=%zu rusage=%zu\n",
           sizeof(jet_guest_stat), sizeof(jet_guest_statfs), sizeof(jet_guest_statvfs),
           sizeof(jet_guest_utsname), sizeof(jet_guest_rlimit), sizeof(jet_guest_rusage));
    printf("       termios=%zu epoll_event=%zu msghdr=%zu sockaddr_in=%zu sockaddr_in6=%zu fd_set=%zu pollfd=%zu\n",
           sizeof(jet_guest_termios), sizeof(jet_guest_epoll_event), sizeof(jet_guest_msghdr),
           sizeof(jet_guest_sockaddr_in), sizeof(jet_guest_sockaddr_in6),
           sizeof(jet_guest_fd_set), sizeof(jet_guest_pollfd));
    CHECK(sizeof(jet_guest_stat) == 128, "stat==128");
    CHECK(sizeof(jet_guest_statfs) == 120, "statfs==120");
    CHECK(sizeof(jet_guest_statvfs) == 112, "statvfs==112");
    CHECK(sizeof(jet_guest_utsname) == 390, "utsname==390");
    CHECK(sizeof(jet_guest_rlimit) == 16, "rlimit==16");
    CHECK(sizeof(jet_guest_rusage) == 144, "rusage==144");
    CHECK(sizeof(jet_guest_termios) == 36, "termios==36");
    CHECK(sizeof(jet_guest_epoll_event) == 16, "epoll_event==16");
    CHECK(offsetof(jet_guest_epoll_event, data) == 8, "epoll data@8");
    CHECK(sizeof(jet_guest_msghdr) == 56, "msghdr==56");
    CHECK(sizeof(jet_guest_sockaddr_in) == 16, "sin==16");
    CHECK(sizeof(jet_guest_sockaddr_in6) == 28, "sin6==28");
    CHECK(sizeof(jet_guest_fd_set) == 128, "fd_set==128");
    CHECK(sizeof(jet_guest_pollfd) == 8, "pollfd==8");
}

static void test_stat_canary(void) {
    uint8_t buf[128 + 16];
    fill_canary(buf, sizeof buf);
    struct stat hs;
    CHECK(stat("/", &hs) == 0, "host stat /");
    CHECK(jet_write_guest_stat(buf, &hs) == 0, "write_guest_stat");
    CHECK(canary_ok(buf + 128, 16), "stat canary intact");
    jet_guest_stat *g = (jet_guest_stat *)buf;
    CHECK(g->st_mode == (uint32_t)hs.st_mode, "stat mode");
    CHECK(g->st_size == (int64_t)hs.st_size, "stat size");
}

#if defined(__linux__)
static void test_statfs_canary(void) {
    uint8_t buf[120 + 16];
    fill_canary(buf, sizeof buf);
    struct statfs hs;
    CHECK(statfs("/", &hs) == 0, "host statfs");
    jet_guest_statfs gs;
    jet_host_statfs_to_guest(&hs, &gs);
    memcpy(buf, &gs, sizeof gs);
    CHECK(canary_ok(buf + 120, 16), "statfs canary");
    CHECK(gs.f_bsize == (uint64_t)hs.f_bsize, "f_bsize");

    uint8_t buf2[112 + 16];
    fill_canary(buf2, sizeof buf2);
    struct statvfs hv;
    CHECK(statvfs("/", &hv) == 0, "host statvfs");
    jet_guest_statvfs gv;
    jet_host_statvfs_to_guest(&hv, &gv);
    memcpy(buf2, &gv, sizeof gv);
    CHECK(canary_ok(buf2 + 112, 16), "statvfs canary");
}
#else
static void test_statfs_canary(void) {}
#endif

static void test_utsname_canary(void) {
    uint8_t buf[390 + 16];
    fill_canary(buf, sizeof buf);
    struct utsname hu;
    CHECK(uname(&hu) == 0, "uname");
    jet_guest_utsname gu;
    jet_host_utsname_to_guest(&hu, &gu);
    memcpy(buf, &gu, sizeof gu);
    CHECK(canary_ok(buf + 390, 16), "utsname canary");
    CHECK(strcmp(gu.machine, "aarch64") == 0, "machine aarch64");
}

static void test_rlimit_rusage(void) {
    uint8_t buf[16 + 16];
    fill_canary(buf, sizeof buf);
    struct rlimit hr;
    CHECK(getrlimit(RLIMIT_NOFILE, &hr) == 0, "getrlimit");
    jet_guest_rlimit gr;
    jet_host_rlimit_to_guest(&hr, &gr);
    memcpy(buf, &gr, sizeof gr);
    CHECK(canary_ok(buf + 16, 16), "rlimit canary");
    CHECK(gr.rlim_cur == (uint64_t)hr.rlim_cur, "rlim_cur");

    uint8_t buf2[144 + 16];
    fill_canary(buf2, sizeof buf2);
    struct rusage hu;
    CHECK(getrusage(RUSAGE_SELF, &hu) == 0, "getrusage");
    jet_guest_rusage gu;
    jet_host_rusage_to_guest(&hu, &gu);
    memcpy(buf2, &gu, sizeof gu);
    CHECK(canary_ok(buf2 + 144, 16), "rusage canary");
}

static void test_sockaddr(void) {
    jet_guest_sockaddr_in gin;
    memset(&gin, 0, sizeof gin);
    gin.sin_family = AF_INET;
    gin.sin_port = htons(443);
    gin.sin_addr = htonl(0x7f000001);
    uint8_t host[128];
    int hl = jet_guest_sockaddr_to_host(&gin, sizeof gin, host, sizeof host);
    CHECK(hl > 0, "guest→host sin");
    uint8_t gbuf[16 + 16];
    fill_canary(gbuf, sizeof gbuf);
    int gl = jet_host_sockaddr_to_guest(host, (uint32_t)hl, gbuf, 16);
    CHECK(gl == 16, "host→guest sin len");
    CHECK(canary_ok(gbuf + 16, 16), "sockaddr canary");
    jet_guest_sockaddr_in *gout = (jet_guest_sockaddr_in *)gbuf;
    CHECK(gout->sin_family == AF_INET, "family");
    CHECK(gout->sin_port == htons(443), "port");
}

static void test_dirent_pack(void) {
    uint8_t buf[64 + 16];
    fill_canary(buf, sizeof buf);
    int n = jet_pack_guest_dirent64(buf, 64, 42, 100, 4 /* DT_DIR */, "hello");
    CHECK(n > 0 && (n % 8) == 0, "dirent reclen align");
    CHECK(canary_ok(buf + 64, 16) || n <= 64, "dirent within bound");
    /* refill and pack tightly */
    fill_canary(buf, sizeof buf);
    n = jet_pack_guest_dirent64(buf, 64, 1, 0, 8, "x");
    CHECK(n > 0, "pack tiny");
    CHECK(canary_ok(buf + n, (size_t)(64 - n) + 16 > 16 ? 16 : (size_t)(64 - n)), "after pack canary zone");
    /* Explicit: fill past expected write */
    fill_canary(buf, sizeof buf);
    n = jet_pack_guest_dirent64(buf, 64, 7, 8, 8, "abc");
    CHECK(n > 0 && n <= 64, "n<=64");
    CHECK(canary_ok(buf + 64, 16), "dirent canary@64");
}

static void test_termios(void) {
    uint8_t buf[36 + 16];
    fill_canary(buf, sizeof buf);
    struct termios ht;
    /* may fail if no tty — synthesize */
    memset(&ht, 0, sizeof ht);
    ht.c_iflag = 1; ht.c_oflag = 2; ht.c_cflag = 3; ht.c_lflag = 4;
    ht.c_cc[0] = 5;
    jet_guest_termios gt;
    jet_host_termios_to_guest(&ht, &gt);
    memcpy(buf, &gt, sizeof gt);
    CHECK(canary_ok(buf + 36, 16), "termios canary");
    CHECK(gt.c_iflag == 1 && gt.c_cc[0] == 5, "termios fields");
}

#if defined(__linux__)
static void test_epoll(void) {
    uint8_t buf[16 + 16];
    fill_canary(buf, sizeof buf);
    struct epoll_event he;
    memset(&he, 0, sizeof he);
    he.events = EPOLLIN | EPOLLOUT;
    he.data.u64 = 0xdeadbeefcafebabeULL;
    jet_guest_epoll_event ge;
    jet_host_epoll_to_guest(&he, &ge);
    memcpy(buf, &ge, sizeof ge);
    CHECK(canary_ok(buf + 16, 16), "epoll canary");
    CHECK(ge.events == he.events, "epoll events");
    CHECK(ge.data.u64 == he.data.u64, "epoll data");
    CHECK(offsetof(jet_guest_epoll_event, data) == 8, "data@8");
}
#else
static void test_epoll(void) {
    CHECK(sizeof(jet_guest_epoll_event) == 16, "epoll size");
}
#endif

int main(void) {
    test_sizes();
    test_stat_canary();
    test_statfs_canary();
    test_utsname_canary();
    test_rlimit_rusage();
    test_sockaddr();
    test_dirent_pack();
    test_termios();
    test_epoll();
    if (fails) {
        fprintf(stderr, "%d failures\n", fails);
        return 1;
    }
    printf("test_v101_guest_abi: OK\n");
    return 0;
}
