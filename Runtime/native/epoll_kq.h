/* epoll_kq.h — Linux epoll semantics on a BSD kqueue (Darwin host).
 * Pure logic over a pluggable kevent backend: real kevent() on __APPLE__,
 * a mock in tests/test_v101_darwin_abi.c on Linux. Guest errors are returned
 * as negative *Linux* errno values. */
#ifndef JET_EPOLL_KQ_H
#define JET_EPOLL_KQ_H
#include <pthread.h>
#include "darwin_abi.h"

typedef int (*jet_kevent_fn)(void *u, int kq, const jet_kevent *chg, int nchg,
                             jet_kevent *ev, int nev, int timeout_ms);

#define JET_EPOLLIN      0x001u
#define JET_EPOLLPRI     0x002u
#define JET_EPOLLOUT     0x004u
#define JET_EPOLLERR     0x008u
#define JET_EPOLLHUP     0x010u
#define JET_EPOLLRDNORM  0x040u
#define JET_EPOLLWRNORM  0x100u
#define JET_EPOLLRDHUP   0x2000u
#define JET_EPOLLEXCLUSIVE (1u<<28)
#define JET_EPOLLWAKEUP  (1u<<29)
#define JET_EPOLLONESHOT (1u<<30)
#define JET_EPOLLET      (1u<<31)

typedef struct {
    int fd; uint32_t events; uint64_t data;
    uint8_t armed_r, armed_w, disabled;
} jet_ekq_item;

typedef struct jet_ekq {
    int kq;
    int n, cap;
    jet_ekq_item *items;
    jet_kevent_fn fn; void *fn_u;
    pthread_mutex_t mu;
} jet_ekq;

int  jet_ekq_init(jet_ekq *e, int kq, jet_kevent_fn fn, void *u);
void jet_ekq_destroy(jet_ekq *e);
int  jet_ekq_ctl(jet_ekq *e, int op, int fd, uint32_t events, uint64_t data); /* 0 / -errno */
int  jet_ekq_wait(jet_ekq *e, jet_guest_epoll_event *out, int maxevents, int timeout_ms);
/* Forget an fd that the guest close()d (kqueue drops its filters automatically). */
void jet_ekq_forget_fd(jet_ekq *e, int fd);

/* Process-wide epfd -> jet_ekq registry (Darwin host). */
jet_ekq *jet_ekq_registry_add(int kq, jet_kevent_fn fn, void *u);
jet_ekq *jet_ekq_registry_get(int epfd);
int      jet_ekq_registry_close(int epfd);   /* 1 if epfd was an epoll instance */
void     jet_ekq_registry_forget_fd(int fd); /* fd closed: drop from all instances */
#endif
