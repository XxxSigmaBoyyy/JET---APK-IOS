package android.hardware.display;
import android.view.Display;
public final class DisplayManager {
    public static final String DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION";
    public interface DisplayListener { void onDisplayAdded(int id); void onDisplayRemoved(int id); void onDisplayChanged(int id); }
    public DisplayManager() {}
    public Display getDisplay(int id) { return id == 0 ? Display.jetDefault() : null; }
    public Display[] getDisplays() { return new Display[] { Display.jetDefault() }; }
    public Display[] getDisplays(String category) { return category == null ? getDisplays() : new Display[0]; }
    public void registerDisplayListener(DisplayListener l, android.os.Handler h) {}
    public void unregisterDisplayListener(DisplayListener l) {}
}
