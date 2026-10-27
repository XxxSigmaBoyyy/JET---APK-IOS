/* elf_loader.c - load ET_DYN AArch64 ELF into identity-mapped RW memory. */
#include "jet_internal.h"
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

/* ELF constants (avoid <elf.h> so we stay portable toward ios-arm64). */
#define EI_NIDENT 16
#define ET_DYN 3
#define EM_AARCH64 183
#define PT_LOAD 1
#define PT_DYNAMIC 2
#define PT_TLS 7
#define PT_GNU_RELRO 0x6474e552
#define PT_GNU_EH_FRAME 0x6474e550
#define PT_PHDR 6
#define SHT_RELA 4
#define SHT_RELR 19 /* or 0x6fffff00 variants; we use DT tags */
#define DT_NULL 0
#define DT_NEEDED 1
#define DT_PLTRELSZ 2
#define DT_STRTAB 5
#define DT_SYMTAB 6
#define DT_RELA 7
#define DT_RELASZ 8
#define DT_RELAENT 9
#define DT_STRSZ 10
#define DT_SYMENT 11
#define DT_INIT 12
#define DT_FINI 13
#define DT_SONAME 14
#define DT_JMPREL 23
#define DT_INIT_ARRAY 25
#define DT_FINI_ARRAY 26
#define DT_INIT_ARRAYSZ 27
#define DT_FINI_ARRAYSZ 28
#define DT_FLAGS_1 0x6ffffffb
#define DT_VERSYM 0x6ffffff0
#define DT_RELACOUNT 0x6ffffff9
#define DT_GNU_HASH 0x6ffffef5
#define DT_HASH 4
#define DT_PLTREL 20
#define DT_RELR 0x24           /* generic */
#define DT_RELRSZ 0x23
#define DT_RELRENT 0x25
#define DT_ANDROID_RELR 0x6fffe000
#define DT_ANDROID_RELRSZ 0x6fffe001
#define DT_ANDROID_RELRENT 0x6fffe003
#define DT_ANDROID_RELA 0x60000011 /* APS2 encoded */
#define DT_ANDROID_RELASZ 0x60000012

#define R_AARCH64_NONE        0
#define R_AARCH64_ABS64       257
#define R_AARCH64_GLOB_DAT    1025
#define R_AARCH64_JUMP_SLOT   1026
#define R_AARCH64_RELATIVE    1027
#define R_AARCH64_TLS_TPREL64 1030
#define R_AARCH64_TLS_DTPREL  1028
#define R_AARCH64_TLS_DTPMOD  1029
#define R_AARCH64_TLSDESC     1031
#define R_AARCH64_IRELATIVE   1032

#define STB_LOCAL 0
#define STB_GLOBAL 1
#define STB_WEAK 2
#define STT_NOTYPE 0
#define STT_OBJECT 1
#define STT_FUNC 2
#define STT_TLS 6
#define SHN_UNDEF 0

typedef struct {
    uint8_t  e_ident[EI_NIDENT];
    uint16_t e_type, e_machine;
    uint32_t e_version;
    uint64_t e_entry, e_phoff, e_shoff;
    uint32_t e_flags;
    uint16_t e_ehsize, e_phentsize, e_phnum, e_shentsize, e_shnum, e_shstrndx;
} Elf64_Ehdr;

typedef struct {
    uint32_t p_type, p_flags;
    uint64_t p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_align;
} Elf64_Phdr;

typedef struct {
    uint64_t d_tag;
    union { uint64_t d_val; uint64_t d_ptr; } d_un;
} Elf64_Dyn;

typedef struct {
    uint32_t st_name;
    uint8_t  st_info, st_other;
    uint16_t st_shndx;
    uint64_t st_value, st_size;
} Elf64_Sym;

typedef struct {
    uint64_t r_offset, r_info, r_addend;
} Elf64_Rela;

