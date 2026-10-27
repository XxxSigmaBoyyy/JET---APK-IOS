/* netdb_abi.h — guest bionic (aarch64 LP64) getaddrinfo/getnameinfo ABI and host converters.
 * Bionic uses BSD numbering (positive EAI_*, AI_NUMERICSERV=8, NI_* BSD order) and the BSD
 * struct addrinfo field order (canonname@24, addr@32). glibc swaps addr/canonname and uses
 * negative EAI_* / different AI_ and NI_ bits, so nothing here is a memcpy. */
#ifndef JET_NETDB_ABI_H
#define JET_NETDB_ABI_H
#include <stddef.h>
#include <stdint.h>

typedef struct {
    int32_t  ai_flags;      /* 0 */
    int32_t  ai_family;     /* 4 */
    int32_t  ai_socktype;   /* 8 */
    int32_t  ai_protocol;   /* 12 */
    uint32_t ai_addrlen;    /* 16 socklen_t */
    uint32_t __pad;         /* 20 */
    uint64_t ai_canonname;  /* 24 char* */
    uint64_t ai_addr;       /* 32 struct sockaddr* */
    uint64_t ai_next;       /* 40 struct addrinfo* */
} jet_guest_addrinfo;
_Static_assert(sizeof(jet_guest_addrinfo) == 48, "bionic LP64 struct addrinfo");
_Static_assert(offsetof(jet_guest_addrinfo, ai_canonname) == 24, "ai_canonname@24 (BSD order)");
_Static_assert(offsetof(jet_guest_addrinfo, ai_addr) == 32, "ai_addr@32");
_Static_assert(offsetof(jet_guest_addrinfo, ai_next) == 40, "ai_next@40");

/* bionic <netdb.h> */
#define JG_AI_PASSIVE      0x001
#define JG_AI_CANONNAME    0x002
#define JG_AI_NUMERICHOST  0x004
#define JG_AI_NUMERICSERV  0x008
#define JG_AI_ALL          0x100
#define JG_AI_V4MAPPED_CFG 0x200
#define JG_AI_ADDRCONFIG   0x400
#define JG_AI_V4MAPPED     0x800
#define JG_AI_MASK (JG_AI_PASSIVE|JG_AI_CANONNAME|JG_AI_NUMERICHOST|JG_AI_NUMERICSERV|JG_AI_ADDRCONFIG)
enum { JG_EAI_ADDRFAMILY = 1, JG_EAI_AGAIN, JG_EAI_BADFLAGS, JG_EAI_FAIL, JG_EAI_FAMILY, JG_EAI_MEMORY,
       JG_EAI_NODATA, JG_EAI_NONAME, JG_EAI_SERVICE, JG_EAI_SOCKTYPE, JG_EAI_SYSTEM, JG_EAI_BADHINTS,
       JG_EAI_PROTOCOL, JG_EAI_OVERFLOW, JG_EAI_MAX };
#define JG_NI_NOFQDN       0x01
#define JG_NI_NUMERICHOST  0x02
#define JG_NI_NAMEREQD     0x04
#define JG_NI_NUMERICSERV  0x08
#define JG_NI_DGRAM        0x10

int         jet_gai_flags_g2h(int guest_flags);      /* guest AI_* -> host AI_* */
int         jet_gai_err_h2g(int host_eai);           /* host EAI_* -> bionic EAI_* (0 -> 0) */
int         jet_ni_flags_g2h(int guest_flags);
const char *jet_gai_strerror_guest(int bionic_eai);  /* static bionic strings */

/* Full bionic getaddrinfo. Result nodes are single malloc() blocks (guest heap == host heap,
 * identity-mapped) laid out as [addrinfo 48][sockaddr][canonname]. Returns bionic EAI_*;
 * on EAI_SYSTEM host errno is left set (caller translates). */
int  jet_getaddrinfo_guest(const char *node, const char *service,
                           const jet_guest_addrinfo *hints, uint64_t *res_out);
void jet_freeaddrinfo_guest(uint64_t res);
int  jet_getnameinfo_guest(const void *guest_sa, uint32_t guest_len, char *host, size_t hostlen,
                           char *serv, size_t servlen, int guest_flags);
#endif
