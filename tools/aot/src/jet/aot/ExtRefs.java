package jet.aot;
import soot.*; import soot.jimple.*; import soot.options.Options; import java.util.*;
/** Lists app classes and every external (non-app) method/field the app's Jimple references. */
public class ExtRefs {
    public static void main(String[] a) {
        G.reset(); Options o = Options.v(); o.set_allow_phantom_refs(true); o.set_whole_program(false); o.set_process_dir(List.of(a[0]));
        o.set_soot_classpath(a[0] + ":" + a[1]); o.set_prepend_classpath(false); o.set_output_format(Options.output_format_none); o.set_src_prec(Options.src_prec_class);
        Scene.v().loadNecessaryClasses();
        Set<String> app = new TreeSet<>(); for (SootClass c : Scene.v().getApplicationClasses()) app.add(c.getName());
        TreeMap<String,Integer> ext = new TreeMap<>(); int stmts = 0;
        for (SootClass c : new ArrayList<>(Scene.v().getApplicationClasses())) {
            if (!c.getSuperclass().getName().startsWith("java.lang.Object") && !app.contains(c.getSuperclass().getName())) ext.merge("SUPER " + c.getSuperclass().getName(), 1, Integer::sum);
            for (SootMethod m : new ArrayList<>(c.getMethods())) { if (!m.isConcrete()) continue; Body b = m.retrieveActiveBody();
                for (Unit u : b.getUnits()) { stmts++; Stmt s = (Stmt) u;
                    if (s.containsInvokeExpr()) { SootMethodRef r = s.getInvokeExpr().getMethodRef(); if (!app.contains(r.getDeclaringClass().getName())) ext.merge("M " + r.getSignature(), 1, Integer::sum); }
                    if (s.containsFieldRef()) { SootFieldRef r = s.getFieldRef().getFieldRef(); if (!app.contains(r.declaringClass().getName())) ext.merge("F " + r.getSignature(), 1, Integer::sum); }
                    for (ValueBox vb : s.getUseAndDefBoxes()) { Value v = vb.getValue(); if (v instanceof NewExpr && !app.contains(((NewExpr) v).getBaseType().getClassName())) ext.merge("NEW " + ((NewExpr) v).getBaseType(), 1, Integer::sum);
                        if (v instanceof StringConstant) ext.merge("STRCONST", 1, Integer::sum); } } } }
        System.out.println("app classes " + app.size() + ", jimple stmts " + stmts);
        ext.forEach((k, v) -> System.out.println(v + "\t" + k));
    }
}
