package dev.jet;

import java.util.*;
import java.util.stream.*;

/** M2 step 1: first Java code run by the embedded no-JIT JVM inside JET on iOS. */
public final class Hello {
    public static String run(String arg) {
        long t0 = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        sb.append("Hello from Java inside JET on iOS\n");
        for (String k : new String[]{"java.vm.name", "java.vm.info", "java.version", "os.name", "os.arch", "java.home"})
            sb.append(k).append(" = ").append(System.getProperty(k)).append('\n');
        // collections, lambdas (invokedynamic -> hidden classes), streams, string formatting
        Map<String, Integer> m = new TreeMap<>();
        for (String w : "the quick brown fox jumps over the lazy dog the end".split(" ")) m.merge(w, 1, Integer::sum);
        sb.append("words = ").append(m).append('\n');
        long primes = IntStream.rangeClosed(2, 20000).filter(n -> IntStream.rangeClosed(2, (int) Math.sqrt(n)).noneMatch(d -> n % d == 0)).count();
        sb.append("primes<=20000 = ").append(primes).append('\n');
        sb.append(String.format("pi ~ %.6f%n", Math.PI));
        sb.append("arg = ").append(arg).append('\n');
        sb.append(String.format("hello ran in %.1f ms (interpreted)%n", (System.nanoTime() - t0) / 1e6));
        System.out.println(sb);
        return sb.toString();
    }

    public static void main(String[] a) { System.out.print(run(a.length > 0 ? a[0] : "")); }
}
