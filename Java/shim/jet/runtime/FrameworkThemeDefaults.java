package jet.runtime;
import java.util.HashMap;
/** Device-default (Material) values for framework theme attributes that app themes rarely redefine. Metrics only. */
public final class FrameworkThemeDefaults {
    private static final HashMap<Integer, String> M = new HashMap<>();
    static {
        M.put(16843499, "56.000000dip");   // actionBarSize
        M.put(16842829, "64.000000dip");   // listPreferredItemHeight
        M.put(16843654, "80.000000dip");   // listPreferredItemHeightLarge
        M.put(16843655, "48.000000dip");   // listPreferredItemHeightSmall
        M.put(16843683, "16.000000dip"); M.put(16843684, "16.000000dip"); M.put(16843709, "16.000000dip"); M.put(16843710, "16.000000dip"); // listPreferredItemPadding*
        M.put(16843987, "24.000000dip");   // dialogPreferredPadding
        M.put(16842851, "4.000000dip");    // scrollbarSize
        M.put(16842803, "0.5");            // disabledAlpha
        M.put(16842802, "0.6");            // backgroundDimAmount
        M.put(16843827, "#ff3f51b5");      // colorPrimary (Material Indigo 500) — android.R.attr.colorPrimary
        M.put(16843828, "#ff303f9f");      // colorPrimaryDark
        M.put(16843829, "#ffff4081");      // colorAccent
        M.put(16843818, "#ff3f51b5");      // colorControlNormal (approx)
        M.put(16843825, "#ff3f51b5");      // colorControlActivated
        M.put(16843826, "#7f3f51b5");      // colorControlHighlight
    }
    public static String get(int attr) { return M.get(attr); }
}
