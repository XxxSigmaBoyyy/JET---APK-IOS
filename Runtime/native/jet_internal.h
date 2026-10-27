/* jet_internal.h - private structures for the JET AArch64 interpreter. */
#ifndef JET_INTERNAL_H
#define JET_INTERNAL_H

#include "jet_native.h"
#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

typedef unsigned __int128 u128;
typedef __int128          s128;

/* ---- magic stub (trap) addresses ----
 * Trap stubs live in a host-allocated RW page. Guest "branches" to
 * stub_base + idx*4; the run loop checks PC against this range and
 * dispatches the host function. Index 0 is the return-to-host sentinel
 * (written into guest LR on jet_native_call entry). */
#define JET_STUB_RETURN   0u
#define JET_STUB_STRIDE   4u
#define JET_MAX_TRAPS     4096
#define JET_MAX_LIBS      64
#define JET_MAX_TLS_MODS  64
#define JET_BLOCK_CACHE   65536
#define JET_BLOCK_MAX_INS 64
#define JET_TRAMP_POOL    256
#define JET_GUEST_STACK_DEFAULT (8ull << 20)

/* Bionic arm64 TCB: TPIDR_EL0 points here. 8 words reserved so that
 * TLS_SLOT_STACK_GUARD (slot 5) lives at TPIDR+0x28 — required by
 * -fstack-protector-strong in NDK-built SOs (~1200 sites in libtmessages). */
#define JET_BIONIC_TLS_SLOTS   8
#define JET_BIONIC_TCB_SIZE    (JET_BIONIC_TLS_SLOTS * 8)
#define JET_TLS_SLOT_DTV       0
#define JET_TLS_SLOT_STACK_GUARD 5

/* AArch64 HWCAP bits we advertise (Linux uapi). */
#define JET_HWCAP_FP    (1ull << 0)
#define JET_HWCAP_ASIMD (1ull << 1)
#define JET_HWCAP_EVTSTRM (1ull << 2)
#define JET_HWCAP_ATOMICS (1ull << 8) /* LSE - we prefer OFF so libs avoid it */

typedef struct jet_trap {
    char       *name;
    jet_host_fn fn;
    void       *user;
    unsigned    flags;    /* JET_HOST_RET_F64 / F32 */
    int         is_data;  /* 1 => `user` is a data address (OBJECT import) */
} jet_trap;

/* Per-guest-thread CPU state. Vector regs held as 2x u64 (low, high). */
typedef struct jet_cpu {
    uint64_t x[31];          /* X0..X30 (LR = X30) */
    uint64_t sp;
    uint64_t pc;
    uint32_t nzcv;           /* N Z C V in bits 31..28 */
    uint32_t fpcr;
    uint32_t fpsr;
    uint64_t v[32][2];       /* V0..V31 as 128-bit (lo, hi) */
    uint64_t tpidr_el0;      /* TLS base for this thread */
    /* setjmp/longjmp support */
    int      jumping;        /* 1 => longjmp in progress */
    int      stop;           /* 1 => return to host (trap/error) */
    int      err_code;       /* nonzero on fatal */
    char     err_msg[256];
    uint64_t stack_base;     /* mmap base of guest stack */
    size_t   stack_size;
    void    *tls_alloc;      /* calloc base for TLS (or NULL) */
} jet_cpu;

