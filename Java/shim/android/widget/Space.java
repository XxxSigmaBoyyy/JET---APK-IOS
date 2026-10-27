package android.widget;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
/** AOSP Space: lightweight gap view; onMeasure uses getDefaultSize. */
public class Space extends View {
    public Space(Context c) { super(c); }
    public Space(Context c, AttributeSet a) { super(c, a); }
    public Space(Context c, AttributeSet a, int defStyleAttr) { super(c, a); }
    public Space(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { super(c, a); }
    @Override public void draw(Canvas canvas) {}
    @Override protected void onDraw(Canvas canvas) {}
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize2(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize2(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
    private static int getDefaultSize2(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.AT_MOST: result = Math.min(size, specSize); break;
            case MeasureSpec.EXACTLY: result = specSize; break;
            case MeasureSpec.UNSPECIFIED: result = size; break;
        }
        return result;
    }
}
