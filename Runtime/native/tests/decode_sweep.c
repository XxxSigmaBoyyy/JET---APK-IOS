/* decode_sweep — reusable AArch64 decode-coverage tool.
 * Usage:
 *   decode_sweep [--no-mnem] <so> [so...]
 *   decode_sweep [--no-mnem] --corpus <dir>
 */
#include "jet_internal.h"
#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dirent.h>

static int g_dump; /* --dump: print "va raw" for every undef */

typedef struct {
    uint32_t raw;
    uint64_t count;
    uint64_t sample_va;
    char mnemonic[96];
    char klass[48];
} undef_ent;

typedef struct {
    undef_ent *v;
    size_t n, cap;
    uint64_t total_insns, undef_insns, undef_crypto, undef_other, undef_data;
} sweep_stats;

static int ent_cmp(const void *a, const void *b) {
    const undef_ent *x = a, *y = b;
    if (y->count != x->count) return (y->count > x->count) ? 1 : -1;
    return (x->raw > y->raw) - (x->raw < y->raw);
}

static void classify(const char *mnem, char *out, size_t n) {
    char buf[96];
    size_t i;
    for (i = 0; mnem[i] && i + 1 < sizeof buf; i++) {
        char c = mnem[i];
        if (c >= 'A' && c <= 'Z') c = (char)(c - 'A' + 'a');
        buf[i] = c;
    }
    buf[i] = 0;
    char *s = buf;
    while (*s == ' ' || *s == '\t') s++;
    if (!s[0] || strstr(s, ".inst") || strstr(s, "undefined") || strstr(s, "<unknown>") ||
        strstr(s, "udf")) {
        snprintf(out, n, "data_or_unknown"); return;
    }
    if (!strncmp(s, "aes", 3) || !strncmp(s, "pmull", 5) ||
        !strncmp(s, "sha1", 4) || !strncmp(s, "sha256", 6) ||
        !strncmp(s, "sha512", 6) || !strncmp(s, "eor3", 4) ||
        !strncmp(s, "rax1", 4) || !strncmp(s, "xar ", 4) || !strncmp(s, "xar\t", 4) ||
        !strncmp(s, "bcax", 4) || !strncmp(s, "sm3", 3) || !strncmp(s, "sm4", 3)) {
        snprintf(out, n, "crypto_ext"); return;
    }
    if (!strncmp(s, "fmov", 4)) { snprintf(out, n, "fmov"); return; }
    if (!strncmp(s, "dmb", 3) || !strncmp(s, "dsb", 3) || !strncmp(s, "isb", 3)) {
        snprintf(out, n, "barrier"); return;
    }
    if (!strncmp(s, "hint", 4) || !strncmp(s, "nop", 3) || !strncmp(s, "yield", 5) ||
        !strncmp(s, "wfe", 3) || !strncmp(s, "wfi", 3) || !strncmp(s, "sev", 3) ||
        !strncmp(s, "bti", 3) || !strncmp(s, "pac", 3) || !strncmp(s, "aut", 3) ||
        !strncmp(s, "xpac", 4) || !strncmp(s, "clrex", 5)) {
        snprintf(out, n, "hint_sys"); return;
    }
    if (!strncmp(s, "cas", 3) || !strncmp(s, "swp", 3) ||
        !strncmp(s, "ldadd", 5) || !strncmp(s, "ldclr", 5) ||
        !strncmp(s, "ldeor", 5) || !strncmp(s, "ldset", 5) ||
        !strncmp(s, "ldsmax", 6) || !strncmp(s, "ldsmin", 6) ||
        !strncmp(s, "ldumax", 6) || !strncmp(s, "ldumin", 6) ||
        !strncmp(s, "stadd", 5) || !strncmp(s, "stclr", 5) ||
        !strncmp(s, "steor", 5) || !strncmp(s, "stset", 5) ||
        !strncmp(s, "stsmax", 6) || !strncmp(s, "stsmin", 6) ||
        !strncmp(s, "stumax", 6) || !strncmp(s, "stumin", 6) ||
        !strncmp(s, "ldxr", 4) || !strncmp(s, "stxr", 4) ||
        !strncmp(s, "ldaxr", 5) || !strncmp(s, "stlxr", 5)) {
        snprintf(out, n, "atomic_lse"); return;
    }
    if (!strncmp(s, "mrs", 3) || !strncmp(s, "msr", 3) || !strncmp(s, "sys", 3) ||
        !strncmp(s, "dc ", 3) || !strncmp(s, "ic ", 3) || !strncmp(s, "tlbi", 4)) {
        snprintf(out, n, "system"); return;
    }
    if (!strncmp(s, "ptrue", 5) || !strncmp(s, "while", 5) || !strncmp(s, "movprfx", 7) ||
        !strncmp(s, "compact", 7) || !strncmp(s, "punpk", 5) || !strncmp(s, "sve", 3) ||
        (s[0] == 'z' && (s[1] >= '0' && s[1] <= '9'))) {
        snprintf(out, n, "sve_sme"); return;
    }
    if (s[0] == 'f' || !strncmp(s, "ucvtf", 5) || !strncmp(s, "scvtf", 5) ||
        !strncmp(s, "fcvt", 4) || !strncmp(s, "frint", 5)) {
        snprintf(out, n, "fp_asimd"); return;
    }
    if (!strncmp(s, "ld", 2) || !strncmp(s, "st", 2) || !strncmp(s, "prfm", 4)) {
        snprintf(out, n, "load_store"); return;
    }
    snprintf(out, n, "other");
}

