package jet.aot;

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import java.io.*;
import java.util.*;

/**
 * JET AOT backend, stage 1: legalised Jimple (same Soot/dexpler bodies the JVM frontend uses) -> portable C
 * against aot/runtime/jetrt.h. clang then compiles it to x86_64-linux (linked, runs with no JVM) and to
 * arm64-apple-ios objects. Exceptions use a pending-exception word checked after each call/throw (no setjmp),
 * dispatch uses per-callsite inline caches over name/subsignature tables. External (android.*, java.*) members
 * are resolved at runtime against the native kernel (aot/runtime/kernel.c); misses abort with AOT-MISSING.
 * Usage: JimpleToC <app.jar> <android.jar> <out.c>
 */
public class JimpleToC {
    final Map<SootClass, Integer> cid = new LinkedHashMap<>();
    final Map<SootMethod, String> fname = new HashMap<>();
    final Set<String> app = new HashSet<>();
    final List<String> strs = new ArrayList<>();
    final Map<String, Integer> strIdx = new HashMap<>();
    int site = 0, unsupported = 0;
    final List<String> notes = new ArrayList<>();

    public static void main(String[] a) throws Exception {
        G.reset(); Options o = Options.v(); o.set_allow_phantom_refs(true); o.set_process_dir(List.of(a[0]));
        o.set_soot_classpath(a[0] + ":" + a[1]); o.set_prepend_classpath(false);
        o.set_output_format(Options.output_format_none); o.set_src_prec(Options.src_prec_class);
        Scene.v().loadNecessaryClasses();
        new JimpleToC().run(a[2]);
    }

