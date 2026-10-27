/* host_bionic.c - bionic/Linux host trap implementations for guest imports. */
#include "jet_internal.h"
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <time.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/utsname.h>
#include <poll.h>
#include <dirent.h>
#include <pthread.h>
#include <sched.h>
#include <semaphore.h>
#include <dlfcn.h>
#include <locale.h>
#include <signal.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <stdarg.h>
#include <ctype.h>
#include <wchar.h>
#include <wctype.h>
/* zlib: prefer real headers; fall back to stub if absent. */
#if defined(__has_include)
#  if __has_include(<zlib.h>)
#    include <zlib.h>
#    define JET_HAVE_ZLIB 1
#  elif __has_include("third_party/zlib.h")
#    include "third_party/zlib.h"
#    define JET_HAVE_ZLIB 1
#  else
#    include "zlib_stub.h"
#  endif
#else
#  include "zlib_stub.h"
#endif

/* Linux-only host headers — guest getauxval/prctl are emulated, never call host. */
#ifdef __linux__
#  include <sys/prctl.h>
#  include <sys/auxv.h>
#  include <sys/syscall.h>
#endif

/* ---- helpers to read Linux AAPCS64 va_list from guest memory ---- */
typedef struct {
    void *__stack;
    void *__gr_top;
    void *__vr_top;
    int   __gr_offs;
    int   __vr_offs;
} jet_va_list_aapcs64;

static uint64_t va_arg_gr(jet_va_list_aapcs64 *ap) {
    if (ap->__gr_offs < 0) {
        int offs = ap->__gr_offs;
        ap->__gr_offs += 8;
        return *(uint64_t *)((uint8_t *)ap->__gr_top + offs);
    }
    uint64_t v = *(uint64_t *)ap->__stack;
    ap->__stack = (uint8_t *)ap->__stack + 8;
    return v;
}

/* ---- errno ---- */
static uint64_t h___errno(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)ctx; (void)a; (void)u;
#if defined(__linux__)
    return H2G(__errno_location());
#elif defined(__APPLE__)
    return H2G(__error());
#else
    return H2G(&errno);
#endif
}

/* ---- logging ---- */
static uint64_t h_android_log_print(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)u;
    int prio = (int)a->x[0];
    const char *tag = (const char *)G2H(a->x[1]);
    const char *fmt = (const char *)G2H(a->x[2]);
    /* Limited: format with remaining regs as args (common pattern). */
    char buf[1024];
    snprintf(buf, sizeof buf, fmt,
             (void *)(uintptr_t)a->x[3], (void *)(uintptr_t)a->x[4],
             (void *)(uintptr_t)a->x[5], (void *)(uintptr_t)a->x[6],
             (void *)(uintptr_t)a->x[7]);
    jet_log(ctx, 2, "android_log(p=%d,%s): %s", prio, tag ? tag : "?", buf);
    return 0;
}
static uint64_t h_android_log_write(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)u;
    jet_log(ctx, 2, "android_log_write(p=%d,%s): %s",
            (int)a->x[0], (char *)G2H(a->x[1]), (char *)G2H(a->x[2]));
    return 0;
}
static uint64_t h_android_log_assert(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)u;
    jet_log(ctx, 0, "android_assert(%s): %s", (char*)G2H(a->x[0]), (char*)G2H(a->x[1]));
    jet_cpu *cpu = jet_cpu_current(ctx);
    if (cpu) { snprintf(cpu->err_msg, sizeof cpu->err_msg, "android_assert");
               cpu->err_code = 5; cpu->stop = 1; }
    return 0;
}

/* ---- memory ---- */
static uint64_t h_malloc(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(malloc((size_t)a->x[0])); }
static uint64_t h_calloc(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(calloc((size_t)a->x[0],(size_t)a->x[1])); }
static uint64_t h_realloc(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(realloc(G2H(a->x[0]),(size_t)a->x[1])); }
static uint64_t h_free(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; free(G2H(a->x[0])); return 0; }
static uint64_t h_posix_memalign(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    void *p=NULL; int r=posix_memalign(&p,(size_t)a->x[1],(size_t)a->x[2]);
    if (r==0) *(uint64_t*)G2H(a->x[0]) = H2G(p);
    return (uint64_t)(int64_t)r;
}
static uint64_t h_memcpy(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return H2G(memcpy(G2H(a->x[0]),G2H(a->x[1]),(size_t)a->x[2]));
}
static uint64_t h_memmove(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(memmove(G2H(a->x[0]),G2H(a->x[1]),(size_t)a->x[2])); }
static uint64_t h_memset(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(memset(G2H(a->x[0]),(int)a->x[1],(size_t)a->x[2])); }
static uint64_t h_memcmp(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)memcmp(G2H(a->x[0]),G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_memchr(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(memchr(G2H(a->x[0]),(int)a->x[1],(size_t)a->x[2])); }
static uint64_t h_strlen(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)strlen((char*)G2H(a->x[0])); }
static uint64_t h_strcpy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strcpy((char*)G2H(a->x[0]),(char*)G2H(a->x[1]))); }
static uint64_t h_strncpy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strncpy((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(size_t)a->x[2])); }
static uint64_t h_strcmp(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)strcmp((char*)G2H(a->x[0]),(char*)G2H(a->x[1])); }
static uint64_t h_strncmp(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)strncmp((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_strcasecmp(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)strcasecmp((char*)G2H(a->x[0]),(char*)G2H(a->x[1])); }
static uint64_t h_strchr(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strchr((char*)G2H(a->x[0]),(int)a->x[1])); }
static uint64_t h_strrchr(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strrchr((char*)G2H(a->x[0]),(int)a->x[1])); }
static uint64_t h_strstr(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strstr((char*)G2H(a->x[0]),(char*)G2H(a->x[1]))); }
static uint64_t h_strcat(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strcat((char*)G2H(a->x[0]),(char*)G2H(a->x[1]))); }
static uint64_t h_strncat(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strncat((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(size_t)a->x[2])); }
static uint64_t h_strdup_like(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strdup((char*)G2H(a->x[0]))); }
static uint64_t h_strtol(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *end=NULL; long r=strtol((char*)G2H(a->x[0]), &end, (int)a->x[2]);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = H2G(end);
    return (uint64_t)(int64_t)r;
}
static uint64_t h_strtoul(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *end=NULL; unsigned long r=strtoul((char*)G2H(a->x[0]), &end, (int)a->x[2]);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = H2G(end);
    return (uint64_t)r;
}
static uint64_t h_strtoll(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *end=NULL; long long r=strtoll((char*)G2H(a->x[0]), &end, (int)a->x[2]);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = H2G(end);
    return (uint64_t)(int64_t)r;
}
static uint64_t h_strtoull(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *end=NULL; unsigned long long r=strtoull((char*)G2H(a->x[0]), &end, (int)a->x[2]);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = H2G(end);
    return (uint64_t)r;
}
static uint64_t h_strtod(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *end=NULL; double r=strtod((char*)G2H(a->x[0]), &end);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = H2G(end);
    uint64_t bits; memcpy(&bits,&r,8); return bits;
}
static uint64_t h_atoi(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)atoi((char*)G2H(a->x[0])); }
static uint64_t h_strerror(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(strerror((int)a->x[0])); }
static uint64_t h_abort(jet_ctx *c, const jet_args *a, void *u){
    (void)a;(void)u;
    jet_cpu *cpu = jet_cpu_current(c);
    jet_log(c,0,"guest abort() pc=%llx lr=%llx x0=%llx x1=%llx sp=%llx last_trap=%s",
            (unsigned long long)(cpu?cpu->pc:0),
            (unsigned long long)(cpu?cpu->x[30]:0),
            (unsigned long long)(cpu?cpu->x[0]:0),
            (unsigned long long)(cpu?cpu->x[1]:0),
            (unsigned long long)(cpu?cpu->sp:0),
            c->last_trap_name[0]?c->last_trap_name:"-");
    if (cpu) {
        snprintf(cpu->err_msg, sizeof cpu->err_msg,
                 "abort() lr=%llx x0=%llx last=%s",
                 (unsigned long long)cpu->x[30],
                 (unsigned long long)cpu->x[0],
                 c->last_trap_name[0]?c->last_trap_name:"-");
        cpu->err_code = 5; cpu->stop = 1;
    }
    return 0;
}
static uint64_t h_exit_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    jet_log(c, 0, "guest exit(%d) — soft-stop (not killing host)", (int)a->x[0]);
    jet_cpu *cpu = pthread_getspecific(c->cpu_key);
    if (cpu) { cpu->stop = 1; cpu->err_code = 0; snprintf(c->last_error, sizeof c->last_error, "exit(%d)", (int)a->x[0]); }
    return 0;
}
static uint64_t h__exit(jet_ctx *c, const jet_args *a, void *u){
    return h_exit_fn(c, a, u);
}

