/* host_bionic.c - bionic/Linux host trap implementations for guest imports. */
#include "jet_internal.h"
#include "guest_abi.h"
#include "darwin_abi.h"
#include "epoll_kq.h"
#include "netdb_abi.h"
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
#include <termios.h>
#include <poll.h>
#include <sys/resource.h>
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
#  include <sys/eventfd.h>
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

/* Guest struct layouts + converters live in guest_abi.h / guest_abi.c (v10.1). */
static int write_guest_stat(uint64_t guest_buf, const struct stat *h) {
    if (!guest_buf) { errno = EFAULT; return -1; }
    return jet_write_guest_stat(G2H(guest_buf), h);
}



/* ---- errno ----
 * Guest sees Linux errno numbers. Linux host: same numbering, hand out host errno.
 * Darwin: per-thread guest slot; a pending (non-zero) host errno is translated into
 * it and cleared, so guest writes (errno = 0) persist until the next host failure. */
#if defined(__APPLE__)
#  define JET_ERRNO_FROM_LINUX(e) jet_xl_errno_l2d(e)
#  define JET_ERRNO_TO_GUEST(e)   jet_xl_errno_d2l(e)
static __thread int jet_guest_errno_slot;
#else
#  define JET_ERRNO_FROM_LINUX(e) (e)
#  define JET_ERRNO_TO_GUEST(e)   (e)
#endif
/* Return a negative-Linux-errno result to a libc-style trap (-1 + errno). */
static uint64_t jet_ret_lerr(int64_t r) {
    if (r < 0) { errno = JET_ERRNO_FROM_LINUX((int)-r); return (uint64_t)(int64_t)-1; }
    return (uint64_t)r;
}
static uint64_t h___errno(jet_ctx *ctx, const jet_args *a, void *u) {
    (void)ctx; (void)a; (void)u;
#if defined(__linux__)
    return H2G(__errno_location());
#elif defined(__APPLE__)
    if (errno) { jet_guest_errno_slot = jet_xl_errno_d2l(errno); errno = 0; }
    return H2G(&jet_guest_errno_slot);
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
/* Dump guest FP-chain backtrace with PCs relative to each loaded .so base. */
static void jet_dump_guest_backtrace(jet_ctx *c, jet_cpu *cpu, const char *why) {
    if (!c || !cpu) return;
    jet_log(c, 0, "guest backtrace (%s)%s%s",
            why ? why : "?",
            c->abort_msg[0] ? ": " : "",
            c->abort_msg[0] ? c->abort_msg : "");
    uint64_t fp = cpu->x[29];
    uint64_t lr = cpu->x[30];
    uint64_t pc = cpu->pc;
    for (int frame = 0; frame < 32; frame++) {
        /* Resolve PC against loaded libs */
        const char *libn = "?";
        uint64_t base = 0, off = pc;
        for (jet_lib *L = c->libs; L; L = L->next) {
            if (pc >= L->load_bias && pc < L->load_bias + L->map_size) {
                libn = L->path ? L->path : "lib";
                base = L->load_bias;
                off = pc - base;
                break;
            }
        }
        if (base)
            jet_log(c, 0, "  #%d pc=%llx %s+0x%llx lr=%llx fp=%llx",
                    frame, (unsigned long long)pc, libn, (unsigned long long)off,
                    (unsigned long long)lr, (unsigned long long)fp);
        else
            jet_log(c, 0, "  #%d pc=%llx lr=%llx fp=%llx",
                    frame, (unsigned long long)pc,
                    (unsigned long long)lr, (unsigned long long)fp);
        if (!fp || fp < 0x1000) break;
        /* AAPCS64 frame: [fp]=saved fp, [fp+8]=saved lr */
        uint64_t *slot = (uint64_t *)G2H(fp);
        uint64_t next_fp = slot[0];
        uint64_t next_lr = slot[1];
        if (next_fp == fp) break;
        pc = lr;
        lr = next_lr;
        fp = next_fp;
    }
}

static uint64_t h_android_set_abort_message(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    const char *msg = a->x[0] ? (const char *)G2H(a->x[0]) : "";
    if (!msg) msg = "";
    snprintf(c->abort_msg, sizeof c->abort_msg, "%s", msg);
    jet_log(c, 0, "android_set_abort_message: %s", c->abort_msg);
    fprintf(stderr, "[jet:E] abort_message: %s\n", c->abort_msg);
    fflush(stderr);
    return 0;
}

static uint64_t h_abort(jet_ctx *c, const jet_args *a, void *u){
    (void)a;(void)u;
    jet_cpu *cpu = jet_cpu_current(c);
    jet_log(c,0,"guest abort() pc=%llx lr=%llx x0=%llx x1=%llx sp=%llx last_trap=%s abort_msg=%s",
            (unsigned long long)(cpu?cpu->pc:0),
            (unsigned long long)(cpu?cpu->x[30]:0),
            (unsigned long long)(cpu?cpu->x[0]:0),
            (unsigned long long)(cpu?cpu->x[1]:0),
            (unsigned long long)(cpu?cpu->sp:0),
            c->last_trap_name[0]?c->last_trap_name:"-",
            c->abort_msg[0]?c->abort_msg:"-");
    jet_dump_guest_backtrace(c, cpu, "abort");
    if (cpu) {
        if (c->abort_msg[0])
            snprintf(cpu->err_msg, sizeof cpu->err_msg, "abort: %s", c->abort_msg);
        else
            snprintf(cpu->err_msg, sizeof cpu->err_msg,
                     "abort() lr=%llx x0=%llx last=%s",
                     (unsigned long long)cpu->x[30],
                     (unsigned long long)cpu->x[0],
                     c->last_trap_name[0]?c->last_trap_name:"-");
        cpu->err_code = 5; cpu->stop = 1;
    }
    return 0;
}

/* Format guest AAPCS64 va_list into out[]; returns length. */
static size_t jet_format_guest_va(char *out, size_t outsz, const char *fmt,
                                  jet_va_list_aapcs64 *ap) {
    size_t o = 0;
    if (!out || !outsz) return 0;
    out[0] = 0;
    if (!fmt) return 0;
    for (const char *p = fmt; *p && o + 1 < outsz; p++) {
        if (*p != '%') { out[o++] = *p; continue; }
        p++;
        int longness = 0;
        while (*p == 'l' || *p == 'z' || *p == 't' || *p == 'h' || *p == 'j') {
            if (*p == 'l') longness++;
            p++;
        }
        char tmp[192];
        switch (*p) {
        case '%': out[o++] = '%'; break;
        case 's': {
            uint64_t gp = va_arg_gr(ap);
            char *s = gp ? (char *)G2H(gp) : NULL;
            if (!s) s = "(null)";
            size_t l = strlen(s); if (o + l >= outsz) l = outsz - o - 1;
            memcpy(out + o, s, l); o += l; break;
        }
        case 'd': case 'i': {
            long long v = (long long)(int64_t)va_arg_gr(ap);
            snprintf(tmp, sizeof tmp, "%lld", v);
            size_t l = strlen(tmp); if (o + l >= outsz) l = outsz - o - 1;
            memcpy(out + o, tmp, l); o += l; break;
        }
        case 'u': case 'x': case 'X': case 'p': case 'o': {
            unsigned long long v = va_arg_gr(ap);
            const char *f = (*p == 'p') ? "%p" : (*p == 'x') ? "%llx"
                          : (*p == 'X') ? "%llX" : (*p == 'o') ? "%llo" : "%llu";
            snprintf(tmp, sizeof tmp, f, v);
            size_t l = strlen(tmp); if (o + l >= outsz) l = outsz - o - 1;
            memcpy(out + o, tmp, l); o += l; break;
        }
        case 'c': { out[o++] = (char)va_arg_gr(ap); break; }
        case 'f': case 'g': case 'e': case 'F': case 'G': case 'E': {
            (void)longness;
            if (ap->__vr_offs < 0) ap->__vr_offs += 16;
            snprintf(tmp, sizeof tmp, "0");
            size_t l = strlen(tmp); if (o + l >= outsz) l = outsz - o - 1;
            memcpy(out + o, tmp, l); o += l; break;
        }
        default:
            out[o++] = '%';
            if (*p && o + 1 < outsz) out[o++] = *p;
            break;
        }
    }
    out[o] = 0;
    return o;
}

static uint64_t h_vfprintf(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* vfprintf(FILE*, fmt, va_list) — print formatted message to stderr via jet_log. */
    const char *fmt = a->x[1] ? (const char *)G2H(a->x[1]) : "";
    jet_va_list_aapcs64 *ap = a->x[2] ? (jet_va_list_aapcs64 *)G2H(a->x[2]) : NULL;
    char buf[4096];
    if (ap) {
        jet_format_guest_va(buf, sizeof buf, fmt, ap);
    } else {
        snprintf(buf, sizeof buf, "%s", fmt ? fmt : "");
    }
    jet_log(c, 1, "vfprintf: %s", buf);
    fputs(buf, stderr);
    if (buf[0] && buf[strlen(buf)-1] != '\n') fputc('\n', stderr);
    fflush(stderr);
    /* Capture likely abort/terminate messages */
    if (fmt && (strstr(fmt, "terminat") || strstr(fmt, "exception") || strstr(buf, "terminat")))
        snprintf(c->abort_msg, sizeof c->abort_msg, "%s", buf);
    return (uint64_t)strlen(buf);
}

static uint64_t h_android_log_vprint(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    /* __android_log_vprint(prio, tag, fmt, va_list) */
    int prio = (int)a->x[0];
    const char *tag = a->x[1] ? (const char *)G2H(a->x[1]) : "?";
    const char *fmt = a->x[2] ? (const char *)G2H(a->x[2]) : "";
    jet_va_list_aapcs64 *ap = a->x[3] ? (jet_va_list_aapcs64 *)G2H(a->x[3]) : NULL;
    char buf[2048];
    if (ap) jet_format_guest_va(buf, sizeof buf, fmt, ap);
    else snprintf(buf, sizeof buf, "%s", fmt ? fmt : "");
    jet_log(c, 2, "android_log_v(p=%d,%s): %s", prio, tag, buf);
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
#if defined(__APPLE__)
    /* Linux MAP_ANONYMOUS(0x20)=Darwin MAP_ANON(0x1000) etc.; no host PROT_EXEC on iOS. */
    void *p = mmap(G2H(a->x[0]), (size_t)a->x[1], (int)a->x[2] & ~PROT_EXEC,
                   jet_xl_mmap_flags_l2d((int)a->x[3]), (int)a->x[4], (off_t)a->x[5]);
#else
    void *p = mmap(G2H(a->x[0]), (size_t)a->x[1], (int)a->x[2], (int)a->x[3], (int)a->x[4], (off_t)a->x[5]);
#endif
    return (p==MAP_FAILED) ? (uint64_t)(int64_t)-1 : H2G(p);
}
static uint64_t h_madvise(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__APPLE__)
    int adv = jet_xl_madvise_l2d((int)a->x[2]);    /* MADV_FREE 8 -> 5 */
    if (adv < 0) return 0;                          /* Linux-only advice: hint, ignore */
    return (uint64_t)(int64_t)madvise(G2H(a->x[0]), (size_t)a->x[1], adv);
#else
    return (uint64_t)(int64_t)madvise(G2H(a->x[0]), (size_t)a->x[1], (int)a->x[2]);
#endif
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
/* Linux clockid -> host clockid (-1 = unsupported). */
static int jet_clock_to_host(uint64_t id) {
#if defined(__APPLE__)
    return jet_xl_clock_l2d((int)id);
#else
    return (int)id;
#endif
}
static uint64_t h_clock_gettime(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int id = jet_clock_to_host(a->x[0]);
    if (id < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)clock_gettime((clockid_t)id, (struct timespec*)G2H(a->x[1]));
}
static uint64_t h_clock_getres(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int id = jet_clock_to_host(a->x[0]);
    if (id < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)clock_getres((clockid_t)id, a->x[1] ? (struct timespec*)G2H(a->x[1]) : NULL);
}
static uint64_t h_gettimeofday(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__APPLE__)
    /* Darwin timeval: tv_usec is int32 + pad — never let host write guest's 8-byte usec. */
    struct timeval tv;
    int rc = gettimeofday(&tv, NULL);
    if (rc == 0 && a->x[0]) {
        jet_guest_timeval *g = (jet_guest_timeval*)G2H(a->x[0]);
        g->tv_sec = (int64_t)tv.tv_sec; g->tv_usec = (int64_t)tv.tv_usec;
    }
    if (a->x[1]) memset(G2H(a->x[1]), 0, 8);   /* struct timezone */
    return (uint64_t)(int64_t)rc;
#else
    return (uint64_t)(int64_t)gettimeofday((struct timeval*)G2H(a->x[0]), NULL);
#endif
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
static uint64_t h_uname(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct utsname hu;
    int rc = uname(&hu);
    if (rc == 0 && a->x[0]) {
        jet_guest_utsname gu;
        jet_host_utsname_to_guest(&hu, &gu);
        memcpy(G2H(a->x[0]), &gu, sizeof gu);
    }
    return (uint64_t)(int64_t)rc;
}
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

/* ---- FD tracking: real FDs + Darwin kqueue-backed epoll ---- */
static int jet_fake_fd_next = 100;
static uint8_t jet_fake_fd_map[4096];
static int jet_fake_fd_alloc(void) {
    int fd = jet_fake_fd_next++;
    if (jet_fake_fd_next >= 4000) jet_fake_fd_next = 100;
    if (fd >= 0 && fd < 4096) jet_fake_fd_map[fd] = 1;
    return fd;
}
static int jet_is_fake_fd(int fd) {
    return fd >= 0 && fd < 4096 && jet_fake_fd_map[fd];
}

/* DIR* table for getdents64 / opendir emulation (fd or cookie → DIR*). */
#define JET_DIR_SLOTS 64
static struct { int used; int fd; DIR *dp; long tell; } jet_dirs[JET_DIR_SLOTS];
static int jet_dir_alloc(DIR *dp, int fd) {
    for (int i = 0; i < JET_DIR_SLOTS; i++) if (!jet_dirs[i].used) {
        jet_dirs[i].used = 1; jet_dirs[i].fd = fd; jet_dirs[i].dp = dp; jet_dirs[i].tell = 0;
        return i;
    }
    return -1;
}
static void jet_dir_free_slot(int i) {
    if (i < 0 || i >= JET_DIR_SLOTS) return;
    if (jet_dirs[i].dp) closedir(jet_dirs[i].dp);
    memset(&jet_dirs[i], 0, sizeof jet_dirs[i]);
}

#if defined(__APPLE__)
#  include <sys/event.h>
#  include <sys/mount.h>
/* epoll-on-kqueue: semantics live in epoll_kq.c (portable, unit-tested);
 * this is the real kevent backend. jet_kevent is static_assert'ed == struct kevent. */
static int jet_kevent_real(void *u, int kq, const jet_kevent *chg, int nchg,
                           jet_kevent *ev, int nev, int timeout_ms) {
    (void)u;
    struct timespec ts, *tsp = NULL;
    if (timeout_ms >= 0) { ts.tv_sec = timeout_ms / 1000; ts.tv_nsec = (long)(timeout_ms % 1000) * 1000000L; tsp = &ts; }
    return kevent(kq, (const struct kevent *)chg, nchg, (struct kevent *)ev, nev, tsp);
}
#endif

#if defined(__linux__)
#  include <sys/epoll.h>
#  include <sys/statfs.h>
#  include <sys/statvfs.h>
#  include <sys/syscall.h>
#endif

static uint64_t h_socket(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
#if defined(__APPLE__)
    /* Real sockets on Darwin so Telegram network can progress under kqueue epoll.
     * Linux AF_INET6=10 -> 30; SOCK_NONBLOCK/SOCK_CLOEXEC are not type bits on Darwin. */
    int nb = 0, cx = 0;
    int dom = jet_xl_af_l2d((int)a->x[0]);
    int type = jet_xl_socktype_l2d((int)a->x[1], &nb, &cx);
    if (dom < 0) { errno = EAFNOSUPPORT; return (uint64_t)(int64_t)-1; }
    int fd = socket(dom, type, (int)a->x[2]);
    if (fd >= 0) {
        int one = 1;
        setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, &one, sizeof one); /* MSG_NOSIGNAL equivalent */
        if (nb) fcntl(fd, F_SETFL, fcntl(fd, F_GETFL) | O_NONBLOCK);
        if (cx) fcntl(fd, F_SETFD, FD_CLOEXEC);
    }
    jet_log(c, 3, "socket(%d->%d) -> %d", (int)a->x[0], dom, fd);
    return (uint64_t)(int64_t)fd;
#else
    /* Linux harness: keep fake sockets (partner network stub) unless JET_REAL_SOCKETS=1 */
    const char *real = getenv("JET_REAL_SOCKETS");
    if (real && real[0] == '1') {
        int fd = socket((int)a->x[0], (int)a->x[1], (int)a->x[2]);
        return (uint64_t)(int64_t)fd;
    }
    int fd = jet_fake_fd_alloc();
    jet_log(c, 3, "socket() -> fake fd %d", fd);
    return (uint64_t)(int64_t)fd;
#endif
}

