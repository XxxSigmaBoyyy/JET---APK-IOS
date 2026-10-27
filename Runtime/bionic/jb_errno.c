#include "jb_internal.h"
#include <errno.h>
#include <pthread.h>
// Linux errno (bionic) <-> Darwin. Only the values that differ for the symbols we cover.
static const int L2D[][2] = {
    {11,35},{35,45},{36,46},{37,47},{38,48},{39,49},{40,50},{41,51},{42,52},{43,53},{44,54},
    {45,55},{46,56},{47,57},{48,58},{49,59},{50,60},{51,61},{52,62},{53,63},{54,64},{55,65},
    {56,66},{57,67},{58,68},{59,69},{60,70},{61,71},{62,72},{63,73},{64,74},{65,75},{66,76},
    {67,77},{68,78},{69,79},{70,80},{71,81},{72,82},{73,83},{74,84},{75,85},{76,86},{77,87},
    {78,88},{79,89},{80,90},{81,91},{82,92},{83,93},{84,94},{85,95},{86,96},{87,97},{88,98},
    {89,99},{90,100},{91,101},{92,102},{93,103},{94,104},{95,105},{96,106},{97,107},{98,108},
    {99,109},{100,110},{101,111},{102,112},{103,113},{104,114},{105,115},{106,116},{107,117},
    {108,118},{109,119},{110,60 /*ETIMEDOUT*/},{111,61 /*ECONNREFUSED*/},{112,64},{113,65},
    {114,36 /*EINPROGRESS*/},{115,35 /*EAGAIN again for EWOULDBLOCK path*/},{116,70},{122,63},
    {125,89},{130,53},{131,55},{132,69},{133,70},
};
static pthread_key_t k_err; static pthread_once_t once = PTHREAD_ONCE_INIT;
static void mk(void){ pthread_key_create(&k_err,0); }
int jb_errno_to_linux(int e){ for (size_t i=0;i<sizeof L2D/sizeof*L2D;i++) if (L2D[i][1]==e) return L2D[i][0]; return e; }
int jb_errno_from_linux(int e){ for (size_t i=0;i<sizeof L2D/sizeof*L2D;i++) if (L2D[i][0]==e) return L2D[i][1]; return e; }
void jb_set_errno_linux(int e){ pthread_once(&once,mk); pthread_setspecific(k_err,(void*)(intptr_t)e); errno = jb_errno_from_linux(e); }
JBX int *__errno(void){ pthread_once(&once,mk); static __thread int v; v = jb_errno_to_linux(errno); return &v; }