/* Decoded instruction (fixed-size for the block cache). */
typedef enum {
    OP_UNDEF = 0,
    OP_NOP, OP_HINT,
    OP_MOVZ, OP_MOVN, OP_MOVK,
    OP_ADDI, OP_SUBI, OP_ADDIS, OP_SUBIS,
    OP_ANDI, OP_ORRI, OP_EORI, OP_ANDIS,
    OP_ADR, OP_ADRP,
    OP_ADD, OP_SUB, OP_ADDS, OP_SUBS,
    OP_AND, OP_ORR, OP_EOR, OP_ANDS, OP_BIC, OP_ORN, OP_EON, OP_BICS,
    OP_ASRV, OP_LSLV, OP_LSRV, OP_RORV,
    OP_EXTR,
    OP_MADD, OP_MSUB, OP_SMADDL, OP_UMADDL, OP_SMSUBL, OP_UMSUBL,
    OP_SMULH, OP_UMULH,
    OP_SDIV, OP_UDIV,
    OP_CLS, OP_CLZ, OP_RBIT, OP_REV, OP_REV16, OP_REV32,
    OP_CSINC, OP_CSINV, OP_CSNEG, OP_CSEL,
    OP_CCMP, OP_CCMN,
    OP_BFM, OP_SBFM, OP_UBFM,        /* covers BFXIL, BFI, ASR/LSL/LSR imm, SXTH/SXTB/UXT* */
    OP_LDR_LIT, OP_LDRSW_LIT, OP_PRFM_LIT,
    OP_LDR, OP_LDRB, OP_LDRH, OP_LDRSB, OP_LDRSH, OP_LDRSW,
    OP_STR, OP_STRB, OP_STRH,
    OP_LDP, OP_LDPSW, OP_STP,
    OP_LDXR, OP_LDAXR, OP_LDAR, OP_LDLAR,
    OP_STXR, OP_STLXR, OP_STLR, OP_STLLR,
    OP_CAS, OP_CASP, OP_SWP, OP_LDADD, OP_LDCLR, OP_LDEOR, OP_LDSET,
    OP_B, OP_BL, OP_BR, OP_BLR, OP_RET,
    OP_BCOND, OP_CBZ, OP_CBNZ, OP_TBZ, OP_TBNZ,
    OP_SVC, OP_BRK, OP_HLT,
    OP_MRS, OP_MSR,
    OP_SYS,                          /* DC ZVA etc. */
    OP_DMB, OP_DSB, OP_ISB, OP_CLREX,
    /* FP / ASIMD (scalar + common vector) */
    OP_FMOV_I, OP_FMOV_G, OP_FMOV_F,
    OP_FADD, OP_FSUB, OP_FMUL, OP_FDIV, OP_FABS, OP_FNEG, OP_FSQRT, OP_FRINT,
    OP_FCMP, OP_FCCMP, OP_FCSEL,
    OP_FCVT, OP_FCVTZS, OP_FCVTZU, OP_SCVTF, OP_UCVTF,
    OP_FMADD, OP_FMSUB, OP_FNMADD, OP_FNMSUB,
    OP_FNMUL,
    OP_FMIN, OP_FMAX, OP_FMINNM, OP_FMAXNM,
    OP_LDR_FP, OP_STR_FP, OP_LDP_FP, OP_STP_FP,
    /* ASIMD memory & simple ALU used by memcpy/strlen */
    OP_LD1, OP_ST1, OP_LDUR_FP, OP_STUR_FP,
    OP_AND_V, OP_ORR_V, OP_EOR_V, OP_BIC_V,
    OP_ADD_V, OP_SUB_V, OP_MUL_V,
    OP_DUP, OP_INS, OP_UMOV, OP_SMOV, OP_EXT_V,
    OP_CNT, OP_REV64_V, OP_REV32_V, OP_REV16_V,
    OP_ZIP1, OP_ZIP2, OP_UZP1, OP_UZP2, OP_TRN1, OP_TRN2,
    OP_TBL, OP_TBX,
    OP_CMEQ, OP_CMGT, OP_CMGE, OP_CMHI, OP_CMHS, OP_CMTST,
    OP_SSHLL, OP_USHLL, OP_XTN, OP_SHL, OP_USHR, OP_SSHR,
    OP_MOVI_V, OP_MVNI_V, OP_FMOV_V,
    OP_ADDP_V, OP_ADDV, OP_UADDLV,
    OP_BIC_IMM_V, OP_ORR_IMM_V,
    /* ASIMD encoding-group catch-alls (opc/N carry opcode & U). */
    OP_ASIMD_3SAME,   /* opc=opcode[15:10], N=U, size, Q=regsize */
    OP_ASIMD_3DIFF,   /* opc=opcode[15:12], N=U */
    OP_ASIMD_2MISC,   /* opc=opcode[16:12], N=U  (incl. narrow/long/xtn/cnt/…) */
    OP_ASIMD_ACROSS,  /* opc=opcode[16:12], N=U */
    OP_ASIMD_SHIFT,   /* opc=opcode[15:11], N=U, imm=immh:immb */
    OP_ASIMD_BYELEM,  /* opc=opcode[15:12], N=U, imm=H:L:M index bits */
    OP_ASIMD_PAIRWISE,
    OP_UDOT, OP_SDOT, OP_USDOT, OP_CRC32,/* vector pairwise (addp/smaxp/…); opc like 3same */
    OP_LD1R,          /* LD1R..LD4R; opc=nregs-1, size, Q */
    OP_LD1_LANE, OP_ST1_LANE, /* single-structure lane; opc=nregs-1 */
    OP_LAST
} jet_op;

