/* host_bionic.c - bionic/Linux host trap implementations for guest imports. */
#include "jet_internal.h"
#include <errno.h>
#include <math.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/utsname.h>
#include <sys/syscall.h>
#include <poll.h>
#include <dirent.h>
#include <pthread.h>
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
#include "zlib_stub.h"
#include <sys/prctl.h>
#include <sys/auxv.h>

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
    return H2G(__errno_location());
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
    abort();
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
static uint64_t h_memcpy(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return H2G(memcpy(G2H(a->x[0]),G2H(a->x[1]),(size_t)a->x[2])); }
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
static uint64_t h_abort(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; abort(); return 0; }
static uint64_t h_exit_fn(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; exit((int)a->x[0]); return 0; }
static uint64_t h__exit(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; _exit((int)a->x[0]); return 0; }

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
        int longness=0;
        while (*p=='l'||*p=='z'||*p=='t'||*p=='h'||*p=='j'){ if(*p=='l')longness++; p++; }
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
    unsigned long type = (unsigned long)a->x[0];
    if (type == 16 /*AT_HWCAP*/) return c->cfg.hwcap;
    if (type == 26 /*AT_HWCAP2*/) return c->cfg.hwcap2;
    if (type == 6  /*AT_PAGESZ*/) return (uint64_t)sysconf(_SC_PAGESIZE);
    return getauxval(type);
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
static uint64_t h_gettid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)syscall(SYS_gettid); }
static uint64_t h_getuid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)getuid(); }
static uint64_t h_geteuid(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)a;(void)u; return (uint64_t)geteuid(); }
static uint64_t h_uname(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)uname((struct utsname*)G2H(a->x[0])); }
static uint64_t h_prctl(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return (uint64_t)(int64_t)prctl((int)a->x[0], a->x[1], a->x[2], a->x[3], a->x[4]);
}

/* file IO */
static uint64_t h_open(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)open((char*)G2H(a->x[0]),(int)a->x[1],(mode_t)a->x[2]); }
static uint64_t h_close(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)close((int)a->x[0]); }
static uint64_t h_read(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)read((int)a->x[0],G2H(a->x[1]),(size_t)a->x[2]); }
static uint64_t h_write(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)write((int)a->x[0],G2H(a->x[1]),(size_t)a->x[2]); }
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
static uint64_t h_pthread_mutex_lock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_mutex_lock((pthread_mutex_t*)G2H(a->x[0])); }
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
static uint64_t h_pthread_condattr_setclock(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_condattr_setclock((pthread_condattr_t*)G2H(a->x[0]),(clockid_t)a->x[1]); }
static uint64_t h_pthread_setname_np(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)pthread_setname_np((pthread_t)a->x[0], (char*)G2H(a->x[1])); }
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
#define HF1(name,fn) static uint64_t h_##name(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; float x; uint32_t bits=(uint32_t)(a->d[0]/*wrong*/); (void)bits; float xf; uint32_t lo; memcpy(&lo,&a->x[0],4); /* FP args in d[]: low 32 of d0 */ memcpy(&xf,&a->d[0],4); float r=fn(xf); uint32_t rb; memcpy(&rb,&r,4); return rb; }
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
uint64_t jet_host_stub0(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return 0; }
static uint64_t h_stub0(jet_ctx *c,const jet_args *a,void *u){ return jet_host_stub0(c,a,u); }
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
static uintptr_t jet_stack_chk_guard = 0x12345678abcdefull;
static uint64_t h_stack_chk_fail(jet_ctx *c,const jet_args *a,void *u){
    (void)a;(void)u; jet_log(c,0,"__stack_chk_fail"); abort(); return 0;
}

/* __cxa_atexit / finalize */
static uint64_t h_cxa_atexit(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return 0; /* TODO: record */ }
static uint64_t h_cxa_finalize(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return 0; }

/* FILE* stubs — expose host stdin/stdout/stderr as __sF-like */
static FILE *jet_sf[3];

static void reg(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host(ctx,n,f,NULL); }
static void regf64(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host_ex(ctx,n,f,NULL,JET_HOST_RET_F64); }
static void regf32(jet_ctx *ctx, const char *n, jet_host_fn f){ jet_native_register_host_ex(ctx,n,f,NULL,JET_HOST_RET_F32); }

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
    reg(ctx,"pthread_sigmask",h_stub0);
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
    reg(ctx,"syscall",h_stub0);
    reg(ctx,"sched_yield",h_stub0);
    reg(ctx,"poll",h_stub0); reg(ctx,"select",h_stub0);
    reg(ctx,"socket",h_stub0); reg(ctx,"bind",h_stub0); reg(ctx,"listen",h_stub0);
    reg(ctx,"accept",h_stub0); reg(ctx,"connect",h_stub0); reg(ctx,"setsockopt",h_stub0);
    reg(ctx,"getsockopt",h_stub0); reg(ctx,"getaddrinfo",h_stub0); reg(ctx,"freeaddrinfo",h_free);
    reg(ctx,"ioctl",h_stub0); reg(ctx,"fcntl",h_stub0);
    reg(ctx,"pipe",h_stub0); reg(ctx,"eventfd",h_stub0);
    reg(ctx,"epoll_create",h_stub0); reg(ctx,"epoll_ctl",h_stub0); reg(ctx,"epoll_wait",h_stub0);
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
    reg(ctx,"getcwd",h_stub0); reg(ctx,"mkdir",h_stub0); reg(ctx,"remove",h_stub0);
    reg(ctx,"rename",h_stub0); reg(ctx,"unlink",h_stub0); reg(ctx,"rmdir",h_stub0);
    reg(ctx,"chmod",h_stub0); reg(ctx,"fchmod",h_stub0); reg(ctx,"fchown",h_stub0);
    reg(ctx,"ftruncate",h_stub0); reg(ctx,"fsync",h_stub0);
    reg(ctx,"opendir",h_stub0); reg(ctx,"readdir",h_stub0); reg(ctx,"closedir",h_stub0);
    reg(ctx,"sigaction",h_stub0); reg(ctx,"sigemptyset",h_stub0); reg(ctx,"sigfillset",h_stub0);
    reg(ctx,"sigaddset",h_stub0); reg(ctx,"tgkill",h_stub0);
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
