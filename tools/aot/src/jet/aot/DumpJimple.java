package jet.aot;
import soot.*; import soot.options.Options; import java.util.*;
public class DumpJimple {
    public static void main(String[] a) {
        G.reset(); Options o = Options.v(); o.set_allow_phantom_refs(true); o.set_process_dir(List.of(a[0]));
        o.set_soot_classpath(a[0] + ":" + a[1]); o.set_prepend_classpath(false); o.set_output_format(Options.output_format_jimple); o.set_src_prec(Options.src_prec_class);
        o.set_output_dir(a[2]); Scene.v().loadNecessaryClasses();
        for (SootClass c : new ArrayList<>(Scene.v().getApplicationClasses())) for (SootMethod m : new ArrayList<>(c.getMethods())) if (m.isConcrete()) m.retrieveActiveBody();
        PackManager.v().writeOutput();
        System.out.println("wrote jimple to " + a[2]);
    }
}
