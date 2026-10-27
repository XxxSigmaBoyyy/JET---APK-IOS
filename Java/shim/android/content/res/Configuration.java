package android.content.res;
public final class Configuration implements java.lang.Comparable<Configuration>, android.os.Parcelable {
    public Configuration() {}
    public Configuration(Configuration o) {
        if (o != null) {
            densityDpi=o.densityDpi; fontScale=o.fontScale; orientation=o.orientation;
            screenWidthDp=o.screenWidthDp; screenHeightDp=o.screenHeightDp;
            smallestScreenWidthDp=o.smallestScreenWidthDp; uiMode=o.uiMode; colorMode=o.colorMode; keyboard=o.keyboard; keyboardHidden=o.keyboardHidden; hardKeyboardHidden=o.hardKeyboardHidden; navigation=o.navigation; navigationHidden=o.navigationHidden; screenLayout=o.screenLayout; touchscreen=o.touchscreen; locale=o.locale; mcc=o.mcc; mnc=o.mnc;
        }
    }
    public int densityDpi;
    public float fontScale;
    public int orientation;
    public int screenWidthDp;
    public int screenHeightDp;
    public int smallestScreenWidthDp;
    public int uiMode;
    public int colorMode, fontWeightAdjustment, hardKeyboardHidden, keyboard, keyboardHidden, mcc, mnc, navigation, navigationHidden, screenLayout, touchscreen;
    public java.util.Locale locale;
    public static final int COLOR_MODE_HDR_MASK = 12;
    public static final int COLOR_MODE_HDR_NO = 4;
    public static final int COLOR_MODE_HDR_SHIFT = 2;
    public static final int COLOR_MODE_HDR_UNDEFINED = 0;
    public static final int COLOR_MODE_HDR_YES = 8;
    public static final int COLOR_MODE_UNDEFINED = 0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_MASK = 3;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_NO = 1;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED = 0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_YES = 2;
    public static final int DENSITY_DPI_UNDEFINED = 0;
    public static final int FONT_WEIGHT_ADJUSTMENT_UNDEFINED = 2147483647;
    public static final int HARDKEYBOARDHIDDEN_NO = 1;
    public static final int HARDKEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int HARDKEYBOARDHIDDEN_YES = 2;
    public static final int KEYBOARDHIDDEN_NO = 1;
    public static final int KEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int KEYBOARDHIDDEN_YES = 2;
    public static final int KEYBOARD_12KEY = 3;
    public static final int KEYBOARD_NOKEYS = 1;
    public static final int KEYBOARD_QWERTY = 2;
    public static final int KEYBOARD_UNDEFINED = 0;
    public static final int MNC_ZERO = 65535;
    public static final int NAVIGATIONHIDDEN_NO = 1;
    public static final int NAVIGATIONHIDDEN_UNDEFINED = 0;
    public static final int NAVIGATIONHIDDEN_YES = 2;
    public static final int NAVIGATION_DPAD = 2;
    public static final int NAVIGATION_NONAV = 1;
    public static final int NAVIGATION_TRACKBALL = 3;
    public static final int NAVIGATION_UNDEFINED = 0;
    public static final int NAVIGATION_WHEEL = 4;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_SQUARE = 3;
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 64;
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 192;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 128;
    public static final int SCREENLAYOUT_LAYOUTDIR_SHIFT = 6;
    public static final int SCREENLAYOUT_LAYOUTDIR_UNDEFINED = 0;
    public static final int SCREENLAYOUT_LONG_MASK = 48;
    public static final int SCREENLAYOUT_LONG_NO = 16;
    public static final int SCREENLAYOUT_LONG_UNDEFINED = 0;
    public static final int SCREENLAYOUT_LONG_YES = 32;
    public static final int SCREENLAYOUT_ROUND_MASK = 768;
    public static final int SCREENLAYOUT_ROUND_NO = 256;
    public static final int SCREENLAYOUT_ROUND_UNDEFINED = 0;
    public static final int SCREENLAYOUT_ROUND_YES = 512;
    public static final int SCREENLAYOUT_SIZE_LARGE = 3;
    public static final int SCREENLAYOUT_SIZE_MASK = 15;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 2;
    public static final int SCREENLAYOUT_SIZE_SMALL = 1;
    public static final int SCREENLAYOUT_SIZE_UNDEFINED = 0;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 4;
    public static final int SCREENLAYOUT_UNDEFINED = 0;
    public static final int SCREEN_HEIGHT_DP_UNDEFINED = 0;
    public static final int SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int TOUCHSCREEN_FINGER = 3;
    public static final int TOUCHSCREEN_NOTOUCH = 1;
    public static final int TOUCHSCREEN_STYLUS = 2;
    public static final int TOUCHSCREEN_UNDEFINED = 0;
    public static final int UI_MODE_NIGHT_MASK = 48;
    public static final int UI_MODE_NIGHT_NO = 16;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0;
    public static final int UI_MODE_NIGHT_YES = 32;
    public static final int UI_MODE_TYPE_APPLIANCE = 5;
    public static final int UI_MODE_TYPE_CAR = 3;
    public static final int UI_MODE_TYPE_DESK = 2;
    public static final int UI_MODE_TYPE_MASK = 15;
    public static final int UI_MODE_TYPE_NORMAL = 1;
    public static final int UI_MODE_TYPE_TELEVISION = 4;
    public static final int UI_MODE_TYPE_UNDEFINED = 0;
    public static final int UI_MODE_TYPE_VR_HEADSET = 7;
    public static final int UI_MODE_TYPE_WATCH = 6;

