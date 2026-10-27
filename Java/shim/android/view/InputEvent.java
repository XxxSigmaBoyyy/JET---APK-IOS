package android.view;
public abstract class InputEvent {
    InputEvent() {}
    public abstract int getDeviceId();
    public abstract int getSource();
    public abstract long getEventTime();
    /** AOSP: (getSource() & source) == source. Compose MotionEventAdapter calls this on InputEvent. */
    public boolean isFromSource(int source) { return (getSource() & source) == source; }
}
