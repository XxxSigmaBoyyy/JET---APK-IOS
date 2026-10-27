package android.view;
/** JET: API 31 RoundedCorner (the host display has square corners, so none are reported). */
public final class RoundedCorner implements android.os.Parcelable {
    public static final int POSITION_TOP_LEFT = 0, POSITION_TOP_RIGHT = 1, POSITION_BOTTOM_RIGHT = 2, POSITION_BOTTOM_LEFT = 3;
    private final int pos, radius, cx, cy;
    public RoundedCorner(int position, int radius, int centerX, int centerY) { pos = position; this.radius = radius; cx = centerX; cy = centerY; }
    public int getPosition() { return pos; } public int getRadius() { return radius; }
    public android.graphics.Point getCenter() { return new android.graphics.Point(cx, cy); }
    public int describeContents() { return 0; } public void writeToParcel(android.os.Parcel p, int f) {}
}
