package jet.runtime;
import java.util.HashMap;
import java.util.Map;
/** Built-in android.R.layout.* / android.R.transition.* (AOSP simple_*_item + move/fade/slide). */
public final class FrameworkLayouts {
    private static final String NS = "xmlns:android=\"http://schemas.android.com/apk/res/android\" ";
    private static final String T1 = "android:id=\"@android:01020014\" ";
    private static String tv(String extra, String size, String pad) {
        return "<TextView " + NS + T1 + "android:layout_width=\"-1\" android:layout_height=\"-2\" android:textSize=\"" + size + "\" android:textColor=\"?android:01010036\" android:gravity=\"0x800013\" " + pad + extra + "/>";
    }
    static final Map<Integer, String> L = new HashMap<>();
    static {
        String listPad = "android:minHeight=\"48.000000dip\" android:paddingLeft=\"16.000000dip\" android:paddingRight=\"16.000000dip\" ";
        L.put(0x01090003, tv("", "16.000000sp", listPad));                                   // simple_list_item_1
        // AOSP simple_spinner_item: style="?android:attr/spinnerItemStyle" (theme decides size/colour: Widget.TextView.SpinnerItem,
        // Holo/Material variants); textSize/textColor here are only the no-theme fallback layer below that style.
        L.put(0x01090008, "<TextView " + NS + T1 + "style=\"?android:01010089\" android:layout_width=\"-2\" android:layout_height=\"-2\" android:singleLine=\"true\" android:ellipsize=\"3\" android:textAlignment=\"5\"/>"); // simple_spinner_item
        L.put(0x01090009, tv("android:singleLine=\"true\" android:ellipsize=\"3\" ", "16.000000sp", listPad)); // simple_spinner_dropdown_item
        L.put(0x0109000a, tv("android:singleLine=\"true\" android:ellipsize=\"3\" ", "16.000000sp", listPad)); // simple_dropdown_item_1line
        L.put(0x01090011, tv("", "16.000000sp", listPad));                                   // select_dialog_item
        L.put(0x01090016, tv("", "16.000000sp", listPad));                                   // simple_list_item_activated_1
        L.put(0x0109001d, tv("", "16.000000sp", listPad));                                   // simple_selectable_list_item
        L.put(0x01090006, tv("", "16.000000sp", listPad));                                   // simple_expandable_list_item_1
        L.put(0x01090005, tv("", "16.000000sp", listPad));                                   // simple_list_item_checked
        L.put(0x0109000f, tv("", "16.000000sp", listPad));                                   // simple_list_item_single_choice
        L.put(0x01090010, tv("", "16.000000sp", listPad));                                   // simple_list_item_multiple_choice
        L.put(0x01090012, tv("", "16.000000sp", listPad));                                   // select_dialog_singlechoice
        L.put(0x01090013, tv("", "16.000000sp", listPad));                                   // select_dialog_multichoice
        String two = "<LinearLayout " + NS + "android:orientation=\"1\" android:layout_width=\"-1\" android:layout_height=\"-2\" android:minHeight=\"64.000000dip\" android:paddingLeft=\"16.000000dip\" android:paddingRight=\"16.000000dip\" android:gravity=\"16\">"
            + "<TextView android:id=\"@android:01020014\" android:layout_width=\"-1\" android:layout_height=\"-2\" android:textSize=\"16.000000sp\" android:textColor=\"?android:01010036\"/>"
            + "<TextView android:id=\"@android:01020015\" android:layout_width=\"-1\" android:layout_height=\"-2\" android:textSize=\"14.000000sp\" android:textColor=\"?android:01010038\"/></LinearLayout>";
        L.put(0x01090004, two); L.put(0x01090017, two); L.put(0x01090007, two); L.put(0x0109000d, two); // list_item_2, activated_2, expandable_2, two_line
        // android.R.transition.* (AOSP frameworks/base/core/res/res/transition)
        String tns = "xmlns:android=\"http://schemas.android.com/apk/res/android\"";
        L.put(0x010f0001, "<transitionSet " + tns + "><changeBounds/></transitionSet>"); // move
        L.put(0x010f0002, "<explode " + tns + "/>"); // explode
        L.put(0x010f0003, "<fade " + tns + " android:fadingMode=\"fade_in\"/>"); // fade
        L.put(0x010f0004, "<slide " + tns + " android:slideEdge=\"3\"/>"); // slide_bottom
        L.put(0x010f0005, "<slide " + tns + " android:slideEdge=\"3\"/>"); // slide_left
        L.put(0x010f0006, "<slide " + tns + " android:slideEdge=\"5\"/>"); // slide_right
        L.put(0x010f0007, "<slide " + tns + " android:slideEdge=\"48\"/>"); // slide_top
    }
    public static String get(int id) { return L.get(id); }
}