static uint64_t h_epoll_create(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a;
#if defined(__APPLE__)
    int kq = kqueue();
    if (kq < 0) return (uint64_t)(int64_t)-1;
    if (!jet_ekq_registry_add(kq, jet_kevent_real, NULL)) { close(kq); errno = EMFILE; return (uint64_t)(int64_t)-1; }
    jet_log(c, 3, "epoll_create -> kq %d", kq);
    return (uint64_t)(int64_t)kq;
#elif defined(__linux__)
    int fd = epoll_create1(0);
    jet_log(c, 3, "epoll_create -> %d", fd);
    return (uint64_t)(int64_t)fd;
#else
    return (uint64_t)(int64_t)jet_fake_fd_alloc();
#endif
}
static uint64_t h_epoll_create1(jet_ctx *c, const jet_args *a, void *u) {
#if defined(__linux__)
    (void)u;
    int flags = (int)a->x[0];
    int fd = epoll_create1(flags);
    jet_log(c, 3, "epoll_create1(%d) -> %d", flags, fd);
    return (uint64_t)(int64_t)fd;
#else
    uint64_t r = h_epoll_create(c, a, u);
    if ((int64_t)r >= 0 && (a->x[0] & 0x80000)) fcntl((int)r, F_SETFD, FD_CLOEXEC); /* EPOLL_CLOEXEC */
    return r;
#endif
}

/* Linux epoll event flags (guest) */
#ifndef EPOLLIN
#define EPOLLIN 0x001
#define EPOLLOUT 0x004
#define EPOLLERR 0x008
#define EPOLLHUP 0x010
#define EPOLLET 0x80000000u
#define EPOLLONESHOT 0x40000000u
#define EPOLL_CTL_ADD 1
#define EPOLL_CTL_DEL 2
#define EPOLL_CTL_MOD 3
#endif

static uint64_t h_epoll_ctl(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)c;
    int epfd = (int)a->x[0], op = (int)a->x[1], fd = (int)a->x[2];
    jet_guest_epoll_event *gev = a->x[3] ? (jet_guest_epoll_event *)G2H(a->x[3]) : NULL;
#if defined(__linux__)
    struct epoll_event hev;
    memset(&hev, 0, sizeof hev);
    if (gev) jet_guest_epoll_to_host(gev, &hev);
    return (uint64_t)(int64_t)epoll_ctl(epfd, op, fd, gev ? &hev : NULL);
#elif defined(__APPLE__)
    jet_ekq *e = jet_ekq_registry_get(epfd);
    if (!e) { errno = EBADF; return (uint64_t)(int64_t)-1; }
    if (op != 2 && !gev) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
    return jet_ret_lerr(jet_ekq_ctl(e, op, fd, gev ? gev->events : 0, gev ? gev->data.u64 : 0));
#else
    (void)epfd;(void)op;(void)fd;(void)gev; return 0;
#endif
}

static uint64_t h_epoll_wait(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)c;
    int epfd = (int)a->x[0];
    jet_guest_epoll_event *gev = a->x[1] ? (jet_guest_epoll_event *)G2H(a->x[1]) : NULL;
    int maxevents = (int)a->x[2];
    int timeout = (int)a->x[3];
    if (maxevents <= 0 || !gev) return 0;
#if defined(__linux__)
    struct epoll_event *hev = calloc((size_t)maxevents, sizeof *hev);
    if (!hev) { errno = ENOMEM; return (uint64_t)(int64_t)-1; }
    int n = epoll_wait(epfd, hev, maxevents, timeout);
    for (int i = 0; i < n; i++) jet_host_epoll_to_guest(&hev[i], &gev[i]);
    free(hev);
    return (uint64_t)(int64_t)n;
#elif defined(__APPLE__)
    jet_ekq *e = jet_ekq_registry_get(epfd);
    if (!e) { errno = EBADF; return (uint64_t)(int64_t)-1; }
    return jet_ret_lerr(jet_ekq_wait(e, gev, maxevents, timeout));
#else
    (void)epfd;(void)timeout;
    struct timespec ts = {0, 50 * 1000 * 1000};
    nanosleep(&ts, NULL);
    return 0;
#endif
}

static uint64_t h_eventfd(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)a; (void)c;
#if defined(__linux__)
    return (uint64_t)(int64_t)eventfd((unsigned)a->x[0], (int)a->x[1]);
#else
    /* Darwin: counter on a pipe pair (darwin_abi.c); read/write/close are routed by fd. */
    return (uint64_t)(int64_t)jet_efd_create((unsigned)a->x[0], (int)a->x[1]);
#endif
}
static uint64_t h_eventfd_read(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    int fd = (int)a->x[0]; uint64_t v = 0; long r;
#if defined(__APPLE__)
    if (jet_efd_is(fd)) r = jet_efd_read(fd, &v, 8); else
#endif
    r = (long)read(fd, &v, 8);
    if (r != 8) { if (r >= 0) errno = EINVAL; return (uint64_t)(int64_t)-1; }
    if (a->x[1]) memcpy(G2H(a->x[1]), &v, 8);
    return 0;
}
static uint64_t h_eventfd_write(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    int fd = (int)a->x[0]; uint64_t v = a->x[1]; long r;
#if defined(__APPLE__)
    if (jet_efd_is(fd)) r = jet_efd_write(fd, &v, 8); else
#endif
    r = (long)write(fd, &v, 8);
    return r == 8 ? 0 : (uint64_t)(int64_t)-1;
}
static uint64_t h_pipe_fn(jet_ctx *c, const jet_args *a, void *u) {
    (void)u; (void)c;
    int hs[2];
    if (pipe(hs) != 0) return (uint64_t)(int64_t)-1;
    int *p = a->x[0] ? (int*)G2H(a->x[0]) : NULL;
    if (p) { p[0] = hs[0]; p[1] = hs[1]; }
    return 0;
}

/* sockaddr-aware bind/connect/accept/sendto/recvfrom/getsockname/getpeername */
static uint64_t h_bind_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return 0;
    uint8_t host_sa[128];
    int hlen = jet_guest_sockaddr_to_host(a->x[1] ? G2H(a->x[1]) : NULL, (uint32_t)a->x[2],
                                          host_sa, sizeof host_sa);
    if (hlen < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)bind(fd, (struct sockaddr*)host_sa, (socklen_t)hlen);
}
static uint64_t h_connect(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) {
        jet_log(c, 2, "connect(fd=%d) stub → EINPROGRESS", fd);
        errno = EINPROGRESS; return (uint64_t)(int64_t)-1;
    }
    uint8_t host_sa[128];
    int hlen = jet_guest_sockaddr_to_host(a->x[1] ? G2H(a->x[1]) : NULL, (uint32_t)a->x[2],
                                          host_sa, sizeof host_sa);
    if (hlen < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)connect(fd, (struct sockaddr*)host_sa, (socklen_t)hlen);
}
static uint64_t h_listen_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return 0;
    return (uint64_t)(int64_t)listen(fd, (int)a->x[1]);
}
static uint64_t h_accept(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) { errno = EAGAIN; return (uint64_t)(int64_t)-1; }
    uint8_t host_sa[128];
    socklen_t hlen = sizeof host_sa;
    int nfd = accept(fd, (struct sockaddr*)host_sa, &hlen);
    if (nfd < 0) return (uint64_t)(int64_t)-1;
#if defined(__APPLE__)
    { int one = 1; setsockopt(nfd, SOL_SOCKET, SO_NOSIGPIPE, &one, sizeof one); }
