package android.text.method;
public class DigitsKeyListener implements KeyListener {
    public static DigitsKeyListener getInstance() { return new DigitsKeyListener(); }
    public static DigitsKeyListener getInstance(String accepted) { return new DigitsKeyListener(); }
    public static DigitsKeyListener getInstance(boolean sign, boolean dec) { return new DigitsKeyListener(); }
    public static DigitsKeyListener getInstance(java.util.Locale l) { return new DigitsKeyListener(); }
    public int getInputType() { return 2; }
}
