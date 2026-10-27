package android.content;
/** Minimal ClipData stub for Intent.setClipData / getClipData. */
public class ClipData {
    public static class Item {
        public Item(CharSequence text) {}
        public Item(android.net.Uri uri) {}
        public CharSequence getText() { return null; }
        public android.net.Uri getUri() { return null; }
    }
    public ClipData(CharSequence label, String[] mime, Item item) {}
    public ClipData(ClipData other) {}
    public void addItem(Item item) {}
    public int getItemCount() { return 0; }
    public Item getItemAt(int i) { return null; }
    public static ClipData newPlainText(CharSequence label, CharSequence text) { return new ClipData(label, new String[]{"text/plain"}, new Item(text)); }
}
