#include "jb_internal.h"
#include <stdarg.h>
#include <math.h>
#include <mach-o/dyld.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/sysctl.h>
#include <sys/utsname.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <signal.h>
#include <netdb.h>
#include <pthread.h>
#include <semaphore.h>
#include <dlfcn.h>
#include <stdio.h>
// ---- Linux struct layouts (arm64 bionic) we must present to the .so ----
typedef struct { uint64_t st_dev; uint64_t st_ino; uint32_t st_mode; uint32_t st_nlink; uint32_t st_uid; uint32_t st_gid;
    uint64_t st_rdev; uint64_t __pad1; int64_t st_size; int32_t st_blksize; int32_t __pad2; int64_t st_blocks;
    int64_t st_atime; uint64_t st_atime_nsec; int64_t st_mtime; uint64_t st_mtime_nsec; int64_t st_ctime; uint64_t st_ctime_nsec;
    uint32_t __unused[2]; } jb_stat; // 128 B
_Static_assert(sizeof(jb_stat)==128, "bionic stat");
static void to_lstat(const struct stat *d, jb_stat *l){
    memset(l,0,sizeof *l);
    l->st_dev=d->st_dev; l->st_ino=d->st_ino; l->st_mode=d->st_mode; l->st_nlink=d->st_nlink;
    l->st_uid=d->st_uid; l->st_gid=d->st_gid; l->st_rdev=d->st_rdev; l->st_size=d->st_size;
    l->st_blksize=(int32_t)d->st_blksize; l->st_blocks=d->st_blocks;
    l->st_atime=d->st_atimespec.tv_sec; l->st_atime_nsec=d->st_atimespec.tv_nsec;
    l->st_mtime=d->st_mtimespec.tv_sec; l->st_mtime_nsec=d->st_mtimespec.tv_nsec;
    l->st_ctime=d->st_ctimespec.tv_sec; l->st_ctime_nsec=d->st_ctimespec.tv_nsec;
}
JBX int jb_stat(const char *p, jb_stat *o){ struct stat d; if (stat(p,&d)) { jb_set_errno_linux(errno); return -1; } to_lstat(&d,o); return 0; }
JBX int jb_fstat(int fd, jb_stat *o){ struct stat d; if (fstat(fd,&d)) { jb_set_errno_linux(errno); return -1; } to_lstat(&d,o); return 0; }
JBX int jb_lstat(const char *p, jb_stat *o){ struct stat d; if (lstat(p,&d)) { jb_set_errno_linux(errno); return -1; } to_lstat(&d,o); return 0; }

// open: O_* flags that differ
enum { L_O_CREAT=0100, L_O_EXCL=0200, L_O_NOCTTY=0400, L_O_TRUNC=01000, L_O_APPEND=02000,
       L_O_NONBLOCK=04000, L_O_DIRECTORY=0200000, L_O_NOFOLLOW=0400000, L_O_CLOEXEC=02000000, L_O_SYNC=04010000 };
static int oflags_from_linux(int f){
    int o = f & 3; // ACCMODE same
    if (f & L_O_CREAT) o |= O_CREAT; if (f & L_O_EXCL) o |= O_EXCL; if (f & L_O_NOCTTY) o |= O_NOCTTY;
    if (f & L_O_TRUNC) o |= O_TRUNC; if (f & L_O_APPEND) o |= O_APPEND; if (f & L_O_NONBLOCK) o |= O_NONBLOCK;
    if (f & L_O_DIRECTORY) o |= O_DIRECTORY; if (f & L_O_NOFOLLOW) o |= O_NOFOLLOW;
    if (f & L_O_CLOEXEC) o |= O_CLOEXEC; if (f & L_O_SYNC) o |= O_SYNC;
    return o;
}
JBX int jb_open(const char *p, int flags, ...){
    int mode=0; if (flags & L_O_CREAT){ va_list ap; va_start(ap,flags); mode=va_arg(ap,int); va_end(ap); }
    // fixed-arg open is fine either ABI for the first 2; mode in x2 on both when present
    int r = open(p, oflags_from_linux(flags), mode);
    if (r<0) jb_set_errno_linux(errno); return r;
}
JBX int __open_2(const char *p, int flags){ return jb_open(p, flags); }

