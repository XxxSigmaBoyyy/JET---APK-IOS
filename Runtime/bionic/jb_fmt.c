#include "jb_internal.h"
#include <string.h>
#include <stdio.h>
#include <wchar.h>
#include <stdlib.h>
#include <syslog.h>
#include <unistd.h>
// Walk a printf/scanf format and pack Darwin-style 8-byte slots (everything is promoted / pointer-sized).
static int push(uint64_t *s, int n, int max, uint64_t v){ if (n>=max) return n; s[n]=v; return n+1; }
int jb_marshal_printf(const char *fmt, jb_cur *c, uint64_t *slots, int max){
    int n=0; if (!fmt) return 0;
    for (const char *p=fmt; *p; p++){
        if (*p!='%') continue; p++;
        while (*p && strchr("-+ #0",*p)) p++;
        while (*p>='0'&&*p<='9') p++;
        if (*p=='*'){ n=push(slots,n,max,(uint32_t)jb_next_gp(c)); p++; while (*p>='0'&&*p<='9') p++; }
        if (*p=='.'){ p++; if (*p=='*'){ n=push(slots,n,max,(uint32_t)jb_next_gp(c)); p++; } else while (*p>='0'&&*p<='9') p++; }
        int len=0; // 0 def, 1 h, 2 hh, 3 l, 4 ll, 5 L/q, 6 z/t, 7 j
        if (*p=='h'){ len=(*(p+1)=='h')?2:1; p+=len; }
        else if (*p=='l'){ len=(*(p+1)=='l')?4:3; p+= (len==4?2:1); }
        else if (*p=='L'||*p=='q'){ len=5; p++; }
        else if (*p=='z'||*p=='t'){ len=6; p++; }
        else if (*p=='j'){ len=7; p++; }
        char spec=*p; if (!spec) break;
        if (strchr("diouxX",spec)){ n=push(slots,n,max,jb_next_gp(c)); }
        else if (spec=='c'){ n=push(slots,n,max,(uint32_t)jb_next_gp(c)); }
        else if (spec=='s'||spec=='p'||spec=='n'){ n=push(slots,n,max,jb_next_gp(c)); }
        else if (strchr("aAeEfFgG",spec)){
            if (len==5){ double d=jb_next_ld(c); uint64_t u; memcpy(&u,&d,8); n=push(slots,n,max,u); }
            else { double d=jb_next_fp(c); uint64_t u; memcpy(&u,&d,8); n=push(slots,n,max,u); }
        } else if (spec=='%'){ /* literal */ }
        else { n=push(slots,n,max,jb_next_gp(c)); }
    }
    return n;
}
int jb_marshal_wprintf(const wchar_t *fmt, jb_cur *c, uint64_t *slots, int max){
    // Convert to narrow for the walker (formats used by Android apps are ASCII).
    char buf[1024]; size_t i=0; if (!fmt) return 0;
    for (; fmt[i] && i+1<sizeof buf; i++) buf[i] = (fmt[i]<128)?(char)fmt[i]:'?'; buf[i]=0;
    return jb_marshal_printf(buf,c,slots,max);
}
int jb_marshal_scanf(const char *fmt, jb_cur *c, uint64_t *slots, int max){
    // scanf args are all pointers (or * suppression has none)
    int n=0; if (!fmt) return 0;
    for (const char *p=fmt; *p; p++){
        if (*p!='%') continue; p++;
        int suppress=0; if (*p=='*'){ suppress=1; p++; }
        while (*p>='0'&&*p<='9') p++;
        if (*p=='h'||*p=='l'||*p=='L'||*p=='z'||*p=='t'||*p=='j'||*p=='q'){
            if ((*p=='h'||*p=='l') && *(p+1)==*p) p+=2; else p++;
        }
        char spec=*p; if (!spec||spec=='%') continue;
        if (!suppress) n=push(slots,n,max,jb_next_gp(c));
    }
    return n;
}
int jb_marshal_wscanf(const wchar_t *fmt, jb_cur *c, uint64_t *slots, int max){
    char buf[1024]; size_t i=0; if (!fmt) return 0;
    for (; fmt[i] && i+1<sizeof buf; i++) buf[i]=(fmt[i]<128)?(char)fmt[i]:'?'; buf[i]=0;
    return jb_marshal_scanf(buf,c,slots,max);
}
// Darwin va_list on arm64 is a pointer to the first slot.
#define CALL_V(fn, fixed, slots) ({ va_list __ap; * (void **)&__ap = (void *)(slots); fn fixed __ap; })