    static String kind(Type t) {
        if (t instanceof LongType) return "j"; if (t instanceof FloatType) return "f"; if (t instanceof DoubleType) return "d";
        if (t instanceof RefLikeType) return "l"; return "i";
    }
    static String ctype(Type t) {
        switch (kind(t)) { case "j": return "int64_t"; case "f": return "float"; case "d": return "double"; case "l": return "void *"; default: return "int32_t"; }
    }
    static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (byte x : s.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            int c = x & 0xff;
            if (c >= 32 && c < 127 && c != '"' && c != '\\' && c != '?') b.append((char) c); else b.append(String.format("\\%03o", c));
        }
        return b.append('"').toString();
    }
    String cref(SootClass c) { return cid.containsKey(c) ? "(&C" + cid.get(c) + ")" : "rt_class(" + q(c.getName()) + ")"; }
    String cref(String name) { return app.contains(name) ? cref(Scene.v().getSootClass(name)) : "rt_class(" + q(name) + ")"; }

    void run(String out) throws IOException {
        List<SootClass> cls = new ArrayList<>(Scene.v().getApplicationClasses());
        cls.sort(Comparator.comparing(SootClass::getName));
        for (SootClass c : cls) { cid.put(c, cid.size()); app.add(c.getName()); }
        StringBuilder body = new StringBuilder(), protos = new StringBuilder(), tables = new StringBuilder();
        int nm = 0, nstmt = 0;
        for (SootClass c : cls) {
            int ci = cid.get(c); int mi = 0;
            for (SootMethod m : c.getMethods()) { if (m.isConcrete()) { String f = "M" + ci + "_" + (mi++); fname.put(m, f); protos.append("static jv ").append(f).append("(jv *A); /* ").append(m.getSignature().replace("*/", "")).append(" */\n"); } }
        }
        for (SootClass c : cls) {
            int ci = cid.get(c);
            // field layout: kernel slots, then app superclass fields, then own
            for (SootMethod m : c.getMethods()) if (m.isConcrete()) { nstmt += m.retrieveActiveBody().getUnits().size(); body.append(method(m)); nm++; }
            List<String> mt = new ArrayList<>();
            for (SootMethod m : c.getMethods()) if (m.isConcrete()) mt.add("{" + q(m.getSubSignature()) + "," + fname.get(m) + "}");
            List<String> ifs = new ArrayList<>(); for (SootClass i : c.getInterfaces()) ifs.add(q(i.getName()));
            int nstat = 0; for (SootField f : c.getFields()) if (f.isStatic()) nstat++;
            SootMethod cl = c.getMethodUnsafe("void <clinit>()");
            tables.append("static const JMeth MT").append(ci).append("[] = {").append(mt.isEmpty() ? "{NULL,NULL}" : String.join(",", mt)).append("};\n");
            tables.append("static const char *IF").append(ci).append("[] = {").append(ifs.isEmpty() ? "NULL" : String.join(",", ifs)).append("};\n");
            tables.append("JClass C").append(ci).append(" = {").append(q(c.getName())).append(",").append(c.hasSuperclass() ? q(c.getSuperclass().getName()) : "NULL")
                  .append(",NULL,").append(ifs.size()).append(",IF").append(ci).append(",NULL,").append(JET_KSLOTS + instFields(c)).append(",").append(mt.size()).append(",MT").append(ci)
                  .append(",ST").append(ci).append(",").append(cl != null && cl.isConcrete() ? fname.get(cl) : "NULL").append(",0,0};\n");
        }
        StringBuilder all = new StringBuilder();
        all.append("/* generated by jet.aot.JimpleToC: ").append(cls.size()).append(" classes, ").append(nm).append(" methods, ").append(nstmt).append(" Jimple stmts */\n");
        all.append("#include \"jetrt.h\"\n#include <math.h>\n");
        StringBuilder ext = new StringBuilder();
        for (SootClass c : cls) { int ns = 0; for (SootField f : c.getFields()) if (f.isStatic()) ns++; ext.append("extern JClass C").append(cid.get(c)).append(";\nstatic jv ST").append(cid.get(c)).append("[").append(Math.max(1, ns)).append("];\n"); }
        all.append(ext).append(protos);
        all.append("static void *STR[").append(Math.max(1, strs.size())).append("];\n");
        all.append("static const char *const STRLIT[] = {").append(strs.isEmpty() ? "NULL" : String.join(",\n  ", strs)).append("};\n");
        all.append("#define RT_STR(k) (STR[k] ? STR[k] : (STR[k] = rt_str(STRLIT[k])))\n");
        all.append(body).append(tables);
        all.append("void jet_app_register(void) {\n");
        for (SootClass c : cls) all.append("  rt_register(&C").append(cid.get(c)).append(");\n");
        all.append("}\n");
        try (Writer w = new FileWriter(out)) { w.write(all.toString()); }
        System.out.println("classes=" + cls.size() + " methods=" + nm + " stmts=" + nstmt + " strings=" + strs.size() + " callsites=" + site + " unsupported=" + unsupported);
        for (String n : notes) System.out.println("NOTE " + n);
    }
    static final int JET_KSLOTS = 8;

    int instFields(SootClass c) {
        int n = 0;
        for (SootClass k = c; k != null && app.contains(k.getName()); k = k.hasSuperclass() ? k.getSuperclass() : null)
            for (SootField f : k.getFields()) if (!f.isStatic()) n++;
        return n;
    }
    String fieldAccess(String base, SootFieldRef r) {
        SootField f = null;
        try { f = r.resolve(); } catch (Exception e) { }
        if (f == null || !app.contains(f.getDeclaringClass().getName())) { unsupported++; notes.add("external instance field " + r.getSignature()); return "RT_F(" + base + ",0)"; }
        SootClass d = f.getDeclaringClass();
        int idx = JET_KSLOTS + (d.hasSuperclass() ? instFields(d.getSuperclass()) : 0);
        for (SootField g : d.getFields()) { if (g.isStatic()) continue; if (g == f) break; idx++; }
        return "RT_F(" + base + "," + idx + ")." + kind(f.getType());
    }
    String staticAccess(SootFieldRef r) {
        SootField f = null;
        try { f = r.resolve(); } catch (Exception e) { }
        if (f == null || !app.contains(f.getDeclaringClass().getName()))
            return "(*({ static jv *_p; if (!_p) _p = jet_kstatic(" + q(r.declaringClass().getName()) + "," + q(r.name()) + "); _p; }))." + kind(r.type());
        SootClass d = f.getDeclaringClass(); int idx = 0;
        for (SootField g : d.getFields()) { if (!g.isStatic()) continue; if (g == f) break; idx++; }
        return "ST" + cid.get(d) + "[" + idx + "]." + kind(f.getType());
    }

    // ---------------------------------------------------------------- method bodies
    Body cur; Map<Unit, String> labels; List<Trap> traps; SootMethod curM;

    String method(SootMethod m) {
        Body b = m.retrieveActiveBody(); cur = b; curM = m; traps = new ArrayList<>(b.getTraps());
        labels = new HashMap<>(); int ln = 0;
        for (UnitBox ub : b.getAllUnitBoxes()) if (!labels.containsKey(ub.getUnit())) labels.put(ub.getUnit(), "L" + (ln++));
        for (Trap t : traps) if (!labels.containsKey(t.getHandlerUnit())) labels.put(t.getHandlerUnit(), "L" + (ln++));
        StringBuilder s = new StringBuilder();
        s.append("/* ").append(m.getSignature().replace("*/", "")).append(" */\nstatic jv ").append(fname.get(m)).append("(jv *A) {\n");
        for (Local l : b.getLocals()) s.append("  ").append(ctype(l.getType())).append(" ").append(loc(l)).append(" = 0;\n");
        if (m.isStatic() && !m.getName().equals("<clinit>")) s.append("  RT_INIT(C").append(cid.get(m.getDeclaringClass())).append(");\n");
        for (Unit u : b.getUnits()) {
            if (labels.containsKey(u)) s.append(labels.get(u)).append(":;\n");
            s.append("  ").append(stmt((Stmt) u)).append("\n");
        }
        s.append("  return (jv){0};\n}\n");
        return s.toString();
    }
    static String loc(Local l) { return "v_" + l.getName().replace('$', '_'); }

    String clinitGuard(Stmt s) {
        StringBuilder g = new StringBuilder();
        Set<SootClass> need = new LinkedHashSet<>();
        if (s.containsFieldRef() && s.getFieldRef() instanceof StaticFieldRef) { try { need.add(s.getFieldRef().getField().getDeclaringClass()); } catch (Exception e) { } }
        for (ValueBox vb : s.getUseAndDefBoxes()) if (vb.getValue() instanceof NewExpr) need.add(((NewExpr) vb.getValue()).getBaseType().getSootClass());
        for (SootClass c : need) if (cid.containsKey(c) && c != curM.getDeclaringClass()) g.append("RT_INIT(C").append(cid.get(c)).append("); ");
        return g.toString();
    }

    /** after an invoke/throw: if an exception is pending, jump to the matching handler or propagate. */
    String excCheck(Unit u) {
        StringBuilder d = new StringBuilder(" if (rt_exc) {");
        for (Trap t : traps) {
            if (covers(t, u)) d.append(" if (rt_instanceof(rt_exc, ").append(cref(t.getException())).append(")) goto ").append(labels.get(t.getHandlerUnit())).append(";");
        }
        return d.append(" return (jv){0}; }").toString();
    }
    boolean covers(Trap t, Unit u) {
        PatchingChain<Unit> ch = cur.getUnits();
        for (Unit x = t.getBeginUnit(); x != null && x != t.getEndUnit(); x = ch.getSuccOf(x)) if (x == u) return true;
        return false;
    }

    String stmt(Stmt s) {
        String g = clinitGuard(s);
        if (s instanceof IdentityStmt) {
            IdentityStmt is = (IdentityStmt) s; Value r = is.getRightOp(); String lhs = ex(is.getLeftOp());
            if (r instanceof ThisRef) return lhs + " = A[0].l;";
            if (r instanceof ParameterRef) { ParameterRef p = (ParameterRef) r; return lhs + " = A[" + (p.getIndex() + (curM.isStatic() ? 0 : 1)) + "]." + kind(p.getType()) + ";"; }
            if (r instanceof CaughtExceptionRef) return lhs + " = rt_exc; rt_exc = NULL;";
        }
        if (s instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) s; Value l = as.getLeftOp(), r = as.getRightOp();
            String rhs = ex(r);
            if (r instanceof CastExpr && ((CastExpr) r).getCastType() instanceof RefType && l instanceof Local && ((Local) l).getType() instanceof RefLikeType) { }
            String a = g + lval(l) + " = " + rhs + ";";
            if (s.containsInvokeExpr()) a += excCheck(s);
            return a;
        }
        if (s instanceof InvokeStmt) return g + "(void) " + inv(s.getInvokeExpr()) + ";" + excCheck(s);
        if (s instanceof GotoStmt) return "goto " + labels.get(((GotoStmt) s).getTarget()) + ";";
        if (s instanceof IfStmt) return "if (" + ex(((IfStmt) s).getCondition()) + ") goto " + labels.get(((IfStmt) s).getTarget()) + ";";
        if (s instanceof ReturnVoidStmt) return "return (jv){0};";
        if (s instanceof ReturnStmt) { Value v = ((ReturnStmt) s).getOp(); return "return (jv){." + kind(curM.getReturnType()) + " = " + ex(v) + "};"; }
        if (s instanceof ThrowStmt) return "rt_exc = RT_NN(" + ex(((ThrowStmt) s).getOp()) + ");" + excCheck(s);
        if (s instanceof LookupSwitchStmt) {
            LookupSwitchStmt w = (LookupSwitchStmt) s; StringBuilder b = new StringBuilder("switch (" + ex(w.getKey()) + ") {");
            for (int i = 0; i < w.getTargetCount(); i++) b.append(" case ").append(w.getLookupValue(i)).append(": goto ").append(labels.get(w.getTarget(i))).append(";");
            return b.append(" default: goto ").append(labels.get(w.getDefaultTarget())).append("; }").toString();
        }
        if (s instanceof TableSwitchStmt) {
            TableSwitchStmt w = (TableSwitchStmt) s; StringBuilder b = new StringBuilder("switch (" + ex(w.getKey()) + ") {");
            for (int i = w.getLowIndex(); i <= w.getHighIndex(); i++) b.append(" case ").append(i).append(": goto ").append(labels.get(w.getTarget(i - w.getLowIndex()))).append(";");
            return b.append(" default: goto ").append(labels.get(w.getDefaultTarget())).append("; }").toString();
        }
        if (s instanceof NopStmt || s instanceof MonitorStmt) return ";";
        unsupported++; notes.add("stmt " + s.getClass().getSimpleName() + " in " + curM.getSignature());
        return "rt_fatal(\"AOT-UNSUPPORTED\", " + q(s.toString()) + ");";
    }

    String lval(Value l) {
        if (l instanceof Local) return loc((Local) l);
        if (l instanceof InstanceFieldRef) { InstanceFieldRef f = (InstanceFieldRef) l; return fieldAccess(ex(f.getBase()), f.getFieldRef()); }
        if (l instanceof StaticFieldRef) return staticAccess(((StaticFieldRef) l).getFieldRef());
        if (l instanceof ArrayRef) { ArrayRef r = (ArrayRef) l; return "RT_A(" + ex(r.getBase()) + "," + ex(r.getIndex()) + ")." + kind(r.getType()); }
        unsupported++; return "/*?lval*/ (void) 0";
    }

    String num(Constant c) {
        if (c instanceof IntConstant) { int v = ((IntConstant) c).value; return v == Integer.MIN_VALUE ? "(-2147483647-1)" : "(" + v + ")"; }
        if (c instanceof LongConstant) { long v = ((LongConstant) c).value; return v == Long.MIN_VALUE ? "(-INT64_C(9223372036854775807)-1)" : "INT64_C(" + v + ")"; }
        if (c instanceof DoubleConstant) { double v = ((DoubleConstant) c).value; if (Double.isNaN(v)) return "((double) NAN)"; if (Double.isInfinite(v)) return v > 0 ? "((double) INFINITY)" : "(-(double) INFINITY)"; return "(" + Double.toHexString(v) + ")"; }
        if (c instanceof FloatConstant) { float v = ((FloatConstant) c).value; if (Float.isNaN(v)) return "((float) NAN)"; if (Float.isInfinite(v)) return v > 0 ? "((float) INFINITY)" : "(-(float) INFINITY)"; return "(" + Double.toHexString(v) + "f)"; }
        return null;
    }

    String ex(Value v) {
        if (v instanceof Local) return loc((Local) v);
        if (v instanceof Constant) {
            String n = num((Constant) v); if (n != null) return n;
            if (v instanceof NullConstant) return "((void *) 0)";
            if (v instanceof StringConstant) { String s = ((StringConstant) v).value; Integer k = strIdx.get(s); if (k == null) { k = strs.size(); strIdx.put(s, k); strs.add(q(s)); } return "RT_STR(" + k + ")"; }
            if (v instanceof ClassConstant) return "jet_classobj(" + q(((ClassConstant) v).toSootType().toString()) + ")";
        }
        if (v instanceof InstanceFieldRef || v instanceof StaticFieldRef || v instanceof ArrayRef) return lval(v);
        if (v instanceof InvokeExpr) return inv((InvokeExpr) v) + "." + kind(((InvokeExpr) v).getMethodRef().getReturnType());
        if (v instanceof LengthExpr) return "RT_LEN(" + ex(((LengthExpr) v).getOp()) + ")";
        if (v instanceof NegExpr) return "(-" + ex(((NegExpr) v).getOp()) + ")";
        if (v instanceof NewExpr) return "rt_new(" + cref(((NewExpr) v).getBaseType().getSootClass()) + ")";
        if (v instanceof NewArrayExpr) return "rt_newarray(" + ex(((NewArrayExpr) v).getSize()) + ")";
        if (v instanceof InstanceOfExpr) { InstanceOfExpr i = (InstanceOfExpr) v; Type t = i.getCheckType();
            return t instanceof RefType ? "rt_instanceof(" + ex(i.getOp()) + "," + cref(((RefType) t).getClassName()) + ")" : "rt_instanceof(" + ex(i.getOp()) + ",&rt_array_class)"; }
        if (v instanceof CastExpr) return cast((CastExpr) v);
        if (v instanceof BinopExpr) return binop((BinopExpr) v);
        unsupported++; notes.add("expr " + v.getClass().getSimpleName() + " in " + curM.getSignature());
        return "(rt_fatal(\"AOT-UNSUPPORTED\", " + q(v.toString()) + "), 0)";
    }

    String cast(CastExpr c) {
        Type to = c.getCastType(), from = c.getOp().getType(); String e = ex(c.getOp());
        if (to instanceof RefLikeType) {
            if (to instanceof RefType && app.contains(((RefType) to).getClassName())) return "rt_checkcast(" + e + "," + cref(((RefType) to).getClassName()) + ")";
            return e; // kernel types: checked by the kernel on use
        }
        boolean fromFp = from instanceof FloatType || from instanceof DoubleType;
        if (to instanceof IntType && fromFp) return "rt_d2i(" + e + ")";
        if (to instanceof LongType && fromFp) return "rt_d2l(" + e + ")";
        if (to instanceof ByteType) return "((int32_t)(int8_t)(" + e + "))";
        if (to instanceof ShortType) return "((int32_t)(int16_t)(" + e + "))";
        if (to instanceof CharType) return "((int32_t)(uint16_t)(" + e + "))";
        if (to instanceof BooleanType) return "((int32_t)(" + e + "))";
        return "((" + ctype(to) + ")(" + e + "))";
    }

    String binop(BinopExpr b) {
        String x = ex(b.getOp1()), y = ex(b.getOp2()); Type t = b.getOp1().getType();
        boolean isLong = t instanceof LongType, fp = t instanceof FloatType || t instanceof DoubleType;
        if (b instanceof CmpExpr) return "RT_LCMP(" + x + "," + y + ")";
        if (b instanceof CmplExpr) return "RT_CMPL(" + x + "," + y + ")";
        if (b instanceof CmpgExpr) return "RT_CMPG(" + x + "," + y + ")";
        if (b instanceof DivExpr && !fp) return (isLong ? "rt_ldiv(" : "rt_idiv(") + x + "," + y + ")";
        if (b instanceof RemExpr) return fp ? (t instanceof FloatType ? "fmodf(" : "fmod(") + x + "," + y + ")" : (isLong ? "rt_lrem(" : "rt_irem(") + x + "," + y + ")";
        String m = isLong ? "63" : "31", u = isLong ? "uint64_t" : "uint32_t", st = isLong ? "int64_t" : "int32_t";
        if (b instanceof ShlExpr) return "((" + st + ")((" + u + ")(" + x + ") << ((" + y + ") & " + m + ")))";
        if (b instanceof ShrExpr) return "((" + x + ") >> ((" + y + ") & " + m + "))";
        if (b instanceof UshrExpr) return "((" + st + ")((" + u + ")(" + x + ") >> ((" + y + ") & " + m + ")))";
        String op = b.getSymbol().trim();
        if (b instanceof ConditionExpr && b.getOp1().getType() instanceof RefLikeType) return "((void *)(" + x + ") " + op + " (void *)(" + y + "))";
        return "((" + x + ") " + op + " (" + y + "))";
    }

    SootMethod resolveApp(SootMethodRef r) {
        for (SootClass c = r.getDeclaringClass(); c != null && app.contains(c.getName()); c = c.hasSuperclass() ? c.getSuperclass() : null) {
            SootMethod m = c.getMethodUnsafe(r.getSubSignature().getString());
            if (m != null && m.isConcrete()) return m;
        }
        return null;
    }

    String inv(InvokeExpr e) {
        SootMethodRef r = e.getMethodRef(); site++;
        StringBuilder args = new StringBuilder();
        if (e instanceof InstanceInvokeExpr) args.append("{.l = _o}");
        for (int i = 0; i < e.getArgCount(); i++) { if (args.length() > 0) args.append(", "); args.append("{.").append(kind(r.getParameterType(i))).append(" = ").append(ex(e.getArg(i))).append("}"); }
        if (args.length() == 0) args.append("{0}");
        String sub = q(r.getSubSignature().getString()), cls = q(r.getDeclaringClass().getName());
        if (e instanceof StaticInvokeExpr) {
            SootMethod m = resolveApp(r);
            if (m != null) return "({ jv _a[] = {" + args + "}; " + fname.get(m) + "(_a); })";
            return "({ static JFn _f; if (!_f) _f = rt_sfind(" + cls + "," + sub + "); jv _a[] = {" + args + "}; _f(_a); })";
        }
        String base = ex(((InstanceInvokeExpr) e).getBase());
        if (e instanceof SpecialInvokeExpr) {
            SootMethod m = resolveApp(r);
            if (m != null) return "({ void *_o = RT_NN(" + base + "); jv _a[] = {" + args + "}; " + fname.get(m) + "(_a); })";
            return "({ void *_o = RT_NN(" + base + "); static JFn _f; if (!_f) _f = rt_sfind(" + cls + "," + sub + "); jv _a[] = {" + args + "}; _f(_a); })";
        }
        return "({ void *_o = RT_NN(" + base + "); static JClass *_c; static JFn _f; JClass *_k = ((JObj *) _o)->cls; if (_k != _c) { _f = rt_vfind(_k," + sub + "); _c = _k; } jv _a[] = {" + args + "}; _f(_a); })";
    }
}