#define ELF64_R_SYM(i)  ((i) >> 32)
#define ELF64_R_TYPE(i) ((uint32_t)(i))
#define ELF64_ST_BIND(i) ((i) >> 4)
#define ELF64_ST_TYPE(i) ((i) & 0xf)

static uint32_t gnu_hash(const char *s) {
    uint32_t h = 5381;
    for (unsigned char c; (c = (unsigned char)*s++); )
        h = h * 33 + c;
    return h;
}

static uint32_t sysv_hash(const char *s) {
    uint32_t h = 0, g;
    while (*s) {
        h = (h << 4) + (unsigned char)*s++;
        g = h & 0xf0000000u;
        if (g) h ^= g >> 24;
        h &= ~g;
    }
    return h;
}

static const Elf64_Sym *sym_lookup_local(jet_lib *lib, const char *name) {
    if (lib->has_gnu_hash) {
        uint32_t h = gnu_hash(name);
        uint32_t word = lib->gnu_bloom[(h / 64) % lib->gnu_maskwords];
        uint64_t mask = (1ull << (h % 64)) |
                        (1ull << ((h >> lib->gnu_shift2) % 64));
        /* bloom is array of uint64 on ELF64; we stored as uint32* — fix: */
        const uint64_t *bloom64 = (const uint64_t *)lib->gnu_bloom;
        word = 0; /* silence */
        (void)word;
        uint64_t b = bloom64[(h / 64) % lib->gnu_maskwords];
        if ((b & mask) != mask) return NULL;
        uint32_t i = lib->gnu_buckets[h % lib->gnu_nbucket];
        if (i < lib->gnu_symoffset) return NULL;
        const uint32_t *chain = lib->gnu_chain + (i - lib->gnu_symoffset);
        const Elf64_Sym *syms = lib->dynsym;
        do {
            if (((*chain ^ h) >> 1) == 0) {
                const Elf64_Sym *s = &syms[i];
                if (strcmp(lib->dynstr + s->st_name, name) == 0) return s;
            }
            i++;
        } while ((*chain++ & 1u) == 0);
        return NULL;
    }
    /* sysv hash */
    if (!lib->buckets) return NULL;
    uint32_t h = sysv_hash(name);
    for (uint32_t i = lib->buckets[h % lib->nbucket]; i; i = lib->chains[i]) {
        const Elf64_Sym *s = &((const Elf64_Sym *)lib->dynsym)[i];
        if (strcmp(lib->dynstr + s->st_name, name) == 0) return s;
    }
    return NULL;
}

uint64_t jet_elf_sym(jet_lib *lib, const char *name) {
    const Elf64_Sym *s = sym_lookup_local(lib, name);
    if (!s || s->st_shndx == SHN_UNDEF) return 0;
    return lib->load_bias + s->st_value;
}

/* Resolve an undefined symbol against the trap table (or previously loaded libs). */
static uint64_t resolve_sym(jet_ctx *ctx, jet_lib *self, const char *name,
                            int *is_data_out) {
    if (is_data_out) *is_data_out = 0;
    /* search loaded libs first (for multi-lib later) */
    for (jet_lib *L = ctx->libs; L; L = L->next) {
        if (L == self) continue;
        const Elf64_Sym *s = sym_lookup_local(L, name);
        if (s && s->st_shndx != SHN_UNDEF) {
            if (is_data_out) *is_data_out = (ELF64_ST_TYPE(s->st_info) == STT_OBJECT);
            return L->load_bias + s->st_value;
        }
    }
    /* also allow self-defined */
    {
        const Elf64_Sym *s = sym_lookup_local(self, name);
        if (s && s->st_shndx != SHN_UNDEF) {
            if (is_data_out) *is_data_out = (ELF64_ST_TYPE(s->st_info) == STT_OBJECT);
            return self->load_bias + s->st_value;
        }
    }
    /* trap table */
    for (int i = 1; i < ctx->ntraps; i++) {
        if (ctx->traps[i].name && strcmp(ctx->traps[i].name, name) == 0) {
            if (ctx->traps[i].is_data) {
                if (is_data_out) *is_data_out = 1;
                return H2G(ctx->traps[i].user);
            }
            return jet_stub_addr(ctx, i);
        }
    }
    return 0;
}

