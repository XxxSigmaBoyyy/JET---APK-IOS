package android.graphics.drawable;
public final class Icon {
    public static final int TYPE_BITMAP = 1, TYPE_RESOURCE = 2, TYPE_DATA = 3, TYPE_URI = 4, TYPE_ADAPTIVE_BITMAP = 5;
    private final int type, resId; private final android.graphics.Bitmap bmp;
    private Icon(int t, int r, android.graphics.Bitmap b) { type = t; resId = r; bmp = b; }
    public static Icon createWithResource(android.content.Context c, int id) { return new Icon(TYPE_RESOURCE, id, null); }
    public static Icon createWithResource(String pkg, int id) { return new Icon(TYPE_RESOURCE, id, null); }
    public static Icon createWithBitmap(android.graphics.Bitmap b) { return new Icon(TYPE_BITMAP, 0, b); }
    public static Icon createWithAdaptiveBitmap(android.graphics.Bitmap b) { return new Icon(TYPE_ADAPTIVE_BITMAP, 0, b); }
    public static Icon createWithData(byte[] d, int o, int l) { return new Icon(TYPE_DATA, 0, null); }
    public static Icon createWithContentUri(String u) { return new Icon(TYPE_URI, 0, null); } public static Icon createWithContentUri(android.net.Uri u) { return new Icon(TYPE_URI, 0, null); }
    public int getType() { return type; } public int getResId() { return resId; }
    public Icon setTint(int c) { return this; } public Icon setTintList(android.content.res.ColorStateList l) { return this; } public Icon setTintMode(android.graphics.PorterDuff.Mode m) { return this; }
    public Drawable loadDrawable(android.content.Context c) { return type == TYPE_RESOURCE ? c.getDrawable(resId) : bmp != null ? new BitmapDrawable(bmp) : null; }
}