/* snprintf / vsnprintf — Linux AAPCS64 va_list */
static uint64_t h_vsnprintf(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    char *buf = (char*)G2H(a->x[0]);
    size_t n = (size_t)a->x[1];
    const char *fmt = (const char*)G2H(a->x[2]);
    jet_va_list_aapcs64 *ap = (jet_va_list_aapcs64*)G2H(a->x[3]);
    /* Very limited formatter: walk fmt and pull GR args. Enough for many logs. */
    char out[4096]; size_t o=0;
    for (const char *p=fmt; *p && o+1<sizeof out; p++) {
        if (*p!='%'){ out[o++]=*p; continue; }
        p++;
        while (*p=='l'||*p=='z'||*p=='t'||*p=='h'||*p=='j'){ p++; }
        char tmp[128];
        switch (*p) {
        case '%': out[o++]='%'; break;
        case 's': { char *s=(char*)G2H(va_arg_gr(ap)); if(!s)s="(null)";
            size_t l=strlen(s); if(o+l>=sizeof out)l=sizeof out-o-1;
            memcpy(out+o,s,l); o+=l; break; }
        case 'd': case 'i': {
            long long v = (long long)(int64_t)va_arg_gr(ap);
            snprintf(tmp,sizeof tmp, "%lld", (long long)v);
            size_t l=strlen(tmp); memcpy(out+o,tmp,l); o+=l; break; }
        case 'u': case 'x': case 'X': case 'p': {
            unsigned long long v = va_arg_gr(ap);
            const char *f = (*p=='p')?"%p":(*p=='x')?"%llx":(*p=='X')?"%llX":"%llu";
            snprintf(tmp,sizeof tmp, f, v);
            size_t l=strlen(tmp); memcpy(out+o,tmp,l); o+=l; break; }
        case 'c': { out[o++]=(char)va_arg_gr(ap); break; }
        case 'f': case 'g': case 'e': {
            /* pull from VR — skip for now, print 0 */
            snprintf(tmp,sizeof tmp,"0"); size_t l=strlen(tmp); memcpy(out+o,tmp,l); o+=l;
            /* advance vr offs if possible */
            if (ap->__vr_offs < 0) ap->__vr_offs += 16;
            break; }
        default: out[o++]='%'; if(o<sizeof out-1) out[o++]=*p; break;
        }
    }
    out[o]=0;
    if (n){ size_t cpy=o<n?o:n-1; memcpy(buf,out,cpy); buf[cpy]=0; }
    return (uint64_t)o;
}
static uint64_t h_snprintf(jet_ctx *c, const jet_args *a, void *u){
    /* Build a fake va_list from remaining GPRs / stack per AAPCS64. */
    jet_cpu *cpu = jet_cpu_current(c);
    uint64_t gr_save[8];
    /* named args: x0=buf x1=n x2=fmt; varargs start at x3 */
    gr_save[0]=a->x[3]; gr_save[1]=a->x[4]; gr_save[2]=a->x[5]; gr_save[3]=a->x[6];
    gr_save[4]=a->x[7];
    /* remaining from stack */
    jet_va_list_aapcs64 ap = {
        .__stack = (void*)(uintptr_t)(cpu ? cpu->sp : 0), /* not quite: named used 3 regs, so stack starts at caller's SP;
              but our trap receives SP at call entry which is correct for stack args. */
        .__gr_top = (uint8_t*)gr_save + 5*8,
        .__vr_top = NULL,
        .__gr_offs = -(int)(5*8),
        .__vr_offs = 0
    };
    jet_args a2 = *a; a2.x[3] = H2G(&ap);
    (void)u;
    return h_vsnprintf(c, &a2, NULL);
}
static uint64_t h_sprintf(jet_ctx *c, const jet_args *a, void *u){
    jet_args a2 = *a;
    a2.x[1] = 0x7fffffff; /* huge n */
    /* shift: sprintf(buf,fmt,...) → vsnprintf(buf,huge,fmt,ap) but args at x1.. */
    jet_args a3 = {0};
    a3.x[0]=a->x[0]; a3.x[1]=0x7fffffff; a3.x[2]=a->x[1];
    a3.x[3]=a->x[2]; a3.x[4]=a->x[3]; a3.x[5]=a->x[4]; a3.x[6]=a->x[5]; a3.x[7]=a->x[6];
    a3.stack=a->stack;
    (void)u;
    return h_snprintf(c, &a3, NULL);
}

static uint64_t h_printf(jet_ctx *c, const jet_args *a, void *u){
    char buf[2048];
    jet_args a2={0};
    a2.x[0]=H2G(buf); a2.x[1]=sizeof buf; a2.x[2]=a->x[0];
    a2.x[3]=a->x[1]; a2.x[4]=a->x[2]; a2.x[5]=a->x[3]; a2.x[6]=a->x[4]; a2.x[7]=a->x[5];
    a2.stack=a->stack;
    uint64_t n = h_snprintf(c,&a2,u);
    fputs(buf, stdout);
    return n;
}

/* ---- mmap / sys ---- */
static uint64_t h_mmap(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    void *p = mmap(G2H(a->x[0]), (size_t)a->x[1], (int)a->x[2], (int)a->x[3], (int)a->x[4], (off_t)a->x[5]);
    return (p==MAP_FAILED) ? (uint64_t)(int64_t)-1 : H2G(p);
}
static uint64_t h_munmap(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)munmap(G2H(a->x[0]),(size_t)a->x[1]);
}
static uint64_t h_mprotect(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int prot = (int)a->x[2] & ~PROT_EXEC; /* never allow EXEC */
    return (uint64_t)(int64_t)mprotect(G2H(a->x[0]),(size_t)a->x[1],prot);
}
static uint64_t h_getauxval(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* Emulated guest auxv — do NOT call host getauxval. */
    unsigned long type = (unsigned long)a->x[0];
    switch (type) {
    case 16: return c->cfg.hwcap;          /* AT_HWCAP */
    case 26: return c->cfg.hwcap2;         /* AT_HWCAP2 */
    case 6:  return (uint64_t)sysconf(_SC_PAGESIZE); /* AT_PAGESZ */
    case 11: return (uint64_t)getuid();    /* AT_UID */
    case 12: return (uint64_t)geteuid();   /* AT_EUID */
    case 13: return (uint64_t)getgid();    /* AT_GID */
    case 14: return (uint64_t)getegid();   /* AT_EGID */
    case 23: return 0;                     /* AT_SECURE */
    case 25: {                             /* AT_RANDOM */
        static uint8_t rnd[16];
        static int once;
        if (!once) { for (int i=0;i<16;i++) rnd[i]=(uint8_t)(rand()&0xff); once=1; }
        return H2G(rnd);
    }
    case 31: return H2G("/system/bin/app_process64"); /* AT_EXECFN */
    default:
        jet_log(c, 2, "getauxval(%lu) -> 0", type);
        return 0;
    }
}
static uint64_t h_sysconf(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)sysconf((int)a->x[0]);
}
static uint64_t h_getpagesize(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)a;(void)u; return (uint64_t)sysconf(_SC_PAGESIZE);
}
static uint64_t h_clock_gettime(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)clock_gettime((clockid_t)a->x[0], (struct timespec*)G2H(a->x[1]));
}
static uint64_t h_gettimeofday(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)gettimeofday((struct timeval*)G2H(a->x[0]), NULL);
}
static uint64_t h_nanosleep(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)nanosleep((struct timespec*)G2H(a->x[0]), (struct timespec*)G2H(a->x[1]));
}
static uint64_t h_usleep(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)usleep((useconds_t)a->x[0]);
}
static uint64_t h_getpid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)getpid(); }
static uint64_t h_gettid(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)a;(void)u;
#if defined(__linux__) && defined(SYS_gettid)
    return (uint64_t)syscall(SYS_gettid);
#elif defined(__APPLE__)
    uint64_t tid = 0; pthread_threadid_np(NULL, &tid); return tid;
#else
    return (uint64_t)(uintptr_t)pthread_self();
#endif
}
static uint64_t h_getuid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)getuid(); }
static uint64_t h_geteuid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)geteuid(); }
static uint64_t h_uname(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)uname((struct utsname*)G2H(a->x[0])); }
static uint64_t h_prctl(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* Emulate Linux prctl; do not require host <sys/prctl.h> on Darwin. */
    int op = (int)a->x[0];
    if (op == 15 /*PR_SET_NAME*/) {
        const char *name = (const char *)G2H(a->x[1]);
#if defined(__APPLE__)
        if (name) (void)pthread_setname_np(name);
#elif defined(__linux__)
        if (name) (void)prctl(15, (unsigned long)name, 0, 0, 0);
#else
        (void)name;
#endif
        return 0;
    }
    if (op == 16 /*PR_GET_NAME*/) {
        char *buf = (char *)G2H(a->x[1]);
        if (buf) {
#if defined(__APPLE__)
            pthread_getname_np(pthread_self(), buf, 16);
#else
            buf[0] = '\0';
#endif
        }
        return 0;
    }
    jet_log(c, 2, "prctl(%d) stubbed", op);
    return 0;
}

/* Fake FD allocator so socket/epoll/eventfd never collide with stdin/out/err.
 * Network I/O is intentionally stubbed: connect fails, epoll_wait/poll idle. */
static int jet_fake_fd_next = 100;
static uint8_t jet_fake_fd_map[4096]; /* fd -> 1 if fake */
static int jet_fake_fd_alloc(void) {
    int fd = jet_fake_fd_next++;
    if (jet_fake_fd_next >= 4000) jet_fake_fd_next = 100;
    if (fd >= 0 && fd < 4096) jet_fake_fd_map[fd] = 1;
    return fd;
}
static int jet_is_fake_fd(int fd) {
    return fd >= 0 && fd < 4096 && jet_fake_fd_map[fd];
}
static uint64_t h_socket(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a;
    int fd = jet_fake_fd_alloc();
    jet_log(c, 3, "socket() -> fake fd %d", fd);
    return (uint64_t)(int64_t)fd;
}
static uint64_t h_epoll_create(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a;
    int fd = jet_fake_fd_alloc();
    jet_log(c, 3, "epoll_create -> fake fd %d", fd);
    return (uint64_t)(int64_t)fd;
}
static uint64_t h_epoll_create1(jet_ctx *c, const jet_args *a, void *u) {
    return h_epoll_create(c, a, u);
}
static uint64_t h_eventfd(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a;
    return (uint64_t)(int64_t)jet_fake_fd_alloc();
}
static uint64_t h_pipe_fn(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int *p = a->x[0] ? (int*)G2H(a->x[0]) : NULL;
    if (p) { p[0] = jet_fake_fd_alloc(); p[1] = jet_fake_fd_alloc(); }
    return 0;
}
static uint64_t h_connect(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a;
    jet_log(c, 2, "connect(fd=%d) stub → EINPROGRESS", (int)a->x[0]);
    errno = EINPROGRESS;
    return (uint64_t)(int64_t)-1;
}
static uint64_t h_bind_fn(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u;(void)a; return 0; }
static uint64_t h_listen_fn(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u;(void)a; return 0; }
static uint64_t h_setsockopt(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u;(void)a; return 0; }
static uint64_t h_getsockopt(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u;(void)a; return 0; }
static uint64_t h_epoll_ctl(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u;(void)a; return 0; }
static uint64_t h_epoll_wait(jet_ctx *c, const jet_args *a, void *u){
    (void)u; (void)a; (void)c;
    /* Idle briefly so network thread doesn't hot-spin the host. */
    struct timespec ts = {0, 50 * 1000 * 1000}; /* 50ms */
    nanosleep(&ts, NULL);
    return 0; /* no events */
}
static uint64_t h_poll_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)u; (void)c;
    int timeout = (int)a->x[2];
    if (timeout != 0) {
        struct timespec ts = {0, 20 * 1000 * 1000};
        nanosleep(&ts, NULL);
    }
    return 0;
}
static uint64_t h_getaddrinfo(jet_ctx *c, const jet_args *a, void *u){
    (void)u; (void)a;
    jet_log(c, 2, "getaddrinfo stub → EAI_FAIL");
    return (uint64_t)(int64_t)-1; /* EAI_FAIL-ish */
}

