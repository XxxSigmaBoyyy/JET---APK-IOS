package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
/** ViewSwitcher: two children from factory; required by Telegram TextViewSwitcher. */
public class ViewSwitcher extends ViewAnimator {
    ViewFactory mFactory;
    public ViewSwitcher(Context c) { super(c); }
    public ViewSwitcher(Context c, AttributeSet a) { super(c, a); }
    @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() >= 2) throw new IllegalStateException("Can't add more than 2 views to a ViewSwitcher");
        super.addView(child, index, params);
    }
    public View getNextView() {
        int which = mWhichChild == 0 ? 1 : 0;
        return getChildAt(which);
    }
    private View obtainView() {
        View child = mFactory.makeView();
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (!(lp instanceof LayoutParams)) {
            LayoutParams flp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (lp != null) { flp.width = lp.width; flp.height = lp.height; }
            lp = flp;
        }
        addView(child, -1, lp);
        return child;
    }
    public void setFactory(ViewFactory factory) {
        mFactory = factory;
        obtainView();
        obtainView();
    }
    public void reset() {
        mFirstTime = true;
        View v0 = getChildAt(0); if (v0 != null) v0.setVisibility(View.GONE);
        View v1 = getChildAt(1); if (v1 != null) v1.setVisibility(View.GONE);
    }
    public interface ViewFactory {
        View makeView();
    }
}
