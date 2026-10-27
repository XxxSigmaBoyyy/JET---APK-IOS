#include "jb_internal.h"
#include <stdio.h>
#include <string.h>
#include <syslog.h>
void jb_log(int prio, const char *tag, const char *msg){
    static const char *P[] = {"VERBOSE","DEBUG","INFO","WARN","ERROR","FATAL","SILENT","ASSERT"};
    const char *p = (prio>=0 && prio<8) ? P[prio] : "?";
    fprintf(stderr, "A/%s(%s): %s\n", tag?tag:"?", p, msg?msg:"");
    syslog(prio<=3?LOG_ERR:LOG_INFO, "A/%s: %s", tag?tag:"?", msg?msg:"");
}
JBX int __android_log_write(int prio, const char *tag, const char *msg){ jb_log(prio,tag,msg); return (int)strlen(msg?msg:""); }
JBX int __android_log_vprint(int prio, const char *tag, const char *fmt, jb_aapcs_va *ap){
    jb_cur c; jb_cur_va(&c, ap); uint64_t s[64]; jb_marshal_printf(fmt,&c,s,64);
    char buf[1024]; va_list va; *(void**)&va=(void*)s; vsnprintf(buf,sizeof buf,fmt,va);
    jb_log(prio,tag,buf); return (int)strlen(buf);
}
JBX void android_set_abort_message(const char *m){ jb_log(7,"abort",m?m:""); }
JBX void __assert2(const char *file, int line, const char *func, const char *msg){
    char b[512]; snprintf(b,sizeof b,"%s:%d: %s: %s",file,line,func,msg); jb_log(7,"assert",b); abort();
}