/* file IO */
static uint64_t h_open(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)open((char*)G2H(a->x[0]),(int)a->x[1],(mode_t)a->x[2]); }
static uint64_t h_close(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd=(int)a->x[0];
    if (jet_is_fake_fd(fd)) { jet_fake_fd_map[fd]=0; return 0; }
    return (uint64_t)(int64_t)close(fd);
}
static uint64_t h_read(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; if (jet_is_fake_fd((int)a->x[0])) { errno=EAGAIN; return (uint64_t)(int64_t)-1; } return (uint64_t)(int64_t)read((int)a->x[0],G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_write(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; if (jet_is_fake_fd((int)a->x[0])) return a->x[2]; return (uint64_t)(int64_t)write((int)a->x[0],G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_lseek(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)lseek((int)a->x[0],(off_t)a->x[1],(int)a->x[2]); }
static uint64_t h_fstat(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)fstat((int)a->x[0],(struct stat*)G2H(a->x[1])); }
static uint64_t h_stat_fn(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)stat((char*)G2H(a->x[0]),(struct stat*)G2H(a->x[1])); }
static uint64_t h_access(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)access((char*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_readlink(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)readlink((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_getenv(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(getenv((char*)G2H(a->x[0]))); }
static uint64_t h_setenv(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)setenv((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(int)a->x[2]); }

/* pthread — with trampoline for start_routine */
struct thr_arg { jet_ctx *ctx; uint64_t guest_fn; uint64_t guest_arg; };
static void *thr_entry(void *p) {
    struct thr_arg *ta = p;
    /* Network/worker threads (ConnectionsManager::ThreadProc etc.) exercise a
     * long tail of epoll/DNS/TLS ASIMD. For harness stability we park the host
     * thread and do not enter the guest — native_init only needs create→ok.
     * Set JET_RUN_GUEST_THREADS=1 to run guest start_routine for real. */
    const char *run = getenv("JET_RUN_GUEST_THREADS");
    if (!run || run[0] != '1') {
        jet_log(ta->ctx, 1,
                "pthread_create: parking host thread (guest_fn=%llx) — network stub; "
                "export JET_RUN_GUEST_THREADS=1 to run",
                (unsigned long long)ta->guest_fn);
        for (;;) {
            struct timespec ts = {60, 0};
            nanosleep(&ts, NULL);
        }
    }
    jet_cpu *cpu = jet_cpu_create(ta->ctx);
    pthread_setspecific(ta->ctx->cpu_key, cpu);
    jet_args a = {0}; a.x[0] = ta->guest_arg;
    uint64_t r = 0;
    jet_native_call(ta->ctx, ta->guest_fn, &a, &r, NULL);
    pthread_setspecific(ta->ctx->cpu_key, NULL);
    jet_cpu_destroy(ta->ctx, cpu);
    free(ta);
    return (void*)(uintptr_t)r;
}
static uint64_t h_pthread_create(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    pthread_t *th = (pthread_t*)G2H(a->x[0]);
    const pthread_attr_t *attr = a->x[1] ? (pthread_attr_t*)G2H(a->x[1]) : NULL;
    struct thr_arg *ta = malloc(sizeof(*ta));
    ta->ctx = c; ta->guest_fn = a->x[2]; ta->guest_arg = a->x[3];
    return (uint64_t)(int64_t)pthread_create(th, attr, thr_entry, ta);
}
static uint64_t h_pthread_join(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; void *r=NULL; int rc=pthread_join((pthread_t)a->x[0], &r);
    if (a->x[1]) *(uint64_t*)G2H(a->x[1]) = (uint64_t)(uintptr_t)r;
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_pthread_self(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)pthread_self(); }
static uint64_t h_pthread_detach(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_detach((pthread_t)a->x[0]); }
static uint64_t h_pthread_mutex_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutex_init((pthread_mutex_t*)G2H(a->x[0]), a->x[1]?(pthread_mutexattr_t*)G2H(a->x[1]):NULL); }
static uint64_t h_pthread_mutex_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutex_destroy((pthread_mutex_t*)G2H(a->x[0])); }
static uint64_t h_pthread_mutex_lock(jet_ctx *c, const jet_args *a, void *u){ (void)u; if (!a->x[0]) { jet_log(c,1,"pthread_mutex_lock(NULL)"); return (uint64_t)(int64_t)-1; } return (uint64_t)(int64_t)pthread_mutex_lock((pthread_mutex_t*)G2H(a->x[0])); }
static uint64_t h_pthread_mutex_unlock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutex_unlock((pthread_mutex_t*)G2H(a->x[0])); }
static uint64_t h_pthread_mutex_trylock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutex_trylock((pthread_mutex_t*)G2H(a->x[0])); }
static uint64_t h_pthread_cond_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_cond_init((pthread_cond_t*)G2H(a->x[0]), a->x[1]?(pthread_condattr_t*)G2H(a->x[1]):NULL); }
static uint64_t h_pthread_cond_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_cond_destroy((pthread_cond_t*)G2H(a->x[0])); }
static uint64_t h_pthread_cond_wait(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_cond_wait((pthread_cond_t*)G2H(a->x[0]),(pthread_mutex_t*)G2H(a->x[1])); }
static uint64_t h_pthread_cond_signal(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_cond_signal((pthread_cond_t*)G2H(a->x[0])); }
static uint64_t h_pthread_cond_broadcast(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_cond_broadcast((pthread_cond_t*)G2H(a->x[0])); }
static uint64_t h_pthread_once(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* pthread_once_t *once, void (*init)(void) — init is guest */
    static pthread_mutex_t once_mu = PTHREAD_MUTEX_INITIALIZER;
    pthread_mutex_lock(&once_mu);
    volatile int *flag = (volatile int*)G2H(a->x[0]);
    if (*flag == 0) {
        jet_args aa={0};
        jet_native_call(c, a->x[1], &aa, NULL, NULL);
        *flag = 1;
    }
    pthread_mutex_unlock(&once_mu);
    return 0;
}
static uint64_t h_pthread_key_create(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* destructor may be guest — wrap */
    pthread_key_t *k = (pthread_key_t*)G2H(a->x[0]);
    return (uint64_t)(int64_t)pthread_key_create(k, NULL); /* TODO: trampoline dtor */
}
static uint64_t h_pthread_getspecific(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(uintptr_t)pthread_getspecific((pthread_key_t)a->x[0]); }
static uint64_t h_pthread_setspecific(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_setspecific((pthread_key_t)a->x[0], G2H(a->x[1])); }
static uint64_t h_pthread_attr_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_attr_init((pthread_attr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_attr_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_attr_destroy((pthread_attr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_attr_setdetachstate(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_attr_setdetachstate((pthread_attr_t*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_pthread_attr_setstacksize(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_attr_setstacksize((pthread_attr_t*)G2H(a->x[0]),(size_t)a->x[1]); }
static uint64_t h_pthread_mutexattr_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutexattr_init((pthread_mutexattr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_mutexattr_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutexattr_destroy((pthread_mutexattr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_mutexattr_settype(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutexattr_settype((pthread_mutexattr_t*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_pthread_condattr_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_condattr_init((pthread_condattr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_condattr_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_condattr_destroy((pthread_condattr_t*)G2H(a->x[0])); }
static uint64_t h_pthread_condattr_setclock(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__APPLE__)
    (void)a; return 0; /* Darwin: no setclock; cond uses realtime. */
#else
    return (uint64_t)(int64_t)pthread_condattr_setclock((pthread_condattr_t*)G2H(a->x[0]),(clockid_t)a->x[1]);
#endif
}
static uint64_t h_pthread_setname_np(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__APPLE__)
    const char *name = (const char *)G2H(a->x[1] ? a->x[1] : a->x[0]);
    return name ? (uint64_t)(int64_t)pthread_setname_np(name) : 0;
#else
    return (uint64_t)(int64_t)pthread_setname_np((pthread_t)a->x[0], (char*)G2H(a->x[1]));
#endif
}
static uint64_t h_pthread_key_delete(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_key_delete((pthread_key_t)a->x[0]); }
static uint64_t h_pthread_equal(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)pthread_equal((pthread_t)a->x[0],(pthread_t)a->x[1]); }
static uint64_t h_pthread_rwlock_init(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_rwlock_init((pthread_rwlock_t*)G2H(a->x[0]),NULL); }
static uint64_t h_pthread_rwlock_destroy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_rwlock_destroy((pthread_rwlock_t*)G2H(a->x[0])); }
static uint64_t h_pthread_rwlock_rdlock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_rwlock_rdlock((pthread_rwlock_t*)G2H(a->x[0])); }
static uint64_t h_pthread_rwlock_wrlock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_rwlock_wrlock((pthread_rwlock_t*)G2H(a->x[0])); }
static uint64_t h_pthread_rwlock_unlock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_rwlock_unlock((pthread_rwlock_t*)G2H(a->x[0])); }

/* qsort / bsearch with trampoline comparator */
static jet_ctx *g_qsort_ctx;
static uint64_t g_qsort_cmp;
static int qsort_cmp_wrap(const void *a, const void *b) {
    jet_args aa={0}; aa.x[0]=H2G(a); aa.x[1]=H2G(b);
    uint64_t r=0; jet_native_call(g_qsort_ctx, g_qsort_cmp, &aa, &r, NULL);
    return (int)(int64_t)r;
}
static uint64_t h_qsort(jet_ctx *c, const jet_args *a, void *u){
    (void)u; g_qsort_ctx=c; g_qsort_cmp=a->x[3];
    qsort(G2H(a->x[0]), (size_t)a->x[1], (size_t)a->x[2], qsort_cmp_wrap);
    return 0;
}
static uint64_t h_bsearch(jet_ctx *c, const jet_args *a, void *u){
    (void)u; g_qsort_ctx=c; g_qsort_cmp=a->x[4];
    return H2G(bsearch(G2H(a->x[0]), G2H(a->x[1]), (size_t)a->x[2], (size_t)a->x[3], qsort_cmp_wrap));
}

/* math */
#define HM1(name) static uint64_t h_##name(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; double x; memcpy(&x,&a->d[0],8); double r=name(x); uint64_t b; memcpy(&b,&r,8); return b; }
#define HM2(name) static uint64_t h_##name(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; double x,y; memcpy(&x,&a->d[0],8); memcpy(&y,&a->d[1],8); double r=name(x,y); uint64_t b; memcpy(&b,&r,8); return b; }
HM1(sin) HM1(cos) HM1(tan) HM1(asin) HM1(acos) HM1(atan) HM2(atan2) HM1(sinh) HM1(tanh)
HM1(exp) HM1(exp2) HM1(expm1) HM1(log) HM1(log10) HM1(log2) HM1(log1p) HM2(pow) HM1(sqrt) HM1(cbrt)
HM1(fabs) HM1(floor) HM1(ceil) HM1(round) HM1(trunc) HM2(fmod) HM2(hypot)
static uint64_t h_sincos(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; double x; memcpy(&x,&a->d[0],8);
    double s=sin(x), co=cos(x);
    *(double*)G2H(a->x[0])=s; *(double*)G2H(a->x[1])=co; return 0;
}
/* float variants return in s0 — use F32 flag */
#define HF1(name,fn) static uint64_t h_##name(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; float xf; memcpy(&xf,&a->d[0],4); float r=fn(xf); uint32_t rb; memcpy(&rb,&r,4); return rb; }
HF1(sinf,sinf) HF1(cosf,cosf) HF1(atanf,atanf) HF1(expf,expf) HF1(exp2f,exp2f) HF1(logf,logf) HF1(log10f,log10f) HF1(log2f,log2f) HF1(tanhf,tanhf) HF1(fabsf,fabsf)
static uint64_t h_powf(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; float x,y; memcpy(&x,&a->d[0],4); memcpy(&y,&a->d[1],4); float r=powf(x,y); uint32_t b; memcpy(&b,&r,4); return b; }
static uint64_t h_atan2f(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; float x,y; memcpy(&x,&a->d[0],4); memcpy(&y,&a->d[1],4); float r=atan2f(x,y); uint32_t b; memcpy(&b,&r,4); return b; }
static uint64_t h_fmodf(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; float x,y; memcpy(&x,&a->d[0],4); memcpy(&y,&a->d[1],4); float r=fmodf(x,y); uint32_t b; memcpy(&b,&r,4); return b; }
static uint64_t h_sincosf(jet_ctx *c,const jet_args *a,void *u){
    (void)c;(void)u; float x; memcpy(&x,&a->d[0],4);
    float s=sinf(x), co=cosf(x);
    *(float*)G2H(a->x[0])=s; *(float*)G2H(a->x[1])=co; return 0;
}

/* zlib */
static uint64_t h_inflateInit2_(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)inflateInit2_((z_stream*)G2H(a->x[0]),(int)a->x[1],(char*)G2H(a->x[2]),(int)a->x[3]); }
static uint64_t h_inflate(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)inflate((z_stream*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_inflateEnd(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)inflateEnd((z_stream*)G2H(a->x[0])); }
static uint64_t h_deflateInit2_(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)deflateInit2_((z_stream*)G2H(a->x[0]),(int)a->x[1],(int)a->x[2],(int)a->x[3],(int)a->x[4],(int)a->x[5],(char*)G2H(a->stack?a->stack[0]:0),(int)(a->stack?a->stack[1]:0)); }
static uint64_t h_deflate(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)deflate((z_stream*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_deflateEnd(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)deflateEnd((z_stream*)G2H(a->x[0])); }

/* dl* */
static uint64_t h_dlopen(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return H2G(dlopen((char*)G2H(a->x[0]),(int)a->x[1])); }
static uint64_t h_dlsym(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return H2G(dlsym(G2H(a->x[0]),(char*)G2H(a->x[1]))); }
static uint64_t h_dlclose(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)dlclose(G2H(a->x[0])); }
static uint64_t h_dlerror(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return H2G(dlerror()); }
static uint64_t h_dladdr(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)dladdr(G2H(a->x[0]),(Dl_info*)G2H(a->x[1])); }

/* stubs for GLES / EGL / OpenSLES / android / jnigraphics — return 0 / no-op */


/* Emulated getcwd: returns ctx->guest_cwd. buf=NULL allocates (bionic/glibc). */
static uint64_t h_getcwd(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    const char *cwd = c->guest_cwd[0] ? c->guest_cwd : "/data/data/app";
    size_t need = strlen(cwd) + 1;
    uint64_t buf_g = a->x[0];
    size_t size = (size_t)a->x[1];
    char *buf;
    if (!buf_g) {
        /* POSIX/bionic: allocate. size==0 → allocate need; else allocate size. */
        size_t alloc = size ? size : need;
        if (alloc < need) { errno = ERANGE; return 0; }
        buf = malloc(alloc);
        if (!buf) { errno = ENOMEM; return 0; }
        memcpy(buf, cwd, need);
        if (alloc > need) memset(buf + need, 0, alloc - need);
        return H2G(buf);
    }
    buf = (char*)G2H(buf_g);
    if (size == 0) { errno = EINVAL; return 0; }
    if (size < need) { errno = ERANGE; return 0; }
    memcpy(buf, cwd, need);
    return buf_g;
}

uint64_t jet_host_stub0(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return 0; }
static uint64_t h_stub0(jet_ctx *c,const jet_args *a,void *u){ return jet_host_stub0(c,a,u); }

/* bionic syscall(number, ...): Linux AArch64 numbers in x0, args in x1.. */
static uint64_t h_syscall(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    jet_cpu *cpu = jet_cpu_current(c);
    int64_t r = jet_emulate_linux_syscall(c, cpu, a->x[0],
                                          a->x[1], a->x[2], a->x[3],
                                          a->x[4], a->x[5], a->x[6]);
    if (r < 0 && r >= -4095) {
        /* Match glibc/bionic: set errno and return -1. */
        errno = (int)(-r);
        return (uint64_t)(int64_t)-1;
    }
    return (uint64_t)r;
}
static uint64_t h_fcntl(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    return (uint64_t)(int64_t)fcntl((int)a->x[0], (int)a->x[1], a->x[2]);
}
static uint64_t h_ioctl(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    return (uint64_t)(int64_t)ioctl((int)a->x[0], (unsigned long)a->x[1], (void*)G2H(a->x[2]));
}
static uint64_t h_stub1(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return 1; }
static uint64_t h_system_property_get(jet_ctx *c,const jet_args *a,void *u){
    (void)c;(void)u;
    const char *name=(char*)G2H(a->x[0]); char *val=(char*)G2H(a->x[1]);
    #ifndef PROP_VALUE_MAX
#define PROP_VALUE_MAX 92
#endif
    const char *v="";
    if (!strcmp(name,"ro.build.version.sdk")) v="33";
    else if (!strcmp(name,"ro.product.cpu.abi")) v="arm64-v8a";
    else if (!strcmp(name,"ro.build.version.release")) v="13";
    size_t n=strlen(v); if(n>PROP_VALUE_MAX-1) n=PROP_VALUE_MAX-1;
    memcpy(val,v,n); val[n]=0; return (uint64_t)n;
}

/* ctype / wchar passthrough macros */
#define HC1(name) static uint64_t h_##name(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)name((int)a->x[0]); }
HC1(isalnum) HC1(isalpha) HC1(isdigit) HC1(islower) HC1(isspace) HC1(isupper) HC1(isxdigit) HC1(isascii) HC1(isprint)
HC1(tolower) HC1(toupper)
HC1(iswalpha) HC1(iswdigit) HC1(iswlower) HC1(iswspace) HC1(iswupper) HC1(iswxdigit) HC1(iswprint) HC1(iswpunct) HC1(iswblank) HC1(iswcntrl)
static uint64_t h_towlower(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)towlower((wint_t)a->x[0]); }
static uint64_t h_towupper(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)towupper((wint_t)a->x[0]); }

/* stack chk */
uintptr_t jet_stack_chk_guard = 0x12345678abcdefull;
static uint64_t h_stack_chk_fail(jet_ctx *c,const jet_args *a,void *u){
    (void)a;(void)u;
    jet_cpu *cpu = jet_cpu_current(c);
    jet_log(c,0,"__stack_chk_fail pc=%llx lr=%llx",
            (unsigned long long)(cpu?cpu->pc:0),
            (unsigned long long)(cpu?cpu->x[30]:0));
    if (cpu) {
        snprintf(cpu->err_msg, sizeof cpu->err_msg, "__stack_chk_fail");
        cpu->err_code = 4; cpu->stop = 1;
    }
    return 0;
}

/* __cxa_atexit / finalize */
/* __cxa_atexit(dtor, arg, dso_handle) — record; we don't run dtors yet. */
#define JET_ATEXIT_MAX 256
static struct { uint64_t fn, arg, dso; } jet_atexit_list[JET_ATEXIT_MAX];
static int jet_atexit_n;
static uint64_t h_cxa_atexit(jet_ctx *c,const jet_args *a,void *u){
    (void)u;
    if (jet_atexit_n < JET_ATEXIT_MAX) {
        jet_atexit_list[jet_atexit_n].fn = a->x[0];
        jet_atexit_list[jet_atexit_n].arg = a->x[1];
        jet_atexit_list[jet_atexit_n].dso = a->x[2];
        jet_atexit_n++;
    }
    jet_log(c, 3, "__cxa_atexit(fn=%llx,arg=%llx,dso=%llx) n=%d",
            (unsigned long long)a->x[0], (unsigned long long)a->x[1],
            (unsigned long long)a->x[2], jet_atexit_n);
    return 0;
}
static uint64_t h_cxa_finalize(jet_ctx *c,const jet_args *a,void *u){
    (void)c;(void)a;(void)u; return 0;
}

/* FILE* stubs — expose host stdin/stdout/stderr as __sF-like */
static FILE *jet_sf[3];

static void reg(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host(ctx,n,f,NULL); }
static void regf64(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host_ex(ctx,n,f,NULL,JET_HOST_RET_F64); }
static void regf32(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host_ex(ctx,n,f,NULL,JET_HOST_RET_F32); }


/* Forward decls: guest signal emulation (defs later; never touch host). */
static uint64_t h_guest_sigemptyset(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigfillset(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigaddset(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigismember(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigaction(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_signal(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigaltstack(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_sigprocmask(jet_ctx *c, const jet_args *a, void *u);
static uint64_t h_guest_pthread_sigmask(jet_ctx *c, const jet_args *a, void *u);

int jet_host_install_bionic(jet_ctx *ctx) {
    jet_sf[0]=stdin; jet_sf[1]=stdout; jet_sf[2]=stderr;
    jet_native_register_data(ctx, "__sF", jet_sf);
    jet_native_register_data(ctx, "__stack_chk_guard", &jet_stack_chk_guard);

    reg(ctx,"__errno",h___errno);
    reg(ctx,"__android_log_print",h_android_log_print);
    reg(ctx,"__android_log_vprint",h_android_log_print);
    reg(ctx,"__android_log_write",h_android_log_write);
    reg(ctx,"__android_log_assert",h_android_log_assert);
    reg(ctx,"__system_property_get",h_system_property_get);
    reg(ctx,"__stack_chk_fail",h_stack_chk_fail);
    reg(ctx,"__cxa_atexit",h_cxa_atexit);
    reg(ctx,"__cxa_finalize",h_cxa_finalize);
    reg(ctx,"__cxa_thread_atexit_impl",h_cxa_atexit);

    reg(ctx,"malloc",h_malloc); reg(ctx,"calloc",h_calloc); reg(ctx,"realloc",h_realloc);
    reg(ctx,"free",h_free); reg(ctx,"posix_memalign",h_posix_memalign);
    reg(ctx,"sdallocx",h_free);
    reg(ctx,"memcpy",h_memcpy); reg(ctx,"memmove",h_memmove); reg(ctx,"memset",h_memset);
    reg(ctx,"memcmp",h_memcmp); reg(ctx,"memchr",h_memchr);
    reg(ctx,"__memcpy_chk",h_memcpy); reg(ctx,"__memmove_chk",h_memmove); reg(ctx,"__memset_chk",h_memset);
    reg(ctx,"strlen",h_strlen); reg(ctx,"__strlen_chk",h_strlen);
    reg(ctx,"strcpy",h_strcpy); reg(ctx,"strncpy",h_strncpy);
    reg(ctx,"strcmp",h_strcmp); reg(ctx,"strncmp",h_strncmp); reg(ctx,"strcasecmp",h_strcasecmp);
    reg(ctx,"strchr",h_strchr); reg(ctx,"strrchr",h_strrchr); reg(ctx,"strstr",h_strstr);
    reg(ctx,"strcat",h_strcat); reg(ctx,"strncat",h_strncat);
    reg(ctx,"strtol",h_strtol); reg(ctx,"strtoul",h_strtoul);
    reg(ctx,"strtoll",h_strtoll); reg(ctx,"strtoull",h_strtoull);
    regf64(ctx,"strtod",h_strtod);
    reg(ctx,"atoi",h_atoi); reg(ctx,"strerror",h_strerror);
    reg(ctx,"abort",h_abort); reg(ctx,"exit",h_exit_fn); reg(ctx,"_exit",h__exit);
    reg(ctx,"snprintf",h_snprintf); reg(ctx,"vsnprintf",h_vsnprintf);
    reg(ctx,"__vsnprintf_chk",h_vsnprintf);
    reg(ctx,"sprintf",h_sprintf); reg(ctx,"printf",h_printf);

    reg(ctx,"mmap",h_mmap); reg(ctx,"munmap",h_munmap); reg(ctx,"mprotect",h_mprotect);
    reg(ctx,"mremap",h_stub0);
    reg(ctx,"getauxval",h_getauxval); reg(ctx,"sysconf",h_sysconf); reg(ctx,"getpagesize",h_getpagesize);
    reg(ctx,"clock_gettime",h_clock_gettime); reg(ctx,"gettimeofday",h_gettimeofday);
    reg(ctx,"nanosleep",h_nanosleep); reg(ctx,"usleep",h_usleep); reg(ctx,"sleep",h_stub0);
    reg(ctx,"getpid",h_getpid); reg(ctx,"gettid",h_gettid);
    reg(ctx,"getuid",h_getuid); reg(ctx,"geteuid",h_geteuid); reg(ctx,"getgid",h_getuid); reg(ctx,"getegid",h_geteuid);
    reg(ctx,"uname",h_uname); reg(ctx,"prctl",h_prctl);

    reg(ctx,"open",h_open); reg(ctx,"__open_2",h_open); reg(ctx,"close",h_close);
    reg(ctx,"read",h_read); reg(ctx,"__read_chk",h_read); reg(ctx,"write",h_write);
    reg(ctx,"lseek",h_lseek); reg(ctx,"lseek64",h_lseek);
    reg(ctx,"fstat",h_fstat); reg(ctx,"stat",h_stat_fn); reg(ctx,"lstat",h_stat_fn);
    reg(ctx,"access",h_access); reg(ctx,"readlink",h_readlink);
    reg(ctx,"getenv",h_getenv); reg(ctx,"setenv",h_setenv);

    reg(ctx,"pthread_create",h_pthread_create); reg(ctx,"pthread_join",h_pthread_join);
    reg(ctx,"pthread_self",h_pthread_self); reg(ctx,"pthread_detach",h_pthread_detach);
    reg(ctx,"pthread_equal",h_pthread_equal);
    reg(ctx,"pthread_mutex_init",h_pthread_mutex_init);
    reg(ctx,"pthread_mutex_destroy",h_pthread_mutex_destroy);
    reg(ctx,"pthread_mutex_lock",h_pthread_mutex_lock);
    reg(ctx,"pthread_mutex_unlock",h_pthread_mutex_unlock);
    reg(ctx,"pthread_mutex_trylock",h_pthread_mutex_trylock);
    reg(ctx,"pthread_cond_init",h_pthread_cond_init);
    reg(ctx,"pthread_cond_destroy",h_pthread_cond_destroy);
    reg(ctx,"pthread_cond_wait",h_pthread_cond_wait);
    reg(ctx,"pthread_cond_timedwait",h_pthread_cond_wait);
    reg(ctx,"pthread_cond_signal",h_pthread_cond_signal);
    reg(ctx,"pthread_cond_broadcast",h_pthread_cond_broadcast);
    reg(ctx,"pthread_once",h_pthread_once);
    reg(ctx,"pthread_key_create",h_pthread_key_create);
    reg(ctx,"pthread_key_delete",h_pthread_key_delete);
    reg(ctx,"pthread_getspecific",h_pthread_getspecific);
    reg(ctx,"pthread_setspecific",h_pthread_setspecific);
    reg(ctx,"pthread_attr_init",h_pthread_attr_init);
    reg(ctx,"pthread_attr_destroy",h_pthread_attr_destroy);
    reg(ctx,"pthread_attr_setdetachstate",h_pthread_attr_setdetachstate);
    reg(ctx,"pthread_attr_setstacksize",h_pthread_attr_setstacksize);
    reg(ctx,"pthread_mutexattr_init",h_pthread_mutexattr_init);
    reg(ctx,"pthread_mutexattr_destroy",h_pthread_mutexattr_destroy);
    reg(ctx,"pthread_mutexattr_settype",h_pthread_mutexattr_settype);
    reg(ctx,"pthread_condattr_init",h_pthread_condattr_init);
    reg(ctx,"pthread_condattr_destroy",h_pthread_condattr_destroy);
    reg(ctx,"pthread_condattr_setclock",h_pthread_condattr_setclock);
    reg(ctx,"pthread_setname_np",h_pthread_setname_np);
    reg(ctx,"pthread_rwlock_init",h_pthread_rwlock_init);
    reg(ctx,"pthread_rwlock_destroy",h_pthread_rwlock_destroy);
    reg(ctx,"pthread_rwlock_rdlock",h_pthread_rwlock_rdlock);
    reg(ctx,"pthread_rwlock_wrlock",h_pthread_rwlock_wrlock);
    reg(ctx,"pthread_rwlock_unlock",h_pthread_rwlock_unlock);
    reg(ctx,"pthread_sigmask",h_guest_pthread_sigmask);
    reg(ctx,"pthread_setschedparam",h_stub0);

    reg(ctx,"qsort",h_qsort); reg(ctx,"bsearch",h_bsearch);

    regf64(ctx,"sin",h_sin); regf64(ctx,"cos",h_cos); regf64(ctx,"tan",h_tan);
    regf64(ctx,"asin",h_asin); regf64(ctx,"acos",h_acos); regf64(ctx,"atan",h_atan); regf64(ctx,"atan2",h_atan2);
    regf64(ctx,"sinh",h_sinh); regf64(ctx,"tanh",h_tanh);
    regf64(ctx,"exp",h_exp); regf64(ctx,"exp2",h_exp2); regf64(ctx,"expm1",h_expm1);
    regf64(ctx,"log",h_log); regf64(ctx,"log10",h_log10); regf64(ctx,"log2",h_log);
    regf64(ctx,"pow",h_pow); regf64(ctx,"sqrt",h_sqrt); regf64(ctx,"cbrt",h_cbrt);
    regf64(ctx,"fabs",h_fabs); regf64(ctx,"fmod",h_fmod); regf64(ctx,"hypot",h_hypot);
    reg(ctx,"sincos",h_sincos);
    regf32(ctx,"sinf",h_sinf); regf32(ctx,"cosf",h_cosf); regf32(ctx,"atanf",h_atanf);
    regf32(ctx,"expf",h_expf); regf32(ctx,"exp2f",h_exp2f);
    regf32(ctx,"logf",h_logf); regf32(ctx,"log10f",h_log10f); regf32(ctx,"log2f",h_log2f);
    regf32(ctx,"powf",h_powf); regf32(ctx,"atan2f",h_atan2f); regf32(ctx,"fmodf",h_fmodf);
    regf32(ctx,"tanhf",h_tanhf); reg(ctx,"sincosf",h_sincosf);

    reg(ctx,"inflateInit2_",h_inflateInit2_); reg(ctx,"inflate",h_inflate); reg(ctx,"inflateEnd",h_inflateEnd);
    reg(ctx,"deflateInit2_",h_deflateInit2_); reg(ctx,"deflate",h_deflate); reg(ctx,"deflateEnd",h_deflateEnd);

    reg(ctx,"dlopen",h_dlopen); reg(ctx,"dlsym",h_dlsym); reg(ctx,"dlclose",h_dlclose);
    reg(ctx,"dlerror",h_dlerror); reg(ctx,"dladdr",h_dladdr); reg(ctx,"dl_iterate_phdr",h_stub0);

    /* GLES / EGL / Android graphics — stubs */
    const char *stubs[] = {
        "glActiveTexture","glAttachShader","glBindBuffer","glBindTexture","glBlendFunc",
        "glBufferData","glBufferSubData","glClear","glClearColor","glClearDepthf",
        "glColorMask","glCompileShader","glCreateProgram","glCreateShader","glDepthFunc",
        "glDepthMask","glDisable","glDrawArrays","glEnable","glEnableVertexAttribArray",
        "glGenBuffers","glGetAttribLocation","glGetProgramiv","glGetShaderiv",
        "glGetUniformLocation","glLinkProgram","glShaderSource","glUniform1f","glUniform1i",
        "glUniform4fv","glUniformMatrix4fv","glUseProgram","glVertexAttribPointer","glViewport",
        "eglGetCurrentContext",
        "AAssetManager_fromJava","AAssetManager_open","AAsset_close","AAsset_read",
        "AndroidBitmap_getInfo","AndroidBitmap_lockPixels","AndroidBitmap_unlockPixels",
        "slCreateEngine",
        NULL
    };
    for (int i=0; stubs[i]; i++) reg(ctx, stubs[i], h_stub0);

    /* OpenSLES IIDs as data */
    static int iid_placeholder[8];
    jet_native_register_data(ctx,"SL_IID_ENGINE",&iid_placeholder[0]);
    jet_native_register_data(ctx,"SL_IID_PLAY",&iid_placeholder[1]);
    jet_native_register_data(ctx,"SL_IID_RECORD",&iid_placeholder[2]);
    jet_native_register_data(ctx,"SL_IID_VOLUME",&iid_placeholder[3]);
    jet_native_register_data(ctx,"SL_IID_BUFFERQUEUE",&iid_placeholder[4]);
    jet_native_register_data(ctx,"SL_IID_ANDROIDSIMPLEBUFFERQUEUE",&iid_placeholder[5]);
    jet_native_register_data(ctx,"SL_IID_ANDROIDCONFIGURATION",&iid_placeholder[6]);

    reg(ctx,"isalnum",h_isalnum); reg(ctx,"isalpha",h_isalpha); reg(ctx,"isdigit",h_isdigit);
    reg(ctx,"islower",h_islower); reg(ctx,"isspace",h_isspace); reg(ctx,"isupper",h_isupper);
    reg(ctx,"isxdigit",h_isxdigit); reg(ctx,"isascii",h_isascii); reg(ctx,"isprint",h_isprint);
    reg(ctx,"tolower",h_tolower); reg(ctx,"toupper",h_toupper);
    reg(ctx,"iswalpha",h_iswalpha); reg(ctx,"iswdigit",h_iswdigit);
    reg(ctx,"iswlower",h_iswlower); reg(ctx,"iswspace",h_iswspace);
    reg(ctx,"iswupper",h_iswupper); reg(ctx,"iswxdigit",h_iswxdigit);
    reg(ctx,"iswprint",h_iswprint); reg(ctx,"iswpunct",h_iswpunct);
    reg(ctx,"iswblank",h_iswblank); reg(ctx,"iswcntrl",h_iswcntrl);
    reg(ctx,"towlower",h_towlower); reg(ctx,"towupper",h_towupper);

    /* OPENSSL_memory_* used by boringssl inside tmessages */
    reg(ctx,"OPENSSL_memory_alloc",h_malloc);
    reg(ctx,"OPENSSL_memory_free",h_free);
    reg(ctx,"OPENSSL_memory_get_size",h_stub0);

    /* misc frequently needed */
    reg(ctx,"syscall",h_syscall);
    reg(ctx,"sched_yield",h_stub0);
    reg(ctx,"poll",h_poll_fn); reg(ctx,"select",h_stub0);
    reg(ctx,"socket",h_socket); reg(ctx,"bind",h_bind_fn); reg(ctx,"listen",h_listen_fn);
    reg(ctx,"accept",h_stub0); reg(ctx,"connect",h_connect); reg(ctx,"setsockopt",h_setsockopt);
    reg(ctx,"getsockopt",h_getsockopt); reg(ctx,"getaddrinfo",h_getaddrinfo); reg(ctx,"freeaddrinfo",h_free);
    reg(ctx,"ioctl",h_ioctl); reg(ctx,"fcntl",h_fcntl);
    reg(ctx,"pipe",h_pipe_fn); reg(ctx,"eventfd",h_eventfd);
    reg(ctx,"epoll_create",h_epoll_create); reg(ctx,"epoll_create1",h_epoll_create1);
    reg(ctx,"epoll_ctl",h_epoll_ctl); reg(ctx,"epoll_wait",h_epoll_wait);
    reg(ctx,"sem_init",h_stub0); reg(ctx,"sem_destroy",h_stub0);
    reg(ctx,"sem_wait",h_stub0); reg(ctx,"sem_post",h_stub0); reg(ctx,"sem_trywait",h_stub0);
    reg(ctx,"sem_timedwait",h_stub0);
    reg(ctx,"arc4random",h_stub0); reg(ctx,"rand",h_stub0); reg(ctx,"srand",h_stub0);
    reg(ctx,"random",h_stub0); reg(ctx,"srandom",h_stub0); reg(ctx,"lrand48",h_stub0);
    reg(ctx,"time",h_stub0); reg(ctx,"localtime",h_stub0); reg(ctx,"localtime_r",h_stub0);
    reg(ctx,"gmtime_r",h_stub0); reg(ctx,"mktime",h_stub0); reg(ctx,"strftime",h_stub0);
    reg(ctx,"fopen",h_stub0); reg(ctx,"fclose",h_stub0); reg(ctx,"fread",h_stub0);
    reg(ctx,"fwrite",h_stub0); reg(ctx,"fprintf",h_printf); reg(ctx,"fputs",h_stub0);
    reg(ctx,"fgets",h_stub0); reg(ctx,"fflush",h_stub0); reg(ctx,"fseek",h_stub0);
    reg(ctx,"ftell",h_stub0); reg(ctx,"fileno",h_stub0);
    reg(ctx,"getcwd",h_getcwd); reg(ctx,"mkdir",h_stub0); reg(ctx,"remove",h_stub0);
    reg(ctx,"rename",h_stub0); reg(ctx,"unlink",h_stub0); reg(ctx,"rmdir",h_stub0);
    reg(ctx,"chmod",h_stub0); reg(ctx,"fchmod",h_stub0); reg(ctx,"fchown",h_stub0);
    reg(ctx,"ftruncate",h_stub0); reg(ctx,"fsync",h_stub0);
    reg(ctx,"opendir",h_stub0); reg(ctx,"readdir",h_stub0); reg(ctx,"closedir",h_stub0);
    reg(ctx,"sigaction",h_guest_sigaction); reg(ctx,"sigemptyset",h_guest_sigemptyset);
    reg(ctx,"sigfillset",h_guest_sigfillset); reg(ctx,"sigaddset",h_guest_sigaddset);
    reg(ctx,"sigismember",h_guest_sigismember);
    reg(ctx,"signal",h_guest_signal); reg(ctx,"bsd_signal",h_guest_signal);
    reg(ctx,"sigprocmask",h_guest_sigprocmask); reg(ctx,"sigaltstack",h_guest_sigaltstack);
    reg(ctx,"tgkill",h_stub0);
    reg(ctx,"android_set_abort_message",h_stub0);
    reg(ctx,"__assert2",h_abort);


    /* bulk missing imports seen from libtmessages */
    const char *more_stubs[] = {
        "feof","ferror","eventfd_write","eventfd_read","vfprintf","closelog","openlog","syslog",
        "strtoull_l","strtoll_l","strtold_l","strtof","strtold","strcoll","strxfrm","strspn","strcspn",
        "strnlen","strerror_r","sscanf","vsscanf","fscanf","vasprintf","perror","utimes","utimensat",
        "wcsnrtombs","wcrtomb","wcslen","wcstoul","wcstoll","wcstod","wcstof","wmemmove","wmemcpy",
        "wmemset","wmemcmp","wctob","btowc","mbrtowc","mbrlen","mbsnrtowcs","mbsrtowcs","mbtowc",
        "frexp","fseeko","ftello","getc","putc","fputc","ungetc","freopen","fdopen","mkstemp",
        "gai_strerror","getsockname","getpeername","if_indextoname","inet_ntop","inet_pton","inet_ntoa",
        "shutdown","send","recv","sendto","recvfrom","socketpair","sched_get_priority_max",
        "sched_get_priority_min","sched_getaffinity","__sched_cpucount","abs","labs","nan",
        "freelocale","newlocale","uselocale","setlocale","localeconv","__ctype_get_mb_cur_max",
        "clock","isatty","putenv","madvise","swprintf","wcscoll","cosh","__FD_CLR_chk","__FD_ISSET_chk",
        "__FD_SET_chk",
        NULL
    };
    for (int i=0; more_stubs[i]; i++) reg(ctx, more_stubs[i], h_stub0);
    reg(ctx,"floor",h_floor); reg(ctx,"ceil",h_ceil); reg(ctx,"round",h_round); reg(ctx,"trunc",h_trunc);
    return 0;
}

/* ---- Linux AArch64 syscall emulation (portable; no host syscall() on Darwin) ---- */
/* Numbers from linux/include/uapi/asm-generic/unistd.h (AArch64). */
enum {
    JET_NR_io_setup = 0,
    JET_NR_setxattr = 5,
    JET_NR_getcwd = 17,
    JET_NR_eventfd2 = 19,
    JET_NR_epoll_create1 = 20,
    JET_NR_epoll_ctl = 21,
    JET_NR_epoll_pwait = 22,
    JET_NR_dup = 23,
    JET_NR_dup3 = 24,
    JET_NR_fcntl = 25,
    JET_NR_ioctl = 29,
    JET_NR_mknodat = 33,
    JET_NR_mkdirat = 34,
    JET_NR_unlinkat = 35,
    JET_NR_symlinkat = 36,
    JET_NR_linkat = 37,
    JET_NR_renameat = 38,
    JET_NR_umount2 = 39,
    JET_NR_mount = 40,
    JET_NR_statfs = 43,
    JET_NR_fstatfs = 44,
    JET_NR_truncate = 45,
    JET_NR_ftruncate = 46,
    JET_NR_fallocate = 47,
    JET_NR_faccessat = 48,
    JET_NR_chdir = 49,
    JET_NR_fchdir = 50,
    JET_NR_chroot = 51,
    JET_NR_fchmod = 52,
    JET_NR_fchmodat = 53,
    JET_NR_fchownat = 54,
    JET_NR_fchown = 55,
    JET_NR_openat = 56,
    JET_NR_close = 57,
    JET_NR_vhangup = 58,
    JET_NR_pipe2 = 59,
    JET_NR_getdents64 = 61,
    JET_NR_lseek = 62,
    JET_NR_read = 63,
    JET_NR_write = 64,
    JET_NR_readv = 65,
    JET_NR_writev = 66,
    JET_NR_pread64 = 67,
    JET_NR_pwrite64 = 68,
    JET_NR_preadv = 69,
    JET_NR_pwritev = 70,
    JET_NR_pselect6 = 72,
    JET_NR_ppoll = 73,
    JET_NR_readlinkat = 78,
    JET_NR_newfstatat = 79,
    JET_NR_fstat = 80,
    JET_NR_sync = 81,
    JET_NR_fsync = 82,
    JET_NR_fdatasync = 83,
    JET_NR_timer_create = 107,
    JET_NR_clock_gettime = 113,
    JET_NR_clock_getres = 114,
    JET_NR_clock_nanosleep = 115,
    JET_NR_syslog = 116,
    JET_NR_ptrace = 117,
    JET_NR_sched_yield = 124,
    JET_NR_kill = 129,
    JET_NR_tkill = 130,
    JET_NR_tgkill = 131,
    JET_NR_sigaltstack = 132,
    JET_NR_rt_sigsuspend = 133,
    JET_NR_rt_sigaction = 134,
    JET_NR_rt_sigprocmask = 135,
    JET_NR_rt_sigpending = 136,
    JET_NR_rt_sigtimedwait = 137,
    JET_NR_rt_sigqueueinfo = 138,
    JET_NR_rt_sigreturn = 139,
    JET_NR_setpriority = 140,
    JET_NR_getpriority = 141,
    JET_NR_reboot = 142,
    JET_NR_setregid = 143,
    JET_NR_setgid = 144,
    JET_NR_setreuid = 145,
    JET_NR_setuid = 146,
    JET_NR_getpgid = 155,
    JET_NR_getsid = 156,
    JET_NR_setsid = 157,
    JET_NR_getgroups = 158,
    JET_NR_uname = 160,
    JET_NR_getrlimit = 163,
    JET_NR_setrlimit = 164,
    JET_NR_getrusage = 165,
    JET_NR_umask = 166,
    JET_NR_prctl = 167,
    JET_NR_gettimeofday = 169,
    JET_NR_getpid = 172,
    JET_NR_getppid = 173,
    JET_NR_getuid = 174,
    JET_NR_geteuid = 175,
    JET_NR_getgid = 176,
    JET_NR_getegid = 177,
    JET_NR_gettid = 178,
    JET_NR_sysinfo = 179,
    JET_NR_mq_open = 180,
    JET_NR_semget = 190,
    JET_NR_shmget = 194,
    JET_NR_socket = 198,
    JET_NR_socketpair = 199,
    JET_NR_bind = 200,
    JET_NR_listen = 201,
    JET_NR_accept = 202,
    JET_NR_connect = 203,
    JET_NR_getsockname = 204,
    JET_NR_getpeername = 205,
    JET_NR_sendto = 206,
    JET_NR_recvfrom = 207,
    JET_NR_setsockopt = 208,
    JET_NR_getsockopt = 209,
    JET_NR_shutdown = 210,
    JET_NR_sendmsg = 211,
    JET_NR_recvmsg = 212,
    JET_NR_readahead = 213,
    JET_NR_brk = 214,
    JET_NR_munmap = 215,
    JET_NR_mremap = 216,
    JET_NR_clone = 220,
    JET_NR_execve = 221,
    JET_NR_mmap = 222,
    JET_NR_fadvise = 223,
    JET_NR_mprotect = 226,
    JET_NR_msync = 227,
    JET_NR_mlock = 228,
    JET_NR_munlock = 229,
    JET_NR_madvise = 233,
    JET_NR_getrandom = 278,
    JET_NR_memfd_create = 279,
    JET_NR_futex = 98,
    JET_NR_set_tid_address = 96,
    JET_NR_set_robust_list = 99,
    JET_NR_get_robust_list = 100,
    JET_NR_nanosleep = 101,
    JET_NR_getitimer = 102,
    JET_NR_setitimer = 103,
    JET_NR_exit = 93,
    JET_NR_exit_group = 94
};

/* ---- Guest signal emulation (never touch host handlers/masks) ---- */
/* Linux AArch64 sigaction layout (roughly): handler, flags, restorer, mask[16]. */
typedef struct {
    uint64_t handler;
    uint64_t flags;
    uint64_t restorer;
    uint8_t  mask[16];
} jet_guest_sigaction;

static uint64_t h_guest_sigemptyset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (a->x[0]) memset(G2H(a->x[0]), 0, 16);
    return 0;
}
static uint64_t h_guest_sigfillset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (a->x[0]) memset(G2H(a->x[0]), 0xff, 16);
    return 0;
}
static uint64_t h_guest_sigaddset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (!a->x[0]) return 0;
    int sig = (int)a->x[1];
    if (sig < 1 || sig > 128) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint8_t *set = (uint8_t*)G2H(a->x[0]);
    set[(sig-1)/8] |= (uint8_t)(1u << ((sig-1)%8));
    return 0;
}
static uint64_t h_guest_sigismember(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (!a->x[0]) return 0;
    int sig = (int)a->x[1];
    if (sig < 1 || sig > 128) return 0;
    uint8_t *set = (uint8_t*)G2H(a->x[0]);
    return (set[(sig-1)/8] & (1u << ((sig-1)%8))) ? 1 : 0;
}

static uint64_t h_guest_sigaction(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int sig = (int)a->x[0];
    const jet_guest_sigaction *act = a->x[1] ? (const jet_guest_sigaction*)G2H(a->x[1]) : NULL;
    jet_guest_sigaction *oact = a->x[2] ? (jet_guest_sigaction*)G2H(a->x[2]) : NULL;
    if (sig < 1 || sig >= 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    if (oact) {
        memset(oact, 0, sizeof *oact);
        if (c->guest_sig_action[sig].set) {
            oact->handler = c->guest_sig_action[sig].handler;
            oact->flags = c->guest_sig_action[sig].flags;
            memcpy(oact->mask, c->guest_sig_action[sig].mask, 16);
        }
    }
    if (act) {
        c->guest_sig_action[sig].handler = act->handler;
        c->guest_sig_action[sig].flags = act->flags;
        memcpy(c->guest_sig_action[sig].mask, act->mask, 16);
        c->guest_sig_action[sig].set = 1;
    }
    /* Intentionally do NOT call host sigaction — JVM owns SIGSEGV/SIGBUS. */
    return 0;
}

static uint64_t h_guest_signal(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int sig = (int)a->x[0];
    uint64_t handler = a->x[1];
    if (sig < 1 || sig >= 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint64_t prev = c->guest_sig_action[sig].set ? c->guest_sig_action[sig].handler : 0;
    c->guest_sig_action[sig].handler = handler;
    c->guest_sig_action[sig].flags = 0;
    memset(c->guest_sig_action[sig].mask, 0, 16);
    c->guest_sig_action[sig].set = 1;
    return prev;
}

static uint64_t h_guest_sigaltstack(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    /* stack_t: ss_sp, ss_flags, ss_size (Linux aarch64) */
    typedef struct { uint64_t ss_sp; int ss_flags; int _pad; uint64_t ss_size; } jet_stack_t;
    const jet_stack_t *ss = a->x[0] ? (const jet_stack_t*)G2H(a->x[0]) : NULL;
    jet_stack_t *oss = a->x[1] ? (jet_stack_t*)G2H(a->x[1]) : NULL;
    if (oss) {
        memset(oss, 0, sizeof *oss);
        oss->ss_sp = c->guest_ss_sp;
        oss->ss_size = c->guest_ss_size;
        oss->ss_flags = c->guest_ss_flags;
    }
    if (ss) {
        c->guest_ss_sp = ss->ss_sp;
        c->guest_ss_size = ss->ss_size;
        c->guest_ss_flags = ss->ss_flags;
    }
    return 0;
}

static uint64_t h_guest_sigprocmask(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int how = (int)a->x[0];
    const uint8_t *set = a->x[1] ? (const uint8_t*)G2H(a->x[1]) : NULL;
    uint8_t *oldset = a->x[2] ? (uint8_t*)G2H(a->x[2]) : NULL;
    if (oldset) memcpy(oldset, c->guest_sig_mask, 16);
    if (set) {
        if (how == 0 /* SIG_BLOCK */) {
            for (int i=0;i<16;i++) c->guest_sig_mask[i] |= set[i];
        } else if (how == 1 /* SIG_UNBLOCK */) {
            for (int i=0;i<16;i++) c->guest_sig_mask[i] &= (uint8_t)~set[i];
        } else if (how == 2 /* SIG_SETMASK */) {
            memcpy(c->guest_sig_mask, set, 16);
        } else {
            errno = EINVAL; return (uint64_t)(int64_t)-1;
        }
    }
    return 0;
}

static uint64_t h_guest_pthread_sigmask(jet_ctx *c, const jet_args *a, void *u) {
    /* Same guest-mask semantics; never call host pthread_sigmask. */
    return h_guest_sigprocmask(c, a, u);
}

int64_t jet_emulate_linux_syscall(jet_ctx *ctx, jet_cpu *cpu,
                                  uint64_t nr, uint64_t a0, uint64_t a1,
                                  uint64_t a2, uint64_t a3, uint64_t a4,
                                  uint64_t a5) {
    (void)cpu; (void)a4; (void)a5;
    switch ((int)nr) {
    case JET_NR_getcwd: {
        /* a0=buf, a1=size — same semantics as h_getcwd */
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        uint64_t r = h_getcwd(ctx, &aa, NULL);
        return r ? (int64_t)r : -1;
    }
    case JET_NR_getpid:  return (int64_t)getpid();
    case JET_NR_getppid: return (int64_t)getppid();
    case JET_NR_getuid:  return (int64_t)getuid();
    case JET_NR_geteuid: return (int64_t)geteuid();
    case JET_NR_getgid:  return (int64_t)getgid();
    case JET_NR_getegid: return (int64_t)getegid();
    case JET_NR_gettid: {
#if defined(__APPLE__)
        uint64_t tid=0; pthread_threadid_np(NULL,&tid); return (int64_t)tid;
#elif defined(__linux__) && defined(SYS_gettid)
        return (int64_t)syscall(SYS_gettid);
#else
        return (int64_t)(uintptr_t)pthread_self();
#endif
    }
    case JET_NR_clock_gettime:
        return (int64_t)clock_gettime((clockid_t)a0, (struct timespec*)G2H(a1));
    case JET_NR_clock_getres:
        return (int64_t)clock_getres((clockid_t)a0, (struct timespec*)G2H(a1));
    case JET_NR_gettimeofday:
        return (int64_t)gettimeofday((struct timeval*)G2H(a0), NULL);
    case JET_NR_nanosleep:
        return (int64_t)nanosleep((struct timespec*)G2H(a0), (struct timespec*)G2H(a1));
    case JET_NR_clock_nanosleep: {
        /* Darwin has no clock_nanosleep. Emulate with nanosleep.
         * TIMER_ABSTIME: sleep until absolute time on clockid. */
#ifndef TIMER_ABSTIME
#define TIMER_ABSTIME 1
#endif
        int flags = (int)a1;
        const struct timespec *req = (const struct timespec *)G2H(a2);
        struct timespec *rem = a3 ? (struct timespec *)G2H(a3) : NULL;
        if (!req) { errno = EFAULT; return -1; }
#if defined(__APPLE__)
        if (flags & TIMER_ABSTIME) {
            for (;;) {
                struct timespec now, delta;
                if (clock_gettime((clockid_t)a0, &now) != 0) return -1;
                delta.tv_sec = req->tv_sec - now.tv_sec;
                delta.tv_nsec = req->tv_nsec - now.tv_nsec;
                if (delta.tv_nsec < 0) { delta.tv_sec--; delta.tv_nsec += 1000000000L; }
                if (delta.tv_sec < 0 || (delta.tv_sec == 0 && delta.tv_nsec <= 0))
                    return 0;
                int rc = nanosleep(&delta, NULL);
                if (rc == 0) return 0;
                if (errno != EINTR) return -1;
                /* EINTR: recompute remaining absolute delta */
            }
        } else {
            struct timespec left = *req;
            for (;;) {
                struct timespec out;
                int rc = nanosleep(&left, &out);
                if (rc == 0) { if (rem) { rem->tv_sec = 0; rem->tv_nsec = 0; } return 0; }
                if (errno != EINTR) return -1;
                left = out;
                if (rem) *rem = out;
            }
        }
#else
        return (int64_t)clock_nanosleep((clockid_t)a0, flags, req, rem);
#endif
    }
    case JET_NR_sched_yield:
        return (int64_t)sched_yield();
    case JET_NR_close:
        if (jet_is_fake_fd((int)a0)) { jet_fake_fd_map[(int)a0]=0; return 0; }
        return (int64_t)close((int)a0);
    case JET_NR_read:
        if (jet_is_fake_fd((int)a0)) { errno = EAGAIN; return -1; }
        return (int64_t)read((int)a0, G2H(a1), (size_t)a2);
    case JET_NR_write:
        if (jet_is_fake_fd((int)a0)) return (int64_t)a2; /* pretend written */
        return (int64_t)write((int)a0, G2H(a1), (size_t)a2);
    case JET_NR_lseek:
        return (int64_t)lseek((int)a0, (off_t)a1, (int)a2);
    case JET_NR_openat: {
        /* Linux openat flags differ slightly; pass through best-effort. */
        return (int64_t)openat((int)a0, (const char*)G2H(a1), (int)a2, (mode_t)a3);
    }
    case JET_NR_fstat:
        return (int64_t)fstat((int)a0, (struct stat*)G2H(a1));
    case JET_NR_newfstatat:
        return (int64_t)fstatat((int)a0, (const char*)G2H(a1), (struct stat*)G2H(a2), (int)a3);
    case JET_NR_readlinkat:
        return (int64_t)readlinkat((int)a0, (const char*)G2H(a1), (char*)G2H(a2), (size_t)a3);
    case JET_NR_faccessat:
        return (int64_t)faccessat((int)a0, (const char*)G2H(a1), (int)a2, (int)a3);
    case JET_NR_mmap: {
        int prot = (int)a2 & ~PROT_EXEC;
        int flags = (int)a3;
        void *p = mmap(G2H(a0), (size_t)a1, prot, flags, (int)a4, (off_t)a5);
        if (p == MAP_FAILED) return -errno;
        return (int64_t)(uintptr_t)p;
    }
    case JET_NR_munmap:
        return (int64_t)munmap(G2H(a0), (size_t)a1);
    case JET_NR_mprotect: {
        int prot = (int)a2 & ~PROT_EXEC;
        return (int64_t)mprotect(G2H(a0), (size_t)a1, prot);
    }
    case JET_NR_madvise:
#ifdef MADV_DONTNEED
        return (int64_t)madvise(G2H(a0), (size_t)a1, (int)a2);
#else
        (void)a0;(void)a1;(void)a2; return 0;
#endif
    case JET_NR_brk:
        /* Don't support brk heap; return current. */
        return (int64_t)a0;
    case JET_NR_uname:
        return (int64_t)uname((struct utsname*)G2H(a0));
    case JET_NR_prctl: {
        jet_args aa = {0};
        aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4;
        return (int64_t)h_prctl(ctx, &aa, NULL);
    }
    case JET_NR_futex:
        /* Minimal: FUTEX_WAKE / FUTEX_WAIT no-op / success. Enough for some ctors. */
        jet_log(ctx, 3, "futex stub nr op=%llx", (unsigned long long)a1);
        return 0;
    case JET_NR_set_tid_address:
        return (int64_t)getpid(); /* pretend */
    case JET_NR_set_robust_list:
    case JET_NR_get_robust_list:
        return 0;
    case JET_NR_getrandom: {
        uint8_t *buf = (uint8_t*)G2H(a0);
        size_t n = (size_t)a1;
        for (size_t i=0;i<n;i++) buf[i]=(uint8_t)(rand() & 0xff);
        return (int64_t)n;
    }
    case JET_NR_ioctl:
        return (int64_t)ioctl((int)a0, (unsigned long)a1, (void*)G2H(a2));
    case JET_NR_fcntl:
        return (int64_t)fcntl((int)a0, (int)a1, a2);
    case JET_NR_epoll_create1: {
        int fd = jet_fake_fd_alloc();
        return (int64_t)fd;
    }
    case JET_NR_epoll_ctl:
        return 0;
    case JET_NR_epoll_pwait: {
        struct timespec ts = {0, 50 * 1000 * 1000};
        nanosleep(&ts, NULL);
        return 0;
    }
    case JET_NR_sigaltstack: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_guest_sigaltstack(ctx, &aa, NULL);
    }
    case JET_NR_rt_sigaction: {
        /* a0=sig, a1=act, a2=oact, a3=sigsetsize — ignore size */
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_guest_sigaction(ctx, &aa, NULL);
    }
    case JET_NR_rt_sigprocmask: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_guest_sigprocmask(ctx, &aa, NULL);
    }
    case JET_NR_rt_sigpending:
        if (a0) memset(G2H(a0), 0, a1 ? (size_t)a1 : 16);
        return 0;
    case JET_NR_rt_sigsuspend:
    case JET_NR_rt_sigtimedwait:
    case JET_NR_rt_sigqueueinfo:
    case JET_NR_rt_sigreturn:
        /* No host delivery — report as if no signal pending / success. */
        return 0;
    case JET_NR_exit:
    case JET_NR_exit_group:
        jet_log(ctx, 0, "guest exit(%d) via SVC", (int)a0);
        if (cpu) { cpu->stop = 1; cpu->err_code = 0; }
        return 0;
    default:
        jet_log(ctx, 1, "unimplemented Linux syscall %llu", (unsigned long long)nr);
        errno = ENOSYS;
        return -ENOSYS;
    }
}

