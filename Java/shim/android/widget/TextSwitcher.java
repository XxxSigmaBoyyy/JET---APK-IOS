package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
public class TextSwitcher extends ViewSwitcher {
    public TextSwitcher(Context c) { super(c); }
    public TextSwitcher(Context c, AttributeSet a) { super(c, a); }
    @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof TextView)) throw new IllegalArgumentException("TextSwitcher children must be TextViews");
        super.addView(child, index, params);
    }
    public void setText(CharSequence text) {
        TextView t = (TextView) getNextView();
        t.setText(text);
        showNext();
    }
    public void setCurrentText(CharSequence text) {
        ((TextView) getCurrentView()).setText(text);
    }
}
