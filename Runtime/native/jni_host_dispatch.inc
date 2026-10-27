static uint64_t jni_host_dispatch(jet_ctx *ctx, const jet_args *a, int idx) {
    void **f = host_fns(ctx); void *e = host_env(ctx);
    if (!f) jni_abort_unimplemented(ctx, idx, 0);
    switch (idx) {
    case 0: case 1: case 2: case 3: return 0;
    case 4: return (uint64_t)(int64_t)((fn_i0)f[4])(e);
    case 5: {
        typedef jclass (*fn)(void*, const char*, jobject, const jbyte*, jsize);
        return (uint64_t)(uintptr_t)((fn)f[5])(e,
            a->x[1]?(const char*)G2H(a->x[1]):NULL, (jobject)(uintptr_t)a->x[2],
            a->x[3]?(const jbyte*)G2H(a->x[3]):NULL, (jsize)a->x[4]);
    }
    case 6: { /* FindClass — ClassLoader of current native / OnLoad */
        const char *name = a->x[1] ? (const char*)G2H(a->x[1]) : NULL;
        return host_find_class_loader(ctx, name);
    }
    case 7: case 8:
        return (uint64_t)(uintptr_t)((fn_l1p)f[idx])(e, (jobject)(uintptr_t)a->x[1]);
    case 9: {
        typedef jobject (*fn)(void*, jclass, jmethodID, jboolean);
        return (uint64_t)(uintptr_t)((fn)f[9])(e, (jclass)(uintptr_t)a->x[1],
            (jmethodID)(uintptr_t)a->x[2], (jboolean)a->x[3]);
    }
    case 10: return (uint64_t)(uintptr_t)((fn_l1p)f[10])(e, (jclass)(uintptr_t)a->x[1]);
    case 11: return (uint64_t)((fn_z2p)f[11])(e, (jclass)(uintptr_t)a->x[1], (jclass)(uintptr_t)a->x[2]);
    case 12: {
        typedef jobject (*fn)(void*, jclass, jfieldID, jboolean);
        return (uint64_t)(uintptr_t)((fn)f[12])(e, (jclass)(uintptr_t)a->x[1],
            (jfieldID)(uintptr_t)a->x[2], (jboolean)a->x[3]);
    }
    case 13: return (uint64_t)(int64_t)((fn_i1p)f[13])(e, (jthrowable)(uintptr_t)a->x[1]);
    case 14: {
        typedef jint (*fn)(void*, jclass, const char*);
        return (uint64_t)(int64_t)((fn)f[14])(e, (jclass)(uintptr_t)a->x[1],
            a->x[2]?(const char*)G2H(a->x[2]):NULL);
    }
    case 15: return (uint64_t)(uintptr_t)((fn_l0)f[15])(e);
    case 16: ((fn_v0)f[16])(e); return 0;
    case 17: ((fn_v0)f[17])(e); return 0;
    case 18: {
        typedef void (*fn)(void*, const char*);
        ((fn)f[18])(e, a->x[1]?(const char*)G2H(a->x[1]):NULL); return 0;
    }
    case 19: { typedef jint (*fn)(void*, jint); return (uint64_t)(int64_t)((fn)f[19])(e, (jint)a->x[1]); }
    case 20: return (uint64_t)(uintptr_t)((fn_l1p)f[20])(e, (jobject)(uintptr_t)a->x[1]);
    case 21: return (uint64_t)(uintptr_t)((fn_l1p)f[21])(e, (jobject)(uintptr_t)a->x[1]);
    case 22: ((fn_v1p)f[22])(e, (jobject)(uintptr_t)a->x[1]); return 0;
    case 23: ((fn_v1p)f[23])(e, (jobject)(uintptr_t)a->x[1]); return 0;
    case 24: return (uint64_t)((fn_z2p)f[24])(e, (jobject)(uintptr_t)a->x[1], (jobject)(uintptr_t)a->x[2]);
    case 25: return (uint64_t)(uintptr_t)((fn_l1p)f[25])(e, (jobject)(uintptr_t)a->x[1]);
    case 26: { typedef jint (*fn)(void*, jint); return (uint64_t)(int64_t)((fn)f[26])(e, (jint)a->x[1]); }
    case 27: return (uint64_t)(uintptr_t)((fn_l1p)f[27])(e, (jclass)(uintptr_t)a->x[1]);
    case 28: case 29: case 30: return host_call_family(ctx, a, idx, 0, 0, 1);
    case 31: return (uint64_t)(uintptr_t)((fn_l1p)f[31])(e, (jobject)(uintptr_t)a->x[1]);
    case 32: return (uint64_t)((fn_z2p)f[32])(e, (jobject)(uintptr_t)a->x[1], (jclass)(uintptr_t)a->x[2]);
    case 33: {
        typedef jmethodID (*fn)(void*, jclass, const char*, const char*);
        const char *name = a->x[2]?(const char*)G2H(a->x[2]):NULL;
        const char *sig  = a->x[3]?(const char*)G2H(a->x[3]):NULL;
        jmethodID mid = ((fn)f[33])(e, (jclass)(uintptr_t)a->x[1], name, sig);
        mid_cache_put(mid, name, sig);
        return (uint64_t)(uintptr_t)mid;
    }

    case 34: case 35: case 36: case 37: case 38: case 39:
    case 40: case 41: case 42: case 43: case 44: case 45:
    case 46: case 47: case 48: case 49: case 50: case 51:
    case 52: case 53: case 54: case 55: case 56: case 57:
    case 58: case 59: case 60: case 61: case 62: case 63:
        return host_call_family(ctx, a, idx, 0, 0, 0);
    case 64: case 65: case 66: case 67: case 68: case 69:
    case 70: case 71: case 72: case 73: case 74: case 75:
    case 76: case 77: case 78: case 79: case 80: case 81:
    case 82: case 83: case 84: case 85: case 86: case 87:
    case 88: case 89: case 90: case 91: case 92: case 93:
        return host_call_family(ctx, a, idx, 0, 1, 0);
    case 94: {
        typedef jfieldID (*fn)(void*, jclass, const char*, const char*);
        return (uint64_t)(uintptr_t)((fn)f[94])(e, (jclass)(uintptr_t)a->x[1],
            a->x[2]?(const char*)G2H(a->x[2]):NULL, a->x[3]?(const char*)G2H(a->x[3]):NULL);
    }
    case 95: return (uint64_t)(uintptr_t)((fn_l2p)f[95])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]);
    case 96: { typedef jint (*fn)(void*,jobject,jfieldID); return (uint64_t)(int64_t)((fn)f[96])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 97: { typedef jint (*fn)(void*,jobject,jfieldID); return (uint64_t)(int64_t)((fn)f[97])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 98: { typedef jint (*fn)(void*,jobject,jfieldID); return (uint64_t)(int64_t)((fn)f[98])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 99: { typedef jint (*fn)(void*,jobject,jfieldID); return (uint64_t)(int64_t)((fn)f[99])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 100: { typedef jint (*fn)(void*,jobject,jfieldID); return (uint64_t)(int64_t)((fn)f[100])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 101: return (uint64_t)((fn_j2p)f[101])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]);
    case 102: { jfloat r=((fn_fl2p)f[102])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); uint32_t u; memcpy(&u,&r,4); return u; }
    case 103: { jdouble r=((fn_d2p)f[103])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); uint64_t u; memcpy(&u,&r,8); return u; }
    case 104: {
        typedef void (*fn)(void*, jobject, jfieldID, jobject);
        ((fn)f[104])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jobject)(uintptr_t)a->x[3]); return 0; }
    case 105: {
        typedef void (*fn)(void*, jobject, jfieldID, jint);
        ((fn)f[105])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 106: {
        typedef void (*fn)(void*, jobject, jfieldID, jint);
        ((fn)f[106])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 107: {
        typedef void (*fn)(void*, jobject, jfieldID, jint);
        ((fn)f[107])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 108: {
        typedef void (*fn)(void*, jobject, jfieldID, jint);
        ((fn)f[108])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 109: {
        typedef void (*fn)(void*, jobject, jfieldID, jint);
        ((fn)f[109])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 110: {
        typedef void (*fn)(void*, jobject, jfieldID, jlong);
        ((fn)f[110])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jlong)a->x[3]); return 0; }
    case 111: {
        typedef void (*fn)(void*, jobject, jfieldID, jfloat);
        jfloat fv; uint32_t u=(uint32_t)a->x[3]; memcpy(&fv,&u,4);
        ((fn)f[111])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],fv); return 0; }
    case 112: {
        typedef void (*fn)(void*, jobject, jfieldID, jdouble);
        jdouble dv; memcpy(&dv,&a->d[0],8); /* value may be in d0 or x3 */
        if (a->x[3]) memcpy(&dv,&a->x[3],8);
        ((fn)f[112])(e,(jobject)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],dv); return 0; }

    case 113: { /* GetStaticMethodID */
        typedef jmethodID (*fn)(void*, jclass, const char*, const char*);
        const char *name = a->x[2]?(const char*)G2H(a->x[2]):NULL;
        const char *sig  = a->x[3]?(const char*)G2H(a->x[3]):NULL;
        jmethodID mid = ((fn)f[113])(e, (jclass)(uintptr_t)a->x[1], name, sig);
        mid_cache_put(mid, name, sig);
        return (uint64_t)(uintptr_t)mid;
    }
    case 114: case 115: case 116: case 117: case 118: case 119:
    case 120: case 121: case 122: case 123: case 124: case 125:
    case 126: case 127: case 128: case 129: case 130: case 131:
    case 132: case 133: case 134: case 135: case 136: case 137:
    case 138: case 139: case 140: case 141: case 142: case 143:
        return host_call_family(ctx, a, idx, 1, 0, 0);
    case 144: {
        typedef jfieldID (*fn)(void*, jclass, const char*, const char*);
        return (uint64_t)(uintptr_t)((fn)f[144])(e, (jclass)(uintptr_t)a->x[1],
            a->x[2]?(const char*)G2H(a->x[2]):NULL, a->x[3]?(const char*)G2H(a->x[3]):NULL);
    }
    case 145: return (uint64_t)(uintptr_t)((fn_l2p)f[145])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]);
    case 146: { typedef jint (*fn)(void*,jclass,jfieldID); return (uint64_t)(int64_t)((fn)f[146])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 147: { typedef jint (*fn)(void*,jclass,jfieldID); return (uint64_t)(int64_t)((fn)f[147])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 148: { typedef jint (*fn)(void*,jclass,jfieldID); return (uint64_t)(int64_t)((fn)f[148])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 149: { typedef jint (*fn)(void*,jclass,jfieldID); return (uint64_t)(int64_t)((fn)f[149])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 150: { typedef jint (*fn)(void*,jclass,jfieldID); return (uint64_t)(int64_t)((fn)f[150])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); }
    case 151: return (uint64_t)((fn_j2p)f[151])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]);
    case 152: { jfloat r=((fn_fl2p)f[152])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); uint32_t u; memcpy(&u,&r,4); return u; }
    case 153: { jdouble r=((fn_d2p)f[153])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2]); uint64_t u; memcpy(&u,&r,8); return u; }
    case 154: { typedef void (*fn)(void*,jclass,jfieldID,jobject); ((fn)f[154])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jobject)(uintptr_t)a->x[3]); return 0; }
    case 155: { typedef void (*fn)(void*,jclass,jfieldID,jint); ((fn)f[155])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 156: { typedef void (*fn)(void*,jclass,jfieldID,jint); ((fn)f[156])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 157: { typedef void (*fn)(void*,jclass,jfieldID,jint); ((fn)f[157])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 158: { typedef void (*fn)(void*,jclass,jfieldID,jint); ((fn)f[158])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 159: { typedef void (*fn)(void*,jclass,jfieldID,jint); ((fn)f[159])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jint)a->x[3]); return 0; }
    case 160: { typedef void (*fn)(void*,jclass,jfieldID,jlong); ((fn)f[160])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],(jlong)a->x[3]); return 0; }
    case 161: { typedef void (*fn)(void*,jclass,jfieldID,jfloat); jfloat fv; uint32_t u=(uint32_t)a->x[3]; memcpy(&fv,&u,4); ((fn)f[161])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],fv); return 0; }
    case 162: { typedef void (*fn)(void*,jclass,jfieldID,jdouble); jdouble dv; memcpy(&dv,&a->x[3],8); ((fn)f[162])(e,(jclass)(uintptr_t)a->x[1],(jfieldID)(uintptr_t)a->x[2],dv); return 0; }

    case 163: { /* NewString */
        typedef jstring (*fn)(void*, const jchar*, jsize);
        return (uint64_t)(uintptr_t)((fn)f[163])(e, a->x[1]?(const jchar*)G2H(a->x[1]):NULL, (jsize)a->x[2]);
    }
    case 164: { typedef jsize (*fn)(void*, jstring); return (uint64_t)(int64_t)((fn)f[164])(e,(jstring)(uintptr_t)a->x[1]); }
    case 165: {
        typedef const jchar* (*fn)(void*, jstring, jboolean*);
        jboolean *isCopy = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[165])(e,(jstring)(uintptr_t)a->x[1],isCopy);
    }
    case 166: {
        typedef void (*fn)(void*, jstring, const jchar*);
        ((fn)f[166])(e,(jstring)(uintptr_t)a->x[1], a->x[2]?(const jchar*)G2H(a->x[2]):NULL); return 0;
    }
    case 167: {
        typedef jstring (*fn)(void*, const char*);
        return (uint64_t)(uintptr_t)((fn)f[167])(e, a->x[1]?(const char*)G2H(a->x[1]):NULL);
    }
    case 168: { typedef jsize (*fn)(void*, jstring); return (uint64_t)(int64_t)((fn)f[168])(e,(jstring)(uintptr_t)a->x[1]); }
    case 169: {
        typedef const char* (*fn)(void*, jstring, jboolean*);
        jboolean *isCopy = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[169])(e,(jstring)(uintptr_t)a->x[1],isCopy);
    }
    case 170: {
        typedef void (*fn)(void*, jstring, const char*);
        ((fn)f[170])(e,(jstring)(uintptr_t)a->x[1], a->x[2]?(const char*)G2H(a->x[2]):NULL); return 0;
    }
    case 171: { typedef jsize (*fn)(void*, jarray); return (uint64_t)(int64_t)((fn)f[171])(e,(jarray)(uintptr_t)a->x[1]); }
    case 172: {
        typedef jobjectArray (*fn)(void*, jsize, jclass, jobject);
        return (uint64_t)(uintptr_t)((fn)f[172])(e,(jsize)a->x[1],(jclass)(uintptr_t)a->x[2],(jobject)(uintptr_t)a->x[3]);
    }
    case 173: {
        typedef jobject (*fn)(void*, jobjectArray, jsize);
        return (uint64_t)(uintptr_t)((fn)f[173])(e,(jobjectArray)(uintptr_t)a->x[1],(jsize)a->x[2]);
    }
    case 174: {
        typedef void (*fn)(void*, jobjectArray, jsize, jobject);
        ((fn)f[174])(e,(jobjectArray)(uintptr_t)a->x[1],(jsize)a->x[2],(jobject)(uintptr_t)a->x[3]); return 0;
    }
    case 175: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[175])(e,(jsize)a->x[1]); }
    case 176: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[176])(e,(jsize)a->x[1]); }
    case 177: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[177])(e,(jsize)a->x[1]); }
    case 178: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[178])(e,(jsize)a->x[1]); }
    case 179: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[179])(e,(jsize)a->x[1]); }
    case 180: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[180])(e,(jsize)a->x[1]); }
    case 181: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[181])(e,(jsize)a->x[1]); }
    case 182: { typedef jarray (*fn)(void*, jsize); return (uint64_t)(uintptr_t)((fn)f[182])(e,(jsize)a->x[1]); }
    case 183: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[183])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 184: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[184])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 185: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[185])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 186: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[186])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 187: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[187])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 188: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[188])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 189: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[189])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 190: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        jboolean *c = a->x[2]?(jboolean*)G2H(a->x[2]):NULL;
        return (uint64_t)(uintptr_t)((fn)f[190])(e,(jarray)(uintptr_t)a->x[1],c); }
    case 191: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[191])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 192: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[192])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 193: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[193])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 194: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[194])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 195: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[195])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 196: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[196])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 197: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[197])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 198: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[198])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0; }
    case 199: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[199])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 200: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[200])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 201: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[201])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 202: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[202])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 203: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[203])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 204: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[204])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 205: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[205])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 206: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[206])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 207: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[207])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 208: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[208])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 209: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[209])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 210: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[210])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 211: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[211])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 212: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[212])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 213: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[213])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }
    case 214: {
        typedef void (*fn)(void*, jarray, jsize, jsize, void*);
        ((fn)f[214])(e,(jarray)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL); return 0; }

    case 215: /* RegisterNatives — ALWAYS intercept; never forward arm64 ptrs */
        return host_register_natives(ctx, a);
    case 216: {
        typedef jint (*fn)(void*, jclass);
        return (uint64_t)(int64_t)((fn)f[216])(e, (jclass)(uintptr_t)a->x[1]);
    }
    case 217: case 218: {
        typedef jint (*fn)(void*, jobject);
        return (uint64_t)(int64_t)((fn)f[idx])(e, (jobject)(uintptr_t)a->x[1]);
    }
    case 219: { /* GetJavaVM */
        typedef jint (*fn)(void*, void**);
        void *hvm = NULL;
        jint rc = ((fn)f[219])(e, &hvm);
        if (rc == 0) {
            ctx->host_javavm = hvm;
            uint64_t *out = a->x[1]?(uint64_t*)G2H(a->x[1]):NULL;
            if (out) *out = H2G(ctx->guest_javavm);
        }
        return (uint64_t)(int64_t)rc;
    }
    case 220: case 221: { /* GetStringRegion / GetStringUTFRegion */
        typedef void (*fn)(void*, jstring, jsize, jsize, void*);
        ((fn)f[idx])(e,(jstring)(uintptr_t)a->x[1],(jsize)a->x[2],(jsize)a->x[3], a->x[4]?G2H(a->x[4]):NULL);
        return 0;
    }
    case 222: {
        typedef void* (*fn)(void*, jarray, jboolean*);
        return (uint64_t)(uintptr_t)((fn)f[222])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?(jboolean*)G2H(a->x[2]):NULL);
    }
    case 223: {
        typedef void (*fn)(void*, jarray, void*, jint);
        ((fn)f[223])(e,(jarray)(uintptr_t)a->x[1], a->x[2]?G2H(a->x[2]):NULL, (jint)a->x[3]); return 0;
    }
    case 224: {
        typedef const jchar* (*fn)(void*, jstring, jboolean*);
        return (uint64_t)(uintptr_t)((fn)f[224])(e,(jstring)(uintptr_t)a->x[1], a->x[2]?(jboolean*)G2H(a->x[2]):NULL);
    }
    case 225: {
        typedef void (*fn)(void*, jstring, const jchar*);
        ((fn)f[225])(e,(jstring)(uintptr_t)a->x[1], a->x[2]?(const jchar*)G2H(a->x[2]):NULL); return 0;
    }
    case 226: return (uint64_t)(uintptr_t)((fn_l1p)f[226])(e,(jobject)(uintptr_t)a->x[1]);
    case 227: ((fn_v1p)f[227])(e,(jobject)(uintptr_t)a->x[1]); return 0;
    case 228: { typedef jboolean (*fn)(void*); return (uint64_t)((fn)f[228])(e); }
    case 229: {
        typedef jobject (*fn)(void*, void*, jlong);
        return (uint64_t)(uintptr_t)((fn)f[229])(e, a->x[1]?G2H(a->x[1]):NULL, (jlong)a->x[2]);
    }
    case 230: {
        typedef void* (*fn)(void*, jobject);
        return (uint64_t)(uintptr_t)((fn)f[230])(e,(jobject)(uintptr_t)a->x[1]);
    }
    case 231: {
        typedef jlong (*fn)(void*, jobject);
        return (uint64_t)((fn)f[231])(e,(jobject)(uintptr_t)a->x[1]);
    }
    case 232: {
        typedef jobjectRefType (*fn)(void*, jobject);
        return (uint64_t)((fn)f[232])(e,(jobject)(uintptr_t)a->x[1]);
    }
    default:
        jni_abort_unimplemented(ctx, idx, 0);
    }
    return 0;
}