typedef struct jet_insn {
    jet_op   op;
    uint8_t  rd, rn, rm, ra;   /* registers; for loads rt2 in ra */
    uint8_t  sf;               /* 1 => 64-bit */
    uint8_t  size;             /* log2 bytes for load/store, or fp size */
    uint8_t  cond;             /* condition code */
    uint8_t  shift;            /* shift type 0=LSL 1=LSR 2=ASR 3=ROR */
    uint8_t  N, immr, imms;    /* logical imm / bitfield */
    uint8_t  scale;            /* for pair loads */
    uint8_t  opc;              /* miscellaneous opcode bits */
    uint8_t  regsize;          /* vector: Q bit */
    int32_t  imm;              /* signed immediate (or offset) */
    uint32_t uimm;             /* unsigned immediate */
    uint32_t raw;
} jet_insn;

typedef struct jet_block {
    uint64_t pc;               /* start PC */
    uint32_t n;                /* number of insns */
    uint32_t hash_next;
    jet_insn ins[JET_BLOCK_MAX_INS];
} jet_block;

/* ELF-loaded library. Identity-mapped: load_bias such that guest VA == host ptr. */
typedef struct jet_lib {
    jet_ctx   *ctx;
    char      *path;
    uint8_t   *base;           /* host pointer == guest VA of ELF vaddr 0 + bias */
    uint64_t   load_bias;      /* guest VA of ELF vaddr 0 (== (uint64_t)base) */
    size_t     map_size;
    /* dynsym */
    const void *dynsym;
    const char *dynstr;
    size_t      dynsym_count;
    const uint32_t *buckets;
    const uint32_t *chains;
    uint32_t    nbucket;
    /* gnu hash */
    const uint32_t *gnu_bloom;
    const uint32_t *gnu_buckets;
    const uint32_t *gnu_chain;
    uint32_t    gnu_nbucket;
    uint32_t    gnu_symoffset;
    uint32_t    gnu_maskwords;
    uint32_t    gnu_shift2;
    int         has_gnu_hash;
    /* Program headers (full copy for dl_iterate_phdr / unwinder) */
    void       *phdrs;         /* Elf64_Phdr[phnum] */
    uint16_t    phnum;
    uint64_t    dlpi_adds;     /* load generation counters */
    /* TLS */
    size_t      tls_image_size;
    size_t      tls_image_align;
    const void *tls_image;     /* init image (file) */
    size_t      tls_offset;    /* offset within thread TLS block (module) */
    int         tls_modid;
    /* Class loader for this library (host jobject), used during JNI_OnLoad */
    void       *class_loader;
    /* init/fini */
    uint64_t   *init_array;
    size_t      init_count;
    uint64_t   *fini_array;
    size_t      fini_count;
    uint64_t    init_fn, fini_fn;
    int         initialized;
    /* link */
    struct jet_lib *next;
} jet_lib;

/* Host->guest trampoline slot. */
typedef struct {
    int      used;
    jet_ctx *ctx;
    uint64_t guest_fn;
    void    *class_loader;   /* host jobject ClassLoader for FindClass */
    char     class_name[256];
} jet_tramp_slot;

struct jet_ctx {
    jet_config cfg;
    /* trap table; index 0 = return sentinel (fn=NULL) */
    jet_trap  traps[JET_MAX_TRAPS];
    int       ntraps;
    uint8_t  *stub_page;       /* RW page of ntraps * 4 bytes (contents unused) */
    uint64_t  stub_base;       /* guest VA (== host ptr) of stub_page */
    size_t    stub_page_size;

    jet_lib  *libs;

    /* TLS modules: module 1..N (module 0 unused; matches ELF TLS) */
    size_t    tls_mod_size[JET_MAX_TLS_MODS];
    size_t    tls_mod_align[JET_MAX_TLS_MODS];
    const void *tls_mod_image[JET_MAX_TLS_MODS];
    int       tls_nmods;       /* highest modid assigned */
    size_t    tls_total;       /* sum of all module sizes (padded) */

