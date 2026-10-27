package jet.runtime;
/** Called by generated SDK-surface defaults in shim classes (jetc/gen_sdk_surface.py). */
public final class ShimStubs {
    public static final java.util.Set<String> HIT = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());
    public static void hit(String sig) { if (HIT.add(sig)) System.err.println("[SHIM-STUB] " + sig); }
}