#endif
    if (a->x[1] && a->x[2]) {
        uint32_t *glenp = (uint32_t*)G2H(a->x[2]);
        uint32_t cap = glenp ? *glenp : 128;
        int gl = jet_host_sockaddr_to_guest(host_sa, hlen, G2H(a->x[1]), cap);
        if (glenp && gl >= 0) *glenp = (uint32_t)gl;
    }
    return (uint64_t)(int64_t)nfd;
}
static uint64_t h_getsockname_peer(jet_ctx *c, const jet_args *a, void *u, int peer){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint8_t host_sa[128];
    socklen_t hlen = sizeof host_sa;
    int rc = peer ? getpeername(fd, (struct sockaddr*)host_sa, &hlen)
                  : getsockname(fd, (struct sockaddr*)host_sa, &hlen);
    if (rc != 0) return (uint64_t)(int64_t)rc;
    if (a->x[1] && a->x[2]) {
        uint32_t *glenp = (uint32_t*)G2H(a->x[2]);
        uint32_t cap = glenp ? *glenp : 128;
        int gl = jet_host_sockaddr_to_guest(host_sa, hlen, G2H(a->x[1]), cap);
        if (glenp && gl >= 0) *glenp = (uint32_t)gl;
    }
    return 0;
}
static uint64_t h_getsockname(jet_ctx *c, const jet_args *a, void *u){ return h_getsockname_peer(c,a,u,0); }
static uint64_t h_getpeername(jet_ctx *c, const jet_args *a, void *u){ return h_getsockname_peer(c,a,u,1); }

static int jet_msgflags_to_host(uint64_t f) {
#if defined(__APPLE__)
    return jet_xl_msgflags_l2d((int)f);   /* MSG_NOSIGNAL -> SO_NOSIGPIPE set at socket() */
#else
    return (int)f;
#endif
}
static uint64_t h_sendto(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return a->x[2];
    const void *buf = a->x[1] ? G2H(a->x[1]) : NULL;
    size_t len = (size_t)a->x[2];
    int flags = jet_msgflags_to_host(a->x[3]);
    if (!a->x[4] || !a->x[5]) return (uint64_t)(int64_t)send(fd, buf, len, flags);
    uint8_t host_sa[128];
    int hlen = jet_guest_sockaddr_to_host(G2H(a->x[4]), (uint32_t)a->x[5], host_sa, sizeof host_sa);
    if (hlen < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)sendto(fd, buf, len, flags, (struct sockaddr*)host_sa, (socklen_t)hlen);
}
static uint64_t h_recvfrom(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) { errno = EAGAIN; return (uint64_t)(int64_t)-1; }
    void *buf = a->x[1] ? G2H(a->x[1]) : NULL;
    size_t len = (size_t)a->x[2];
    int flags = jet_msgflags_to_host(a->x[3]);
    if (!a->x[4]) return (uint64_t)(int64_t)recv(fd, buf, len, flags);
    uint8_t host_sa[128];
    socklen_t hlen = sizeof host_sa;
    ssize_t n = recvfrom(fd, buf, len, flags, (struct sockaddr*)host_sa, &hlen);
    if (n < 0) return (uint64_t)(int64_t)-1;
    if (a->x[5]) {
        uint32_t *glenp = (uint32_t*)G2H(a->x[5]);
        uint32_t cap = glenp ? *glenp : 128;
        int gl = jet_host_sockaddr_to_guest(host_sa, hlen, G2H(a->x[4]), cap);
        if (glenp && gl >= 0) *glenp = (uint32_t)gl;
    } else {
        jet_host_sockaddr_to_guest(host_sa, hlen, G2H(a->x[4]), 128);
    }
    return (uint64_t)(int64_t)n;
}
static uint64_t h_send(jet_ctx *c, const jet_args *a, void *u){
    jet_args a2 = *a; a2.x[4]=0; a2.x[5]=0; return h_sendto(c,&a2,u);
}
static uint64_t h_recv(jet_ctx *c, const jet_args *a, void *u){
    jet_args a2 = *a; a2.x[4]=0; a2.x[5]=0; return h_recvfrom(c,&a2,u);
}

/* sendmsg/recvmsg — convert guest msghdr; iov bases are guest ptrs (identity-mapped).
 * Control data: Linux cmsghdr (16-byte hdr, 8-aligned) vs Darwin (12-byte hdr, 4-aligned)
 * is re-packed through ctrl[] on Darwin; Linux host uses the guest buffer directly. */
#define JET_CMSG_BUF 2048
static int guest_msghdr_to_host(const jet_guest_msghdr *gm, struct msghdr *hm,
                                struct iovec *iov_storage, int iov_cap,
                                uint8_t *name_storage, size_t name_cap,
                                socklen_t *name_len_out,
                                uint8_t *ctrl, size_t ctrl_cap, int for_recv) {
    memset(hm, 0, sizeof *hm);
    if (gm->msg_iovlen > (uint64_t)iov_cap) return -1;
    jet_guest_iovec *giov = gm->msg_iov ? (jet_guest_iovec*)G2H(gm->msg_iov) : NULL;
    for (uint64_t i = 0; i < gm->msg_iovlen; i++) {
        iov_storage[i].iov_base = giov[i].iov_base ? G2H(giov[i].iov_base) : NULL;
        iov_storage[i].iov_len = (size_t)giov[i].iov_len;
    }
    hm->msg_iov = iov_storage;
    hm->msg_iovlen =
#if defined(__APPLE__)
        (int)
#endif
        gm->msg_iovlen;
    if (gm->msg_name && gm->msg_namelen && !for_recv) {
        int hl = jet_guest_sockaddr_to_host(G2H(gm->msg_name), gm->msg_namelen,
                                            name_storage, name_cap);
        if (hl < 0) return -1;
        hm->msg_name = name_storage;
        hm->msg_namelen = (socklen_t)hl;
        if (name_len_out) *name_len_out = (socklen_t)hl;
    }
    if (gm->msg_control && gm->msg_controllen) {
#if defined(__APPLE__)
        if (for_recv) {
            hm->msg_control = ctrl;
            hm->msg_controllen = (socklen_t)(gm->msg_controllen < ctrl_cap ? gm->msg_controllen : ctrl_cap);
        } else {
            long n = jet_xl_cmsg_l2bsd(G2H(gm->msg_control), (size_t)gm->msg_controllen, ctrl, ctrl_cap);
            if (n < 0) return -1;
            hm->msg_control = n ? ctrl : NULL;
            hm->msg_controllen = (socklen_t)n;
        }
#else
        (void)ctrl; (void)ctrl_cap;
        hm->msg_control = G2H(gm->msg_control);
        hm->msg_controllen = gm->msg_controllen;
#endif
    }
    hm->msg_flags = 0;
    return 0;
}

static uint64_t h_sendmsg(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return 0;
    jet_guest_msghdr *gm = a->x[1] ? (jet_guest_msghdr*)G2H(a->x[1]) : NULL;
    if (!gm) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    struct iovec iov[64];
    uint8_t name[128];
    _Alignas(8) uint8_t ctrl[JET_CMSG_BUF];
    struct msghdr hm;
    if (guest_msghdr_to_host(gm, &hm, iov, 64, name, sizeof name, NULL, ctrl, sizeof ctrl, 0) < 0) {
        errno = EINVAL; return (uint64_t)(int64_t)-1;
    }
    return (uint64_t)(int64_t)sendmsg(fd, &hm, jet_msgflags_to_host(a->x[2]));
}
static uint64_t h_recvmsg(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) { errno = EAGAIN; return (uint64_t)(int64_t)-1; }
    jet_guest_msghdr *gm = a->x[1] ? (jet_guest_msghdr*)G2H(a->x[1]) : NULL;
    if (!gm) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    struct iovec iov[64];
    uint8_t name[128];
    _Alignas(8) uint8_t ctrl[JET_CMSG_BUF];
    struct msghdr hm;
    if (guest_msghdr_to_host(gm, &hm, iov, 64, name, sizeof name, NULL, ctrl, sizeof ctrl, 1) < 0) {
        errno = EINVAL; return (uint64_t)(int64_t)-1;
    }
    if (gm->msg_name) { hm.msg_name = name; hm.msg_namelen = sizeof name; }
    ssize_t n = recvmsg(fd, &hm, jet_msgflags_to_host(a->x[2]));
    if (n < 0) return (uint64_t)(int64_t)-1;
    if (gm->msg_name) {
        if (hm.msg_namelen) {
            int gl = jet_host_sockaddr_to_guest(name, hm.msg_namelen, G2H(gm->msg_name), gm->msg_namelen);
            if (gl >= 0) gm->msg_namelen = (uint32_t)gl;
        } else gm->msg_namelen = 0;
    }
#if defined(__APPLE__)
    int ctrunc = 0;
    if (gm->msg_control && gm->msg_controllen)
        gm->msg_controllen = (uint64_t)jet_xl_cmsg_bsd2l(ctrl, hm.msg_controllen, G2H(gm->msg_control),
                                                         (size_t)gm->msg_controllen, &ctrunc);
    else gm->msg_controllen = 0;
    gm->msg_flags = jet_xl_msgflags_d2l(hm.msg_flags) | (ctrunc ? 0x8 : 0);
#else
    gm->msg_controllen = (uint64_t)hm.msg_controllen;
    gm->msg_flags = hm.msg_flags;
#endif
    return (uint64_t)(int64_t)n;
}

static uint64_t h_setsockopt(jet_ctx *c, const jet_args *a, void *u){
    (void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return 0;
#if defined(__APPLE__)
    int dl, dopt;
    int kind = jet_xl_sockopt_l2d((int)a->x[1], (int)a->x[2], &dl, &dopt);
    if (kind < 0) {   /* Linux-only knob (SO_PRIORITY, TCP_USER_TIMEOUT, ...): accept as no-op */
        jet_log(c, 3, "setsockopt(%d,%d) ignored on Darwin", (int)a->x[1], (int)a->x[2]);
        return 0;
    }
    if (kind == 1) {   /* SO_RCVTIMEO/SO_SNDTIMEO: guest timeval{i64,i64} -> Darwin{i64,i32} */
        if (!a->x[3] || a->x[4] < sizeof(jet_guest_timeval)) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
        jet_darwin_timeval dtv; jet_xl_timeval_l2d((const jet_guest_timeval*)G2H(a->x[3]), &dtv);
        return (uint64_t)(int64_t)setsockopt(fd, dl, dopt, &dtv, sizeof(struct timeval));
    }
    return (uint64_t)(int64_t)setsockopt(fd, dl, dopt, a->x[3] ? G2H(a->x[3]) : NULL, (socklen_t)a->x[4]);
#else
    (void)c;
    return (uint64_t)(int64_t)setsockopt(fd, (int)a->x[1], (int)a->x[2],
        a->x[3] ? G2H(a->x[3]) : NULL, (socklen_t)a->x[4]);
#endif
}
static uint64_t h_getsockopt(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return 0;
    socklen_t len = a->x[4] ? *(socklen_t*)G2H(a->x[4]) : 0;
#if defined(__APPLE__)
    int dl, dopt;
    int kind = jet_xl_sockopt_l2d((int)a->x[1], (int)a->x[2], &dl, &dopt);
    if (kind < 0) { errno = ENOPROTOOPT; return (uint64_t)(int64_t)-1; }
    if (kind == 1) {
        jet_darwin_timeval dtv; socklen_t dl2 = sizeof(struct timeval);
        int rc = getsockopt(fd, dl, dopt, &dtv, &dl2);
        if (rc == 0 && a->x[3] && len >= sizeof(jet_guest_timeval)) {
            jet_xl_timeval_d2l(&dtv, (jet_guest_timeval*)G2H(a->x[3]));
            *(uint32_t*)G2H(a->x[4]) = (uint32_t)sizeof(jet_guest_timeval);
        } else if (rc == 0) { errno = EINVAL; rc = -1; }
        return (uint64_t)(int64_t)rc;
    }
    int rc = getsockopt(fd, dl, dopt, a->x[3] ? G2H(a->x[3]) : NULL, a->x[4] ? &len : NULL);
    if (rc == 0 && kind == 2 && a->x[3] && len >= 4) {   /* SO_ERROR carries an errno */
        int *e = (int*)G2H(a->x[3]); *e = jet_xl_errno_d2l(*e);
    }
#else
    int rc = getsockopt(fd, (int)a->x[1], (int)a->x[2],
        a->x[3] ? G2H(a->x[3]) : NULL, a->x[4] ? &len : NULL);
#endif
    if (rc == 0 && a->x[4]) *(uint32_t*)G2H(a->x[4]) = (uint32_t)len;
    return (uint64_t)(int64_t)rc;
}

static uint64_t h_poll_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)u; (void)c;
    jet_guest_pollfd *gp = a->x[0] ? (jet_guest_pollfd*)G2H(a->x[0]) : NULL;
    nfds_t nfds = (nfds_t)a->x[1];
    int timeout = (int)a->x[2];
    if (!gp || nfds == 0) {
        if (timeout > 0) { struct timespec ts = { timeout/1000, (timeout%1000)*1000000L }; nanosleep(&ts, NULL); }
        return 0;
    }
    /* Guest pollfd layout == host (8 bytes). Event bits differ on Darwin above POLLRDBAND. */
    for (nfds_t i = 0; i < nfds; i++) if (jet_is_fake_fd(gp[i].fd)) {
        if (timeout != 0) { struct timespec ts = {0, 20*1000*1000}; nanosleep(&ts, NULL); }
        return 0;
    }