    /* current CPU (thread-local via pthread key, but also a default) */
    pthread_key_t cpu_key;
    jet_cpu      *main_cpu;
    size_t        stack_size;

    /* block cache: open addressing by PC */
    jet_block   *blocks;
    uint32_t     block_mask;   /* size-1, size power of 2 */
    uint64_t     block_hits, block_misses;

    /* JNI */
    void           *host_javavm;
    void           *host_jnienv;
    void           *guest_jnienv;   /* guest-visible JNIEnv (table of stubs) */
    void           *guest_javavm;   /* guest-visible JavaVM */
    void           *jni_pending;     /* fake throwable struct */
    uint64_t        jni_pending_obj; /* guest ref returned by ExceptionOccurred */
    int             jni_pending_is_throwable;
    char            guest_cwd[512];  /* emulated process cwd */
    void           *default_class_loader; /* host jobject, settable via API */
    /* Per-thread-ish stack of class loaders for FindClass (host jobject). */
#define JET_LOADER_STACK 32
    void           *loader_stack[JET_LOADER_STACK];
    int             loader_sp;
    jet_lib        *onload_lib;      /* library currently in JNI_OnLoad */
    jet_register_cb reg_cb;
    void           *reg_cb_user;
    /* captured RegisterNatives for tests */
    struct {
        char cls[256], name[128], sig[256];
        uint64_t fn;
        int used;
    } reg_log[1024];
    int nreg_log;

    /* trampoline pool */
    jet_tramp_slot tramps[JET_TRAMP_POOL];

    /* exclusive-monitor (simplified single global) */
    int       excl_valid;
    uint64_t  excl_addr;
    uint64_t  excl_val;
    pthread_mutex_t excl_mu;
    pthread_mutex_t trap_mu;

    char last_error[512];
    uint64_t insn_count;
    char     last_trap_name[128];
    uint64_t last_trap_pc;
    uint64_t fault_x[8];
    uint64_t fault_sp, fault_lr, fault_pc;

    /* Guest-emulated signal state. NEVER installed on the host process —
     * the embedding JVM owns SIGSEGV/SIGBUS. */
    uint8_t  guest_sig_mask[16];          /* rt_sigset, 128-bit */
    struct {
        uint64_t handler;                 /* sa_handler / sa_sigaction bits */
        uint64_t flags;
        uint8_t  mask[16];
        int      set;
    } guest_sig_action[64];
    uint64_t guest_ss_sp;
    uint64_t guest_ss_size;
    int      guest_ss_flags;
};

/* ---- helpers ---- */
static inline uint64_t jet_x(jet_cpu *c, int i) {
    return i == 31 ? 0 : c->x[i];
}
static inline void jet_set_x(jet_cpu *c, int i, uint64_t v) {
    if (i != 31) c->x[i] = v;
}
static inline uint64_t jet_sp(jet_cpu *c) { return c->sp; }
static inline void jet_set_sp(jet_cpu *c, uint64_t v) { c->sp = v; }

static inline int jet_is_stub(jet_ctx *ctx, uint64_t pc) {
    return pc >= ctx->stub_base &&
           pc <  ctx->stub_base + (uint64_t)ctx->ntraps * JET_STUB_STRIDE &&
           ((pc - ctx->stub_base) % JET_STUB_STRIDE) == 0;
}
static inline int jet_stub_index(jet_ctx *ctx, uint64_t pc) {
    return (int)((pc - ctx->stub_base) / JET_STUB_STRIDE);
}
static inline uint64_t jet_stub_addr(jet_ctx *ctx, int idx) {
    return ctx->stub_base + (uint64_t)idx * JET_STUB_STRIDE;
}

void jet_log(jet_ctx *ctx, int level, const char *fmt, ...);
void jet_set_error(jet_ctx *ctx, const char *fmt, ...);

jet_cpu *jet_cpu_current(jet_ctx *ctx);
jet_cpu *jet_cpu_create(jet_ctx *ctx);
int jet_cpu_setup_tls(jet_ctx *ctx, jet_cpu *c);
extern uintptr_t jet_stack_chk_guard; /* same value in TLS slot 5 */
void     jet_cpu_destroy(jet_ctx *ctx, jet_cpu *cpu);