// clock_gettime: CLOCK_MONOTONIC is 1 on Linux, 6 on Darwin; CLOCK_REALTIME is 0 both
JBX int jb_clock_gettime(int clk, struct timespec *ts){
    clockid_t c = (clk==1) ? CLOCK_MONOTONIC : (clk==0) ? CLOCK_REALTIME : (clockid_t)clk;
    int r = clock_gettime(c, ts); if (r) jb_set_errno_linux(errno); return r;
}
JBX int jb_clock_getres(int clk, struct timespec *ts){
    clockid_t c = (clk==1) ? CLOCK_MONOTONIC : (clk==0) ? CLOCK_REALTIME : (clockid_t)clk;
    int r = clock_getres(c, ts); if (r) jb_set_errno_linux(errno); return r;
}

// mmap: MAP_ANONYMOUS is 0x20 on Linux, 0x1000 on Darwin; MAP_ANON alias
enum { L_MAP_ANON=0x20, L_MAP_FIXED=0x10, L_MAP_PRIVATE=0x02, L_MAP_SHARED=0x01 };
JBX void *jb_mmap(void *a, size_t l, int prot, int flags, int fd, off_t off){
    int f=0; if (flags&L_MAP_SHARED) f|=MAP_SHARED; if (flags&L_MAP_PRIVATE) f|=MAP_PRIVATE;
    if (flags&L_MAP_FIXED) f|=MAP_FIXED; if (flags&L_MAP_ANON) f|=MAP_ANON;
    void *r = mmap(a,l,prot,f,fd,off); if (r==MAP_FAILED) jb_set_errno_linux(errno); return r;
}
JBX void *jb_mremap(void *o, size_t osz, size_t nsz, int flags, ...){
    (void)flags; void *n = mmap(NULL,nsz,PROT_READ|PROT_WRITE,MAP_ANON|MAP_PRIVATE,-1,0);
    if (n==MAP_FAILED){ jb_set_errno_linux(errno); return MAP_FAILED; }
    memcpy(n,o,osz<nsz?osz:nsz); munmap(o,osz); return n;
}

// getauxval / dl_iterate_phdr / gettid
JBX unsigned long jb_getauxval(unsigned long type){
    if (type==6 /*AT_PAGESZ*/) return (unsigned long)getpagesize();
    if (type==16 /*AT_HWCAP*/) return 0; // report none; apps fall back
    if (type==26 /*AT_HWCAP2*/) return 0;
    errno=ENOENT; jb_set_errno_linux(ENOENT); return 0;
}
JBX int jb_gettid(void){ uint64_t t; pthread_threadid_np(NULL,&t); return (int)t; }
JBX int jb_tgkill(int tgid, int tid, int sig){ (void)tgid; (void)tid; return kill(getpid(), jb_sig_from_linux(sig)); }

// dl_iterate_phdr: walk Mach-O images that published __jet_elf_meta
struct jb_dl_phdr_info { uint64_t addr; const char *name; const void *phdr; uint16_t phnum; };
typedef int (*jb_dl_cb)(struct jb_dl_phdr_info*, size_t, void*);
extern const void _dyld_get_image_header(uint32_t);
extern const char *_dyld_get_image_name(uint32_t);
extern uint32_t _dyld_image_count(void);
extern intptr_t _dyld_get_image_vmaddr_slide(uint32_t);
JBX int jb_dl_iterate_phdr(jb_dl_cb cb, void *data){
    uint32_t n = _dyld_image_count();
    for (uint32_t i=0;i<n;i++){
        // look for _jet_elf_meta in the image via dlsym on the image's handle — approximate: scan exported
        // For v0: synthesize a single PT_LOAD covering the whole image; real EH_FRAME walk needs the meta.
        Dl_info di; void *sym = (void*)(uintptr_t)_dyld_get_image_header(i);
        if (!dladdr(sym,&di)) continue;
        // Try to find __jet_elf_meta via nlist would be heavy; export is '_jet_elf_meta'
        void *h = dlopen(_dyld_get_image_name(i), RTLD_NOLOAD);
        void *meta = h ? dlsym(h, "_jet_elf_meta") : NULL;
        if (h) dlclose(h);
        if (!meta) continue;
        uint32_t magic = *(uint32_t*)meta; if (magic != 0x5054454a /*'JETP' LE*/) continue;
        uint32_t phnum = ((uint32_t*)meta)[1];
        const void *phdr = (const char*)meta + 16;
        struct jb_dl_phdr_info info = { (uint64_t)_dyld_get_image_vmaddr_slide(i), _dyld_get_image_name(i), phdr, (uint16_t)phnum };
        int r = cb(&info, sizeof info, data); if (r) return r;
    }
    return 0;
}

