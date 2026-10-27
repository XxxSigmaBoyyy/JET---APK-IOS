package jet.ios;

import android.app.Activity;
import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.View;
import jet.dex.DexClassLoader;
import jet.dex.SdkStubs;
import jet.runtime.ResTable;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/**
 * iOS host entry points for a stock APK imported at runtime (no ahead-of-time translation, no prebuilt module):
 *   importApk  APK -> resource bundle (jet.apk.ApkBundler) + base.apk + lib/arm64-v8a/*.so in the app directory
 *   launch     DEX classes translated per class on demand (jet.dex.DexClassLoader) on the no-JIT JVM, Application +
 *              launcher Activity started on a dedicated UI thread (ActivityThread order), first frame rendered
 *   fb/fbW/fbH/fbSeq  latest frame (ARGB, read by the C side with GetIntArrayRegion and shown as a CGImage)
 *   touch      MotionEvents from UIKit, dispatched on the UI thread; dirty views re-render.
 * All methods are `static String m(String)` so the C side calls them with jet_jvm_call_string; arguments are '\n'-separated.
 */
public final class JetIosHost {
    private JetIosHost() {}

    public static volatile int[] fb; public static volatile int fbW, fbH; public static volatile int fbSeq;
    static final BlockingQueue<float[]> touches = new LinkedBlockingQueue<>();
    static final StringBuilder report = new StringBuilder();
    static volatile Thread ui; static volatile View content; static volatile Activity activity;
    static volatile String state = "idle"; static volatile long firstFrameMs = -1;
    static final CountDownLatch firstFrame = new CountDownLatch(1);

    static synchronized void log(String k, Object v) { report.append(k).append('=').append(v).append('\n'); System.err.println("[jet-ios] " + k + "=" + v); }

