package android.util;
public class DisplayMetrics {
    /** Host screen (harness window, 375x667dp @2x, iPhone-class); swapped for landscape activities. */
    public static int jetScreenW = 750, jetScreenH = 1334;
    public static boolean jetLandscape() { return jetScreenW > jetScreenH; }
    private static final java.util.List<java.lang.ref.WeakReference<DisplayMetrics>> LIVE = new java.util.ArrayList<>();
    /** Rotate the host screen (requested orientation). Returns true if it changed. Updates all live metrics like a config change would. */
    public static synchronized boolean jetSetLandscape(boolean land) {
        int a = Math.min(jetScreenW, jetScreenH), b = Math.max(jetScreenW, jetScreenH);
        int nw = land ? b : a, nh = land ? a : b; if (nw == jetScreenW && nh == jetScreenH) return false;
        for (java.util.Iterator<java.lang.ref.WeakReference<DisplayMetrics>> it = LIVE.iterator(); it.hasNext();) {
            DisplayMetrics m = it.next().get(); if (m == null) { it.remove(); continue; }
            if (m.widthPixels == jetScreenW && m.heightPixels == jetScreenH) { m.widthPixels = nw; m.heightPixels = nh; }
        }
        jetScreenW = nw; jetScreenH = nh; return true;
    }
    public int widthPixels = jetScreenW;
    public int heightPixels = jetScreenH;
    public DisplayMetrics() { synchronized (DisplayMetrics.class) { LIVE.add(new java.lang.ref.WeakReference<>(this)); if (LIVE.size() > 4096) LIVE.removeIf(r -> r.get() == null); } }
    public float density = 2.0f;
    public int densityDpi = 320;
    public float scaledDensity = 2.0f;
    public float xdpi = 320f;
    public float ydpi = 320f;
    public void setToDefaults() { density = 1f; densityDpi = 160; scaledDensity = 1f; }
    public void setTo(DisplayMetrics o) {
        widthPixels=o.widthPixels; heightPixels=o.heightPixels;
        density=o.density; densityDpi=o.densityDpi; scaledDensity=o.scaledDensity;
        xdpi=o.xdpi; ydpi=o.ydpi;
    }
}
