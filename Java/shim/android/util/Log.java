package android.util;
public final class Log {
    public static int v(String t, String m) { System.out.println("V/"+t+": "+m); return 0; }
    public static int d(String t, String m) { System.out.println("D/"+t+": "+m); return 0; }
    public static int i(String t, String m) { System.out.println("I/"+t+": "+m); return 0; }
    public static int w(String t, String m) { System.out.println("W/"+t+": "+m); return 0; }
    public static int e(String t, String m) { System.err.println("E/"+t+": "+m); return 0; }
    public static int v(String t, String m, Throwable tr) { v(t,m); if (tr!=null) tr.printStackTrace(System.out); return 0; }
    public static int d(String t, String m, Throwable tr) { d(t,m); if (tr!=null) tr.printStackTrace(System.out); return 0; }
    public static int i(String t, String m, Throwable tr) { i(t,m); if (tr!=null) tr.printStackTrace(System.out); return 0; }
    public static int w(String t, String m, Throwable tr) { w(t,m); if (tr!=null) tr.printStackTrace(System.out); return 0; }
    public static int e(String t, String m, Throwable tr) { e(t,m); if (tr!=null) tr.printStackTrace(System.err); return 0; }
    public static int wtf(String t, String m) { return e(t,m); }
    public static int wtf(String t, Throwable tr) { return e(t, "", tr); }
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        tr.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
    public static final int VERBOSE = 2, DEBUG = 3, INFO = 4, WARN = 5, ERROR = 6, ASSERT = 7;
    public static boolean isLoggable(String tag, int level) { return level >= INFO; }
    public static int w(String t, Throwable tr) { return w(t, String.valueOf(tr), tr); }
    public static int wtf(String t, String m, Throwable tr) { return e(t, "WTF " + m, tr); }
    public static int println(int p, String t, String m) { System.out.println(p + "/" + t + ": " + m); return 0; }
}
