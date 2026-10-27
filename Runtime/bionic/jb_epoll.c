#include "jb_internal.h"
#include <sys/event.h>
#include <sys/time.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>
// Minimal epoll-on-kqueue. Enough for Telegram's network thread (edge-triggered fd readiness).
enum { L_EPOLLIN=1, L_EPOLLOUT=4, L_EPOLLERR=8, L_EPOLLHUP=0x10, L_EPOLLET=0x80000000,
       L_EPOLL_CTL_ADD=1, L_EPOLL_CTL_DEL=2, L_EPOLL_CTL_MOD=3 };
typedef union { void *ptr; int fd; uint32_t u32; uint64_t u64; } jb_epoll_data;
typedef struct { uint32_t events; jb_epoll_data data; } jb_epoll_event;
typedef struct { int kq; pthread_mutex_t mu; int n; struct { int fd; uint32_t ev; jb_epoll_data d; } m[256]; } jb_ep;
static pthread_mutex_t gmu = PTHREAD_MUTEX_INITIALIZER;
static jb_ep *gtab[4096];
JBX int jb_epoll_create1(int flags){ (void)flags; int kq=kqueue(); if (kq<0){ jb_set_errno_linux(errno); return -1; }
    jb_ep *e=calloc(1,sizeof*e); e->kq=kq; pthread_mutex_init(&e->mu,0);
    pthread_mutex_lock(&gmu); if (kq<(int)(sizeof gtab/sizeof*gtab)) gtab[kq]=e; pthread_mutex_unlock(&gmu); return kq; }
JBX int jb_epoll_create(int sz){ (void)sz; return jb_epoll_create1(0); }
JBX int jb_epoll_ctl(int epfd, int op, int fd, jb_epoll_event *ev){
    jb_ep *e=NULL; pthread_mutex_lock(&gmu); if (epfd>=0&&epfd<(int)(sizeof gtab/sizeof*gtab)) e=gtab[epfd]; pthread_mutex_unlock(&gmu);
    if (!e){ errno=EBADF; jb_set_errno_linux(errno); return -1; }
    struct kevent ke[2]; int n=0;
    if (op==L_EPOLL_CTL_DEL){ EV_SET(&ke[n++],fd,EVFILT_READ,EV_DELETE,0,0,0); EV_SET(&ke[n++],fd,EVFILT_WRITE,EV_DELETE,0,0,0); }
    else {
        unsigned f = (op==L_EPOLL_CTL_ADD)?EV_ADD:EV_ADD;
        if (ev->events & L_EPOLLET) f |= EV_CLEAR;
        if (ev->events & L_EPOLLIN) EV_SET(&ke[n++],fd,EVFILT_READ,f,0,0,(void*)(uintptr_t)ev->data.u64);
        if (ev->events & L_EPOLLOUT) EV_SET(&ke[n++],fd,EVFILT_WRITE,f,0,0,(void*)(uintptr_t)ev->data.u64);
        // stash data
        pthread_mutex_lock(&e->mu);
        int i; for (i=0;i<e->n;i++) if (e->m[i].fd==fd) break;
        if (i==e->n && e->n<256) e->n++;
        if (i<256){ e->m[i].fd=fd; e->m[i].ev=ev->events; e->m[i].d=ev->data; }
        pthread_mutex_unlock(&e->mu);
    }
    int r=kevent(e->kq,ke,n,NULL,0,NULL); if (r<0) jb_set_errno_linux(errno); return r;
}
JBX int jb_epoll_wait(int epfd, jb_epoll_event *evs, int max, int timeout){
    jb_ep *e=NULL; pthread_mutex_lock(&gmu); if (epfd>=0&&epfd<(int)(sizeof gtab/sizeof*gtab)) e=gtab[epfd]; pthread_mutex_unlock(&gmu);
    if (!e){ errno=EBADF; jb_set_errno_linux(errno); return -1; }
    struct kevent ke[64]; struct timespec ts, *tsp=NULL;
    if (timeout>=0){ ts.tv_sec=timeout/1000; ts.tv_nsec=(timeout%1000)*1000000L; tsp=&ts; }
    int n=kevent(e->kq,NULL,0,ke,max<64?max:64,tsp); if (n<0){ jb_set_errno_linux(errno); return -1; }
    for (int i=0;i<n;i++){
        evs[i].data.u64=(uint64_t)(uintptr_t)ke[i].udata;
        uint32_t ev=0; if (ke[i].filter==EVFILT_READ) ev|=L_EPOLLIN; if (ke[i].filter==EVFILT_WRITE) ev|=L_EPOLLOUT;
        if (ke[i].flags & EV_EOF) ev|=L_EPOLLHUP; if (ke[i].flags & EV_ERROR) ev|=L_EPOLLERR;
        evs[i].events=ev;
    }
    return n;
}
JBX int jb_eventfd(unsigned init, int flags){
    int p[2]; if (pipe(p)){ jb_set_errno_linux(errno); return -1; }
    // write init as u64; store both ends... simplified: return read end, write end leaked in a table
    (void)flags; uint64_t v=init; if (init) write(p[1],&v,8);
    return p[0]; // TODO: track write end for eventfd_write
}
JBX int jb_eventfd_read(int fd, uint64_t *v){ return (int)read(fd,v,8)==8?0:-1; }
JBX int jb_eventfd_write(int fd, uint64_t v){ return (int)write(fd,&v,8)==8?0:-1; }
