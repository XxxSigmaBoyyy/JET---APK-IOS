// JET iOS compat shims, force-included (-include) into the app's C sources only, never into scripts/build_native_ios.sh
// (that job reports the partner's Runtime/native diagnostics verbatim). Remove an entry once Runtime/native handles it.
#pragma once
#if defined(__APPLE__) && !defined(__cplusplus) && !defined(__OBJC__)
#include <time.h>
#include <errno.h>
// Linux clock_nanosleep (absent from the iOS SDK): guest clock ids/flags are Linux values (CLOCK_MONOTONIC=1,
// CLOCK_MONOTONIC_RAW=4, CLOCK_BOOTTIME=7, TIMER_ABSTIME=1). Returns 0 or an errno value, like Linux.
static inline int jet_clock_nanosleep(clockid_t clk, int flags, const struct timespec *req, struct timespec *rem) {
    int id = (int)clk;
    clockid_t c = (id == 1 || id == 4 || id == 7) ? CLOCK_MONOTONIC : CLOCK_REALTIME;
    struct timespec d = *req;
    if (flags & 1) {
        struct timespec now; clock_gettime(c, &now);
        d.tv_sec = req->tv_sec - now.tv_sec; d.tv_nsec = req->tv_nsec - now.tv_nsec;
        if (d.tv_nsec < 0) { d.tv_nsec += 1000000000L; d.tv_sec--; }
        if (d.tv_sec < 0) return 0;
        rem = 0;
    }
    return nanosleep(&d, rem) == 0 ? 0 : errno;
}
#define clock_nanosleep jet_clock_nanosleep
#endif
