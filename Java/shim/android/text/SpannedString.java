package android.text;
public final class SpannedString extends SpannableString { public SpannedString(CharSequence s) { super(s); } public static SpannedString valueOf(CharSequence s) { return s instanceof SpannedString ? (SpannedString) s : new SpannedString(s); } }