#if defined(__APPLE__)
    struct pollfd stackp[64], *hp = nfds <= 64 ? stackp : calloc(nfds, sizeof *hp);
    if (!hp) { errno = ENOMEM; return (uint64_t)(int64_t)-1; }
    for (nfds_t i = 0; i < nfds; i++) {
        hp[i].fd = gp[i].fd; hp[i].events = jet_xl_poll_l2d(gp[i].events); hp[i].revents = 0;
    }
    int rc = poll(hp, nfds, timeout);
    if (rc >= 0) for (nfds_t i = 0; i < nfds; i++) {
        int16_t r = jet_xl_poll_d2l(hp[i].revents);
        if ((r & 0x4) && (gp[i].events & 0x100)) r |= 0x100;   /* POLLWRNORM requested */
        gp[i].revents = r;
    }
    if (hp != stackp) free(hp);
    return (uint64_t)(int64_t)rc;
#else
    return (uint64_t)(int64_t)poll((struct pollfd *)gp, nfds, timeout);
#endif
}

/* ppoll(fds, nfds, const timespec *tmo, const sigset_t *mask): mask is guest-emulated. */
static uint64_t h_ppoll_fn(jet_ctx *c, const jet_args *a, void *u){
    jet_args a2 = *a;
    if (a->x[2]) {
        const jet_guest_timespec *ts = (const jet_guest_timespec*)G2H(a->x[2]);
        int64_t ms = ts->tv_sec * 1000 + (ts->tv_nsec + 999999) / 1000000;
        a2.x[2] = (uint64_t)(int64_t)(ms > 0x7fffffff ? 0x7fffffff : ms);
    } else a2.x[2] = (uint64_t)(int64_t)-1;
    return h_poll_fn(c, &a2, u);
}
static uint64_t h_select_fn(jet_ctx *c, const jet_args *a, void *u);
/* pselect(nfds, r, w, e, const timespec *tmo, const sigset_t *mask) */
static uint64_t h_pselect_fn(jet_ctx *c, const jet_args *a, void *u){
    jet_args a2 = *a;
    jet_guest_timeval tv;
    if (a->x[4]) {
        const jet_guest_timespec *ts = (const jet_guest_timespec*)G2H(a->x[4]);
        tv.tv_sec = ts->tv_sec; tv.tv_usec = ts->tv_nsec / 1000;
        a2.x[4] = H2G(&tv);
    }
    return h_select_fn(c, &a2, u);
}
static uint64_t h_select_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int nfds = (int)a->x[0];
    jet_guest_fd_set *gr = a->x[1] ? (jet_guest_fd_set*)G2H(a->x[1]) : NULL;
    jet_guest_fd_set *gw = a->x[2] ? (jet_guest_fd_set*)G2H(a->x[2]) : NULL;
    jet_guest_fd_set *ge = a->x[3] ? (jet_guest_fd_set*)G2H(a->x[3]) : NULL;
    jet_guest_timeval *gtv = a->x[4] ? (jet_guest_timeval*)G2H(a->x[4]) : NULL;
    /* Guest fd_set is 128 bytes (1024 bits) — same as Linux. Darwin FD_SETSIZE may differ;
     * copy into host fd_set safely. */
    fd_set hr, hw, he;
    FD_ZERO(&hr); FD_ZERO(&hw); FD_ZERO(&he);
    if (gr) memcpy(&hr, gr, sizeof *gr < sizeof hr ? sizeof *gr : sizeof hr);
    if (gw) memcpy(&hw, gw, sizeof *gw < sizeof hw ? sizeof *gw : sizeof hw);
    if (ge) memcpy(&he, ge, sizeof *ge < sizeof he ? sizeof *ge : sizeof he);
    struct timeval tv, *tvp = NULL;
    if (gtv) { tv.tv_sec = (time_t)gtv->tv_sec; tv.tv_usec = (suseconds_t)gtv->tv_usec; tvp = &tv; }
    int rc = select(nfds, gr?&hr:NULL, gw?&hw:NULL, ge?&he:NULL, tvp);
    if (rc >= 0) {
        if (gr) memcpy(gr, &hr, sizeof *gr);
        if (gw) memcpy(gw, &hw, sizeof *gw);
        if (ge) memcpy(ge, &he, sizeof *ge);
    }
    return (uint64_t)(int64_t)rc;
}