uint64_t jet_host_tlsdesc(jet_ctx *ctx, const jet_args *a, void *user);

/* Apply a single Rela. */
static int apply_rela(jet_ctx *ctx, jet_lib *lib, const Elf64_Rela *r,
                      const Elf64_Sym *syms) {
    uint64_t *where = (uint64_t *)G2H(lib->load_bias + r->r_offset);
    uint32_t type = ELF64_R_TYPE(r->r_info);
    uint32_t symi = ELF64_R_SYM(r->r_info);
    const Elf64_Sym *sym = symi ? &syms[symi] : NULL;
    const char *sname = (sym && sym->st_name) ? lib->dynstr + sym->st_name : "";

    switch (type) {
    case R_AARCH64_NONE:
        return 0;
    case R_AARCH64_RELATIVE:
        *where = lib->load_bias + (uint64_t)r->r_addend;
        return 0;
    case R_AARCH64_ABS64:
    case R_AARCH64_GLOB_DAT:
    case R_AARCH64_JUMP_SLOT: {
        int is_data = 0;
        uint64_t v = 0;
        if (sym && sym->st_shndx != SHN_UNDEF)
            v = lib->load_bias + sym->st_value + (uint64_t)r->r_addend;
        else {
            v = resolve_sym(ctx, lib, sname, &is_data);
            if (!v && sname[0]) {
                if (ELF64_ST_BIND(sym->st_info) != STB_WEAK) {
                    extern uint64_t jet_host_stub0(jet_ctx *, const jet_args *, void *);
                    /* Log once per name; still bind stub0 so BLR is safe. */
                    static char seen[64][128]; static int nseen;
                    int already = 0;
                    for (int si = 0; si < nseen; si++)
                        if (strcmp(seen[si], sname) == 0) { already = 1; break; }
                    if (!already && nseen < 64) {
                        snprintf(seen[nseen++], sizeof seen[0], "%s", sname);
                        jet_log(ctx, 1, "UNRESOLVED_IMPORT %s (reloc_type=%u)", sname, type);
                    }
                    jet_native_register_host(ctx, sname, jet_host_stub0, NULL);
                    v = resolve_sym(ctx, lib, sname, &is_data);
                }
            } else if (!is_data)
                v += (uint64_t)r->r_addend; /* usually 0 for JUMP_SLOT */
            else
                v += (uint64_t)r->r_addend;
        }
        *where = v;
        return 0;
    }
    case R_AARCH64_IRELATIVE: {
        /* IFUNC: call resolver at load_bias+addend, store result.
         * Resolver is guest code — invoke interpreter. */
        uint64_t resolver = lib->load_bias + (uint64_t)r->r_addend;
        uint64_t result = 0;
        jet_args a = {0};
        if (jet_native_call(ctx, resolver, &a, &result, NULL) != 0) {
            jet_set_error(ctx, "IRELATIVE resolver failed at 0x%llx",
                          (unsigned long long)resolver);
            return -1;
        }
        *where = result;
        return 0;
    }
    case R_AARCH64_TLS_TPREL64:
        /* TPREL: offset from TPIDR_EL0. tls_offset includes bionic TCB prefix. */
        *where = (uint64_t)lib->tls_offset + (sym ? sym->st_value : 0) +
                 (uint64_t)r->r_addend;
        return 0;
    case R_AARCH64_TLS_DTPMOD:
        *where = (uint64_t)lib->tls_modid;
        return 0;
    case R_AARCH64_TLS_DTPREL:
        *where = (sym ? sym->st_value : 0) + (uint64_t)r->r_addend;
        return 0;
    case R_AARCH64_TLSDESC: {
        /* TLSDESC: two words: (resolver, arg). Use a trap resolver. */
        int ti = -1;
        for (int i = 1; i < ctx->ntraps; i++)
            if (ctx->traps[i].name && strcmp(ctx->traps[i].name, "__tls_get_addr") == 0)
                { ti = i; break; }
        /* Our simplified TLSDESC: word0 = stub that returns TPIDR+arg, word1 = TPREL */
        uint64_t tprel = (uint64_t)lib->tls_offset + (sym ? sym->st_value : 0) +
                         (uint64_t)r->r_addend;
        /* Prefer a dedicated tlsdesc resolver if registered; else store TPREL
         * and a magic stub index we recognize specially. */
        int desc_i = -1;
        for (int i = 1; i < ctx->ntraps; i++)
            if (ctx->traps[i].name && strcmp(ctx->traps[i].name, "__jet_tlsdesc") == 0)
                { desc_i = i; break; }
        if (desc_i < 0) {
            /* register lazily */
            jet_native_register_host(ctx, "__jet_tlsdesc", jet_host_tlsdesc, NULL);
            for (int i = 1; i < ctx->ntraps; i++)
                if (ctx->traps[i].name && strcmp(ctx->traps[i].name, "__jet_tlsdesc") == 0)
                    { desc_i = i; break; }
        }
        where[0] = jet_stub_addr(ctx, desc_i);
        where[1] = tprel;
        (void)ti;
        return 0;
    }
    default:
        jet_log(ctx, 1, "unsupported reloc type %u sym=%s", type, sname);
        return 0; /* soft-fail */
    }
}

