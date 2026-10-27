package android.text.method;
public class ScrollingMovementMethod extends BaseMovementMethod implements MovementMethod {
    private static ScrollingMovementMethod sInstance;
    public static MovementMethod getInstance() { if (sInstance == null) sInstance = new ScrollingMovementMethod(); return sInstance; }
}