/* ---- statfs / statvfs ---- */
static uint64_t h_statfs(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__linux__)
    struct statfs hs;
    int rc = statfs((char*)G2H(a->x[0]), &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statfs gs; jet_host_statfs_to_guest(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#elif defined(__APPLE__)
    struct statfs hs;
    int rc = statfs((char*)G2H(a->x[0]), &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statfs gs; jet_host_statfs_to_guest_apple(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#else
    (void)a; errno = ENOSYS; return (uint64_t)(int64_t)-1;
#endif
}
static uint64_t h_fstatfs(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__linux__)
    struct statfs hs;
    int rc = fstatfs((int)a->x[0], &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statfs gs; jet_host_statfs_to_guest(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#elif defined(__APPLE__)
    struct statfs hs;
    int rc = fstatfs((int)a->x[0], &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statfs gs; jet_host_statfs_to_guest_apple(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#else
    (void)a; errno = ENOSYS; return (uint64_t)(int64_t)-1;
#endif
}
static uint64_t h_statvfs(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__linux__)
    struct statvfs hs;
    int rc = statvfs((char*)G2H(a->x[0]), &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statvfs gs; jet_host_statvfs_to_guest(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#elif defined(__APPLE__)
    /* Darwin statvfs uses 32-bit fsblkcnt_t; synthesize from 64-bit statfs instead. */
    struct statfs hs;
    int rc = statfs((char*)G2H(a->x[0]), &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statvfs gs; jet_host_statfs_to_guest_statvfs_apple(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#else
    (void)a; errno = ENOSYS; return (uint64_t)(int64_t)-1;
#endif
}
static uint64_t h_fstatvfs(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
#if defined(__linux__)
    struct statvfs hs;
    int rc = fstatvfs((int)a->x[0], &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statvfs gs; jet_host_statvfs_to_guest(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#elif defined(__APPLE__)
    struct statfs hs;
    int rc = fstatfs((int)a->x[0], &hs);
    if (rc == 0 && a->x[1]) {
        jet_guest_statvfs gs; jet_host_statfs_to_guest_statvfs_apple(&hs, &gs);
        memcpy(G2H(a->x[1]), &gs, sizeof gs);
    }
    return (uint64_t)(int64_t)rc;
#else
    (void)a; errno = ENOSYS; return (uint64_t)(int64_t)-1;
#endif
}

/* ---- getdents64 / opendir / readdir ---- */
static uint64_t h_getdents64(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    void *buf = a->x[1] ? G2H(a->x[1]) : NULL;
    size_t count = (size_t)a->x[2];
    if (!buf || !count) return 0;
#if defined(__linux__)
    /* Prefer raw getdents64; fall back to fdopendir/readdir packing. */
    long n = syscall(SYS_getdents64, fd, buf, count);
    if (n >= 0) return (uint64_t)(int64_t)n;
    /* If ENOSYS etc., fall through to portable path */
#endif
    /* Portable: find/create DIR* for fd via fdopendir duplicate */
    DIR *dp = NULL; int slot = -1;
    for (int i = 0; i < JET_DIR_SLOTS; i++) if (jet_dirs[i].used && jet_dirs[i].fd == fd) {
        dp = jet_dirs[i].dp; slot = i; break;
    }
    if (!dp) {
        int dupfd = dup(fd);
        if (dupfd < 0) return (uint64_t)(int64_t)-1;
        dp = fdopendir(dupfd);
        if (!dp) { close(dupfd); return (uint64_t)(int64_t)-1; }
        slot = jet_dir_alloc(dp, fd);
        if (slot < 0) { closedir(dp); return (uint64_t)(int64_t)-1; }
    }
    size_t filled = 0;
    while (filled < count) {
        errno = 0;
        long tell_before = telldir(dp);
        struct dirent *de = readdir(dp);
        if (!de) break;
        long tell = telldir(dp);
        uint8_t type = 0;
#if defined(DT_UNKNOWN)
        type = (uint8_t)de->d_type;
#endif
        int reclen = jet_pack_guest_dirent64((uint8_t*)buf + filled, count - filled,
                                             (uint64_t)de->d_ino, (int64_t)tell, type, de->d_name);
        if (reclen < 0) {
            /* no room — rewind so next getdents64 sees this entry again */
            if (tell_before >= 0) seekdir(dp, tell_before);
            break;
        }
        filled += (size_t)reclen;
        jet_dirs[slot].tell = tell;
    }
    return (uint64_t)filled;
}

static uint64_t h_opendir(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    DIR *dp = opendir(a->x[0] ? (char*)G2H(a->x[0]) : ".");
    if (!dp) return 0;
    int slot = jet_dir_alloc(dp, -1);
    if (slot < 0) { closedir(dp); return 0; }
    return H2G(&jet_dirs[slot]); /* cookie */
}
static uint64_t h_readdir(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    /* Return pointer to TLS guest dirent buffer (bionic linux_dirent64-shaped) */
    typedef struct {
        uint64_t d_ino; int64_t d_off; uint16_t d_reclen; uint8_t d_type; char d_name[256];
    } guest_dirent_fixed;
    static __thread guest_dirent_fixed gde;
    if (!a->x[0]) return 0;
    /* cookie is &jet_dirs[slot] */
    for (int i = 0; i < JET_DIR_SLOTS; i++) {
        if (!jet_dirs[i].used) continue;
        if ((void*)G2H(a->x[0]) == (void*)&jet_dirs[i] || a->x[0] == H2G(&jet_dirs[i])) {
            struct dirent *de = readdir(jet_dirs[i].dp);
            if (!de) return 0;
            memset(&gde, 0, sizeof gde);
            gde.d_ino = (uint64_t)de->d_ino;
            gde.d_off = (int64_t)telldir(jet_dirs[i].dp);
            gde.d_reclen = (uint16_t)sizeof gde;
#if defined(DT_UNKNOWN)
            gde.d_type = (uint8_t)de->d_type;
#endif
            snprintf(gde.d_name, sizeof gde.d_name, "%s", de->d_name);
            return H2G(&gde);
        }
    }
    return 0;
}
static uint64_t h_closedir(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (!a->x[0]) return 0;
    for (int i = 0; i < JET_DIR_SLOTS; i++) {
        if (jet_dirs[i].used && a->x[0] == H2G(&jet_dirs[i])) {
            jet_dir_free_slot(i); return 0;
        }
    }
    return 0;
}

/* ---- rlimit / rusage ---- */
static int jet_rlimit_res_to_host(uint64_t r) {
#if defined(__APPLE__)
    return jet_xl_rlimit_res_l2d((int)r);
#else
    return (int)r;
#endif
}
static uint64_t h_getrlimit(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int res = jet_rlimit_res_to_host(a->x[0]);
    if (res < 0) {   /* Linux-only limit (RLIMIT_SIGPENDING, ...): report unlimited */
        if (a->x[1]) { jet_guest_rlimit gr = { ~0ULL, ~0ULL }; memcpy(G2H(a->x[1]), &gr, sizeof gr); }
        return 0;
    }
    struct rlimit hr;
    int rc = getrlimit(res, &hr);
    if (rc == 0 && a->x[1]) {
        jet_guest_rlimit gr; jet_host_rlimit_to_guest(&hr, &gr);
        memcpy(G2H(a->x[1]), &gr, sizeof gr);
    }
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_setrlimit(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    jet_guest_rlimit *gr = a->x[1] ? (jet_guest_rlimit*)G2H(a->x[1]) : NULL;
    if (!gr) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
    int res = jet_rlimit_res_to_host(a->x[0]);
    if (res < 0) return 0;
    struct rlimit hr; jet_guest_rlimit_to_host(gr, &hr);
    int rc = setrlimit(res, &hr);
#if defined(__APPLE__) && defined(OPEN_MAX)
    if (rc != 0 && errno == EINVAL && res == RLIMIT_NOFILE && hr.rlim_cur > OPEN_MAX) {
        hr.rlim_cur = OPEN_MAX;       /* Darwin rejects NOFILE > OPEN_MAX */
        rc = setrlimit(res, &hr);
    }
#endif
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_getrusage(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct rusage hr;
    int who = (int)a->x[0];
#if defined(__APPLE__)
    if (who == 1) who = RUSAGE_SELF;   /* RUSAGE_THREAD has no Darwin equivalent */
#endif
    int rc = getrusage(who, &hr);
    if (rc == 0 && a->x[1]) {
        jet_guest_rusage gr; jet_host_rusage_to_guest(&hr, &gr);
        memcpy(G2H(a->x[1]), &gr, sizeof gr);
    }
    return (uint64_t)(int64_t)rc;
}

/* ---- termios ---- */
static uint64_t h_tcgetattr(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct termios ht;
    int rc = tcgetattr((int)a->x[0], &ht);
    if (rc == 0 && a->x[1]) {
        jet_guest_termios gt; jet_host_termios_to_guest(&ht, &gt);
        memcpy(G2H(a->x[1]), &gt, sizeof gt);
    }
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_tcsetattr(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    jet_guest_termios *gt = a->x[2] ? (jet_guest_termios*)G2H(a->x[2]) : NULL;
    if (!gt) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
    struct termios ht; jet_guest_termios_to_host(gt, &ht);
    return (uint64_t)(int64_t)tcsetattr((int)a->x[0], (int)a->x[1], &ht);
}


/* file IO */
/* Linux open(2) flags / AT_* -> host. */
static int jet_oflags_to_host(uint64_t f) {
#if defined(__APPLE__)
    return jet_xl_oflags_l2d((int)f);
#else
    return (int)f;
#endif
}
static int jet_atfd_to_host(uint64_t fd) {
#if defined(__APPLE__)
    return jet_xl_atfd_l2d((int)fd);
#else
    return (int)fd;
#endif
}
static int jet_atflags_to_host(uint64_t f, int for_access) {
#if defined(__APPLE__)
    return jet_xl_atflags_l2d((int)f, for_access);
#else
    (void)for_access; return (int)f;
#endif
}
static uint64_t h_open(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fl = jet_oflags_to_host(a->x[1]);
    if (fl == -1) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)open((char*)G2H(a->x[0]), fl, (mode_t)a->x[2]);
}
static uint64_t h_openat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fl = jet_oflags_to_host(a->x[2]);
    if (fl == -1) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)openat(jet_atfd_to_host(a->x[0]), (char*)G2H(a->x[1]), fl, (mode_t)a->x[3]);
}
static uint64_t h_close(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd=(int)a->x[0];
    if (jet_is_fake_fd(fd)) { jet_fake_fd_map[fd]=0; return 0; }
#if defined(__APPLE__)
    jet_ekq_registry_forget_fd(fd);              /* kqueue drops knotes; keep table in sync */
    if (jet_efd_close(fd)) return 0;
    if (jet_ekq_registry_close(fd)) return (uint64_t)(int64_t)close(fd);
#endif
    return (uint64_t)(int64_t)close(fd);
}
static uint64_t h_read(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) { errno=EAGAIN; return (uint64_t)(int64_t)-1; }
#if defined(__APPLE__)
    if (jet_efd_is(fd)) return (uint64_t)(int64_t)jet_efd_read(fd, G2H(a->x[1]), (size_t)a->x[2]);
#endif
    return (uint64_t)(int64_t)read(fd,G2H(a->x[1]),(size_t)a->x[2]);
}
static uint64_t h_write(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    int fd = (int)a->x[0];
    if (jet_is_fake_fd(fd)) return a->x[2];
#if defined(__APPLE__)
    if (jet_efd_is(fd)) return (uint64_t)(int64_t)jet_efd_write(fd, G2H(a->x[1]), (size_t)a->x[2]);
#endif
    return (uint64_t)(int64_t)write(fd,G2H(a->x[1]),(size_t)a->x[2]);
}
static uint64_t h_lseek(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)lseek((int)a->x[0],(off_t)a->x[1],(int)a->x[2]); }
static uint64_t h_fstat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct stat hs;
    int rc = fstat((int)a->x[0], &hs);
    if (rc == 0) write_guest_stat(a->x[1], &hs);
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_stat_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct stat hs;
    int rc = stat((char*)G2H(a->x[0]), &hs);
    if (rc == 0) write_guest_stat(a->x[1], &hs);
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_lstat_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct stat hs;
    int rc = lstat((char*)G2H(a->x[0]), &hs);
    if (rc == 0) write_guest_stat(a->x[1], &hs);
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_fstatat_fn(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    struct stat hs;
    int rc = fstatat(jet_atfd_to_host(a->x[0]), (const char*)G2H(a->x[1]), &hs, jet_atflags_to_host(a->x[3], 0));
    if (rc == 0) write_guest_stat(a->x[2], &hs);
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_access(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)access((char*)G2H(a->x[0]),(int)a->x[1]); }
static uint64_t h_readlink(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; return (uint64_t)(int64_t)readlink((char*)G2H(a->x[0]),(char*)G2H(a->x[1]),(size_t)a->x[2]); }

/* ---- v10.2 filesystem traps (real; were h_stub0). Flags are validated with Linux rules on
 * every host so the guest sees Linux errors; Darwin gets AT_, UTIME_ and RENAME_ values translated. ---- */
#define JL_AT_SYMLINK_NOFOLLOW 0x100
#define JL_AT_REMOVEDIR        0x200
#define JL_AT_EACCESS          0x200
#define JL_AT_SYMLINK_FOLLOW   0x400
#define JL_AT_EMPTY_PATH       0x1000
#define JET_EINVAL_RET() do { errno = EINVAL; return (uint64_t)(int64_t)-1; } while (0)
#define P(i) ((const char*)G2H(a->x[i]))
#define RET(e) return (uint64_t)(int64_t)(e)

static uint64_t h_mkdir(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(mkdir(P(0), (mode_t)a->x[1])); }
static uint64_t h_mkdirat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; RET(mkdirat(jet_atfd_to_host(a->x[0]), P(1), (mode_t)a->x[2]));
}
#if defined(__APPLE__)
/* Darwin unlink(dir) -> EPERM; Linux -> EISDIR. */
static int jet_unlink_fix(int r, int dfd, const char *p) {
    struct stat st;
    if (r < 0 && errno == EPERM && fstatat(dfd, p, &st, AT_SYMLINK_NOFOLLOW) == 0 && S_ISDIR(st.st_mode)) errno = EISDIR;
    return r;
}
#else
#define jet_unlink_fix(r, dfd, p) (r)
#endif
static uint64_t h_unlink(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(jet_unlink_fix(unlink(P(0)), AT_FDCWD, P(0))); }
static uint64_t h_rmdir(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(rmdir(P(0))); }
static uint64_t h_remove(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(remove(P(0))); }
static uint64_t h_unlinkat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[2];
    if (f & ~JL_AT_REMOVEDIR) JET_EINVAL_RET();
    int dfd = jet_atfd_to_host(a->x[0]);
    int r = unlinkat(dfd, P(1), jet_atflags_to_host(f, 0));                     /* 0x200 -> 0x80 */
    RET(f ? r : jet_unlink_fix(r, dfd, P(1)));
}
static uint64_t h_rename(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(rename(P(0), P(1))); }
static uint64_t h_renameat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; RET(renameat(jet_atfd_to_host(a->x[0]), P(1), jet_atfd_to_host(a->x[2]), P(3)));
}
static uint64_t h_renameat2(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[4];
    int fd1 = jet_atfd_to_host(a->x[0]), fd2 = jet_atfd_to_host(a->x[2]);
#if defined(__APPLE__)
    int df = jet_xl_renameat2_flags_l2d(f);
    if (df < 0) JET_EINVAL_RET();
    if (df == 0) RET(renameat(fd1, P(1), fd2, P(3)));
    RET(renameatx_np(fd1, P(1), fd2, P(3), (unsigned)df));          /* RENAME_EXCL */
#else
    if (f & ~1u) JET_EINVAL_RET();                                   /* only NOREPLACE supported */
    if (f == 0) RET(renameat(fd1, P(1), fd2, P(3)));
#  ifdef SYS_renameat2
    RET(syscall(SYS_renameat2, fd1, P(1), fd2, P(3), f));
#  else
    errno = ENOSYS; RET(-1);
#  endif
#endif
}
static uint64_t h_symlink(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(symlink(P(0), P(1))); }
static uint64_t h_symlinkat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; RET(symlinkat(P(0), jet_atfd_to_host(a->x[1]), P(2)));
}
static uint64_t h_readlinkat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if ((int64_t)a->x[3] <= 0) JET_EINVAL_RET();                     /* Linux: bufsiz <= 0 */
    RET(readlinkat(jet_atfd_to_host(a->x[0]), P(1), (char*)G2H(a->x[2]), (size_t)a->x[3]));
}
static uint64_t h_linkat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[4];
    if (f & ~(JL_AT_SYMLINK_FOLLOW | JL_AT_EMPTY_PATH)) JET_EINVAL_RET();
#if defined(__APPLE__)
    if (f & JL_AT_EMPTY_PATH) { errno = ENOENT; RET(-1); }          /* no fd-linking on Darwin */
#endif
    RET(linkat(jet_atfd_to_host(a->x[0]), P(1), jet_atfd_to_host(a->x[2]), P(3),
               jet_atflags_to_host(f, 0)));                          /* 0x400 -> 0x40 */
}
static uint64_t h_link(jet_ctx *c, const jet_args *a, void *u){
    /* Linux link() never follows a symlink source; Darwin link() may, so go via linkat(…, 0). */
    (void)c;(void)u; RET(linkat(jet_atfd_to_host((uint64_t)-100), P(0), jet_atfd_to_host((uint64_t)-100), P(1), 0));
}
static uint64_t h_access2(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (a->x[1] & ~7ull) JET_EINVAL_RET();
    RET(access(P(0), (int)a->x[1]));
}
static uint64_t h_faccessat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[3];
    if ((a->x[2] & ~7ull) || (f & ~(JL_AT_EACCESS | JL_AT_SYMLINK_NOFOLLOW | JL_AT_EMPTY_PATH))) JET_EINVAL_RET();
    RET(faccessat(jet_atfd_to_host(a->x[0]), P(1), (int)a->x[2], jet_atflags_to_host(f & ~JL_AT_EMPTY_PATH, 1)));
}
static uint64_t h_chmod(jet_ctx *c, const jet_args *a, void *u){ (void)c;(void)u; RET(chmod(P(0), (mode_t)(a->x[1] & 07777))); }
static uint64_t h_fchmod(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (jet_is_fake_fd((int)a->x[0])) return 0;
    RET(fchmod((int)a->x[0], (mode_t)(a->x[1] & 07777)));
}
static uint64_t h_fchmodat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[3];
    if (f & ~JL_AT_SYMLINK_NOFOLLOW) JET_EINVAL_RET();
    RET(fchmodat(jet_atfd_to_host(a->x[0]), P(1), (mode_t)(a->x[2] & 07777), jet_atflags_to_host(f, 0)));
}
static uint64_t h_truncate(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if ((int64_t)a->x[1] < 0) JET_EINVAL_RET();
    RET(truncate(P(0), (off_t)a->x[1]));
}
static uint64_t h_ftruncate(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (jet_is_fake_fd((int)a->x[0]) || (int64_t)a->x[1] < 0) JET_EINVAL_RET();
    RET(ftruncate((int)a->x[0], (off_t)a->x[1]));
}
static int guest_times_to_host(uint64_t gt, struct timespec ts[2]) {
    if (!gt) return 0;
    memcpy(ts, G2H(gt), 2 * sizeof(struct timespec));               /* {i64 sec, i64 nsec} on both */
#if defined(__APPLE__)
    for (int i = 0; i < 2; i++) ts[i].tv_nsec = jet_xl_utime_nsec_l2d(ts[i].tv_nsec);
#endif
    return 1;
}
static uint64_t h_utimensat(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    unsigned f = (unsigned)a->x[3];
    if (f & ~(JL_AT_SYMLINK_NOFOLLOW | JL_AT_EMPTY_PATH)) JET_EINVAL_RET();
    struct timespec ts[2]; int have = guest_times_to_host(a->x[2], ts);
    if (!a->x[1] || ((f & JL_AT_EMPTY_PATH) && !*P(1))) {           /* Linux: NULL path == futimens(dirfd) */
        if (jet_is_fake_fd((int)a->x[0])) return 0;
        RET(futimens((int)a->x[0], have ? ts : NULL));
    }
    RET(utimensat(jet_atfd_to_host(a->x[0]), P(1), have ? ts : NULL,
                  jet_atflags_to_host(f & ~JL_AT_EMPTY_PATH, 0)));
}
static uint64_t h_futimens(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (jet_is_fake_fd((int)a->x[0])) return 0;
    struct timespec ts[2]; int have = guest_times_to_host(a->x[1], ts);
    RET(futimens((int)a->x[0], have ? ts : NULL));
}
static uint64_t h_fsync(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (jet_is_fake_fd((int)a->x[0])) return 0;
    RET(fsync((int)a->x[0]));
}
static uint64_t h_fdatasync(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (jet_is_fake_fd((int)a->x[0])) return 0;
#if defined(__APPLE__)
    RET(fsync((int)a->x[0]));                                       /* fdatasync not in all SDKs */
#else
    RET(fdatasync((int)a->x[0]));
#endif
}
#undef P
#undef RET

/* ---- v10.2 netdb (guest bionic addrinfo; see netdb_abi.c) ---- */
static uint64_t h_getaddrinfo2(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    if (!a->x[3]) return JG_EAI_FAIL;
    uint64_t res = 0;
    int rc = jet_getaddrinfo_guest(a->x[0] ? (const char*)G2H(a->x[0]) : NULL,
                                   a->x[1] ? (const char*)G2H(a->x[1]) : NULL,
                                   a->x[2] ? (const jet_guest_addrinfo*)G2H(a->x[2]) : NULL, &res);
    *(uint64_t*)G2H(a->x[3]) = res;
    return (uint64_t)(int64_t)rc;
}
static uint64_t h_freeaddrinfo(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; jet_freeaddrinfo_guest(a->x[0]); return 0;
}
static uint64_t h_getnameinfo(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;
    return (uint64_t)(int64_t)jet_getnameinfo_guest(a->x[0] ? G2H(a->x[0]) : NULL, (uint32_t)a->x[1],
        a->x[2] ? (char*)G2H(a->x[2]) : NULL, (size_t)a->x[3],
        a->x[4] ? (char*)G2H(a->x[4]) : NULL, (size_t)a->x[5], (int)a->x[6]);
}
static uint64_t h_gai_strerror(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return H2G((void*)jet_gai_strerror_guest((int)a->x[0]));
}
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
    /* Bionic: pthread_once_t is int (0=never, 1=done). Never hold a host
     * mutex across the guest init — nested once deadlocks. Track the once
     * control being initialized on this thread so nested sees "in progress"
     * and returns 0 (glibc/bionic both define that nested once on the same
     * control is undefined; returning is the practical choice). */
    static __thread volatile int *tls_in_once;
    volatile int *flag = (volatile int*)G2H(a->x[0]);
    uint64_t init_fn = a->x[1];
    for (;;) {
        int cur = __atomic_load_n(flag, __ATOMIC_ACQUIRE);
        if (cur == 1) return 0;
        if (tls_in_once == flag) return 0; /* nested on same control */
        if (cur == 0) {
            int expected = 0;
            if (__atomic_compare_exchange_n(flag, &expected, 2 /*running*/,
                    0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
                tls_in_once = flag;
                jet_args aa = {0};
                jet_native_call(c, init_fn, &aa, NULL, NULL);
                tls_in_once = NULL;
                __atomic_store_n(flag, 1, __ATOMIC_RELEASE);
                return 0;
            }
            continue;
        }
        /* running on another thread — wait */
        while (__atomic_load_n(flag, __ATOMIC_ACQUIRE) == 2) { }
    }
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

/* gettext family — return the msgid pointer (no catalog). */
static uint64_t h_gettext(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return a->x[0]; /* char *gettext(const char *) */
}
static uint64_t h_dgettext(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return a->x[1]; /* dgettext(domain, msgid) */
}
static uint64_t h_dcgettext(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return a->x[1];
}
static uint64_t h_ngettext(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return a->x[2]==1 ? a->x[0] : a->x[1];
}
static uint64_t h_textdomain(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u;(void)a; return 0;
}
static uint64_t h_bindtextdomain(jet_ctx *c, const jet_args *a, void *u){
    (void)c;(void)u; return a->x[1];
}

/* dl* */
static uint64_t h_dlopen(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return H2G(dlopen((char*)G2H(a->x[0]),(int)a->x[1])); }
static uint64_t h_dlsym(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return H2G(dlsym(G2H(a->x[0]),(char*)G2H(a->x[1]))); }
static uint64_t h_dlclose(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)dlclose(G2H(a->x[0])); }
static uint64_t h_dlerror(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)a;(void)u; return H2G(dlerror()); }
static uint64_t h_dladdr(jet_ctx *c,const jet_args *a,void *u){ (void)c;(void)u; return (uint64_t)(int64_t)dladdr(G2H(a->x[0]),(Dl_info*)G2H(a->x[1])); }