/* Android APS2 SLEB128 decoder. */
static int64_t decode_sleb128(const uint8_t **pp, const uint8_t *end) {
    int64_t result = 0; int shift = 0; uint8_t byte;
    do {
        if (*pp >= end) return 0;
        byte = *(*pp)++;
        result |= (int64_t)(byte & 0x7f) << shift;
        shift += 7;
    } while (byte & 0x80);
    if (shift < 64 && (byte & 0x40)) result |= -((int64_t)1 << shift);
    return result;
}
static uint64_t decode_uleb128(const uint8_t **pp, const uint8_t *end) {
    uint64_t result = 0; int shift = 0; uint8_t byte;
    do {
        if (*pp >= end) return 0;
        byte = *(*pp)++;
        result |= (uint64_t)(byte & 0x7f) << shift;
        shift += 7;
    } while (byte & 0x80);
    return result;
}

/* Apply Android APS2 packed RELA (DT_ANDROID_RELA). Format: "APS2" + SLEB groups. */
static int apply_aps2_rela(jet_ctx *ctx, jet_lib *lib, const uint8_t *p, size_t sz,
                           const Elf64_Sym *syms) {
    if (sz < 4 || memcmp(p, "APS2", 4) != 0) {
        jet_set_error(ctx, "APS2 magic missing");
        return -1;
    }
    const uint8_t *end = p + sz; p += 4;
    uint64_t reloc_count = (uint64_t)decode_sleb128(&p, end);
    uint64_t offset = 0;
    for (uint64_t n = 0; n < reloc_count && p < end; ) {
        int64_t base_offset_delta = decode_sleb128(&p, end);
        uint64_t num = (uint64_t)decode_sleb128(&p, end);
        offset += (uint64_t)base_offset_delta;
        int64_t addend = 0;
        for (uint64_t i = 0; i < num && p < end; i++, n++) {
            Elf64_Rela r;
            uint64_t info = (uint64_t)decode_sleb128(&p, end);
            int64_t od = decode_sleb128(&p, end);
            int64_t ad = decode_sleb128(&p, end);
            offset += (uint64_t)od;
            addend += ad;
            r.r_offset = offset;
            r.r_info = info;
            r.r_addend = addend;
            if (apply_rela(ctx, lib, &r, syms) != 0) return -1;
        }
    }
    (void)decode_uleb128;
    return 0;
}

