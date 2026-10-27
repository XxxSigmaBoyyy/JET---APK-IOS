/* epoll_kq.c — see epoll_kq.h. */
#include <pthread.h>
#include "epoll_kq.h"
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

int jet_ekq_init(jet_ekq *e, int kq, jet_kevent_fn fn, void *u) {
    memset(e, 0, sizeof *e);
    e->kq = kq; e->fn = fn; e->fn_u = u;
    pthread_mutex_init(&e->mu, NULL);
    return 0;
}
void jet_ekq_destroy(jet_ekq *e) {
    free(e->items); e->items = NULL; e->n = e->cap = 0;
    pthread_mutex_destroy(&e->mu);
}
static jet_ekq_item *find(jet_ekq *e, int fd) {
    for (int i = 0; i < e->n; i++) if (e->items[i].fd == fd) return &e->items[i];
    return NULL;
}
static void kset(jet_kevent *k, int fd, int16_t filt, uint16_t flags) {
    memset(k, 0, sizeof *k);
    k->ident = (uint64_t)(unsigned)fd; k->filter = filt; k->flags = flags;
    k->udata = (uint64_t)(unsigned)fd;           /* udata = fd (stable across table edits) */
}
static int want_read(uint32_t ev)  { return !!(ev & (JET_EPOLLIN | JET_EPOLLRDNORM | JET_EPOLLPRI | JET_EPOLLRDHUP)); }
static int want_write(uint32_t ev) { return !!(ev & (JET_EPOLLOUT | JET_EPOLLWRNORM)); }

/* Arm/disarm filters to match it->events. Caller holds mu. */
static int apply(jet_ekq *e, jet_ekq_item *it, int deleting) {
    jet_kevent ch[2]; int n = 0;
    int wr = !deleting && !it->disabled && want_read(it->events);
    int ww = !deleting && !it->disabled && want_write(it->events);
    uint16_t add = JD_EV_ADD | JD_EV_ENABLE
                 | ((it->events & JET_EPOLLET) ? JD_EV_CLEAR : 0)
                 | ((it->events & JET_EPOLLONESHOT) ? JD_EV_ONESHOT : 0);
    if (wr) kset(&ch[n++], it->fd, JD_EVFILT_READ, add);
    else if (it->armed_r) kset(&ch[n++], it->fd, JD_EVFILT_READ, JD_EV_DELETE);
    if (ww) kset(&ch[n++], it->fd, JD_EVFILT_WRITE, add);
    else if (it->armed_w) kset(&ch[n++], it->fd, JD_EVFILT_WRITE, JD_EV_DELETE);
    int rc = 0;
    /* One change per call: without an eventlist XNU aborts the batch on the first error. */
    for (int i = 0; i < n && rc == 0; i++) {
        rc = e->fn(e->fn_u, e->kq, &ch[i], 1, NULL, 0, 0);
        /* Deleting a filter the kernel already dropped (fd closed / oneshot fired) is fine. */
        if (rc < 0 && (deleting || (ch[i].flags & JD_EV_DELETE))) rc = 0;
    }
    if (rc == 0) { it->armed_r = (uint8_t)wr; it->armed_w = (uint8_t)ww; }
    return rc < 0 ? -jet_xl_errno_d2l(errno) : 0;
}

int jet_ekq_ctl(jet_ekq *e, int op, int fd, uint32_t events, uint64_t data) {
    if (fd == e->kq) return -22;                             /* EINVAL: epfd into itself */
    pthread_mutex_lock(&e->mu);
    jet_ekq_item *it = find(e, fd);
    int rc = 0;
    switch (op) {
    case 1: /* EPOLL_CTL_ADD */
        if (it) { rc = -17; break; }                         /* EEXIST */
        if (e->n == e->cap) {
            int nc = e->cap ? e->cap * 2 : 16;
            jet_ekq_item *ni = realloc(e->items, (size_t)nc * sizeof *ni);
            if (!ni) { rc = -12; break; }
            e->items = ni; e->cap = nc;
        }
        it = &e->items[e->n++];
        memset(it, 0, sizeof *it);
        it->fd = fd; it->events = events; it->data = data;
        rc = apply(e, it, 0);
        if (rc) e->n--;                                      /* roll back (e.g. EBADF) */
        break;
    case 3: /* EPOLL_CTL_MOD */
        if (!it) { rc = -2; break; }                         /* ENOENT */
        it->events = events; it->data = data; it->disabled = 0;
        rc = apply(e, it, 0);
        break;
    case 2: /* EPOLL_CTL_DEL */
        if (!it) { rc = -2; break; }
        apply(e, it, 1);
        *it = e->items[--e->n];
        break;
    default: rc = -22;
    }
    pthread_mutex_unlock(&e->mu);
    return rc;
}

void jet_ekq_forget_fd(jet_ekq *e, int fd) {
    pthread_mutex_lock(&e->mu);
    jet_ekq_item *it = find(e, fd);
    if (it) *it = e->items[--e->n];
    pthread_mutex_unlock(&e->mu);
}