/* Interpreter entry: run until PC hits return stub or fatal. */
int jet_interp_run(jet_ctx *ctx, jet_cpu *cpu);

/* Decode one instruction. Returns 0 on success, -1 on undef. */
int jet_decode(uint32_t raw, jet_insn *out);
int jet_asimd_exec(jet_cpu *c, jet_ctx *ctx, const jet_insn *i);

/* Block cache lookup / build. */
jet_block *jet_block_get(jet_ctx *ctx, uint64_t pc);

/* ELF */
jet_lib *jet_elf_load(jet_ctx *ctx, const char *path, char *err, size_t errlen);
uint64_t jet_elf_sym(jet_lib *lib, const char *name);
int      jet_elf_run_init(jet_ctx *ctx, jet_lib *lib);
void     jet_elf_run_fini(jet_ctx *ctx, jet_lib *lib);

/* Host shims: register default bionic/libc/libm/liblog/libandroid/GLES stubs. */
int jet_host_install_bionic(jet_ctx *ctx);

/* JNI guest table construction. */
int jet_jni_build_tables(jet_ctx *ctx);

/* Trampoline invoke helper (used by pool stubs). */
uint64_t jet_tramp_invoke(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a);

/* Guest setjmp helpers (registered as internal traps). */
int jet_host_install_setjmp(jet_ctx *ctx);
void jet_tramp_set_active(jet_ctx *ctx);
void jet_loader_push(jet_ctx *ctx, void *loader);
void jet_loader_pop(jet_ctx *ctx);
void *jet_loader_current(jet_ctx *ctx);
jet_trampoline jet_native_trampoline_ex(jet_ctx *ctx, uint64_t guest_fn,
                                        void *class_loader, const char *class_name);


/* Memory helpers (identity map: guest VA == host pointer).
 * jet_mem_guard_depth marks interpreter guarded load/store sites so soft-fault
 * only claims SIGSEGV/SIGBUS raised from these helpers (or guest-mapped si_addr). */
extern __thread volatile int jet_mem_guard_depth;
static inline void *G2H(uint64_t g) { return (void *)(uintptr_t)g; }
static inline uint64_t H2G(const void *h) { return (uint64_t)(uintptr_t)h; }

static inline uint8_t  ld1(uint64_t a){
    jet_mem_guard_depth++;
    uint8_t v = *(volatile uint8_t*)G2H(a);
    jet_mem_guard_depth--;
    return v;
}
static inline uint16_t ld2(uint64_t a){
    jet_mem_guard_depth++;
    uint16_t v = *(volatile uint16_t*)G2H(a);
    jet_mem_guard_depth--;
    return v;
}
static inline uint32_t ld4(uint64_t a){
    jet_mem_guard_depth++;
    uint32_t v = *(volatile uint32_t*)G2H(a);
    jet_mem_guard_depth--;
    return v;
}
static inline uint64_t ld8(uint64_t a){
    jet_mem_guard_depth++;
    uint64_t v = *(volatile uint64_t*)G2H(a);
    jet_mem_guard_depth--;
    return v;
}
static inline void st1(uint64_t a, uint8_t  v){
    jet_mem_guard_depth++;
    *(volatile uint8_t*)G2H(a)=v;
    jet_mem_guard_depth--;
}
static inline void st2(uint64_t a, uint16_t v){
    jet_mem_guard_depth++;
    *(volatile uint16_t*)G2H(a)=v;
    jet_mem_guard_depth--;
}
static inline void st4(uint64_t a, uint32_t v){
    jet_mem_guard_depth++;
    *(volatile uint32_t*)G2H(a)=v;
    jet_mem_guard_depth--;
}
static inline void st8(uint64_t a, uint64_t v){
    jet_mem_guard_depth++;
    *(volatile uint64_t*)G2H(a)=v;
    jet_mem_guard_depth--;
}

/* Linux SVC emulation (host_bionic.c) — portable, no raw syscall() on Darwin. */
int64_t jet_emulate_linux_syscall(jet_ctx *ctx, jet_cpu *cpu,
                                  uint64_t nr, uint64_t a0, uint64_t a1,
                                  uint64_t a2, uint64_t a3, uint64_t a4,
                                  uint64_t a5);

#endif /* JET_INTERNAL_H */