JBX int jbv_printf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,1,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[0],&c,s,64); return CALL_V(vprintf, (), s); }
JBX int jbv_fprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[1],&c,s,64); return CALL_V(vfprintf, (jb_file((void*)r->x[0]),), s); }
JBX int jbv_sprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[1],&c,s,64); return CALL_V(vsprintf, ((char*)r->x[0],), s); }
JBX int jbv_snprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,3,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[2],&c,s,64); return CALL_V(vsnprintf, ((char*)r->x[0],(size_t)r->x[1],), s); }
JBX int jbv___snprintf_chk(jb_regs *r){ /* dst,max,flag,osize,fmt,... */ jb_cur c; jb_cur_regs(&c,r,5,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[4],&c,s,64); return CALL_V(vsnprintf, ((char*)r->x[0],(size_t)r->x[1],), s); }
JBX int jbv___sprintf_chk(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,4,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[3],&c,s,64); return CALL_V(vsprintf, ((char*)r->x[0],), s); }
JBX int jbv_swprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,3,0); uint64_t s[64]; jb_marshal_wprintf((wchar_t*)r->x[2],&c,s,64); return CALL_V(vswprintf, ((wchar_t*)r->x[0],(size_t)r->x[1],), s); }
JBX int jbv_asprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[1],&c,s,64); return CALL_V(vasprintf, ((char**)r->x[0],), s); }
JBX int jbv_dprintf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[1],&c,s,64); return CALL_V(vdprintf, ((int)r->x[0],), s); }
JBX int jbv_sscanf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_scanf((char*)r->x[1],&c,s,64); return CALL_V(vsscanf, ((char*)r->x[0],(char*)r->x[1],), s); }
JBX int jbv_fscanf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_scanf((char*)r->x[1],&c,s,64); return CALL_V(vfscanf, (jb_file((void*)r->x[0]),(char*)r->x[1],), s); }
JBX int jbv_scanf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,1,0); uint64_t s[64]; jb_marshal_scanf((char*)r->x[0],&c,s,64); return CALL_V(vscanf, ((char*)r->x[0],), s); }
JBX int jbv_swscanf(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_wscanf((wchar_t*)r->x[1],&c,s,64); return CALL_V(vswscanf, ((wchar_t*)r->x[0],(wchar_t*)r->x[1],), s); }
JBX void jbv_syslog(jb_regs *r){ jb_cur c; jb_cur_regs(&c,r,2,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[1],&c,s,64); CALL_V(vsyslog, ((int)r->x[0],), s); }
static int log_prio(int p){ return p<=3?LOG_ERR:p<=4?LOG_WARNING:p<=5?LOG_NOTICE:LOG_INFO; }
JBX int jbv___android_log_print(jb_regs *r){
    jb_cur c; jb_cur_regs(&c,r,3,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[2],&c,s,64);
    char buf[1024]; CALL_V(vsnprintf, (buf,sizeof buf,), s);
    jb_log((int)r->x[0], (char*)r->x[1], buf); return (int)strlen(buf);
}
JBX void jbv___android_log_assert(jb_regs *r){
    jb_cur c; jb_cur_regs(&c,r,3,0); uint64_t s[64]; jb_marshal_printf((char*)r->x[2],&c,s,64);
    char buf[1024]; CALL_V(vsnprintf, (buf,sizeof buf,), s);
    jb_log(7 /*ASSERT*/, (char*)r->x[0], buf); abort();
}
JBX int jbv___android_log_buf_print(jb_regs *r){ /* skip bufid */ r->x[0]=r->x[1]; r->x[1]=r->x[2]; r->x[2]=r->x[3];
    memmove(&r->x[3],&r->x[4],4*8); return jbv___android_log_print(r); }