// property get: empty
JBX int jb___system_property_get(const char *name, char *value){ (void)name; if (value) value[0]=0; return 0; }

// stack chk
JBX void __stack_chk_fail(void){ jb_log(7,"stack","__stack_chk_fail"); abort(); }
JBX void * __memcpy_chk(void *d, const void *s, size_t n, size_t os){ (void)os; return memcpy(d,s,n); }
JBX void * __memmove_chk(void *d, const void *s, size_t n, size_t os){ (void)os; return memmove(d,s,n); }
JBX void * __memset_chk(void *d, int c, size_t n, size_t os){ (void)os; return memset(d,c,n); }
JBX size_t __strlen_chk(const char *s, size_t os){ (void)os; return strlen(s); }
JBX ssize_t __read_chk(int fd, void *b, size_t n, size_t os){ (void)os; return read(fd,b,n); }
JBX int __vsnprintf_chk(char *s, size_t n, int f, size_t os, const char *fmt, jb_aapcs_va *ap){
    (void)f;(void)os; jb_cur c; jb_cur_va(&c,ap); uint64_t slots[64]; jb_marshal_printf(fmt,&c,slots,64);
    va_list va; *(void**)&va=(void*)slots; return vsnprintf(s,n,fmt,va);
}

// cxa
JBX int __cxa_atexit(void (*f)(void*), void *a, void *d){ (void)d; return atexit((void(*)(void))f); /* leak arg */ }
JBX void __cxa_finalize(void *d){ (void)d; }
JBX int __cxa_thread_atexit_impl(void (*f)(void*), void *a, void *d){ (void)d;(void)a;(void)f; return 0; }
JBX size_t __ctype_get_mb_cur_max(void){ return 4; }
JBX int __sched_cpucount(size_t sz, const void *set){ (void)sz;(void)set; return (int)sysconf(_SC_NPROCESSORS_ONLN); }

// FD_*_chk against Darwin fd_set
JBX void __FD_SET_chk(int fd, fd_set *s, size_t sz){ (void)sz; FD_SET(fd,s); }
JBX void __FD_CLR_chk(int fd, fd_set *s, size_t sz){ (void)sz; FD_CLR(fd,s); }
JBX int  __FD_ISSET_chk(int fd, fd_set *s, size_t sz){ (void)sz; return FD_ISSET(fd,s); }

// sincos
JBX void jb_sincos(double x, double *s, double *c){ *s=sin(x); *c=cos(x); }
JBX void jb_sincosf(float x, float *s, float *c){ *s=sinf(x); *c=cosf(x); }
JBX void jb_sdallocx(void *p, size_t s, int f){ (void)s;(void)f; free(p); }

// signal number translate (partial)
int jb_sig_from_linux(int s){
    switch(s){ case 10: return SIGUSR1; case 12: return SIGUSR2; case 13: return SIGPIPE;
        case 17: return SIGCHLD; case 18: return SIGCONT; case 19: return SIGSTOP; case 20: return SIGTSTP; default: return s; }
}
int jb_sig_to_linux(int s){
    switch(s){ case SIGUSR1: return 10; case SIGUSR2: return 12; case SIGPIPE: return 13;
        case SIGCHLD: return 17; case SIGCONT: return 18; case SIGSTOP: return 19; case SIGTSTP: return 20; default: return s; }
}
