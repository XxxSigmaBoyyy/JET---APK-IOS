package jet.gl;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentHashMap;
/** GL10/GL11 object handed to Renderer callbacks: dispatches by name+signature to android.opengl.GLES20/GLES30 statics.
 * Fixed-function-only entry points (no ES2 equivalent) log [SHIM-STUB] and return defaults. */
public final class JetGL10 {
    private static final ConcurrentHashMap<Method, Method> MAP = new ConcurrentHashMap<>();
    private static final Method NONE; static { try { NONE = Object.class.getMethod("hashCode"); } catch (Exception e) { throw new Error(e); } }
    public static javax.microedition.khronos.opengles.GL create() {
        return (javax.microedition.khronos.opengles.GL) Proxy.newProxyInstance(JetGL10.class.getClassLoader(),
            new Class<?>[] { javax.microedition.khronos.opengles.GL10.class, javax.microedition.khronos.opengles.GL10Ext.class,
                javax.microedition.khronos.opengles.GL11.class, javax.microedition.khronos.opengles.GL11Ext.class,
                javax.microedition.khronos.opengles.GL11ExtensionPack.class }, JetGL10::invoke);
    }
    private static Object invoke(Object self, Method m, Object[] a) throws Throwable {
        if (m.getDeclaringClass() == Object.class) {
            switch (m.getName()) { case "hashCode": return System.identityHashCode(self); case "equals": return self == a[0]; default: return "JetGL10"; }
        }
        Method t = MAP.computeIfAbsent(m, k -> {
            for (Class<?> c : new Class<?>[] { android.opengl.GLES30.class, android.opengl.GLES20.class })
                try { Method r = c.getMethod(k.getName(), k.getParameterTypes()); if (Modifier.isStatic(r.getModifiers())) return r; } catch (NoSuchMethodException e) {}
            return NONE;
        });
        if (t != NONE) { try { return t.invoke(null, a); } catch (InvocationTargetException e) { throw e.getCause(); } }
        jet.runtime.ShimStubs.hit("javax.microedition.khronos.opengles.GL10." + m.getName() + " (fixed-function, no ES2 mapping)");
        Class<?> r = m.getReturnType();
        if (r == boolean.class) return false; if (r == int.class) return 0; if (r == float.class) return 0f; if (r == long.class) return 0L;
        return null;
    }
}
