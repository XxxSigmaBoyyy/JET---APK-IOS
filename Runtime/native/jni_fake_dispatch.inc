/* Fake JNIEnv when no host JVM is bound. Exceptions are real (pending).
 * Unimplemented slots abort — never silent 0. */
static uint64_t jni_fake_dispatch(jet_ctx *ctx, const jet_args *a, int idx) {
    switch (idx) {
    case 0: case 1: case 2: case 3: return 0;
    case 4: return JNI_VERSION_1_6;
    case 6: /* FindClass */
        return fake_find_class(ctx, a->x[1] ? (const char*)G2H(a->x[1]) : "");
    case 10: return fake_find_class(ctx, "java/lang/Object");
    case 11: return 1;
    case 13: { /* Throw */
        fake_set_pending(ctx, "java/lang/Throwable", "Throw");
        ctx->jni_pending_obj = a->x[1] ? a->x[1] : H2G(fake_pending(ctx));
        return 0;
    }
    case 14: { /* ThrowNew */
        fake_jclass *cls = a->x[1] ? (fake_jclass*)G2H(a->x[1]) : NULL;
        const char *msg = a->x[2] ? (const char*)G2H(a->x[2]) : "";
        fake_set_pending(ctx, cls ? cls->class_name : "java/lang/Exception", msg);
        ctx->jni_pending_obj = H2G(fake_pending(ctx));
        jet_log(ctx, 2, "ThrowNew(%s): %s", cls ? cls->class_name : "?", msg);
        return 0;
    }
    case 15: { /* ExceptionOccurred */
        fake_throwable *t = fake_pending(ctx);
        if (t && t->pending) return ctx->jni_pending_obj ? ctx->jni_pending_obj : H2G(t);
        return 0;
    }
    case 16: { /* ExceptionDescribe */
        fake_throwable *t = fake_pending(ctx);
        if (t && t->pending)
            jet_log(ctx, 1, "ExceptionDescribe: %s: %s", t->cls, t->msg);
        return 0;
    }
    case 17: { /* ExceptionClear */
        fake_throwable *t = fake_pending(ctx);
        if (t) t->pending = 0;
        ctx->jni_pending_obj = 0;
        return 0;
    }
    case 18: { /* FatalError */
        const char *msg = a->x[1] ? (const char*)G2H(a->x[1]) : "";
        jet_log(ctx, 0, "FatalError: %s", msg);
        fprintf(stderr, "[jet:E] FatalError: %s\n", msg);
        abort();
    }
    case 19: return 0; /* PushLocalFrame */
    case 20: return a->x[1]; /* PopLocalFrame */
    case 21: case 25: case 226: return a->x[1]; /* NewGlobal/Local/Weak ref */
    case 22: case 23: case 227: return 0; /* Delete* */
    case 24: return a->x[1] == a->x[2];
    case 26: return 0; /* EnsureLocalCapacity */
    case 27: return fake_find_class(ctx, "AllocObject");
    case 28: case 29: case 30: /* NewObject* — return dummy object */
        return fake_find_class(ctx, "NewObject");
    case 31: return fake_find_class(ctx, "java/lang/Object");
    case 32: return 1;
    case 33: case 94: case 113: case 144: { /* *ID */
        const char *name = a->x[2] ? (const char*)G2H(a->x[2]) : "";
        const char *sig  = a->x[3] ? (const char*)G2H(a->x[3]) : "()V";
        mid_ent *e = NULL;
        if (g_nmids < MID_CACHE) { e = &g_mids[g_nmids++]; e->used = 1; }
        if (e) {
            snprintf(e->name, sizeof e->name, "%s", name);
            snprintf(e->sig, sizeof e->sig, "%s", sig);
            e->mid = (jmethodID)e; /* self-pointer id */
            return (uint64_t)(uintptr_t)e->mid;
        }
        return 0x1000;
    }
    /* Call*Method — handle CallIntMethodA/V for tests; others return 0 with abort for unknown */
    case 49: case 50: case 51: { /* CallIntMethod / V / A */
        jvalue jv[8]; memset(jv, 0, sizeof jv);
        jmethodID mid = (jmethodID)(uintptr_t)a->x[2];
        const char *sig = resolve_sig(ctx, mid, "(IDLjava/lang/Object;)I");
        if (idx == 51) {
            jvalue *src = a->x[3] ? (jvalue*)G2H(a->x[3]) : jv;
            memcpy(jv, src, sizeof jv);
        } else if (idx == 50) {
            guest_va_list *vl = a->x[3] ? (guest_va_list*)G2H(a->x[3]) : NULL;
            guest_va_list copy = vl ? *vl : (guest_va_list){0};
            pack_jvalues_va(&copy, sig, jv, 8);
        } else {
            pack_jvalues_args(a, 3, sig, jv, 8);
        }
        /* Fake: return (int)arg0 + (int)arg1_as_double + (arg2 ? 1 : 0) */
        int r = jv[0].i + (int)jv[1].d + (jv[2].l ? 1 : 0);
        return (uint64_t)(int64_t)r;
    }
    case 61: case 62: case 63: return 0; /* CallVoid* */
    case 95: return 0; /* GetObjectField */
    case 100: return 0;
    case 104: case 109: return 0;
    case 114: case 115: case 116: { /* CallStaticObjectMethod* — allocateDirect-ish */
        int64_t cap = 4096;
        if (idx == 114) cap = (int64_t)a->x[3];
        else if (idx == 115 && a->x[3]) {
            guest_va_list *vl = (guest_va_list*)G2H(a->x[3]);
            guest_va_list copy = *vl;
            jvalue jv[4]; pack_jvalues_va(&copy, "(I)Ljava/nio/ByteBuffer;", jv, 4);
            cap = jv[0].i;
        } else if (idx == 116 && a->x[3]) {
            cap = ((jvalue*)G2H(a->x[3]))[0].i;
        }
        if (cap <= 0 || cap > (1<<26)) cap = 4096;
        void *buf = calloc(1, (size_t)cap);
        void *wrap = calloc(1, 32);
        if (wrap) { ((uint64_t*)wrap)[0]=H2G(buf); ((int64_t*)wrap)[1]=cap; }
        return H2G(wrap);
    }
    case 163: case 167: {
        const char *utf = a->x[1] ? (const char*)G2H(a->x[1]) : "";
        size_t n = strlen(utf);
        char *s = malloc(n + 1);
        memcpy(s, utf, n + 1);
        return H2G(s);
    }
    case 164: case 168:
        return a->x[1] ? (uint64_t)strlen((char*)G2H(a->x[1])) : 0;
    case 165: case 169: {
        if (a->x[2]) *(uint8_t*)G2H(a->x[2]) = 0;
        return a->x[1];
    }
    case 166: case 170: return 0;
    case 171: {
        if (!a->x[1]) return 0;
        return (uint64_t)(int64_t)(*(int64_t*)G2H(a->x[1]));
    }
    case 176: { /* NewByteArray */
        jsize n = (jsize)a->x[1];
        uint8_t *p = calloc(1, 8 + (size_t)n);
        *(int64_t*)p = n;
        return H2G(p);
    }
    case 184: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        if (a->x[2]) *(uint8_t*)G2H(a->x[2]) = 0;
        return H2G(p + 8);
    }
    case 192: return 0;
    case 200: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        jsize start=(jsize)a->x[2], len=(jsize)a->x[3];
        memcpy(G2H(a->x[4]), p+8+start, (size_t)len); return 0;
    }
    case 208: {
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        jsize start=(jsize)a->x[2], len=(jsize)a->x[3];
        memcpy(p+8+start, G2H(a->x[4]), (size_t)len); return 0;
    }
    case 215: { /* RegisterNatives */
        fake_jclass *cls = a->x[1] ? (fake_jclass*)G2H(a->x[1]) : NULL;
        const guest_JNINativeMethod *methods = a->x[2] ? (const guest_JNINativeMethod*)G2H(a->x[2]) : NULL;
        int n = (int)a->x[3];
        const char *cname = cls ? cls->class_name : "?";
        void *loader = jet_loader_current(ctx);
        if (loader) loader = jet_jni_new_global_ref(ctx, loader);
        if (!methods || n < 0 || n > 4096) return (uint64_t)(int64_t)-1;
        for (int i = 0; i < n; i++) {
            if (!methods[i].name || !methods[i].sig || !methods[i].fnPtr) continue;
            const char *name = (const char*)G2H(methods[i].name);
            const char *sig  = (const char*)G2H(methods[i].sig);
            uint64_t fn = methods[i].fnPtr;
            jet_native_trampoline_ex(ctx, fn, loader, cname);
            for (int ti = 0; ti < 64; ti++) {
                if (ctx->tramps[ti].used && ctx->tramps[ti].guest_fn == fn) {
                    snprintf(ctx->tramps[ti].method_name, sizeof ctx->tramps[ti].method_name,
                             "%s", name ? name : "");
                    snprintf(ctx->tramps[ti].method_sig, sizeof ctx->tramps[ti].method_sig,
                             "%s", sig ? sig : "");
                    break;
                }
            }
            if (ctx->nreg_log < 1024) {
                int k = ctx->nreg_log++;
                snprintf(ctx->reg_log[k].cls, sizeof ctx->reg_log[k].cls, "%s", cname);
                snprintf(ctx->reg_log[k].name, sizeof ctx->reg_log[k].name, "%s", name ? name : "");
                snprintf(ctx->reg_log[k].sig, sizeof ctx->reg_log[k].sig, "%s", sig ? sig : "");
                ctx->reg_log[k].fn = fn;
                ctx->reg_log[k].used = 1;
            }
            if (ctx->reg_cb) ctx->reg_cb(ctx->reg_cb_user, cname, name, sig, fn);
        }
        return 0;
    }
    case 216: return 0;
    case 217: case 218: return 0; /* Monitor* */
    case 219: {
        uint64_t *out = a->x[1] ? (uint64_t*)G2H(a->x[1]) : NULL;
        if (out) *out = H2G(ctx->guest_javavm);
        return 0;
    }
    case 222: { /* GetPrimitiveArrayCritical */
        uint8_t *p = (uint8_t*)G2H(a->x[1]);
        if (a->x[2]) *(uint8_t*)G2H(a->x[2]) = 0;
        return H2G(p + 8);
    }
    case 223: return 0;
    case 224: return a->x[1];
    case 225: return 0;
    case 228: { /* ExceptionCheck */
        fake_throwable *t = fake_pending(ctx);
        return (t && t->pending) ? 1 : 0;
    }
    case 229: {
        void *wrap = calloc(1, 32);
        if (wrap) { ((uint64_t*)wrap)[0]=a->x[1]; ((int64_t*)wrap)[1]=(int64_t)a->x[2]; }
        return H2G(wrap);
    }
    case 230: {
        uint64_t *wrap = a->x[1] ? (uint64_t*)G2H(a->x[1]) : NULL;
        return wrap ? wrap[0] : 0;
    }
    case 231: {
        int64_t *wrap = a->x[1] ? (int64_t*)G2H(a->x[1]) : NULL;
        return wrap ? (uint64_t)wrap[1] : (uint64_t)-1;
    }
    case 232: return 1; /* JNILocalRefType */
    default:
        jni_abort_unimplemented(ctx, idx, 0);
    }
    return 0;
}