static undef_ent *find_or_add(sweep_stats *st, uint32_t raw, uint64_t va) {
    for (size_t i = 0; i < st->n; i++) if (st->v[i].raw == raw) {
        st->v[i].count++;
        return &st->v[i];
    }
    if (st->n + 1 > st->cap) {
        size_t nc = st->cap ? st->cap * 2 : 512;
        undef_ent *nv = realloc(st->v, nc * sizeof(*nv));
        if (!nv) return NULL;
        st->v = nv; st->cap = nc;
    }
    undef_ent *e = &st->v[st->n++];
    memset(e, 0, sizeof *e);
    e->raw = raw; e->count = 1; e->sample_va = va;
    return e;
}

static void fill_mnemonics(const char *path, sweep_stats *st) {
    (void)path;
    if (!st->n) return;
    char binpath[] = "/tmp/jet_sweep_bin_XXXXXX";
    int fd = mkstemp(binpath);
    if (fd < 0) return;
    for (size_t i = 0; i < st->n; i++) {
        uint32_t r = st->v[i].raw;
        if (write(fd, &r, 4) != 4) { close(fd); unlink(binpath); return; }
    }
    close(fd);
    /* Wrap as a minimal ET_REL aarch64 object via llvm-objcopy, or just use:
     * llvm-mc isn't happy with raw bins; use objdump -D -b binary -m aarch64. */
    char cmd[512];
    snprintf(cmd, sizeof cmd,
             "aarch64-linux-gnu-objdump -D -b binary -m aarch64 %s 2>/dev/null", binpath);
    FILE *fp = popen(cmd, "r");
    if (!fp) { unlink(binpath); return; }
    char line[256];
    while (fgets(line, sizeof line, fp)) {
        char *colon = strchr(line, ':');
        if (!colon) continue;
        char *hp = colon;
        while (hp > line && ((hp[-1] >= '0' && hp[-1] <= '9') ||
                             (hp[-1] >= 'a' && hp[-1] <= 'f') ||
                             (hp[-1] >= 'A' && hp[-1] <= 'F'))) hp--;
        unsigned long off = strtoul(hp, NULL, 16);
        size_t idx = off / 4;
        if (idx >= st->n) continue;
        char *rest = colon + 1;
        while (*rest == ' ' || *rest == '\t') rest++;
        /* skip raw hex words */
        char *mnem = rest;
        if ((rest[0] >= '0' && rest[0] <= '9') || (rest[0] >= 'a' && rest[0] <= 'f') ||
            (rest[0] >= 'A' && rest[0] <= 'F')) {
            char *tab = strchr(rest, '\t');
            if (tab) mnem = tab + 1;
            else {
                while (*mnem && *mnem != ' ' && *mnem != '\t') mnem++;
                while (*mnem == ' ' || *mnem == '\t') mnem++;
            }
        }
        while (*mnem == ' ' || *mnem == '\t') mnem++;
        size_t j = 0;
        while (mnem[j] && mnem[j] != '\n' && j + 1 < sizeof st->v[idx].mnemonic) {
            st->v[idx].mnemonic[j] = mnem[j]; j++;
        }
        st->v[idx].mnemonic[j] = 0;
        classify(st->v[idx].mnemonic, st->v[idx].klass, sizeof st->v[idx].klass);
    }
    pclose(fp);
    unlink(binpath);
    for (size_t i = 0; i < st->n; i++) {
        if (!st->v[i].mnemonic[0]) {
            snprintf(st->v[i].mnemonic, sizeof st->v[i].mnemonic, "<no-dump>");
            classify(st->v[i].mnemonic, st->v[i].klass, sizeof st->v[i].klass);
        }
    }
}

