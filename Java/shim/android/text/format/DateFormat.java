package android.text.format;

import android.content.Context;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** android.text.format.DateFormat on java.text (device locale = JVM default; 24h follows the locale). */
public class DateFormat {
    public static final char QUOTE = '\'', AM_PM = 'a', CAPITAL_AM_PM = 'A', DATE = 'd', DAY = 'E', HOUR = 'h', HOUR_OF_DAY = 'k', MINUTE = 'm', MONTH = 'M', SECONDS = 's', TIME_ZONE = 'z', YEAR = 'y', STANDALONE_MONTH = 'L';
    public DateFormat() {}
    private static Locale loc(Context c) { try { if (c != null && c.getResources() != null && c.getResources().getConfiguration().locale != null) return c.getResources().getConfiguration().locale; } catch (Throwable ignored) {} return Locale.getDefault(); }
    public static boolean is24HourFormat(Context c) {
        java.text.DateFormat f = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, loc(c));
        return !(f instanceof java.text.SimpleDateFormat) || ((java.text.SimpleDateFormat) f).toPattern().indexOf('a') < 0;
    }
    public static String getBestDateTimePattern(Locale l, String skeleton) {
        boolean d = skeleton.matches(".*[dMyE].*"), t = skeleton.matches(".*[hHkKms].*");
        java.text.DateFormat f = d && t ? java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, l)
                : t ? java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, l) : java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, l);
        return f instanceof java.text.SimpleDateFormat ? ((java.text.SimpleDateFormat) f).toPattern() : skeleton;
    }
    public static java.text.DateFormat getTimeFormat(Context c) { return new java.text.SimpleDateFormat(is24HourFormat(c) ? "HH:mm" : "h:mm a", loc(c)); }
    public static java.text.DateFormat getDateFormat(Context c) { return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, loc(c)); }
    public static java.text.DateFormat getLongDateFormat(Context c) { return java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, loc(c)); }
    public static java.text.DateFormat getMediumDateFormat(Context c) { return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, loc(c)); }
    public static char[] getDateFormatOrder(Context c) {
        String p = getDateFormat(c) instanceof java.text.SimpleDateFormat ? ((java.text.SimpleDateFormat) getDateFormat(c)).toPattern() : "MM/dd/yy";
        StringBuilder o = new StringBuilder();
        for (char ch : p.toCharArray()) { char k = ch == 'L' ? 'M' : ch; if ((k == 'd' || k == 'M' || k == 'y') && o.indexOf(String.valueOf(k)) < 0) o.append(k); }
        return o.toString().toCharArray();
    }
    /** Android pattern letters (k = 0-23 hour in Android's legacy formatter; A = AM/PM upper) mapped to java.text. */
    private static String toJava(CharSequence f) {
        StringBuilder b = new StringBuilder(); String s = f.toString();
        for (int i = 0; i < s.length(); i++) { char ch = s.charAt(i); b.append(ch == 'k' ? 'H' : ch == 'A' ? 'a' : ch); }
        return b.toString();
    }
    public static CharSequence format(CharSequence inFormat, long inTimeInMillis) { return new java.text.SimpleDateFormat(toJava(inFormat), Locale.getDefault()).format(new Date(inTimeInMillis)); }
    public static CharSequence format(CharSequence inFormat, Date inDate) { return format(inFormat, inDate.getTime()); }
    public static CharSequence format(CharSequence inFormat, Calendar inDate) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(toJava(inFormat), Locale.getDefault()); f.setTimeZone(inDate.getTimeZone()); return f.format(inDate.getTime());
    }
}