    public int compareTo(Configuration another) { return 0; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel dest, int flags) {}
    /** Android: new Configuration() is all-undefined; setToDefaults() gives defaults. jetDevice() = the host "phone". */
    public void setToDefaults() { fontScale = 1f; mcc = mnc = 0; locale = null; touchscreen = keyboard = keyboardHidden = hardKeyboardHidden = navigation = navigationHidden = orientation = screenLayout = uiMode = 0; colorMode = 0; screenWidthDp = screenHeightDp = smallestScreenWidthDp = 0; densityDpi = 0; fontWeightAdjustment = Integer.MAX_VALUE; }
    public static Configuration jetDevice() {
        Configuration c = new Configuration(); c.setToDefaults();
        boolean land = android.util.DisplayMetrics.jetLandscape(); c.densityDpi = 320; c.orientation = land ? 2 : 1; c.screenWidthDp = land ? 667 : 375; c.screenHeightDp = land ? 375 : 667; c.smallestScreenWidthDp = 375; c.uiMode = 0x11;
        c.colorMode = 5; c.fontWeightAdjustment = 0; c.hardKeyboardHidden = 2; c.keyboard = 1; c.keyboardHidden = 1; c.navigation = 1; c.navigationHidden = 2; c.screenLayout = land ? 0x262 : 0x162; c.touchscreen = 3; c.locale = java.util.Locale.getDefault();
        return c;
    }

    public boolean equals(Configuration that) {
        if (that == null) return false;
        return densityDpi == that.densityDpi
            && Float.compare(fontScale, that.fontScale) == 0
            && orientation == that.orientation
            && screenWidthDp == that.screenWidthDp
            && screenHeightDp == that.screenHeightDp
            && smallestScreenWidthDp == that.smallestScreenWidthDp
            && uiMode == that.uiMode && colorMode == that.colorMode && keyboard == that.keyboard && keyboardHidden == that.keyboardHidden && hardKeyboardHidden == that.hardKeyboardHidden
            && navigation == that.navigation && navigationHidden == that.navigationHidden && screenLayout == that.screenLayout && touchscreen == that.touchscreen && mcc == that.mcc && mnc == that.mnc
            && fontWeightAdjustment == that.fontWeightAdjustment && java.util.Objects.equals(locale, that.locale);
    }
    @Override public boolean equals(Object that) {
        return that instanceof Configuration && equals((Configuration) that);
    }
    @Override public int hashCode() { return densityDpi ^ orientation ^ screenWidthDp; }

