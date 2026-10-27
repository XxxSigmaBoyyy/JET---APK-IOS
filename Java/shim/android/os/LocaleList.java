package android.os;
import java.util.Locale;
/** android.os.LocaleList (API 24+). Matching follows AOSP getFirstMatch: exact tag, then language+script/region, then language, else first locale. */
public final class LocaleList implements Parcelable {
    private final Locale[] locales;
    private static LocaleList sDefault, sAdjusted;
    public LocaleList(Locale... locales) {
        if (locales == null) locales = new Locale[0];
        java.util.LinkedHashSet<Locale> s = new java.util.LinkedHashSet<>();
        for (Locale l : locales) { if (l == null) throw new NullPointerException("list[] contains null"); s.add(l); }
        this.locales = s.toArray(new Locale[0]);
    }
    public static LocaleList getEmptyLocaleList() { return new LocaleList(); }
    public static LocaleList getDefault() { return sDefault != null ? sDefault : new LocaleList(Locale.getDefault()); }
    public static LocaleList getAdjustedDefault() { return sAdjusted != null ? sAdjusted : getDefault(); }
    public static void setDefault(LocaleList list) { if (list == null) throw new NullPointerException(); if (list.isEmpty()) throw new IllegalArgumentException("locales is empty"); sDefault = sAdjusted = list; Locale.setDefault(list.get(0)); }
    public static LocaleList forLanguageTags(String tags) {
        if (tags == null || tags.isEmpty()) return getEmptyLocaleList();
        String[] t = tags.split(","); Locale[] l = new Locale[t.length];
        for (int i = 0; i < t.length; i++) l[i] = Locale.forLanguageTag(t[i]);
        return new LocaleList(l);
    }
    public Locale get(int index) { return index >= 0 && index < locales.length ? locales[index] : null; }
    public int size() { return locales.length; }
    public boolean isEmpty() { return locales.length == 0; }
    public int indexOf(Locale l) { for (int i = 0; i < locales.length; i++) if (locales[i].equals(l)) return i; return -1; }
    public String toLanguageTags() { StringBuilder b = new StringBuilder(); for (Locale l : locales) { if (b.length() > 0) b.append(','); b.append(l.toLanguageTag()); } return b.toString(); }
    private static int score(Locale supported, Locale desired) {
        if (supported.equals(desired)) return 3;
        if (!supported.getLanguage().equals(desired.getLanguage())) return 0;
        if (!supported.getCountry().isEmpty() && supported.getCountry().equals(desired.getCountry())) return 2;
        return 1;
    }
    private int matchIndex(String[] supportedTags) {
        if (locales.length == 1 || supportedTags == null || supportedTags.length == 0) return 0;
        int best = Integer.MAX_VALUE, bestScore = 0;
        for (String tag : supportedTags) {
            Locale s = Locale.forLanguageTag(tag);
            for (int i = 0; i < locales.length; i++) { int sc = score(locales[i], s); if (sc > 0 && (i < best || (i == best && sc > bestScore))) { best = i; bestScore = sc; } }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }
    public Locale getFirstMatch(String[] supportedLocales) { return locales.length == 0 ? null : locales[matchIndex(supportedLocales)]; }
    public int getFirstMatchIndex(String[] supportedLocales) { return matchIndex(supportedLocales); }
    public Locale getFirstMatchWithEnglishSupported(String[] supportedLocales) { return getFirstMatch(supportedLocales); }
    public static boolean isPseudoLocale(Locale l) { return l != null && ("XA".equals(l.getCountry()) || "XB".equals(l.getCountry())); }
    @Override public boolean equals(Object o) { return o instanceof LocaleList && java.util.Arrays.equals(locales, ((LocaleList) o).locales); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(locales); }
    @Override public String toString() { return "[" + toLanguageTags() + "]"; }
    public int describeContents() { return 0; }
    public void writeToParcel(Parcel dest, int flags) { dest.writeString(toLanguageTags()); }
    public static final Parcelable.Creator<LocaleList> CREATOR = new Parcelable.Creator<LocaleList>() {
        public LocaleList createFromParcel(Parcel in) { return forLanguageTags(in.readString()); }
        public LocaleList[] newArray(int n) { return new LocaleList[n]; }
    };
}
