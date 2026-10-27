package android.preference;
public class PreferenceManager {
    public static final String KEY_HAS_SET_DEFAULT_VALUES = "_has_set_default_values";
    public static android.content.SharedPreferences getDefaultSharedPreferences(android.content.Context c) { return c.getSharedPreferences(getDefaultSharedPreferencesName(c), 0); }
    public static String getDefaultSharedPreferencesName(android.content.Context c) { return c.getPackageName() + "_preferences"; }
    public static void setDefaultValues(android.content.Context c, int resId, boolean readAgain) {}
    public static void setDefaultValues(android.content.Context c, String name, int mode, int resId, boolean readAgain) {}
}