/* Linux dl_phdr_info (LP64) — guest AArch64 uses the same layout. */
struct jet_dl_phdr_info {
    uint64_t dlpi_addr;
    const char *dlpi_name;
    const void *dlpi_phdr;
    uint16_t dlpi_phnum;
    uint64_t dlpi_adds;
    uint64_t dlpi_subs;
    size_t   dlpi_tls_modid;
    void    *dlpi_tls_data;
};

static uint64_t h_dl_iterate_phdr(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    uint64_t cb = a->x[0];
    uint64_t data = a->x[1];
    if (!cb) return (uint64_t)(int64_t)-1;
    int rc = 0;
    for (jet_lib *L = c->libs; L; L = L->next) {
        struct jet_dl_phdr_info info;
        memset(&info, 0, sizeof info);
        info.dlpi_addr = L->load_bias;
        info.dlpi_name = L->path ? L->path : "";
        info.dlpi_phdr = L->phdrs;
        info.dlpi_phnum = L->phnum;
        info.dlpi_adds = L->dlpi_adds;
        info.dlpi_subs = 0;
        info.dlpi_tls_modid = (size_t)L->tls_modid;
        /* Per-thread TLS image for this module (TPIDR + tls_offset). */
        {
            jet_cpu *cpu = jet_cpu_current(c);
            if (cpu && L->tls_modid > 0 && L->tls_image_size)
                info.dlpi_tls_data = (void*)(uintptr_t)(cpu->tpidr_el0 + L->tls_offset);
            else
                info.dlpi_tls_data = NULL;
        }
        jet_args ca = {0};
        ca.x[0] = H2G(&info);
        ca.x[1] = sizeof(info);
        ca.x[2] = data;
        uint64_t r = 0;
        if (jet_native_call(c, cb, &ca, &r, NULL) != 0) {
            jet_log(c, 1, "dl_iterate_phdr callback failed: %s", c->last_error);
            return (uint64_t)(int64_t)-1;
        }
        rc = (int)(int64_t)r;
        if (rc != 0) return (uint64_t)(int64_t)rc;
    }
    return 0;
}

/* glibc 2.35+ _dl_find_object — used by libgcc_s unwinder instead of (or with)
 * dl_iterate_phdr. AArch64 struct (DLFO_STRUCT_HAS_EH_DBASE/COUNT = 0): */
struct jet_dl_find_object {
    uint64_t flags;
    void    *map_start;
    void    *map_end;
    void    *link_map;   /* unused; leave NULL */
    void    *eh_frame;
    uint64_t reserved[7];
};

