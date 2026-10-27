#include "jb_internal.h"
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <semaphore.h>
#include <stdatomic.h>
// Bionic pthread_mutex_t is 40 bytes; Darwin is 64. Apps embed them in structs, so we cannot
// change the size. Strategy: treat the first 8 bytes as an atomic pointer to a heap Darwin mutex,
// with lazy init. Bionic static initializers: 0 = normal, 0x4000 = recursive, 0x8000 = errorcheck.
typedef struct { atomic_uintptr_t p; char pad[32]; } jb_mutex; // 40 B
_Static_assert(sizeof(jb_mutex)==40, "bionic mutex");
typedef struct { atomic_uintptr_t p; char pad[48]; } jb_cond;   // 56? actually bionic cond is 48 on some; Telegram uses 48-byte
// Use conservative sizes matching Android arm64 bionic NDK r26:
// mutex=40, cond=48, rwlock=56, once=4, key=4, attr=56, condattr=4, mutexattr=4
typedef struct { atomic_uintptr_t p; char pad[40]; } jb_cond48;
typedef struct { atomic_uintptr_t p; char pad[48]; } jb_rwlock; // 56
typedef atomic_int jb_once; // 4
typedef int jb_key; // 4

static pthread_mutex_t *mut_get(jb_mutex *m, int type){
    uintptr_t v = atomic_load(&m->p);
    if (v > 0x10000) return (pthread_mutex_t*)v;
    pthread_mutex_t *n = malloc(sizeof *n);
    pthread_mutexattr_t a; pthread_mutexattr_init(&a);
    if (type==1 || v==0x4000) pthread_mutexattr_settype(&a, PTHREAD_MUTEX_RECURSIVE);
    else if (type==2 || v==0x8000) pthread_mutexattr_settype(&a, PTHREAD_MUTEX_ERRORCHECK);
    pthread_mutex_init(n,&a); pthread_mutexattr_destroy(&a);
    uintptr_t exp=v;
    if (!atomic_compare_exchange_strong(&m->p,&exp,(uintptr_t)n)){ pthread_mutex_destroy(n); free(n); return (pthread_mutex_t*)atomic_load(&m->p); }
    return n;
}
JBX int jb_pthread_mutex_init(jb_mutex *m, void *attr){
    int type=0; // TODO read bionic mutexattr
    (void)attr; atomic_store(&m->p,0); mut_get(m,type); return 0;
}
JBX int jb_pthread_mutex_lock(jb_mutex *m){ return pthread_mutex_lock(mut_get(m,0)); }
JBX int jb_pthread_mutex_trylock(jb_mutex *m){ return pthread_mutex_trylock(mut_get(m,0)); }
JBX int jb_pthread_mutex_unlock(jb_mutex *m){ return pthread_mutex_unlock(mut_get(m,0)); }
JBX int jb_pthread_mutex_destroy(jb_mutex *m){
    uintptr_t v=atomic_exchange(&m->p,0); if (v>0x10000){ pthread_mutex_destroy((pthread_mutex_t*)v); free((void*)v); } return 0;
}
JBX int jb_pthread_mutexattr_settype(void *a, int t){ (void)a;(void)t; return 0; /* stored by init path via bionic value */ }

static pthread_cond_t *cond_get(jb_cond48 *c){
    uintptr_t v=atomic_load(&c->p); if (v>0x10000) return (pthread_cond_t*)v;
    pthread_cond_t *n=malloc(sizeof*n); pthread_cond_init(n,NULL);
    uintptr_t exp=0; if (!atomic_compare_exchange_strong(&c->p,&exp,(uintptr_t)n)){ pthread_cond_destroy(n); free(n); return (pthread_cond_t*)atomic_load(&c->p); }
    return n;
}
JBX int jb_pthread_cond_init(jb_cond48 *c, void *a){ (void)a; atomic_store(&c->p,0); cond_get(c); return 0; }
JBX int jb_pthread_cond_timedwait(jb_cond48 *c, jb_mutex *m, const struct timespec *ts){
    return pthread_cond_timedwait(cond_get(c), mut_get(m,0), ts);
}
JBX int jb_pthread_condattr_init(void *a){ memset(a,0,4); return 0; }
JBX int jb_pthread_condattr_setclock(void *a, int clk){ (void)a;(void)clk; return 0; /* Darwin timedwait is REALTIME; convert in timedwait if needed */ }

JBX int jb_pthread_rwlock_init(jb_rwlock *r, void *a){
    (void)a; pthread_rwlock_t *n=malloc(sizeof*n); pthread_rwlock_init(n,NULL); atomic_store(&r->p,(uintptr_t)n); return 0;
}
JBX int jb_sem_init(sem_t *s, int pshared, unsigned v){ return sem_init(s,pshared,v); }
JBX int jb_sem_timedwait(sem_t *s, const struct timespec *ts){ return sem_timedwait(s,ts); }
