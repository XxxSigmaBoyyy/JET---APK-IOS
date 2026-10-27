package android.widget;
import android.content.Context;
import android.util.AttributeSet;
public class TableRow extends LinearLayout {
    public TableRow(Context c, AttributeSet a, int defStyleAttr) { this(c, a); }
    public TableRow(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a); }

    public TableRow(Context c) { this(c, null); }
    public TableRow(Context c, AttributeSet a) { super(c, a); mOrientation = HORIZONTAL; mLayoutParams.width = -1; }
    private int fw = -1, fh;
    void forceMeasured(int w, int h) { fw = w; fh = h; setMeasuredDimension(w, h); }
    @Override protected void onMeasure(int ws, int hs) { if (fw >= 0) setMeasuredDimension(fw, fh); else super.onMeasure(ws, hs); }
}