static int sweep_one(const char *path, sweep_stats *st, int do_mnem) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) { fprintf(stderr, "open %s: %s\n", path, strerror(errno)); return -1; }
    struct stat stbuf;
    if (fstat(fd, &stbuf) != 0) { close(fd); return -1; }
    void *map = mmap(NULL, (size_t)stbuf.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (map == MAP_FAILED) { perror("mmap"); return -1; }
    const Elf64_Ehdr *eh = map;
    if (memcmp(eh->e_ident, ELFMAG, SELFMAG) != 0 || eh->e_ident[EI_CLASS] != ELFCLASS64 ||
        eh->e_machine != EM_AARCH64) {
        fprintf(stderr, "%s: not AArch64 ELF64\n", path);
        munmap(map, (size_t)stbuf.st_size);
        return -1;
    }
    const Elf64_Shdr *sh = (const Elf64_Shdr *)((const char *)map + eh->e_shoff);
    for (int i = 0; i < eh->e_shnum; i++) {
        if (!(sh[i].sh_flags & SHF_EXECINSTR) || sh[i].sh_size < 4) continue;
        const uint8_t *base = (const uint8_t *)map + sh[i].sh_offset;
        uint64_t vaddr = sh[i].sh_addr, size = sh[i].sh_size;
        uint64_t off = (vaddr & 3) ? (4 - (vaddr & 3)) : 0;
        for (; off + 4 <= size; off += 4) {
            uint32_t raw; memcpy(&raw, base + off, 4);
            st->total_insns++;
            jet_insn ins; memset(&ins, 0, sizeof ins);
            if (jet_decode(raw, &ins) == 0 && ins.op != OP_UNDEF) continue;
            st->undef_insns++;
            if (g_dump) printf("U %llx %08x\n", (unsigned long long)(vaddr + off), raw);
            else find_or_add(st, raw, vaddr + off);
        }
    }
    munmap(map, (size_t)stbuf.st_size);
    if (do_mnem) fill_mnemonics(path, st);
    else {
        for (size_t i = 0; i < st->n; i++) {
            snprintf(st->v[i].mnemonic, sizeof st->v[i].mnemonic, "?");
            snprintf(st->v[i].klass, sizeof st->v[i].klass, "other");
        }
    }
    return 0;
}

static void finalize(sweep_stats *st) {
    st->undef_crypto = st->undef_other = st->undef_data = 0;
    for (size_t i = 0; i < st->n; i++) {
        if (!strcmp(st->v[i].klass, "crypto_ext")) st->undef_crypto += st->v[i].count;
        else if (!strcmp(st->v[i].klass, "data_or_unknown")) st->undef_data += st->v[i].count;
        else st->undef_other += st->v[i].count;
    }
}

