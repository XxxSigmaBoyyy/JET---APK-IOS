/* netdb_abi.c — see netdb_abi.h. Host-macro based, so correct on glibc and Darwin alike. */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "netdb_abi.h"
#include "guest_abi.h"
#include <errno.h>
#include <netdb.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>

#if defined(__APPLE__)
/* Darwin shares bionic's BSD numbering; translation degenerates to identity. */
_Static_assert(EAI_ADDRFAMILY == 1 && EAI_AGAIN == 2 && EAI_BADFLAGS == 3 && EAI_FAIL == 4 &&
               EAI_FAMILY == 5 && EAI_MEMORY == 6 && EAI_NODATA == 7 && EAI_NONAME == 8 &&
               EAI_SERVICE == 9 && EAI_SOCKTYPE == 10 && EAI_SYSTEM == 11 && EAI_BADHINTS == 12 &&
               EAI_PROTOCOL == 13 && EAI_OVERFLOW == 14, "Darwin EAI_* == bionic");
_Static_assert(AI_PASSIVE == 1 && AI_CANONNAME == 2 && AI_NUMERICHOST == 4 && AI_ADDRCONFIG == 0x400 &&
               AI_ALL == 0x100 && AI_V4MAPPED_CFG == 0x200 && AI_V4MAPPED == 0x800, "Darwin AI_*");
_Static_assert(AI_NUMERICSERV == 0x1000, "Darwin AI_NUMERICSERV differs from bionic (8)");
_Static_assert(NI_NOFQDN == 1 && NI_NUMERICHOST == 2 && NI_NAMEREQD == 4 && NI_NUMERICSERV == 8 &&
               NI_DGRAM == 0x10, "Darwin NI_* == bionic");
_Static_assert(sizeof(struct addrinfo) == 48 && offsetof(struct addrinfo, ai_canonname) == 24 &&
               offsetof(struct addrinfo, ai_addr) == 32, "Darwin addrinfo == bionic layout");
#endif

int jet_gai_flags_g2h(int g) {
    int h = 0;
    if (g & JG_AI_PASSIVE)     h |= AI_PASSIVE;
    if (g & JG_AI_CANONNAME)   h |= AI_CANONNAME;
    if (g & JG_AI_NUMERICHOST) h |= AI_NUMERICHOST;
    if (g & JG_AI_NUMERICSERV) h |= AI_NUMERICSERV;
    if (g & JG_AI_ADDRCONFIG)  h |= AI_ADDRCONFIG;
    if (g & JG_AI_ALL)         h |= AI_ALL;
    if (g & JG_AI_V4MAPPED)    h |= AI_V4MAPPED;
    return h;
}
int jet_gai_err_h2g(int e) {
    if (e == 0) return 0;
#define M(x) if (e == x) return JG_##x;
    M(EAI_AGAIN) M(EAI_BADFLAGS) M(EAI_FAIL) M(EAI_FAMILY) M(EAI_MEMORY) M(EAI_NONAME)
    M(EAI_SERVICE) M(EAI_SOCKTYPE) M(EAI_SYSTEM) M(EAI_OVERFLOW)
#ifdef EAI_ADDRFAMILY
    M(EAI_ADDRFAMILY)
#endif
#if defined(EAI_NODATA) && (!defined(EAI_NONAME) || EAI_NODATA != EAI_NONAME)
    M(EAI_NODATA)
#endif
#ifdef EAI_BADHINTS
    M(EAI_BADHINTS)
#endif
#ifdef EAI_PROTOCOL
    M(EAI_PROTOCOL)
#endif
#undef M
    return JG_EAI_FAIL;                  /* glibc-only (EAI_INPROGRESS, ...) */
}
int jet_ni_flags_g2h(int g) {
    int h = 0;
    if (g & JG_NI_NOFQDN)      h |= NI_NOFQDN;
    if (g & JG_NI_NUMERICHOST) h |= NI_NUMERICHOST;
    if (g & JG_NI_NAMEREQD)    h |= NI_NAMEREQD;
    if (g & JG_NI_NUMERICSERV) h |= NI_NUMERICSERV;
    if (g & JG_NI_DGRAM)       h |= NI_DGRAM;
    return h;
}
/* bionic netbsd/getaddrinfo.c ai_errlist */
static const char *const k_gai_str[] = {
    "Success", "Address family for hostname not supported", "Temporary failure in name resolution",
    "Invalid value for ai_flags", "Non-recoverable failure in name resolution", "ai_family not supported",
    "Memory allocation failure", "No address associated with hostname",
    "hostname nor servname provided, or not known", "servname not supported for ai_socktype",
    "ai_socktype not supported", "System error returned in errno", "Invalid value for hints",
    "Resolved protocol is unknown", "Argument buffer overflow", "Unknown error",
};
const char *jet_gai_strerror_guest(int e) {
    if (e < 0 || e >= JG_EAI_MAX) e = JG_EAI_MAX;
    return k_gai_str[e];
}