    private android.os.LocaleList mLocaleList = android.os.LocaleList.getDefault();
    public android.os.LocaleList getLocales() { return mLocaleList; }
    public void setLocales(android.os.LocaleList locales) { mLocaleList = locales != null ? locales : android.os.LocaleList.getEmptyLocaleList(); }
    public void setTo(Configuration o) {
        if (o == null) return;
        densityDpi=o.densityDpi; fontScale=o.fontScale; orientation=o.orientation;
        screenWidthDp=o.screenWidthDp; screenHeightDp=o.screenHeightDp;
        smallestScreenWidthDp=o.smallestScreenWidthDp; uiMode=o.uiMode;
        mLocaleList = o.mLocaleList;
    }
    public void setLocale(java.util.Locale loc) { setLocales(loc == null ? android.os.LocaleList.getEmptyLocaleList() : new android.os.LocaleList(loc)); locale = loc; setLayoutDirection(loc); }
    public int getLayoutDirection() { return (screenLayout & 0xC0) == 0x80 ? 1 /* RTL */ : 0 /* LTR */; }
    public void setLayoutDirection(java.util.Locale loc) { int dir = loc == null ? 0 : android.text.TextUtils.getLayoutDirectionFromLocale(loc); screenLayout = (screenLayout & ~0xC0) | ((dir + 1) << 6); }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public boolean isLayoutSizeAtLeast(int p0) { jet.runtime.ShimStubs.hit("android.content.res.Configuration.isLayoutSizeAtLeast(I)Z"); return false; }
    /** AOSP semantics: copy every field that is *defined* in delta (non-zero / non-null), return the changed-config mask. */
    public int updateFrom(android.content.res.Configuration d) { int ch = jetDiff(d, true); jetApply(d); return ch; }
    /** AOSP semantics: mask of fields defined in d that differ from this. */
    public int diff(android.content.res.Configuration d) { return jetDiff(d, true); }
    private int jetDiff(Configuration d, boolean definedOnly) {
        if (d == null) return 0; int m = 0;
        if (d.fontScale > 0 && Float.compare(fontScale, d.fontScale) != 0) m |= 0x40000000;
        if (d.mcc != 0 && mcc != d.mcc) m |= 1; if (d.mnc != 0 && mnc != d.mnc) m |= 2;
        if (d.locale != null && !d.locale.equals(locale)) m |= 4 | 0x2000;
        if (d.touchscreen != 0 && touchscreen != d.touchscreen) m |= 8;
        if (d.keyboard != 0 && keyboard != d.keyboard) m |= 0x10;
        if (d.keyboardHidden != 0 && keyboardHidden != d.keyboardHidden) m |= 0x20;
        if (d.hardKeyboardHidden != 0 && hardKeyboardHidden != d.hardKeyboardHidden) m |= 0x20;
        if (d.navigation != 0 && navigation != d.navigation) m |= 0x40;
        if (d.navigationHidden != 0 && navigationHidden != d.navigationHidden) m |= 0x20;
        if (d.orientation != 0 && orientation != d.orientation) m |= 0x80;
        if (d.screenLayout != 0 && screenLayout != d.screenLayout) m |= 0x100;
        if (d.uiMode != 0 && ((d.uiMode & 0x0F) != 0 && (uiMode & 0x0F) != (d.uiMode & 0x0F) || (d.uiMode & 0x30) != 0 && (uiMode & 0x30) != (d.uiMode & 0x30))) m |= 0x200;
        if (d.screenWidthDp != 0 && screenWidthDp != d.screenWidthDp || d.screenHeightDp != 0 && screenHeightDp != d.screenHeightDp) m |= 0x400;
        if (d.smallestScreenWidthDp != 0 && smallestScreenWidthDp != d.smallestScreenWidthDp) m |= 0x800;
        if (d.densityDpi != 0 && densityDpi != d.densityDpi) m |= 0x1000;
        if (d.colorMode != 0 && colorMode != d.colorMode) m |= 0x4000;
        return m;
    }
    private void jetApply(Configuration d) {
        if (d == null) return;
        if (d.fontScale > 0) fontScale = d.fontScale;
        if (d.mcc != 0) mcc = d.mcc; if (d.mnc != 0) mnc = d.mnc;
        if (d.locale != null) locale = d.locale;
        if (d.touchscreen != 0) touchscreen = d.touchscreen; if (d.keyboard != 0) keyboard = d.keyboard;
        if (d.keyboardHidden != 0) keyboardHidden = d.keyboardHidden; if (d.hardKeyboardHidden != 0) hardKeyboardHidden = d.hardKeyboardHidden;
        if (d.navigation != 0) navigation = d.navigation; if (d.navigationHidden != 0) navigationHidden = d.navigationHidden;
        if (d.orientation != 0) orientation = d.orientation; if (d.screenLayout != 0) screenLayout = d.screenLayout;
        if ((d.uiMode & 0x0F) != 0) uiMode = (uiMode & ~0x0F) | (d.uiMode & 0x0F);
        if ((d.uiMode & 0x30) != 0) uiMode = (uiMode & ~0x30) | (d.uiMode & 0x30);
        if (d.screenWidthDp != 0) screenWidthDp = d.screenWidthDp; if (d.screenHeightDp != 0) screenHeightDp = d.screenHeightDp;
        if (d.smallestScreenWidthDp != 0) smallestScreenWidthDp = d.smallestScreenWidthDp;
        if (d.densityDpi != 0) densityDpi = d.densityDpi; if (d.colorMode != 0) colorMode = d.colorMode;
        if (d.fontWeightAdjustment != 0 && d.fontWeightAdjustment != Integer.MAX_VALUE) fontWeightAdjustment = d.fontWeightAdjustment;
    }
    public static boolean needNewResources(int p0, int p1) { jet.runtime.ShimStubs.hit("android.content.res.Configuration.needNewResources(II)Z"); return false; }
    public void readFromParcel(android.os.Parcel p0) { jet.runtime.ShimStubs.hit("android.content.res.Configuration.readFromParcel(Landroid/os/Parcel;)V"); }
    public boolean isNightModeActive() { jet.runtime.ShimStubs.hit("android.content.res.Configuration.isNightModeActive()Z"); return false; }
    public boolean isScreenRound() { jet.runtime.ShimStubs.hit("android.content.res.Configuration.isScreenRound()Z"); return false; }
    public boolean isScreenWideColorGamut() { jet.runtime.ShimStubs.hit("android.content.res.Configuration.isScreenWideColorGamut()Z"); return false; }
    public boolean isScreenHdr() { jet.runtime.ShimStubs.hit("android.content.res.Configuration.isScreenHdr()Z"); return false; }
    public static android.content.res.Configuration generateDelta(android.content.res.Configuration p0, android.content.res.Configuration p1) { jet.runtime.ShimStubs.hit("android.content.res.Configuration.generateDelta(Landroid/content/res/Configuration;Landroid/content/res/Configuration;)Landroid/content/res/Configuration;"); return null; }
    // ---- END generated SDK surface ----
}