static void print_report(const char *path, sweep_stats *st) {
    qsort(st->v, st->n, sizeof(*st->v), ent_cmp);
    finalize(st);
    printf("== %s ==\n", path);
    printf("  scanned=%" PRIu64 "  undef=%" PRIu64 "  unique=%zu\n",
           st->total_insns, st->undef_insns, st->n);
    printf("  (a) real/other=%" PRIu64 "  (b) crypto_ext=%" PRIu64 "  data/udf=%" PRIu64 "\n",
           st->undef_other, st->undef_crypto, st->undef_data);
    typedef struct { char k[48]; uint64_t c; int nenc; } grp;
    grp g[64]; int ng = 0;
    for (size_t i = 0; i < st->n; i++) {
        int f = -1;
        for (int j = 0; j < ng; j++) if (!strcmp(g[j].k, st->v[i].klass)) { f = j; break; }
        if (f < 0 && ng < 64) {
            f = ng++;
            snprintf(g[f].k, sizeof g[f].k, "%s", st->v[i].klass);
            g[f].c = 0; g[f].nenc = 0;
        }
        if (f >= 0) { g[f].c += st->v[i].count; g[f].nenc++; }
    }
    printf("  by class:\n");
    for (int j = 0; j < ng; j++)
        printf("    %-16s  n=%" PRIu64 "  encodings=%d\n", g[j].k, g[j].c, g[j].nenc);
    printf("  top:\n");
    size_t lim = st->n < 30 ? st->n : 30;
    for (size_t i = 0; i < lim; i++)
        printf("    %08x x%-7" PRIu64 " [%-12s] %s\n",
               st->v[i].raw, st->v[i].count, st->v[i].klass, st->v[i].mnemonic);
    printf("  all fmov leftovers:\n");
    for (size_t i = 0; i < st->n; i++)
        if (!strcmp(st->v[i].klass, "fmov"))
            printf("    %08x x%-7" PRIu64 " %s\n",
                   st->v[i].raw, st->v[i].count, st->v[i].mnemonic);
    printf("\n");
}

static void add_sos_walk(const char *dir, char ***out, int *nout) {
    DIR *d = opendir(dir);
    if (!d) return;
    struct dirent *de;
    while ((de = readdir(d))) {
        if (de->d_name[0] == '.') continue;
        char path[1024];
        snprintf(path, sizeof path, "%s/%s", dir, de->d_name);
        struct stat st;
        if (stat(path, &st) != 0) continue;
        if (S_ISDIR(st.st_mode)) add_sos_walk(path, out, nout);
        else {
            size_t n = strlen(de->d_name);
            if (n > 3 && !strcmp(de->d_name + n - 3, ".so")) {
                char **nv = realloc(*out, (size_t)(*nout + 1) * sizeof(char *));
                if (!nv) continue;
                *out = nv;
                (*out)[(*nout)++] = strdup(path);
            }
        }
    }
    closedir(d);
}
static int path_cmp(const void *a, const void *b) {
    return strcmp(*(char *const *)a, *(char *const *)b);
}

int main(int argc, char **argv) {
    char **files = NULL; int nfiles = 0, do_mnem = 1, argi = 1;
    if (argi < argc && !strcmp(argv[argi], "--no-mnem")) { do_mnem = 0; argi++; }
    if (argi < argc && !strcmp(argv[argi], "--dump")) { g_dump = 1; do_mnem = 0; argi++; }
    if (argi < argc && !strcmp(argv[argi], "--corpus")) {
        if (argi + 1 >= argc) { fprintf(stderr, "need --corpus dir\n"); return 2; }
        add_sos_walk(argv[argi + 1], &files, &nfiles);
        if (nfiles) qsort(files, (size_t)nfiles, sizeof(char *), path_cmp);
        if (!nfiles) { printf("corpus empty: %s (skip)\n", argv[argi + 1]); return 0; }
        argi += 2;
    } else {
        for (; argi < argc; argi++) {
            files = realloc(files, (size_t)(nfiles + 1) * sizeof(char *));
            files[nfiles++] = strdup(argv[argi]);
        }
    }
    if (!nfiles) {
        fprintf(stderr, "Usage: %s [--no-mnem] <so>... | --corpus <dir>\n", argv[0]);
        return 2;
    }
    uint64_t ga = 0, gb = 0;
    for (int i = 0; i < nfiles; i++) {
        sweep_stats st; memset(&st, 0, sizeof st);
        fprintf(stderr, "sweep %s ...\n", files[i]);
        if (g_dump) { printf("F %s\n", files[i]); sweep_one(files[i], &st, 0); free(st.v); free(files[i]); continue; }
        if (sweep_one(files[i], &st, do_mnem) == 0) {
            print_report(files[i], &st);
            ga += st.undef_other; gb += st.undef_crypto;
        }
        free(st.v); free(files[i]);
    }
    free(files);
    printf("TOTAL (a) real/other=%" PRIu64 "  (b) crypto_ext=%" PRIu64 "\n", ga, gb);
    return 0;
}