    /** importApk("apk\nappDir\nfwDir") -> "ok ms=..\n" + app.properties essentials, or "error ..." */
    public static String importApk(String arg) {
        String[] a = arg.split("\n");
        try {
            File apk = new File(a[0]), dir = new File(a[1]); File fw = a.length > 2 && !a[2].isEmpty() ? new File(a[2]) : null;
            long t0 = System.nanoTime();
            dir.mkdirs();
            File base = new File(dir, "base.apk");
            if (!apk.getCanonicalPath().equals(base.getCanonicalPath())) Files.copy(apk.toPath(), base.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File bundle = new File(dir, "bundle");
            jet.apk.ApkBundler.bundle(base, bundle, fw);
            long tb = System.nanoTime();
            int libs = 0;
            try (ZipFile z = new ZipFile(base)) {
                for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements(); ) {
                    ZipEntry ze = e.nextElement(); String n = ze.getName();
                    if (!n.startsWith("lib/arm64-v8a/") || !n.endsWith(".so") || n.contains("..")) continue;
                    File out = new File(dir, "lib/" + n.substring("lib/arm64-v8a/".length()));
                    out.getParentFile().mkdirs();
                    try (InputStream in = z.getInputStream(ze)) { Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING); }
                    libs++;
                }
            }
            Properties p = props(bundle);
            // Index the dex now and start translating the entry points (Application, launcher activity and what they
            // reference) on spare cores while the user is still looking at the import; launch reuses this loader.
            long ti = System.nanoTime();
            DexClassLoader cl = loader(dir);
            for (String k : new String[]{"application", "mainActivity"}) { String c = p.getProperty(k, ""); if (!c.isEmpty()) cl.prefetch(c); }
            tableTask(bundle, true);
            long tx = System.nanoTime();
            int dex; try (ZipFile z = new ZipFile(base)) { dex = (int) z.stream().filter(x -> x.getName().matches("classes\\d*\\.dex")).count(); }
            return String.format(Locale.US, "ok bundle_ms=%.0f dex_index_ms=%.0f total_ms=%.0f%npackage=%s%nlabel=%s%nmainActivity=%s%ndex=%d%nnative_libs=%d%n",
                    (tb - t0) / 1e6, (tx - ti) / 1e6, (System.nanoTime() - t0) / 1e6, p.getProperty("package", ""), p.getProperty("label", ""), p.getProperty("mainActivity", ""), dex, libs);
        } catch (Throwable t) { return "error " + root(t) + " @ " + top(root(t)); }
    }

    static final Map<String, DexClassLoader> LOADERS = new HashMap<>();
    static synchronized DexClassLoader loader(File dir) throws IOException {
        String k = dir.getCanonicalPath();
        DexClassLoader cl = LOADERS.get(k);
        if (cl == null) { cl = new DexClassLoader(new File(dir, "base.apk"), new File(dir, "dexcache"), JetIosHost.class.getClassLoader()); LOADERS.put(k, cl); }
        return cl;
    }

    /** One parsed resource table per bundle (values.xml is MBs of XML: parse once, off the main thread when possible,
     *  instead of once for the Application and again for every Activity). */
    static final Map<String, java.util.concurrent.FutureTask<ResTable>> TABLES = new HashMap<>();
    static java.util.concurrent.FutureTask<ResTable> tableTask(File bundle, boolean background) throws IOException {
        String k = bundle.getCanonicalPath(); java.util.concurrent.FutureTask<ResTable> f; boolean mine = false;
        synchronized (TABLES) { f = TABLES.get(k); if (f == null) { f = new java.util.concurrent.FutureTask<>(() -> new ResTable(bundle, 2f)); TABLES.put(k, f); mine = true; } }
        if (mine) { if (background) { Thread t = new Thread(null, f, "jet-res-preload", 16L << 20); t.setDaemon(true); t.start(); } else f.run(); }
        return f;
    }
    static ResTable table(File bundle) throws Exception {
        java.util.concurrent.FutureTask<ResTable> f = tableTask(bundle, false);
        f.run();   // no-op if already run/running elsewhere
        try { return f.get(); } catch (java.util.concurrent.ExecutionException e) { Throwable c = e.getCause(); throw c instanceof Exception ? (Exception) c : new RuntimeException(c); }
    }

    static Properties props(File bundle) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(new File(bundle, "app.properties").toPath())) { p.load(r); }
        return p;
    }

    /** launch("appDir\nwidth\nheight\ntimeoutMs") -> report text (first_frame_ms= ...) after the first frame or a failure. */
    public static String launch(String arg) {
        String[] a = arg.split("\n");
        final File dir = new File(a[0]);
        int w = a.length > 1 ? Integer.parseInt(a[1].trim()) : 750, h = a.length > 2 ? Integer.parseInt(a[2].trim()) : 1334;
        long timeout = a.length > 3 ? Long.parseLong(a[3].trim()) : 600_000;
        if (ui != null) return "error already launched";
        android.util.DisplayMetrics.jetScreenW = w; android.util.DisplayMetrics.jetScreenH = h;
        final long t0 = System.nanoTime();
        ui = new Thread(null, () -> run(dir, t0), "main", 64L << 20);
        ui.setDaemon(true); ui.start();
        try { firstFrame.await(timeout, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {}
        synchronized (JetIosHost.class) {
            return "state=" + state + "\n" + report + String.format(Locale.US, "dex_classes_translated=%d%ndex_cache_hits=%d%ndex_translate_ms=%.0f%ndex_lookahead_queued=%d%ndex_lookahead_hits=%d%ndex_lookahead_wait_ms=%.0f%nsdk_stubs=%d%nsdk_stub_calls=%d%n",
                    DexClassLoader.translated.get(), DexClassLoader.cacheHits.get(), DexClassLoader.translateNanos.get() / 1e6,
                    DexClassLoader.lookaheadQueued.get(), DexClassLoader.lookaheadHits.get(), DexClassLoader.lookaheadWaitNanos.get() / 1e6, SdkStubs.MISSING.size(), SdkStubs.STUB_CALLS.size());
        }
    }

    static void run(File dir, long t0) {
        try {
            File bundle = new File(dir, "bundle"), apk = new File(dir, "base.apk");
            Properties props = props(bundle);
            String pkg = props.getProperty("package", ""), main = props.getProperty("mainActivity", "");
            log("package", pkg); log("label", props.getProperty("label")); log("mainActivity", main);
            System.setProperty("jet.apkPath", apk.getPath()); System.setProperty("jet.package", pkg);
            System.setProperty("jet.app.label", props.getProperty("label", pkg));
            File data = new File(dir, "data"); data.mkdirs(); System.setProperty("jet.data.dir", data.getPath());
            File libDir = new File(dir, "lib"); System.setProperty("jet.nativeLibraryDir", libDir.getPath()); System.setProperty("jet.native.lib.dir", libDir.getPath());
            System.setProperty("java.runtime.name", "Android Runtime"); System.setProperty("java.vm.vendor", "The Android Project");
            System.setProperty("java.vendor", "The Android Project"); System.setProperty("java.vm.name", "Dalvik");
            for (String k : props.stringPropertyNames()) if (k.startsWith("appmeta.")) android.content.Context.jetAppMeta.put(k.substring(8), props.getProperty(k));
            for (String k : props.stringPropertyNames()) if (k.startsWith("service.") && k.contains(".meta.")) { int i = k.indexOf(".meta."); android.content.pm.PackageManager.jetServiceMeta.computeIfAbsent(k.substring(8, i), x -> new LinkedHashMap<>()).put(k.substring(i + 6), props.getProperty(k)); }
            try { jet.security.JetAndroidKeyStoreProvider.install(); } catch (Throwable t) { log("android_keystore_error", t); }
            Thread.setDefaultUncaughtExceptionHandler((th, ex) -> { log("thread_death", th.getName() + ": " + root(ex)); ex.printStackTrace(); });

            long tl = System.nanoTime();
            DexClassLoader cl = loader(dir);
            Thread.currentThread().setContextClassLoader(cl);
            log("dex_index_ms", ms(tl)); log("dex_classes", cl.classCount());

            android.os.Looper.jetAdoptCurrentThread();
            Class<?> actCls = loadActivity(cl, main);
            String appName = props.getProperty("application", "");
            if (!appName.isEmpty() && android.app.Application.jetCurrent == null) {
                long ta = System.nanoTime();
                android.app.Application app = (android.app.Application) cl.loadClass(appName).getDeclaredConstructor().newInstance();
                app.jetAttach(new Resources(table(bundle)));
                android.app.Application.jetCurrent = app;
                int aid = ResTable.refId(props.getProperty("appTheme", "")); if (aid != 0) app.setTheme(aid);
                installProviders(cl, app, props);
                ensureMainThread(cl);
                app.onCreate();
                log("application", appName); log("application_onCreate_ms", ms(ta));
            } else ensureMainThread(cl);

            android.content.Intent intent = null; Activity act = null; View root = null;
            for (int hop = 0; ; hop++) {
                act = (Activity) actCls.getDeclaredConstructor().newInstance();
                if (intent == null) { intent = new android.content.Intent(android.content.Intent.ACTION_MAIN); intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER); intent.setClassName(pkg, main); }
                act.setIntent(intent);
                try { String so = props.getProperty("orientation." + main, ""); if (!so.isEmpty()) { Boolean l = Activity.jetIsLandscape(Integer.decode(so)); if (l != null) android.util.DisplayMetrics.jetSetLandscape(l); } } catch (Throwable ignore) {}
                act.jetAttach(new Resources(table(bundle)));
                String th = props.getProperty("theme." + main, ""); if (th.isEmpty()) th = props.getProperty("appTheme", "");
                int tid = ResTable.refId(th); if (tid != 0) act.setTheme(tid);
                long tc = System.nanoTime();
                act.jetPerformCreate(null);
                log("activity", main); log("onCreate_ms", ms(tc));
                root = rootOf(act);
                try { int n = android.os.Looper.jetPump(5000); if (n > 0) log("looper_msgs", n); } catch (Throwable pe) { log("looper_exception", root(pe) + " @ " + top(root(pe))); }
                root = rootOf(act);
                android.content.Intent next = android.content.Context.jetTakeStartedActivity();
                for (int w = 0; w < 80 && next == null && root == null; w++) { Thread.sleep(100); android.os.Looper.jetPump(100); next = android.content.Context.jetTakeStartedActivity(); root = rootOf(act); }
                if (next != null && next.getComponent() != null && hop < 3 && (root == null || act.isFinishing())) {
                    main = props.getProperty("alias." + next.getComponent().getClassName(), next.getComponent().getClassName());
                    log("startActivity_hop" + hop, main); actCls = cl.loadClass(main); intent = next; continue;
                }
                if (root == null) throw new IllegalStateException("onCreate returned but setContentView was never called");
                break;
            }
            activity = act; content = root;
            int W = android.util.DisplayMetrics.jetScreenW, H = android.util.DisplayMetrics.jetScreenH;
            android.view.ViewGroup.jetAttachTree(root);
            act.onAttachedToWindow();
            try { View.jetSetWindowFocus(root, true); act.onWindowFocusChanged(true); } catch (Throwable t) { log("window_focus_error", root(t)); }
            layout(root, W, H);
            try { android.view.ViewTreeObserver.jetTraversal(); } catch (Throwable t) { log("tree_observer_exception", root(t)); }
            try { android.os.Looper.jetPump(2000); } catch (Throwable t) { log("looper_exception_after_attach", root(t)); }
            layout(root, W, H); try { android.view.ViewGroup.jetAttachTree(root); } catch (Throwable ignore) {}
            layout(root, W, H);
            render(root, W, H);
            firstFrameMs = (long) ms(t0);
            log("screen", W + "x" + H);
            log("first_frame_ms", firstFrameMs + " (launch call -> first frame rendered)");
            log("jvm_uptime_ms", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
            state = "running";
            firstFrame.countDown();
            loop(root, act);
        } catch (Throwable t) {
            Throwable r = root(t);
            log("error", r + " @ " + top(r));
            if (r instanceof NoClassDefFoundError || r instanceof ClassNotFoundException || r instanceof NoSuchMethodError || r instanceof NoSuchFieldError || r instanceof AbstractMethodError) log("phase", "missing_api");
            state = "failed"; firstFrame.countDown();
            StringWriter sw = new StringWriter(); t.printStackTrace(new PrintWriter(sw)); System.err.println(sw);
        }
    }

    static Class<?> loadActivity(ClassLoader cl, String main) throws ClassNotFoundException {
        try { return cl.loadClass(main); }
        catch (ClassNotFoundException e) { int d = main.lastIndexOf('.'); if (d > 0) return cl.loadClass(main.substring(0, d) + "$" + main.substring(d + 1)); throw e; }
    }
    static View rootOf(Activity a) {
        if (a.getWindow() != null && a.getWindow().jetContent() != null) return a.getWindow().getDecorView();
        return a.jetContent();
    }
    static void layout(View v, int W, int H) {
        v.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, W, H);
    }
    static void render(View v, int W, int H) {
        jet.gfx.PixelBuffer img = new jet.gfx.PixelBuffer(W, H); img.opaque = true;
        jet.gfx.Gfx gx = android.graphics.JetGfx.forFrame(img);
        try { android.graphics.Canvas cv = new android.graphics.Canvas(gx); cv.drawColor(0xFF000000); v.draw(cv); } finally { gx.dispose(); }
        fbW = W; fbH = H; fb = img.px; fbSeq++;
        jet.runtime.Looper.dirty = false;
    }

    /** UI loop: touches -> dispatchTouchEvent, ready looper messages, re-layout + render when the tree is dirty. */
    static void loop(View root, Activity act) {
        long down = 0;
        while (true) {
            try {
                float[] t = touches.poll(16, TimeUnit.MILLISECONDS);
                while (t != null) {
                    int action = (int) t[0];
                    if (action == MotionEvent.ACTION_DOWN) down = android.os.SystemClock.uptimeMillis();
                    MotionEvent e = MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), action, t[1], t[2], 0);
                    root.dispatchTouchEvent(e); e.recycle();
                    jet.runtime.Looper.dirty = true;
                    t = touches.poll();
                }
                android.os.Looper.jetPump(0);
                android.content.Intent next = android.content.Context.jetTakeStartedActivity();
                if (next != null) log("startActivity_ignored", next.getComponent());
                if (jet.runtime.Looper.dirty) {
                    int W = android.util.DisplayMetrics.jetScreenW, H = android.util.DisplayMetrics.jetScreenH;
                    try { android.view.ViewTreeObserver.jetTraversal(); } catch (Throwable ignore) {}
                    layout(root, W, H); render(root, W, H);
                }
            } catch (Throwable e) { log("ui_loop_exception", root(e) + " @ " + top(root(e))); jet.runtime.Looper.dirty = false; }
        }
    }

    /** touch("action\nx\ny") with action = MotionEvent.ACTION_DOWN(0)/UP(1)/MOVE(2)/CANCEL(3), x/y in frame pixels. */
    public static String touch(String arg) {
        String[] a = arg.split("\n");
        touches.add(new float[]{Integer.parseInt(a[0].trim()), Float.parseFloat(a[1].trim()), Float.parseFloat(a[2].trim())});
        return "ok";
    }
    /** Wait until the frame sequence passes `seq` (or timeout ms): "seq\ntimeoutMs" -> current seq. */
    public static String waitFrame(String arg) {
        String[] a = arg.split("\n"); int seq = Integer.parseInt(a[0].trim()); long until = System.currentTimeMillis() + Long.parseLong(a[1].trim());
        while (fbSeq <= seq && System.currentTimeMillis() < until) { try { Thread.sleep(10); } catch (InterruptedException e) { break; } }
        return Integer.toString(fbSeq);
    }
    /** screenshot("path.png") -> encodes the current frame (PNG through the shim's Bitmap codec). */
    public static String screenshot(String path) {
        int[] px = fb; int w = fbW, h = fbH;
        if (px == null) return "error no frame";
        try (OutputStream o = new FileOutputStream(path)) {
            jet.gfx.PixelBuffer pb = new jet.gfx.PixelBuffer(w, h, px.clone()); pb.opaque = true;
            return android.graphics.Bitmap.jetWrap(pb, false).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, o) ? "ok " + w + "x" + h : "error encode";
        } catch (Throwable t) { return "error " + t; }
    }
    public static String status(String ignored) {
        synchronized (JetIosHost.class) { return "state=" + state + "\nseq=" + fbSeq + "\n" + report; }
    }

    static double ms(long t0) { return Math.round((System.nanoTime() - t0) / 1e5) / 10.0; }
    static Throwable root(Throwable t) { while (t.getCause() != null && t.getCause() != t) t = t.getCause(); if (t instanceof java.lang.reflect.InvocationTargetException && ((java.lang.reflect.InvocationTargetException) t).getTargetException() != null) return root(((java.lang.reflect.InvocationTargetException) t).getTargetException()); return t; }
    static String top(Throwable t) { StackTraceElement[] s = t.getStackTrace(); return s.length == 0 ? "?" : s[0].toString(); }

    static void ensureMainThread(ClassLoader cl) {
        try {
            Class<?> ate = Class.forName("androidx.arch.core.executor.ArchTaskExecutor", true, cl);
            Object inst = ate.getMethod("getInstance").invoke(null);
            Class<?> dc = Class.forName("androidx.arch.core.executor.TaskExecutor", true, cl);
            Object delegate = java.lang.reflect.Proxy.newProxyInstance(cl, new Class<?>[]{dc}, (px, m, args) -> {
                String n = m.getName();
                if (n.equals("isMainThread")) return Boolean.TRUE;
                if ((n.equals("executeOnDiskIO") || n.equals("postToMainThread") || n.equals("executeOnMainThread")) && args != null && args.length > 0 && args[0] instanceof Runnable) ((Runnable) args[0]).run();
                return null;
            });
            try { ate.getMethod("setDelegate", dc).invoke(inst, delegate); }
            catch (NoSuchMethodException e) {
                for (String fn : new String[]{"mDelegate", "delegate"}) { try { java.lang.reflect.Field f = ate.getDeclaredField(fn); f.setAccessible(true); f.set(inst, delegate); break; } catch (Throwable ignore) {} }
            }
        } catch (Throwable ignore) { /* app without androidx.arch */ }
    }

    static void installProviders(ClassLoader cl, android.content.Context app, Properties props) throws Throwable {
        String list = props.getProperty("providers", "");
        if (list.isEmpty()) return;
        String pkg = props.getProperty("package", "");
        for (String pn : list.split(",")) {
            android.content.pm.ProviderInfo pi = new android.content.pm.ProviderInfo();
            pi.name = pn; pi.packageName = pkg; pi.authority = props.getProperty("provider." + pn + ".authorities", "");
            pi.exported = "true".equals(props.getProperty("provider." + pn + ".exported"));
            pi.grantUriPermissions = "true".equals(props.getProperty("provider." + pn + ".grantUriPermissions"));
            pi.applicationInfo = app.getApplicationInfo(); pi.enabled = true;
            String pre = "provider." + pn + ".meta.";
            for (String k : props.stringPropertyNames()) if (k.startsWith(pre)) {
                String v = props.getProperty(k);
                if (v.matches("@[0-9A-Fa-f]{8}")) pi.metaData.putInt(k.substring(pre.length()), (int) Long.parseLong(v.substring(1), 16));
                else pi.metaData.putString(k.substring(pre.length()), v);
            }
            android.content.pm.PackageManager.jetProviders.put(pn, pi);
        }
        int ok = 0;
        for (android.content.pm.ProviderInfo pi : android.content.pm.PackageManager.jetProviders.values()) {
            Class<?> c; try { c = cl.loadClass(pi.name); } catch (Throwable t) { log("provider_missing", pi.name); continue; }
            if (!android.content.ContentProvider.class.isAssignableFrom(c)) continue;
            android.content.ContentProvider cp = (android.content.ContentProvider) c.getDeclaredConstructor().newInstance();
            cp.attachInfo(app, pi); ok++;
        }
        log("providers_created", ok);
    }

    /** Linux/macOS check without iOS: java jet.ios.JetIosHost <apk> <appDir> <fwDir> <out.png> [tapX tapY out2.png] */
    public static void main(String[] a) throws Exception {
        String host = System.getProperty("jet.jni.host.lib");   // desktop test: jet_jni_bridge.c + Runtime/native built as a JNI lib
        if (host != null) System.load(host);
        System.out.println(importApk(a[0] + "\n" + a[1] + "\n" + a[2]));
        System.out.println(launch(a[1] + "\n750\n1334\n600000"));
        System.out.println(screenshot(a[3]));
        if (a.length >= 7) {
            int s = fbSeq; touch("0\n" + a[4] + "\n" + a[5]); touch("1\n" + a[4] + "\n" + a[5]);
            System.out.println("after tap seq=" + waitFrame(s + "\n5000")); System.out.println(screenshot(a[6]));
        }
        System.out.println(status(""));
        System.exit(0);
    }
}