static int64_t now_ms(void) {
    struct timespec ts; clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

int jet_ekq_wait(jet_ekq *e, jet_guest_epoll_event *out, int maxevents, int timeout_ms) {
    if (maxevents <= 0) return -22;
    int cap = maxevents * 2; if (cap > 1024) cap = 1024;
    jet_kevent *kev = malloc((size_t)cap * sizeof *kev);
    if (!kev) return -12;
    int64_t deadline = timeout_ms > 0 ? now_ms() + timeout_ms : 0;
    int nout = 0;
    for (;;) {
        int t = timeout_ms < 0 ? -1 : timeout_ms == 0 ? 0 : (int)(deadline - now_ms());
        if (timeout_ms > 0 && t < 0) t = 0;
        int n = e->fn(e->fn_u, e->kq, NULL, 0, kev, cap, t);
        if (n < 0) { int er = errno; free(kev); return -jet_xl_errno_d2l(er); }
        pthread_mutex_lock(&e->mu);
        for (int i = 0; i < n; i++) {
            int fd = (int)kev[i].udata;
            jet_ekq_item *it = find(e, fd);
            if (!it || it->disabled) continue;               /* stale (DEL'd / oneshot done) */
            uint32_t want = it->events, ev = 0;
            if (kev[i].flags & JD_EV_ERROR) {
                ev |= JET_EPOLLERR;
            } else if (kev[i].filter == JD_EVFILT_READ) {
                ev |= want & (JET_EPOLLIN | JET_EPOLLRDNORM);
                if (kev[i].flags & JD_EV_EOF) {
                    ev |= want & JET_EPOLLRDHUP;
                    if (kev[i].fflags) ev |= JET_EPOLLERR;   /* socket error on EOF */
                }
            } else if (kev[i].filter == JD_EVFILT_WRITE) {
                ev |= want & (JET_EPOLLOUT | JET_EPOLLWRNORM);
                if (kev[i].flags & JD_EV_EOF) ev |= JET_EPOLLHUP | (kev[i].fflags ? JET_EPOLLERR : 0);
            }
            if (!ev) continue;
            int j;
            for (j = 0; j < nout; j++) if (out[j].__pad == (uint32_t)fd) break;
            if (j == nout) {
                if (nout >= maxevents) continue;
                memset(&out[nout], 0, sizeof out[nout]);
                out[nout].__pad = (uint32_t)fd;               /* merge key; cleared below */
                out[nout].data.u64 = it->data;
                nout++;
            }
            out[j].events |= ev;
            if (want & JET_EPOLLONESHOT) {                    /* epoll: whole fd disarmed */
                it->disabled = 1;
                apply(e, it, 0);
            }
        }
        pthread_mutex_unlock(&e->mu);
        if (nout || timeout_ms == 0) break;
        if (timeout_ms > 0 && now_ms() >= deadline) break;
        if (n == 0 && timeout_ms > 0) break;
    }
    for (int j = 0; j < nout; j++) out[j].__pad = 0;
    free(kev);
    return nout;
}

/* ---------------- registry ---------------- */
#define JET_EKQ_MAX 64
static jet_ekq *g_reg[JET_EKQ_MAX];
static pthread_mutex_t g_reg_mu = PTHREAD_MUTEX_INITIALIZER;

jet_ekq *jet_ekq_registry_add(int kq, jet_kevent_fn fn, void *u) {
    jet_ekq *e = calloc(1, sizeof *e);
    if (!e) return NULL;
    jet_ekq_init(e, kq, fn, u);
    pthread_mutex_lock(&g_reg_mu);
    for (int i = 0; i < JET_EKQ_MAX; i++) if (!g_reg[i]) {
        g_reg[i] = e; pthread_mutex_unlock(&g_reg_mu); return e;
    }
    pthread_mutex_unlock(&g_reg_mu);
    jet_ekq_destroy(e); free(e); errno = EMFILE;
    return NULL;
}
jet_ekq *jet_ekq_registry_get(int epfd) {
    jet_ekq *r = NULL;
    pthread_mutex_lock(&g_reg_mu);
    for (int i = 0; i < JET_EKQ_MAX; i++) if (g_reg[i] && g_reg[i]->kq == epfd) { r = g_reg[i]; break; }
    pthread_mutex_unlock(&g_reg_mu);
    return r;
}
int jet_ekq_registry_close(int epfd) {
    jet_ekq *r = NULL;
    pthread_mutex_lock(&g_reg_mu);
    for (int i = 0; i < JET_EKQ_MAX; i++) if (g_reg[i] && g_reg[i]->kq == epfd) { r = g_reg[i]; g_reg[i] = NULL; break; }
    pthread_mutex_unlock(&g_reg_mu);
    if (!r) return 0;
    jet_ekq_destroy(r); free(r);
    return 1;
}
void jet_ekq_registry_forget_fd(int fd) {
    pthread_mutex_lock(&g_reg_mu);
    for (int i = 0; i < JET_EKQ_MAX; i++) if (g_reg[i]) jet_ekq_forget_fd(g_reg[i], fd);
    pthread_mutex_unlock(&g_reg_mu);
}