static uint64_t h_dl_find_object(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    uint64_t addr = a->x[0];
    struct jet_dl_find_object *out = a->x[1]
        ? (struct jet_dl_find_object *)G2H(a->x[1]) : NULL;
    if (!out) return (uint64_t)(int64_t)-1;
    typedef struct {
        uint32_t p_type, p_flags;
        uint64_t p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align;
    } Ph;
    for (jet_lib *L = c->libs; L; L = L->next) {
        if (!L->phdrs || !L->phnum) continue;
        Ph *ph = (Ph *)L->phdrs;
        uint64_t lo = UINT64_MAX, hi = 0;
        void *eh = NULL;
        for (int i = 0; i < L->phnum; i++) {
            if (ph[i].p_type == 1 /* PT_LOAD */) {
                uint64_t s = L->load_bias + ph[i].p_vaddr;
                uint64_t e = s + ph[i].p_memsz;
                if (s < lo) lo = s;
                if (e > hi) hi = e;
            } else if (ph[i].p_type == 0x6474e550u /* PT_GNU_EH_FRAME */) {
                eh = (void *)(uintptr_t)(L->load_bias + ph[i].p_vaddr);
            }
        }
        if (lo == UINT64_MAX) {
            lo = L->load_bias;
            hi = L->load_bias + L->map_size;
        }
        if (addr >= lo && addr < hi) {
            memset(out, 0, sizeof *out);
            out->map_start = (void *)(uintptr_t)lo;
            out->map_end = (void *)(uintptr_t)hi;
            out->eh_frame = eh;
            return 0;
        }
    }
    return (uint64_t)(int64_t)-1;
}

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
#if defined(__APPLE__)
    int fd = (int)a->x[0], lcmd = (int)a->x[1];
    int cmd = jet_xl_fcntl_cmd_l2d(lcmd);
    if (cmd < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    switch (lcmd) {
    case 3: { int r = fcntl(fd, F_GETFL); return (uint64_t)(int64_t)(r < 0 ? -1 : jet_xl_oflags_d2l(r)); }
    case 4: return (uint64_t)(int64_t)fcntl(fd, F_SETFL, jet_xl_oflags_l2d((int)a->x[2]));
    case 5: case 6: case 7: case 36: case 37: case 38: {
        /* struct flock: Linux {short type,whence; off_t start,len; pid} (32) vs Darwin (24) */
        if (!a->x[2]) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
        jet_guest_flock *g = (jet_guest_flock*)G2H(a->x[2]);
        jet_darwin_flock d;
        if (jet_xl_flock_l2d(g, &d) < 0) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
        int r = fcntl(fd, cmd, (struct flock*)&d);
        if (r == 0 && cmd == F_GETLK) jet_xl_flock_d2l(&d, g);
        return (uint64_t)(int64_t)r;
    }
    default: return (uint64_t)(int64_t)fcntl(fd, cmd, (int)a->x[2]);
    }
#else
    return (uint64_t)(int64_t)fcntl((int)a->x[0], (int)a->x[1], a->x[2]);
#endif
}
static uint64_t h_ioctl(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
#if defined(__APPLE__)
    unsigned long req = jet_xl_ioctl_l2d((unsigned long)(uint32_t)a->x[1]);
    if (req == (unsigned long)-1) { errno = ENOTTY; return (uint64_t)(int64_t)-1; }
    return (uint64_t)(int64_t)ioctl((int)a->x[0], req, (void*)G2H(a->x[2]));
#else
    return (uint64_t)(int64_t)ioctl((int)a->x[0], (unsigned long)a->x[1], (void*)G2H(a->x[2]));
#endif
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
static uint64_t h_guest_sigdelset(jet_ctx *c, const jet_args *a, void *u);
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
    reg(ctx,"__android_log_vprint",h_android_log_vprint);
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
    reg(ctx,"sprintf",h_sprintf); reg(ctx,"printf",h_printf); reg(ctx,"vfprintf",h_vfprintf);

    reg(ctx,"mmap",h_mmap); reg(ctx,"mmap64",h_mmap); reg(ctx,"madvise",h_madvise); reg(ctx,"munmap",h_munmap); reg(ctx,"mprotect",h_mprotect);
    reg(ctx,"mremap",h_stub0);
    reg(ctx,"getauxval",h_getauxval); reg(ctx,"sysconf",h_sysconf); reg(ctx,"getpagesize",h_getpagesize);
    reg(ctx,"clock_gettime",h_clock_gettime); reg(ctx,"clock_getres",h_clock_getres); reg(ctx,"gettimeofday",h_gettimeofday);
    reg(ctx,"nanosleep",h_nanosleep); reg(ctx,"usleep",h_usleep); reg(ctx,"sleep",h_stub0);
    reg(ctx,"getpid",h_getpid); reg(ctx,"gettid",h_gettid);
    reg(ctx,"getuid",h_getuid); reg(ctx,"geteuid",h_geteuid); reg(ctx,"getgid",h_getuid); reg(ctx,"getegid",h_geteuid);
    reg(ctx,"uname",h_uname); reg(ctx,"prctl",h_prctl);

    reg(ctx,"open",h_open); reg(ctx,"openat",h_openat); reg(ctx,"__openat_2",h_openat); reg(ctx,"open64",h_open); reg(ctx,"__open_2",h_open); reg(ctx,"close",h_close);
    reg(ctx,"read",h_read); reg(ctx,"__read_chk",h_read); reg(ctx,"write",h_write);
    reg(ctx,"lseek",h_lseek); reg(ctx,"lseek64",h_lseek);
    reg(ctx,"fstat",h_fstat); reg(ctx,"stat",h_stat_fn); reg(ctx,"lstat",h_lstat_fn); reg(ctx,"fstatat",h_fstatat_fn); reg(ctx,"newfstatat",h_fstatat_fn);
    reg(ctx,"access",h_access2); reg(ctx,"readlink",h_readlink);
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

    reg(ctx,"gettext",h_gettext); reg(ctx,"dgettext",h_dgettext);
    reg(ctx,"dcgettext",h_dcgettext); reg(ctx,"ngettext",h_ngettext);
    reg(ctx,"textdomain",h_textdomain); reg(ctx,"bindtextdomain",h_bindtextdomain);
    reg(ctx,"dlopen",h_dlopen); reg(ctx,"dlsym",h_dlsym); reg(ctx,"dlclose",h_dlclose);
    reg(ctx,"dlerror",h_dlerror); reg(ctx,"dladdr",h_dladdr); reg(ctx,"dl_iterate_phdr",h_dl_iterate_phdr);
    reg(ctx,"_dl_find_object",h_dl_find_object);

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
    reg(ctx,"poll",h_poll_fn); reg(ctx,"ppoll",h_ppoll_fn); reg(ctx,"select",h_select_fn); reg(ctx,"pselect",h_pselect_fn);
    reg(ctx,"socket",h_socket); reg(ctx,"bind",h_bind_fn); reg(ctx,"listen",h_listen_fn);
    reg(ctx,"accept",h_accept); reg(ctx,"connect",h_connect); reg(ctx,"setsockopt",h_setsockopt);
    reg(ctx,"getsockopt",h_getsockopt); reg(ctx,"getaddrinfo",h_getaddrinfo2); reg(ctx,"freeaddrinfo",h_freeaddrinfo);
    reg(ctx,"getnameinfo",h_getnameinfo); reg(ctx,"gai_strerror",h_gai_strerror);
    reg(ctx,"getsockname",h_getsockname); reg(ctx,"getpeername",h_getpeername);
    reg(ctx,"send",h_send); reg(ctx,"recv",h_recv); reg(ctx,"sendto",h_sendto); reg(ctx,"recvfrom",h_recvfrom);
    reg(ctx,"sendmsg",h_sendmsg); reg(ctx,"recvmsg",h_recvmsg);
    reg(ctx,"ioctl",h_ioctl); reg(ctx,"fcntl",h_fcntl);
    reg(ctx,"pipe",h_pipe_fn); reg(ctx,"eventfd",h_eventfd);
    reg(ctx,"eventfd_read",h_eventfd_read); reg(ctx,"eventfd_write",h_eventfd_write);
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
    reg(ctx,"getcwd",h_getcwd); reg(ctx,"mkdir",h_mkdir); reg(ctx,"remove",h_remove);
    reg(ctx,"mkdirat",h_mkdirat); reg(ctx,"unlinkat",h_unlinkat); reg(ctx,"renameat",h_renameat);
    reg(ctx,"renameat2",h_renameat2); reg(ctx,"symlink",h_symlink); reg(ctx,"symlinkat",h_symlinkat);
    reg(ctx,"readlinkat",h_readlinkat); reg(ctx,"link",h_link); reg(ctx,"linkat",h_linkat);
    reg(ctx,"faccessat",h_faccessat); reg(ctx,"fchmodat",h_fchmodat); reg(ctx,"truncate",h_truncate);
    reg(ctx,"truncate64",h_truncate); reg(ctx,"ftruncate64",h_ftruncate);
    reg(ctx,"utimensat",h_utimensat); reg(ctx,"futimens",h_futimens); reg(ctx,"fdatasync",h_fdatasync);
    reg(ctx,"rename",h_rename); reg(ctx,"unlink",h_unlink); reg(ctx,"rmdir",h_rmdir);
    reg(ctx,"chmod",h_chmod); reg(ctx,"fchmod",h_fchmod); reg(ctx,"fchown",h_stub0);
    reg(ctx,"ftruncate",h_ftruncate); reg(ctx,"fsync",h_fsync);
    reg(ctx,"opendir",h_opendir); reg(ctx,"readdir",h_readdir); reg(ctx,"closedir",h_closedir);
    reg(ctx,"statfs",h_statfs); reg(ctx,"fstatfs",h_fstatfs); reg(ctx,"statvfs",h_statvfs); reg(ctx,"fstatvfs",h_fstatvfs);
    reg(ctx,"getdents64",h_getdents64); reg(ctx,"getrlimit",h_getrlimit); reg(ctx,"setrlimit",h_setrlimit);
    reg(ctx,"getrusage",h_getrusage); reg(ctx,"tcgetattr",h_tcgetattr); reg(ctx,"tcsetattr",h_tcsetattr);
    reg(ctx,"sigaction",h_guest_sigaction); reg(ctx,"sigaction64",h_guest_sigaction);
    reg(ctx,"sigdelset",h_guest_sigdelset); reg(ctx,"sigemptyset64",h_guest_sigemptyset);
    reg(ctx,"sigfillset64",h_guest_sigfillset); reg(ctx,"sigaddset64",h_guest_sigaddset);
    reg(ctx,"sigdelset64",h_guest_sigdelset); reg(ctx,"sigismember64",h_guest_sigismember);
    reg(ctx,"sigprocmask64",h_guest_sigprocmask); reg(ctx,"pthread_sigmask64",h_guest_pthread_sigmask); reg(ctx,"sigemptyset",h_guest_sigemptyset);
    reg(ctx,"sigfillset",h_guest_sigfillset); reg(ctx,"sigaddset",h_guest_sigaddset);
    reg(ctx,"sigismember",h_guest_sigismember);
    reg(ctx,"signal",h_guest_signal); reg(ctx,"bsd_signal",h_guest_signal);
    reg(ctx,"sigprocmask",h_guest_sigprocmask); reg(ctx,"sigaltstack",h_guest_sigaltstack);
    reg(ctx,"tgkill",h_stub0);
    reg(ctx,"android_set_abort_message",h_android_set_abort_message);
    reg(ctx,"__assert2",h_abort);


    /* bulk missing imports seen from libtmessages */
    const char *more_stubs[] = {
        "feof","ferror","closelog","openlog","syslog",
        "strtoull_l","strtoll_l","strtold_l","strtof","strtold","strcoll","strxfrm","strspn","strcspn",
        "strnlen","strerror_r","sscanf","vsscanf","fscanf","vasprintf","perror","utimes",
        "wcsnrtombs","wcrtomb","wcslen","wcstoul","wcstoll","wcstod","wcstof","wmemmove","wmemcpy",
        "wmemset","wmemcmp","wctob","btowc","mbrtowc","mbrlen","mbsnrtowcs","mbsrtowcs","mbtowc",
        "frexp","fseeko","ftello","getc","putc","fputc","ungetc","freopen","fdopen","mkstemp",
        "if_indextoname","inet_ntop","inet_pton","inet_ntoa",
        "shutdown","socketpair","sched_get_priority_max",
        "sched_get_priority_min","sched_getaffinity","__sched_cpucount","abs","labs","nan",
        "freelocale","newlocale","uselocale","setlocale","localeconv","__ctype_get_mb_cur_max",
        "clock","isatty","putenv","swprintf","wcscoll","cosh","__FD_CLR_chk","__FD_ISSET_chk",
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
    JET_NR_utimensat = 88,
    JET_NR_renameat2 = 276,
    JET_NR_faccessat2 = 439,
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

/* ---- Guest signal emulation (never touch host handlers/masks) ----
 * Guest layouts (guest_abi.h): bionic LP64 sigset_t = 8 bytes, struct sigaction = 32
 * {flags@0, handler@8, mask@16, restorer@24}; kernel rt_sigaction struct = 32
 * {handler, flags, restorer, mask}. Previously a 40-byte {handler,flags,restorer,mask[16]}
 * was written to oact (8-byte overrun, wrong field order) and sigemptyset/sigprocmask
 * wrote 16 bytes into an 8-byte sigset_t. */
#define JET_SIGSET_BYTES   8    /* sigset_t / sigset64_t on LP64 */

static uint64_t h_guest_sigemptyset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (a->x[0]) memset(G2H(a->x[0]), 0, JET_SIGSET_BYTES);
    return 0;
}
static uint64_t h_guest_sigfillset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (a->x[0]) memset(G2H(a->x[0]), 0xff, JET_SIGSET_BYTES);
    return 0;
}
static uint64_t h_guest_sigaddset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (!a->x[0]) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
    int sig = (int)a->x[1];
    if (sig < 1 || sig > 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint8_t *set = (uint8_t*)G2H(a->x[0]);
    set[(sig-1)/8] |= (uint8_t)(1u << ((sig-1)%8));
    return 0;
}
static uint64_t h_guest_sigdelset(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (!a->x[0]) { errno = EFAULT; return (uint64_t)(int64_t)-1; }
    int sig = (int)a->x[1];
    if (sig < 1 || sig > 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint8_t *set = (uint8_t*)G2H(a->x[0]);
    set[(sig-1)/8] &= (uint8_t)~(1u << ((sig-1)%8));
    return 0;
}
static uint64_t h_guest_sigismember(jet_ctx *c, const jet_args *a, void *u) {
    (void)c;(void)u;
    if (!a->x[0]) return 0;
    int sig = (int)a->x[1];
    if (sig < 1 || sig > 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint8_t *set = (uint8_t*)G2H(a->x[0]);
    return (set[(sig-1)/8] & (1u << ((sig-1)%8))) ? 1 : 0;
}

/* Shared core over neutral storage; layout handled by callers. */
static int guest_sig_get(jet_ctx *c, int sig, uint64_t *h, uint64_t *f, uint64_t *r, uint64_t *m) {
    if (sig < 1 || sig >= 64) return -1;
    *h = *f = *r = *m = 0;
    if (c->guest_sig_action[sig].set) {
        *h = c->guest_sig_action[sig].handler; *f = c->guest_sig_action[sig].flags;
        *r = c->guest_sig_action[sig].restorer;
        memcpy(m, c->guest_sig_action[sig].mask, 8);
    }
    return 0;
}
static void guest_sig_set(jet_ctx *c, int sig, uint64_t h, uint64_t f, uint64_t r, uint64_t m) {
    c->guest_sig_action[sig].handler = h;
    c->guest_sig_action[sig].flags = f;
    c->guest_sig_action[sig].restorer = r;
    memset(c->guest_sig_action[sig].mask, 0, sizeof c->guest_sig_action[sig].mask);
    memcpy(c->guest_sig_action[sig].mask, &m, 8);
    c->guest_sig_action[sig].set = 1;
}

/* libc sigaction()/sigaction64(): bionic userland layout. */
static uint64_t h_guest_sigaction(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int sig = (int)a->x[0];
    if (sig < 1 || sig >= 64 || sig == 9 || sig == 19) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    jet_guest_sigaction in = {0};
    if (a->x[1]) memcpy(&in, G2H(a->x[1]), sizeof in);      /* read before oact (may alias) */
    if (a->x[2]) {
        uint64_t h, f, r, m; guest_sig_get(c, sig, &h, &f, &r, &m);
        jet_guest_sigaction out = {0};
        out.sa_flags = (int32_t)f; out.handler = h; out.sa_mask = m; out.sa_restorer = r;
        memcpy(G2H(a->x[2]), &out, sizeof out);
    }
    if (a->x[1]) guest_sig_set(c, sig, in.handler, (uint32_t)in.sa_flags, in.sa_restorer, in.sa_mask);
    /* Intentionally do NOT call host sigaction — JVM owns SIGSEGV/SIGBUS. */
    return 0;
}
/* rt_sigaction SVC: arm64 kernel layout; returns 0 or -EINVAL (Linux). */
static int64_t guest_rt_sigaction(jet_ctx *c, uint64_t sig64, uint64_t act, uint64_t oact, uint64_t setsize) {
    int sig = (int)sig64;
    if (setsize != 8 || sig < 1 || sig >= 64 || sig == 9 || sig == 19) return -22;
    jet_guest_kernel_sigaction in = {0};
    if (act) memcpy(&in, G2H(act), sizeof in);
    if (oact) {
        uint64_t h, f, r, m; guest_sig_get(c, sig, &h, &f, &r, &m);
        jet_guest_kernel_sigaction out = { h, f, r, m };
        memcpy(G2H(oact), &out, sizeof out);
    }
    if (act) guest_sig_set(c, sig, in.handler, in.sa_flags, in.sa_restorer, in.sa_mask);
    return 0;
}

static uint64_t h_guest_signal(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int sig = (int)a->x[0];
    uint64_t handler = a->x[1];
    if (sig < 1 || sig >= 64) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    uint64_t prev = c->guest_sig_action[sig].set ? c->guest_sig_action[sig].handler : 0;
    guest_sig_set(c, sig, handler, 0, 0, 0);
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

/* Guest-mask core over `bytes` (8 for sigset_t; kernel SVC passes sigsetsize). */
static int guest_sigmask_op(jet_ctx *c, int how, const uint8_t *set, uint8_t *oldset, size_t bytes) {
    if (bytes > 16) bytes = 16;
    uint8_t in[16] = {0};
    if (set) memcpy(in, set, bytes);                 /* set and oldset may alias */
    if (set && how != 0 && how != 1 && how != 2) return -22;
    if (oldset) memcpy(oldset, c->guest_sig_mask, bytes);
    if (set) {
        if (how == 0)      for (size_t i=0;i<bytes;i++) c->guest_sig_mask[i] |= in[i];
        else if (how == 1) for (size_t i=0;i<bytes;i++) c->guest_sig_mask[i] &= (uint8_t)~in[i];
        else               memcpy(c->guest_sig_mask, in, bytes);
        c->guest_sig_mask[(9-1)/8]  &= (uint8_t)~(1u << ((9-1)%8));   /* SIGKILL */
        c->guest_sig_mask[(19-1)/8] &= (uint8_t)~(1u << ((19-1)%8));  /* SIGSTOP */
    }
    return 0;
}
static uint64_t h_guest_sigprocmask(jet_ctx *c, const jet_args *a, void *u) {
    (void)u;
    int rc = guest_sigmask_op(c, (int)a->x[0],
                              a->x[1] ? (const uint8_t*)G2H(a->x[1]) : NULL,
                              a->x[2] ? (uint8_t*)G2H(a->x[2]) : NULL, JET_SIGSET_BYTES);
    if (rc) { errno = EINVAL; return (uint64_t)(int64_t)-1; }
    return 0;
}

static uint64_t h_guest_pthread_sigmask(jet_ctx *c, const jet_args *a, void *u) {
    /* Same guest-mask semantics; never call host pthread_sigmask. Returns errno, not -1. */
    (void)u;
    int rc = guest_sigmask_op(c, (int)a->x[0],
                              a->x[1] ? (const uint8_t*)G2H(a->x[1]) : NULL,
                              a->x[2] ? (uint8_t*)G2H(a->x[2]) : NULL, JET_SIGSET_BYTES);
    return rc ? (uint64_t)22 : 0;
}

static int64_t jet_emulate_linux_syscall_raw(jet_ctx *ctx, jet_cpu *cpu,
                                  uint64_t nr, uint64_t a0, uint64_t a1,
                                  uint64_t a2, uint64_t a3, uint64_t a4,
                                  uint64_t a5);
/* Linux syscall ABI: errors are -errno (Linux numbering). Handlers below either
 * return -1 with host errno set, or a negative host errno value; translate both.
 * Handlers that already produce Linux values return them via JET_SVC_LINUX(). */
#define JET_SVC_LINUX_BIAS  (-(int64_t)(1LL << 40))
#define JET_SVC_LINUX(e)    (JET_SVC_LINUX_BIAS + (int64_t)(e))
int64_t jet_emulate_linux_syscall(jet_ctx *ctx, jet_cpu *cpu,
                                  uint64_t nr, uint64_t a0, uint64_t a1,
                                  uint64_t a2, uint64_t a3, uint64_t a4,
                                  uint64_t a5) {
    errno = 0;
    int64_t r = jet_emulate_linux_syscall_raw(ctx, cpu, nr, a0, a1, a2, a3, a4, a5);
    if (r >= JET_SVC_LINUX_BIAS - 4096 && r <= JET_SVC_LINUX_BIAS + 4096)
        return r - JET_SVC_LINUX_BIAS;                       /* already Linux */
    if (r == -1) return -(int64_t)JET_ERRNO_TO_GUEST(errno ? errno : EINVAL);
    if (r < -1 && r >= -4095) return -(int64_t)JET_ERRNO_TO_GUEST((int)-r);
    return r;
}
static int64_t jet_emulate_linux_syscall_raw(jet_ctx *ctx, jet_cpu *cpu,
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
    case JET_NR_clock_gettime: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_clock_gettime(ctx, &aa, NULL); }
    case JET_NR_clock_getres:  { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_clock_getres(ctx, &aa, NULL); }
    case JET_NR_gettimeofday:  { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_gettimeofday(ctx, &aa, NULL); }
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
        int hclk = jet_clock_to_host(a0);
        if (hclk < 0) return JET_SVC_LINUX(-22);
#if defined(__APPLE__)
        if (flags & TIMER_ABSTIME) {
            for (;;) {
                struct timespec now, delta;
                if (clock_gettime((clockid_t)hclk, &now) != 0) return -1;
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
        { int e = clock_nanosleep((clockid_t)hclk, flags, req, rem); return e ? -(int64_t)e : 0; }
#endif
    }
    case JET_NR_sched_yield:
        return (int64_t)sched_yield();
    case JET_NR_close: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_close(ctx, &aa, NULL); }
    case JET_NR_read:  { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_read(ctx, &aa, NULL); }
    case JET_NR_write: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_write(ctx, &aa, NULL); }
    case JET_NR_lseek:
        return (int64_t)lseek((int)a0, (off_t)a1, (int)a2);
    case JET_NR_openat: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_openat(ctx, &aa, NULL); }
    case JET_NR_fstat: {
        struct stat hs;
        int rc = fstat((int)a0, &hs);
        if (rc == 0) write_guest_stat(a1, &hs);
        return (int64_t)rc;
    }
    case JET_NR_newfstatat: {
        struct stat hs;
        int rc = fstatat(jet_atfd_to_host(a0), (const char*)G2H(a1), &hs, jet_atflags_to_host(a3, 0));
        if (rc == 0) write_guest_stat(a2, &hs);
        return (int64_t)rc;
    }
    case JET_NR_readlinkat:
        { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_readlinkat(ctx, &aa, NULL); }
    case JET_NR_faccessat:    /* kernel faccessat has no flags argument */
        { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; aa.x[3] = 0; return (int64_t)h_faccessat(ctx, &aa, NULL); }
    case JET_NR_faccessat2:   { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_faccessat(ctx, &aa, NULL); }
    case JET_NR_mkdirat:      { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_mkdirat(ctx, &aa, NULL); }
    case JET_NR_unlinkat:     { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_unlinkat(ctx, &aa, NULL); }
    case JET_NR_symlinkat:    { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_symlinkat(ctx, &aa, NULL); }
    case JET_NR_linkat:       { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_linkat(ctx, &aa, NULL); }
    case JET_NR_renameat:     { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_renameat(ctx, &aa, NULL); }
    case JET_NR_renameat2:    { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_renameat2(ctx, &aa, NULL); }
    case JET_NR_truncate:     { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_truncate(ctx, &aa, NULL); }
    case JET_NR_ftruncate:    { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_ftruncate(ctx, &aa, NULL); }
    case JET_NR_fchmod:       { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_fchmod(ctx, &aa, NULL); }
    case JET_NR_fchmodat:     /* kernel fchmodat has no flags argument */
        { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; aa.x[3] = 0; return (int64_t)h_fchmodat(ctx, &aa, NULL); }
    case JET_NR_fsync:        { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_fsync(ctx, &aa, NULL); }
    case JET_NR_fdatasync:    { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_fdatasync(ctx, &aa, NULL); }
    case JET_NR_utimensat:    { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_utimensat(ctx, &aa, NULL); }
    case JET_NR_mmap: {
        int prot = (int)a2 & ~PROT_EXEC;
#if defined(__APPLE__)
        int flags = jet_xl_mmap_flags_l2d((int)a3);
#else
        int flags = (int)a3;
#endif
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
    case JET_NR_madvise: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_madvise(ctx, &aa, NULL); }
    case JET_NR_brk:
        /* Don't support brk heap; return current. */
        return (int64_t)a0;
    case JET_NR_uname: {
        struct utsname hu;
        int rc = uname(&hu);
        if (rc == 0 && a0) {
            jet_guest_utsname gu;
            jet_host_utsname_to_guest(&hu, &gu);
            memcpy(G2H(a0), &gu, sizeof gu);
        }
        return (int64_t)rc;
    }
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
    case JET_NR_ioctl: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_ioctl(ctx, &aa, NULL); }
    case JET_NR_fcntl: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_fcntl(ctx, &aa, NULL); }
    case JET_NR_socket:     { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_socket(ctx, &aa, NULL); }
    case JET_NR_listen:     { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_listen_fn(ctx, &aa, NULL); }
    case JET_NR_setsockopt: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_setsockopt(ctx, &aa, NULL); }
    case JET_NR_getsockopt: { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_getsockopt(ctx, &aa, NULL); }
    case JET_NR_shutdown:
        if (jet_is_fake_fd((int)a0)) return 0;
        return (int64_t)shutdown((int)a0, (int)a1);
    case JET_NR_eventfd2:   { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_eventfd(ctx, &aa, NULL); }
    case JET_NR_ppoll:      { jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5; return (int64_t)h_ppoll_fn(ctx, &aa, NULL); }
    case JET_NR_epoll_create1: {
        jet_args aa = {0}; aa.x[0]=a0;
        return (int64_t)h_epoll_create1(ctx, &aa, NULL);
    }
    case JET_NR_epoll_ctl: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3;
        return (int64_t)h_epoll_ctl(ctx, &aa, NULL);
    }
    case JET_NR_epoll_pwait: {
        /* a0=epfd a1=events a2=maxevents a3=timeout a4=sigmask — ignore mask */
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3;
        return (int64_t)h_epoll_wait(ctx, &aa, NULL);
    }
    case JET_NR_statfs: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_statfs(ctx, &aa, NULL);
    }
    case JET_NR_fstatfs: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_fstatfs(ctx, &aa, NULL);
    }
    case JET_NR_getdents64: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_getdents64(ctx, &aa, NULL);
    }
    case JET_NR_getrlimit: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_getrlimit(ctx, &aa, NULL);
    }
    case JET_NR_setrlimit: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_setrlimit(ctx, &aa, NULL);
    }
    case JET_NR_getrusage: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_getrusage(ctx, &aa, NULL);
    }
    case JET_NR_bind: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_bind_fn(ctx, &aa, NULL);
    }
    case JET_NR_connect: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_connect(ctx, &aa, NULL);
    }
    case JET_NR_accept: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_accept(ctx, &aa, NULL);
    }
    case JET_NR_getsockname: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_getsockname(ctx, &aa, NULL);
    }
    case JET_NR_getpeername: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_getpeername(ctx, &aa, NULL);
    }
    case JET_NR_sendto: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5;
        return (int64_t)h_sendto(ctx, &aa, NULL);
    }
    case JET_NR_recvfrom: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2; aa.x[3]=a3; aa.x[4]=a4; aa.x[5]=a5;
        return (int64_t)h_recvfrom(ctx, &aa, NULL);
    }
    case JET_NR_sendmsg: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_sendmsg(ctx, &aa, NULL);
    }
    case JET_NR_recvmsg: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1; aa.x[2]=a2;
        return (int64_t)h_recvmsg(ctx, &aa, NULL);
    }
    case JET_NR_sigaltstack: {
        jet_args aa = {0}; aa.x[0]=a0; aa.x[1]=a1;
        return (int64_t)h_guest_sigaltstack(ctx, &aa, NULL);
    }
    case JET_NR_rt_sigaction:     /* kernel struct sigaction, a3 = sigsetsize (must be 8) */
        return JET_SVC_LINUX(guest_rt_sigaction(ctx, a0, a1, a2, a3));
    case JET_NR_rt_sigprocmask:
        if (a3 != 8) return JET_SVC_LINUX(-22);
        return JET_SVC_LINUX(guest_sigmask_op(ctx, (int)a0, a1 ? (const uint8_t*)G2H(a1) : NULL,
                                              a2 ? (uint8_t*)G2H(a2) : NULL, 8));
    case JET_NR_rt_sigpending:
        if (a1 != 8) return JET_SVC_LINUX(-22);
        if (a0) memset(G2H(a0), 0, 8);
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
        return JET_SVC_LINUX(-38);   /* Linux ENOSYS (Darwin's is 78) */
    }
}

