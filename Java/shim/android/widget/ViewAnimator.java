package android.widget;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
/** Minimal ViewAnimator for Telegram TextViewSwitcher / country button. */
public class ViewAnimator extends FrameLayout {
    int mWhichChild = 0;
    boolean mFirstTime = true;
    boolean mAnimateFirstTime = true;
    Animation mInAnimation, mOutAnimation;
    public ViewAnimator(Context c) { super(c); }
    public ViewAnimator(Context c, AttributeSet a) { super(c, a); }
    public void setDisplayedChild(int whichChild) {
        mWhichChild = whichChild;
        if (getChildCount() > 0) {
            if (mWhichChild >= getChildCount()) mWhichChild = 0;
            else if (mWhichChild < 0) mWhichChild = getChildCount() - 1;
            boolean animate = !mFirstTime || mAnimateFirstTime;
            showOnly(mWhichChild, animate);
            mFirstTime = false;
        }
    }
    public int getDisplayedChild() { return mWhichChild; }
    public void showNext() { setDisplayedChild(mWhichChild + 1); }
    public void showPrevious() { setDisplayedChild(mWhichChild - 1); }
    void showOnly(int childIndex, boolean animate) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (i == childIndex) {
                if (animate && mInAnimation != null) child.startAnimation(mInAnimation);
                child.setVisibility(View.VISIBLE);
            } else {
                if (animate && mOutAnimation != null && child.getVisibility() == View.VISIBLE)
                    child.startAnimation(mOutAnimation);
                child.setVisibility(View.GONE);
            }
        }
    }
    @Override public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (getChildCount() == 1) child.setVisibility(View.VISIBLE);
        else child.setVisibility(View.GONE);
        if (index >= 0 && mWhichChild >= index) setDisplayedChild(mWhichChild + 1);
    }
    @Override public void removeAllViews() {
        super.removeAllViews();
        mWhichChild = 0;
        mFirstTime = true;
    }
    @Override public void removeView(View view) {
        int index = indexOfChild(view);
        if (index >= 0) removeViewAt(index);
    }
    @Override public void removeViewAt(int index) {
        super.removeViewAt(index);
        int childCount = getChildCount();
        if (childCount == 0) { mWhichChild = 0; mFirstTime = true; }
        else if (mWhichChild >= childCount) setDisplayedChild(childCount - 1);
        else if (mWhichChild == index) setDisplayedChild(mWhichChild);
    }
    public View getCurrentView() { return getChildAt(mWhichChild); }
    public Animation getInAnimation() { return mInAnimation; }
    public void setInAnimation(Animation inAnimation) { mInAnimation = inAnimation; }
    public Animation getOutAnimation() { return mOutAnimation; }
    public void setOutAnimation(Animation outAnimation) { mOutAnimation = outAnimation; }
    public void setInAnimation(Context context, int resourceID) {
        setInAnimation(android.view.animation.AnimationUtils.loadAnimation(context, resourceID));
    }
    public void setOutAnimation(Context context, int resourceID) {
        setOutAnimation(android.view.animation.AnimationUtils.loadAnimation(context, resourceID));
    }
    public void setAnimateFirstView(boolean animate) { mAnimateFirstTime = animate; }
    public int getBaseline() {
        return getCurrentView() != null ? getCurrentView().getBaseline() : super.getBaseline();
    }
}
