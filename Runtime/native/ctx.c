/* ctx.c - jet_ctx lifecycle, trap table, jet_native_call. */
#include "jet_internal.h"
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <sys/mman.h>
#include <unistd.h>

void jet_log(jet_ctx *ctx, int level, const char *fmt, ...) {
    if (!ctx || !ctx->cfg.log) return;
    char buf[1024];
    va_list ap; va_start(ap, fmt);
    vsnprintf(buf, sizeof buf, fmt, ap);
    va_end(ap);
    ctx->cfg.log(ctx->cfg.log_user, level, buf);
}

void jet_set_error(jet_ctx *ctx, const char *fmt, ...) {
    if (!ctx) return;
    va_list ap; va_start(ap, fmt);
    vsnprintf(ctx->last_error, sizeof ctx->last_error, fmt, ap);
    va_end(ap);
    jet_log(ctx, 0, "%s", ctx->last_error);
}

const char *jet_native_last_error(jet_ctx *ctx) {
    return ctx ? ctx->last_error : "null ctx";
}

jet_cpu *jet_cpu_create(jet_ctx *ctx) {
    jet_cpu *c = calloc(1, sizeof(*c));
    if (!c) return NULL;
    size_t ss = ctx->stack_size;
    void *stack = mmap(NULL, ss, PROT_READ|PROT_WRITE,
                       MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
    if (stack == MAP_FAILED) { free(c); return NULL; }
    c->stack_base = H2G(stack);
    c->stack_size = ss;
    c->sp = c->stack_base + ss - 64;
    /* Always install a bionic-compatible TCB so MRS TPIDR_EL0 / LDR [tp,#0x28]
     * stack-canary sequences work even when the SO has no PT_TLS (tgfoss:
     * ~1200 canary sites). Guest x18 is ordinary interpreter state — never
     * alias it to the host's reserved x18 (Apple) or to TPIDR. */
    if (jet_cpu_setup_tls(ctx, c) != 0) {
        munmap(stack, ss); free(c); return NULL;
    }
    return c;
}

/* Build [bionic TCB | ELF TLS modules] and point TPIDR_EL0 at the TCB. */
int jet_cpu_setup_tls(jet_ctx *ctx, jet_cpu *c) {
    size_t mod_bytes = ctx->tls_total;
    size_t ts = JET_BIONIC_TCB_SIZE + mod_bytes + 64; /* padding */
    void *tls = calloc(1, ts);
    if (!tls) return -1;
    free(c->tls_alloc);
    c->tls_alloc = tls;
    uint8_t *base = (uint8_t *)tls;
    /* Slot 0: self pointer (some code expects *TP == TP). */
    ((uint64_t *)base)[JET_TLS_SLOT_DTV] = H2G(base);
    /* Slot 5: stack guard canary — must be nonzero & stable for the thread. */
    /* Must match __stack_chk_guard — some TUs use the global, others TLS. */
    ((uint64_t *)base)[JET_TLS_SLOT_STACK_GUARD] = (uint64_t)jet_stack_chk_guard;
    /* ELF TLS modules follow the 8-word bionic TCB. Prefer the absolute
     * TPIDR-relative offset recorded at load (already includes TCB). */
    for (int m = 1; m <= ctx->tls_nmods; m++) {
        size_t off = JET_BIONIC_TCB_SIZE;
        int found = 0;
        for (jet_lib *L = ctx->libs; L; L = L->next) {
            if (L->tls_modid == m) { off = L->tls_offset; found = 1; break; }
        }
        if (!found) {
            size_t al = ctx->tls_mod_align[m] ? ctx->tls_mod_align[m] : 8;
            off = (off + al - 1) & ~(al - 1);
        }
        if (off + ctx->tls_mod_size[m] > ts) continue;
        if (ctx->tls_mod_image[m] && ctx->tls_mod_size[m])
            memcpy(base + off, ctx->tls_mod_image[m], ctx->tls_mod_size[m]);
    }
    c->tpidr_el0 = H2G(base);
    return 0;
}

void jet_cpu_destroy(jet_ctx *ctx, jet_cpu *cpu) {
    (void)ctx;
    if (!cpu) return;
    if (cpu->stack_base)
        munmap(G2H(cpu->stack_base), cpu->stack_size);
    free(cpu->tls_alloc);
    free(cpu);
}

jet_cpu *jet_cpu_current(jet_ctx *ctx) {
    jet_cpu *c = pthread_getspecific(ctx->cpu_key);
    if (c) return c;
    return ctx->main_cpu;
}

static void default_log(void *user, int level, const char *msg) {
    (void)user;
    static const char *L[] = {"E","W","I","D"};
    fprintf(stderr, "[jet:%s] %s\n", L[level < 0 ? 0 : (level > 3 ? 3 : level)], msg);
}

jet_ctx *jet_native_create(const jet_config *cfg) {
    jet_ctx *ctx = calloc(1, sizeof(*ctx));
    if (!ctx) return NULL;
    if (cfg) ctx->cfg = *cfg;
    if (!ctx->cfg.log) ctx->cfg.log = default_log;
    if (!ctx->cfg.hwcap) ctx->cfg.hwcap = JET_HWCAP_FP | JET_HWCAP_ASIMD;
    ctx->stack_size = ctx->cfg.thread_stack_size
                        ? ctx->cfg.thread_stack_size
                        : JET_GUEST_STACK_DEFAULT;
    pthread_mutex_init(&ctx->excl_mu, NULL);
    pthread_mutex_init(&ctx->trap_mu, NULL);
    pthread_key_create(&ctx->cpu_key, NULL);

    /* stub page */
    size_t psz = (size_t)sysconf(_SC_PAGESIZE);
    size_t need = ((JET_MAX_TRAPS * JET_STUB_STRIDE) + psz - 1) & ~(psz - 1);
    void *page = mmap(NULL, need, PROT_READ|PROT_WRITE,
                      MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
    if (page == MAP_FAILED) { free(ctx); return NULL; }
    ctx->stub_page = page;
    ctx->stub_page_size = need;
    ctx->stub_base = H2G(page);
    /* fill with recognizable marker (BRK #0xJET) just in case something fetches */
    for (size_t i = 0; i < need / 4; i++)
        ((uint32_t *)page)[i] = 0xD4200000u; /* BRK #0 */

    /* trap 0 = return-to-host sentinel */
    ctx->traps[0].name = strdup("<return>");
    ctx->traps[0].fn = NULL;
    ctx->ntraps = 1;

    /* block cache */
    ctx->block_mask = JET_BLOCK_CACHE - 1;
    ctx->blocks = calloc(JET_BLOCK_CACHE, sizeof(jet_block));
    if (!ctx->blocks) { munmap(page, need); free(ctx); return NULL; }

    ctx->main_cpu = jet_cpu_create(ctx);
    if (!ctx->main_cpu) {
        free(ctx->blocks); munmap(page, need); free(ctx); return NULL;
    }
    pthread_setspecific(ctx->cpu_key, ctx->main_cpu);

    jet_host_install_bionic(ctx);
    jet_host_install_setjmp(ctx);
    jet_jni_build_tables(ctx);
    jet_tramp_set_active(ctx);

    jet_log(ctx, 2, "jet_ctx created; stub_base=%p hwcap=0x%llx",
            (void *)(uintptr_t)ctx->stub_base,
            (unsigned long long)ctx->cfg.hwcap);
    return ctx;
}

void jet_native_destroy(jet_ctx *ctx) {
    if (!ctx) return;
    for (jet_lib *L = ctx->libs; L; ) {
        jet_lib *n = L->next;
        /* skip fini on destroy to avoid guest code during teardown */
        if (L->base) munmap(L->base, L->map_size);
        free(L->path); free(L);
        L = n;
    }
    for (int i = 0; i < ctx->ntraps; i++) free(ctx->traps[i].name);
    if (ctx->main_cpu) jet_cpu_destroy(ctx, ctx->main_cpu);
    free(ctx->blocks);
    free(ctx->guest_jnienv); /* guest_javavm lives in the same allocation */
    if (ctx->stub_page) munmap(ctx->stub_page, ctx->stub_page_size);
    pthread_key_delete(ctx->cpu_key);
    pthread_mutex_destroy(&ctx->excl_mu);
    pthread_mutex_destroy(&ctx->trap_mu);
    free(ctx);
}

static int trap_add(jet_ctx *ctx, const char *sym, jet_host_fn fn, void *user,
                    unsigned flags, int is_data) {
    pthread_mutex_lock(&ctx->trap_mu);
    /* replace if exists */
    for (int i = 1; i < ctx->ntraps; i++) {
        if (ctx->traps[i].name && strcmp(ctx->traps[i].name, sym) == 0) {
            ctx->traps[i].fn = fn;
            ctx->traps[i].user = user;
            ctx->traps[i].flags = flags;
            ctx->traps[i].is_data = is_data;
            pthread_mutex_unlock(&ctx->trap_mu);
            return 0;
        }
    }
    if (ctx->ntraps >= JET_MAX_TRAPS) {
        pthread_mutex_unlock(&ctx->trap_mu);
        jet_set_error(ctx, "trap table full");
        return -1;
    }
    int i = ctx->ntraps++;
    ctx->traps[i].name = strdup(sym);
    ctx->traps[i].fn = fn;
    ctx->traps[i].user = user;
    ctx->traps[i].flags = flags;
    ctx->traps[i].is_data = is_data;
    pthread_mutex_unlock(&ctx->trap_mu);
    return 0;
}

int jet_native_register_host(jet_ctx *ctx, const char *sym, jet_host_fn fn, void *user) {
    return trap_add(ctx, sym, fn, user, 0, 0);
}
int jet_native_register_host_ex(jet_ctx *ctx, const char *sym, jet_host_fn fn,
                                void *user, unsigned flags) {
    return trap_add(ctx, sym, fn, user, flags, 0);
}
int jet_native_register_data(jet_ctx *ctx, const char *sym, void *addr) {
    return trap_add(ctx, sym, NULL, addr, 0, 1);
}

/* Lookup trap by name; returns index or -1. */
int jet_trap_find(jet_ctx *ctx, const char *name) {
    for (int i = 1; i < ctx->ntraps; i++)
        if (ctx->traps[i].name && strcmp(ctx->traps[i].name, name) == 0)
            return i;
    return -1;
}

jet_lib *jet_native_load(jet_ctx *ctx, const char *path, char *err, size_t errlen) {
    jet_lib *lib = jet_elf_load(ctx, path, err, errlen);
    if (!lib) return NULL;
    lib->next = ctx->libs;
    ctx->libs = lib;
    /* Rebuild main CPU TLS after load so ELF module images land after the
     * bionic TCB (and so a late PT_TLS still gets a canary-equipped TPIDR). */
    if (ctx->main_cpu) {
        jet_cpu_setup_tls(ctx, ctx->main_cpu);
    }
    if (jet_elf_run_init(ctx, lib) != 0) {
        if (err && errlen) snprintf(err, errlen, "%s", ctx->last_error);
        return lib; /* still return; caller can inspect error */
    }
    return lib;
}

void jet_native_unload(jet_lib *lib) {
    if (!lib) return;
    jet_ctx *ctx = lib->ctx;
    if (lib->initialized) jet_elf_run_fini(ctx, lib);
    /* unlink */
    jet_lib **pp = &ctx->libs;
    while (*pp && *pp != lib) pp = &(*pp)->next;
    if (*pp == lib) *pp = lib->next;
    if (lib->base) munmap(lib->base, lib->map_size);
    free(lib->phdrs); free(lib->path); free(lib);
}

uint64_t jet_native_sym(jet_lib *lib, const char *name) {
    return jet_elf_sym(lib, name);
}

static void setup_call(jet_ctx *ctx, jet_cpu *cpu, uint64_t guest_fn,
                       const jet_args *a, uint32_t nstack) {
    memset(cpu->x, 0, sizeof cpu->x);
    memset(cpu->v, 0, sizeof cpu->v);
    cpu->nzcv = 0;
    cpu->stop = 0;
    cpu->jumping = 0;
    cpu->err_code = 0;
    cpu->err_msg[0] = 0;
    if (a) {
        for (int i = 0; i < 8; i++) cpu->x[i] = a->x[i];
        cpu->x[8] = a->x8; /* AAPCS64 indirect result */
        for (int i = 0; i < 8; i++) {
            uint64_t bits; memcpy(&bits, &a->d[i], 8);
            cpu->v[i][0] = bits; cpu->v[i][1] = 0;
        }
        if (a->stack && nstack) {
            /* push stack args: AAPCS64 stack args at SP+0, SP+8, ... */
            cpu->sp -= (uint64_t)nstack * 8;
            cpu->sp &= ~0xfull; /* 16-byte align */
            for (uint32_t i = 0; i < nstack; i++)
                st8(cpu->sp + 8ull * i, a->stack[i]);
        }
    }
    cpu->sp &= ~0xfull;
    cpu->x[30] = jet_stub_addr(ctx, JET_STUB_RETURN); /* LR = return sentinel */
    cpu->pc = guest_fn;
}

int jet_native_call_ex(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a,
                       uint32_t nstack, uint64_t *ret_x0, double *ret_d0) {
    if (!ctx || !guest_fn) return -1;
    jet_cpu *cpu = jet_cpu_current(ctx);
    if (!cpu) return -1;
    ctx->last_error[0] = 0;
    /* Full register snapshot so host→guest trampolines can nest without
     * destroying the outer guest frame (callee-saved x19-x28/d8-d15, x8 IPRIN,
     * guest x18, NZCV, PC/LR). */
    uint64_t saved_x[31];
    uint64_t saved_v[32][2];
    uint64_t saved_sp = cpu->sp;
    uint64_t saved_pc = cpu->pc;
    uint32_t saved_nzcv = cpu->nzcv;
    uint64_t saved_tpidr = cpu->tpidr_el0;
    int saved_stop = cpu->stop, saved_jumping = cpu->jumping;
    int saved_err = cpu->err_code;
    char saved_errmsg[sizeof cpu->err_msg];
    memcpy(saved_x, cpu->x, sizeof saved_x);
    memcpy(saved_v, cpu->v, sizeof saved_v);
    memcpy(saved_errmsg, cpu->err_msg, sizeof saved_errmsg);

    setup_call(ctx, cpu, guest_fn, a, nstack);
    cpu->tpidr_el0 = saved_tpidr; /* keep TLS */
    cpu->sp = saved_sp;
    if (a && a->stack && nstack) {
        cpu->sp -= (uint64_t)nstack * 8;
        cpu->sp &= ~0xfull;
        for (uint32_t i = 0; i < nstack; i++)
            st8(cpu->sp + 8ull * i, a->stack[i]);
    }
    cpu->sp &= ~0xfull;
    /* Re-apply x8 after setup_call (already set inside), keep SP 16-aligned. */
    if (a) cpu->x[8] = a->x8;
    cpu->x[30] = jet_stub_addr(ctx, JET_STUB_RETURN);
    cpu->pc = guest_fn;

    int rc = jet_interp_run(ctx, cpu);
    if (ret_x0) *ret_x0 = cpu->x[0];
    if (ret_d0) memcpy(ret_d0, &cpu->v[0][0], 8);

    /* Restore outer guest frame. */
    memcpy(cpu->x, saved_x, sizeof saved_x);
    memcpy(cpu->v, saved_v, sizeof saved_v);
    cpu->sp = saved_sp;
    cpu->pc = saved_pc;
    cpu->nzcv = saved_nzcv;
    cpu->tpidr_el0 = saved_tpidr;
    cpu->stop = saved_stop;
    cpu->jumping = saved_jumping;
    cpu->err_code = saved_err;
    memcpy(cpu->err_msg, saved_errmsg, sizeof saved_errmsg);
    return rc;
}

int jet_native_call(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a,
                    uint64_t *ret_x0, double *ret_d0) {
    return jet_native_call_ex(ctx, guest_fn, a, 0, ret_x0, ret_d0);
}

uint64_t jet_tramp_invoke(jet_ctx *ctx, uint64_t guest_fn, const jet_args *a) {
    uint64_t r = 0;
    jet_native_call(ctx, guest_fn, a, &r, NULL);
    return r;
}


int jet_native_call_host(jet_ctx *ctx, const char *sym, const jet_args *a, uint64_t *ret) {
    if (!ctx || !sym) return -1;
    for (int i = 1; i < ctx->ntraps; i++) {
        if (ctx->traps[i].name && strcmp(ctx->traps[i].name, sym) == 0 && ctx->traps[i].fn) {
            uint64_t r = ctx->traps[i].fn(ctx, a, ctx->traps[i].user);
            if (ret) *ret = r;
            return 0;
        }
    }
    return -1;
}


void *jet_jni_new_global_ref(jet_ctx *ctx, void *local);
void jet_jni_delete_global_ref(jet_ctx *ctx, void *global);

int jet_native_set_class_loader(jet_ctx *ctx, void *class_loader) {
    if (!ctx) return -1;
    if (ctx->default_class_loader && ctx->default_loader_is_global)
        jet_jni_delete_global_ref(ctx, ctx->default_class_loader);
    if (class_loader) {
        /* Always NewGlobalRef for the stored default. When host JNI exists,
         * do NOT DeleteLocalRef — caller may still hold the local.
         * jet_jni_new_global_ref deletes locals (for getClassLoader temps);
         * here call NewGlobalRef directly. */
        void **envp = (void **)ctx->host_jnienv;
        void **f = (envp && *envp) ? (void **)*envp : NULL;
        if (f) {
            typedef void *(*fn_ngr)(void *, void *);
            void *g = ((fn_ngr)f[21])(ctx->host_jnienv, class_loader);
            ctx->default_class_loader = g ? g : class_loader;
            ctx->n_global_refs++;
        } else {
            /* Fake JNI / no host: identity promote + count */
            ctx->default_class_loader = jet_jni_new_global_ref(ctx, class_loader);
        }
        ctx->default_loader_is_global = 1;
    } else {
        ctx->default_class_loader = NULL;
        ctx->default_loader_is_global = 0;
    }
    return 0;
}
void *jet_native_get_class_loader(jet_ctx *ctx) {
    return ctx ? jet_loader_current(ctx) : NULL;
}
void jet_native_lib_set_class_loader(jet_lib *lib, void *class_loader) {
    /* Store as-is; caller should pass a GlobalRef or use set_class_loader. */
    if (lib) lib->class_loader = class_loader;
}

int jet_native_global_ref_count(jet_ctx *ctx) {
    return ctx ? ctx->n_global_refs : 0;
}

void jet_native_dump_stats(jet_ctx *ctx) {
    if (!ctx) return;
    fprintf(stderr, "[jet:stats] insn_total=%llu globals=%d\n",
            (unsigned long long)ctx->insn_count, ctx->n_global_refs);
    for (int i = 0; i < JET_TRAMP_POOL; i++) {
        if (!ctx->native_stats[i].used) continue;
        fprintf(stderr, "[jet:stats] %s calls=%llu insn=%llu wall_ns=%llu\n",
                ctx->native_stats[i].key,
                (unsigned long long)ctx->native_stats[i].calls,
                (unsigned long long)ctx->native_stats[i].insn,
                (unsigned long long)ctx->native_stats[i].wall_ns);
    }
    fflush(stderr);
}

void jet_loader_push(jet_ctx *ctx, void *loader) {
    if (!ctx) return;
    if (ctx->loader_sp < JET_LOADER_STACK)
        ctx->loader_stack[ctx->loader_sp++] = loader;
}
void jet_loader_pop(jet_ctx *ctx) {
    if (!ctx || ctx->loader_sp <= 0) return;
    ctx->loader_sp--;
}
void *jet_loader_current(jet_ctx *ctx) {
    if (!ctx) return NULL;
    if (ctx->loader_sp > 0) return ctx->loader_stack[ctx->loader_sp - 1];
    if (ctx->onload_lib && ctx->onload_lib->class_loader)
        return ctx->onload_lib->class_loader;
    return ctx->default_class_loader;
}