/* Apply DT_RELR compact relative relocations. */
static int apply_relr(jet_ctx *ctx, jet_lib *lib, const uint64_t *relr, size_t count) {
    uint64_t *where = NULL;
    for (size_t i = 0; i < count; i++) {
        uint64_t entry = relr[i];
        if ((entry & 1) == 0) {
            where = (uint64_t *)G2H(lib->load_bias + entry);
            *where++ += lib->load_bias;
        } else {
            for (int b = 0; b < 63; b++) {
                if ((entry >> (b + 1)) & 1) {
                    where[b] += lib->load_bias;
                }
            }
            where += 63;
        }
    }
    (void)ctx;
    return 0;
}

jet_lib *jet_elf_load(jet_ctx *ctx, const char *path, char *err, size_t errlen) {
#define ERR(fmt, ...) do { \
    if (err && errlen) snprintf(err, errlen, fmt, ##__VA_ARGS__); \
    jet_set_error(ctx, fmt, ##__VA_ARGS__); \
    if (fd >= 0) close(fd); if (file) munmap(file, (size_t)st.st_size); \
    if (lib) { if (lib->base) munmap(lib->base, lib->map_size); free(lib->phdrs); free(lib->path); free(lib);} \
    return NULL; } while (0)

    int fd = -1; uint8_t *file = NULL; struct stat st; jet_lib *lib = NULL;
    fd = open(path, O_RDONLY);
    if (fd < 0) ERR("open %s: %s", path, strerror(errno));
    if (fstat(fd, &st) < 0) ERR("fstat: %s", strerror(errno));
    file = mmap(NULL, (size_t)st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    if (file == MAP_FAILED) { file = NULL; ERR("mmap file: %s", strerror(errno)); }

    if (st.st_size < (off_t)sizeof(Elf64_Ehdr)) ERR("file too small");
    const Elf64_Ehdr *eh = (const Elf64_Ehdr *)file;
    if (eh->e_ident[0]!=0x7f||eh->e_ident[1]!='E'||eh->e_ident[2]!='L'||eh->e_ident[3]!='F') ERR("not ELF");
    if (eh->e_ident[4] != 2 || eh->e_ident[5] != 1) ERR("need ELF64 LE");
    if (eh->e_type != ET_DYN) ERR("need ET_DYN");
    if (eh->e_machine != EM_AARCH64) ERR("need EM_AARCH64");

    const Elf64_Phdr *ph = (const Elf64_Phdr *)(file + eh->e_phoff);
    uint64_t vmin = UINT64_MAX, vmax = 0;
    for (int i = 0; i < eh->e_phnum; i++) {
        if (ph[i].p_type != PT_LOAD) continue;
        if (ph[i].p_vaddr < vmin) vmin = ph[i].p_vaddr;
        uint64_t end = ph[i].p_vaddr + ph[i].p_memsz;
        if (end > vmax) vmax = end;
    }
    if (vmin == UINT64_MAX) ERR("no PT_LOAD");
    /* align mapping to page; keep identity: guest VA = host ptr + (vaddr - vmin_aligned) */
    size_t page = (size_t)sysconf(_SC_PAGESIZE);
    uint64_t amin = vmin & ~(uint64_t)(page - 1);
    uint64_t amax = (vmax + page - 1) & ~(uint64_t)(page - 1);
    size_t map_size = (size_t)(amax - amin);

    void *base = mmap(NULL, map_size, PROT_READ|PROT_WRITE,
                      MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
    if (base == MAP_FAILED) ERR("mmap image: %s", strerror(errno));

    lib = calloc(1, sizeof(*lib));
    lib->ctx = ctx;
    lib->path = strdup(path);
    lib->base = base;
    lib->map_size = map_size;
    /* load_bias such that guest_addr = load_bias + vaddr, and G2H(guest_addr) works.
     * host_ptr = base + (vaddr - amin), so load_bias = H2G(base) - amin. */
    lib->load_bias = H2G(base) - amin;

    for (int i = 0; i < eh->e_phnum; i++) {
        if (ph[i].p_type != PT_LOAD) continue;
        uint8_t *dst = (uint8_t *)G2H(lib->load_bias + ph[i].p_vaddr);
        if (ph[i].p_filesz)
            memcpy(dst, file + ph[i].p_offset, (size_t)ph[i].p_filesz);
        /* BSS already zero from MAP_ANONYMOUS */
    }

    /* Persist full program header table for dl_iterate_phdr / unwinder. */
    lib->phnum = eh->e_phnum;
    lib->phdrs = calloc(eh->e_phnum ? eh->e_phnum : 1, sizeof(Elf64_Phdr));
    if (lib->phdrs && eh->e_phnum)
        memcpy(lib->phdrs, ph, (size_t)eh->e_phnum * sizeof(Elf64_Phdr));
    static uint64_t g_dlpi_gen;
    lib->dlpi_adds = ++g_dlpi_gen;

    /* DYNAMIC */
    const Elf64_Dyn *dyn = NULL;
    for (int i = 0; i < eh->e_phnum; i++)
        if (ph[i].p_type == PT_DYNAMIC) {
            dyn = (const Elf64_Dyn *)G2H(lib->load_bias + ph[i].p_vaddr);
            break;
        }
    if (!dyn) ERR("no PT_DYNAMIC");

    uint64_t strtab=0, symtab=0, syment=sizeof(Elf64_Sym);
    uint64_t rela=0, relasz=0, jmprel=0, pltrelsz=0;
    uint64_t init_arr=0, init_sz=0, fini_arr=0, fini_sz=0;
    uint64_t hash=0, gnu_hash=0;
    uint64_t relr=0, relrsz=0;
    uint64_t and_rela=0, and_relasz=0;
    uint64_t init_fn=0, fini_fn=0;

    for (const Elf64_Dyn *d = dyn; d->d_tag != DT_NULL; d++) {
        switch (d->d_tag) {
        case DT_STRTAB: strtab = d->d_un.d_ptr; break;
        case DT_SYMTAB: symtab = d->d_un.d_ptr; break;
        case DT_SYMENT: syment = d->d_un.d_val; break;
        case DT_RELA: rela = d->d_un.d_ptr; break;
        case DT_RELASZ: relasz = d->d_un.d_val; break;
        case DT_JMPREL: jmprel = d->d_un.d_ptr; break;
        case DT_PLTRELSZ: pltrelsz = d->d_un.d_val; break;
        case DT_INIT_ARRAY: init_arr = d->d_un.d_ptr; break;
        case DT_INIT_ARRAYSZ: init_sz = d->d_un.d_val; break;
        case DT_FINI_ARRAY: fini_arr = d->d_un.d_ptr; break;
        case DT_FINI_ARRAYSZ: fini_sz = d->d_un.d_val; break;
        case DT_INIT: init_fn = d->d_un.d_ptr; break;
        case DT_FINI: fini_fn = d->d_un.d_ptr; break;
        case DT_HASH: hash = d->d_un.d_ptr; break;
        case DT_GNU_HASH: gnu_hash = d->d_un.d_ptr; break;
        case DT_RELR: case DT_ANDROID_RELR: relr = d->d_un.d_ptr; break;
        case DT_RELRSZ: case DT_ANDROID_RELRSZ: relrsz = d->d_un.d_val; break;
        case DT_ANDROID_RELA: and_rela = d->d_un.d_ptr; break;
        case DT_ANDROID_RELASZ: and_relasz = d->d_un.d_val; break;
        default: break;
        }
    }
    (void)syment;

    lib->dynstr = (const char *)G2H(lib->load_bias + strtab);
    lib->dynsym = (const void *)G2H(lib->load_bias + symtab);

    if (gnu_hash) {
        const uint32_t *gh = (const uint32_t *)G2H(lib->load_bias + gnu_hash);
        lib->gnu_nbucket = gh[0];
        lib->gnu_symoffset = gh[1];
        lib->gnu_maskwords = gh[2];
        lib->gnu_shift2 = gh[3];
        lib->gnu_bloom = (const uint32_t *)(gh + 4); /* actually uint64[] */
        lib->gnu_buckets = (const uint32_t *)((const uint64_t *)(gh + 4) + lib->gnu_maskwords);
        lib->gnu_chain = lib->gnu_buckets + lib->gnu_nbucket;
        lib->has_gnu_hash = 1;
        /* dynsym count unknown precisely; estimate from hash or scan later */
        lib->dynsym_count = 0x100000; /* safe upper for lookups that stop on chain end */
    }
    if (hash) {
        const uint32_t *h = (const uint32_t *)G2H(lib->load_bias + hash);
        lib->nbucket = h[0];
        uint32_t nchain = h[1];
        lib->buckets = h + 2;
        lib->chains = lib->buckets + lib->nbucket;
        lib->dynsym_count = nchain;
    }

    /* TLS */
    for (int i = 0; i < eh->e_phnum; i++) {
        if (ph[i].p_type != PT_TLS) continue;
        if (ctx->tls_nmods + 1 >= JET_MAX_TLS_MODS) ERR("too many TLS modules");
        int mid = ++ctx->tls_nmods;
        lib->tls_modid = mid;
        lib->tls_image_size = (size_t)ph[i].p_memsz;
        lib->tls_image_align = (size_t)ph[i].p_align;
        if (ph[i].p_filesz)
            lib->tls_image = file + ph[i].p_offset; /* borrowed; copy below */
        /* persist init image */
        void *img = calloc(1, lib->tls_image_size ? lib->tls_image_size : 1);
        if (ph[i].p_filesz) memcpy(img, file + ph[i].p_offset, (size_t)ph[i].p_filesz);
        lib->tls_image = img;
        ctx->tls_mod_size[mid] = lib->tls_image_size;
        ctx->tls_mod_align[mid] = lib->tls_image_align ? lib->tls_image_align : 8;
        ctx->tls_mod_image[mid] = img;
        size_t al = ctx->tls_mod_align[mid];
        size_t off = JET_BIONIC_TCB_SIZE + ctx->tls_total;
        off = (off + al - 1) & ~(al - 1);
        lib->tls_offset = off; /* absolute offset from TPIDR_EL0 */
        ctx->tls_total = off + lib->tls_image_size - JET_BIONIC_TCB_SIZE;
        break;
    }

    const Elf64_Sym *syms = lib->dynsym;

    /* Apply relocations. Order: RELR, RELA, JMPREL, APS2. */
    if (relr && relrsz)
        apply_relr(ctx, lib, (const uint64_t *)G2H(lib->load_bias + relr),
                   (size_t)(relrsz / 8));

    if (rela && relasz) {
        const Elf64_Rela *r = (const Elf64_Rela *)G2H(lib->load_bias + rela);
        size_t n = (size_t)(relasz / sizeof(Elf64_Rela));
        for (size_t i = 0; i < n; i++)
            if (apply_rela(ctx, lib, &r[i], syms) != 0)
                ERR("rela failed");
    }
    if (jmprel && pltrelsz) {
        const Elf64_Rela *r = (const Elf64_Rela *)G2H(lib->load_bias + jmprel);
        size_t n = (size_t)(pltrelsz / sizeof(Elf64_Rela));
        for (size_t i = 0; i < n; i++)
            if (apply_rela(ctx, lib, &r[i], syms) != 0)
                ERR("jmprel failed");
    }
    if (and_rela && and_relasz) {
        if (apply_aps2_rela(ctx, lib,
                (const uint8_t *)G2H(lib->load_bias + and_rela),
                (size_t)and_relasz, syms) != 0)
            ERR("APS2 rela failed");
    }

    if (init_arr && init_sz) {
        lib->init_array = (uint64_t *)G2H(lib->load_bias + init_arr);
        lib->init_count = (size_t)(init_sz / 8);
    }
    if (fini_arr && fini_sz) {
        lib->fini_array = (uint64_t *)G2H(lib->load_bias + fini_arr);
        lib->fini_count = (size_t)(fini_sz / 8);
    }
    if (init_fn) lib->init_fn = lib->load_bias + init_fn;
    if (fini_fn) lib->fini_fn = lib->load_bias + fini_fn;

    jet_log(ctx, 2, "loaded %s bias=%p size=0x%zx init_array=%zu tls_mod=%d",
            path, (void *)(uintptr_t)lib->load_bias, map_size,
            lib->init_count, lib->tls_modid);

    munmap(file, (size_t)st.st_size);
    close(fd);
    return lib;
#undef ERR
}

int jet_elf_run_init(jet_ctx *ctx, jet_lib *lib) {
    if (lib->initialized) return 0;
    jet_args a = {0};
    if (lib->init_fn) {
        uint64_t r;
        if (jet_native_call(ctx, lib->init_fn, &a, &r, NULL) != 0) {
            jet_set_error(ctx, "DT_INIT failed: %s", ctx->last_error);
            return -1;
        }
    }
    int any_fail = 0;
    char first_err[256] = {0};
    for (size_t i = 0; i < lib->init_count; i++) {
        uint64_t fn = lib->init_array[i];
        if (!fn || fn == (uint64_t)-1) continue;
        uint64_t r;
        jet_log(ctx, 2, "init_array[%zu] enter 0x%llx", i, (unsigned long long)fn);
        ctx->last_trap_name[0] = 0;
        if (jet_native_call(ctx, fn, &a, &r, NULL) != 0) {
            jet_log(ctx, 0, "init_array[%zu] FAIL: %s", i, ctx->last_error);
            if (!any_fail) {
                snprintf(first_err, sizeof first_err, "init_array[%zu]: %s",
                         i, ctx->last_error);
            }
            any_fail = 1;
            /* continue remaining ctors */
            continue;
        }
        jet_log(ctx, 2, "init_array[%zu] OK", i);
    }
    if (any_fail) {
        jet_set_error(ctx, "%s", first_err);
        /* still mark initialized so fini can run; callers see last_error */
        lib->initialized = 1;
        return -1;
    }
    lib->initialized = 1;
    return 0;
}

void jet_elf_run_fini(jet_ctx *ctx, jet_lib *lib) {
    if (!lib->initialized) return;
    jet_args a = {0};
    for (size_t i = lib->fini_count; i > 0; i--) {
        uint64_t fn = lib->fini_array[i - 1];
        if (!fn || fn == (uint64_t)-1) continue;
        uint64_t r; jet_native_call(ctx, fn, &a, &r, NULL);
    }
    if (lib->fini_fn) {
        uint64_t r; jet_native_call(ctx, lib->fini_fn, &a, &r, NULL);
    }
    lib->initialized = 0;
    (void)ctx;
}

uint64_t jet_host_tlsdesc(jet_ctx *ctx, const jet_args *a, void *user) {
    /* AArch64 TLSDESC ABI: resolver(struct tlsdesc *desc) returns the
     * TPIDR-relative OFFSET of the object (not the absolute address).
     * Caller does:  blr resolver; mrs xN, tpidr_el0; add x0, xN, x0.
     * Returning TP+arg here double-adds and corrupts __cxa_get_globals TLS. */
    (void)user; (void)ctx;
    uint64_t *desc = (uint64_t *)G2H(a->x[0]);
    return desc[1];
}