static int af_g2h(int f) { return f == 10 ? AF_INET6 : f; }   /* AF_INET/UNSPEC/UNIX equal */
static int af_h2g(int f) { return f == AF_INET6 ? 10 : f; }

void jet_freeaddrinfo_guest(uint64_t res) {
    while (res) {
        jet_guest_addrinfo *g = (jet_guest_addrinfo *)(uintptr_t)res;
        uint64_t next = g->ai_next;
        free(g);                                  /* addr + canonname live in the same block */
        res = next;
    }
}

int jet_getaddrinfo_guest(const char *node, const char *service,
                          const jet_guest_addrinfo *gh, uint64_t *res_out) {
    if (!res_out) return JG_EAI_FAIL;
    *res_out = 0;
    if (!node && !service) return JG_EAI_NONAME;
    struct addrinfo hh, *hp = NULL;
    memset(&hh, 0, sizeof hh);
    if (gh) {                                     /* bionic validation, before touching the host */
        if (gh->ai_addrlen || gh->ai_canonname || gh->ai_addr || gh->ai_next) return JG_EAI_BADHINTS;
        if (gh->ai_flags & ~JG_AI_MASK) return JG_EAI_BADFLAGS;
        if (gh->ai_family != 0 && gh->ai_family != 2 && gh->ai_family != 10) return JG_EAI_FAMILY;
        switch (gh->ai_socktype) { case 0: case 1: case 2: case 3: break; default: return JG_EAI_SOCKTYPE; }
        hh.ai_flags = jet_gai_flags_g2h(gh->ai_flags);
        hh.ai_family = af_g2h(gh->ai_family);
        hh.ai_socktype = gh->ai_socktype;        /* SOCK_STREAM/DGRAM/RAW identical on Darwin */
        hh.ai_protocol = gh->ai_protocol;        /* IPPROTO_* identical */
        hp = &hh;
    }
    struct addrinfo *hr = NULL;
    int rc = getaddrinfo(node, service, hp, &hr);
    if (rc) return jet_gai_err_h2g(rc);
    uint64_t head = 0, *link = &head;
    for (struct addrinfo *h = hr; h; h = h->ai_next) {
        uint8_t gsa[128];
        int glen = 0;
        if (h->ai_addr) {
            glen = jet_host_sockaddr_to_guest(h->ai_addr, (uint32_t)h->ai_addrlen, gsa, sizeof gsa);
            if (glen < 0) continue;               /* family the guest can't represent */
            if ((size_t)glen > sizeof gsa) glen = (int)sizeof gsa;
        }
        size_t cn = h->ai_canonname ? strlen(h->ai_canonname) + 1 : 0;
        size_t sa_off = sizeof(jet_guest_addrinfo), cn_off = sa_off + (((size_t)glen + 7) & ~(size_t)7);
        jet_guest_addrinfo *g = calloc(1, cn_off + cn);
        if (!g) { freeaddrinfo(hr); jet_freeaddrinfo_guest(head); return JG_EAI_MEMORY; }
        g->ai_flags = gh ? gh->ai_flags : 0;     /* bionic echoes hint flags */
        g->ai_family = af_h2g(h->ai_family);
        g->ai_socktype = h->ai_socktype;
        g->ai_protocol = h->ai_protocol;
        if (glen) {
            memcpy((uint8_t *)g + sa_off, gsa, (size_t)glen);
            g->ai_addr = (uint64_t)(uintptr_t)((uint8_t *)g + sa_off);
            g->ai_addrlen = (uint32_t)glen;
        }
        if (cn) {
            memcpy((uint8_t *)g + cn_off, h->ai_canonname, cn);
            g->ai_canonname = (uint64_t)(uintptr_t)((uint8_t *)g + cn_off);
        }
        *link = (uint64_t)(uintptr_t)g;
        link = &g->ai_next;
    }
    freeaddrinfo(hr);
    if (!head) return JG_EAI_FAMILY;
    *res_out = head;
    return 0;
}

int jet_getnameinfo_guest(const void *gsa, uint32_t glen, char *host, size_t hostlen,
                          char *serv, size_t servlen, int gflags) {
    if (!gsa) return JG_EAI_FAIL;
    uint8_t hsa[128];
    int hl = jet_guest_sockaddr_to_host(gsa, glen, hsa, sizeof hsa);
    if (hl < 0) return JG_EAI_FAMILY;
    if (!host) hostlen = 0;
    if (!serv) servlen = 0;
    int rc = getnameinfo((const struct sockaddr *)hsa, (socklen_t)hl,
                         hostlen ? host : NULL, (socklen_t)hostlen,
                         servlen ? serv : NULL, (socklen_t)servlen, jet_ni_flags_g2h(gflags));
    return jet_gai_err_h2g(rc);
}
