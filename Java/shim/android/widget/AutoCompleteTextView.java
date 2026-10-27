package android.widget;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import jet.runtime.XmlAttrs;
import android.view.View;
/** AOSP AutoCompleteTextView: filters its adapter as the user types (threshold), tracks the drop-down state, performs completion/validation.
 *  The suggestion list is a PopupWindow-backed state (JET single window); item selection/completion semantics follow AOSP. */
public class AutoCompleteTextView extends EditText implements Filter.FilterListener {
    public interface Validator { boolean isValid(CharSequence text); CharSequence fixText(CharSequence invalidText); }
    public interface OnDismissListener { void onDismiss(); }
    private ListAdapter mAdapter; private Filter mFilter; private int mThreshold = 2, mDropDownWidth = -2, mDropDownHeight = -2, mDropDownAnchor = -1, mVOff, mHOff, mListSelection = -1, mInputMethodMode = 1;
    private CharSequence mHintText; private Drawable mDropDownBg; private AdapterView.OnItemClickListener mItemClick; private AdapterView.OnItemSelectedListener mItemSelected;
    private OnDismissListener mOnDismiss; private Validator mValidator; private boolean mPopupShowing, mBlockCompletion, mPerformingCompletion, mDropDownDismissedOnCompletion = true;
    private View.OnClickListener mPassThroughClick;
    public AutoCompleteTextView(Context c) { this(c, null); }
    public AutoCompleteTextView(Context c, AttributeSet a) { this(c, a, 0x0101006b /*autoCompleteTextViewStyle*/); }
    public AutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr) { this(c, a, defStyleAttr, 0); }
    public AutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes) { this(c, a, defStyleAttr, defStyleRes, null); }
    public AutoCompleteTextView(Context c, AttributeSet a, int defStyleAttr, int defStyleRes, android.content.res.Resources.Theme popupTheme) {
        super(c, a, defStyleAttr, defStyleRes);
        XmlAttrs x = XmlAttrs.of(a);
        if (x != null) { try { String v; if ((v = x.get("completionThreshold")) != null) setThreshold(jet.runtime.ResTable.intVal(res().resolve(v))); if ((v = x.get("completionHint")) != null) mHintText = res().resolve(v);
            if ((v = x.get("dropDownWidth")) != null) mDropDownWidth = (int) res().dimen(v); if ((v = x.get("dropDownHeight")) != null) mDropDownHeight = (int) res().dimen(v); } catch (RuntimeException ignored) {} }
        addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) { if (!mBlockCompletion) refreshAutoCompleteResults(); }
        });
        super.setOnClickListener(v -> { if (isPopupShowing()) ensureImeVisible(); if (mPassThroughClick != null) mPassThroughClick.onClick(v); });
    }
    private void ensureImeVisible() {}
    @Override public void setOnClickListener(View.OnClickListener l) { mPassThroughClick = l; }
    public void setCompletionHint(CharSequence h) { mHintText = h; } public CharSequence getCompletionHint() { return mHintText; }
    public int getDropDownWidth() { return mDropDownWidth; } public void setDropDownWidth(int w) { mDropDownWidth = w; }
    public int getDropDownHeight() { return mDropDownHeight; } public void setDropDownHeight(int h) { mDropDownHeight = h; }
    public int getDropDownAnchor() { return mDropDownAnchor; } public void setDropDownAnchor(int id) { mDropDownAnchor = id; }
    public Drawable getDropDownBackground() { return mDropDownBg; } public void setDropDownBackgroundDrawable(Drawable d) { mDropDownBg = d; }
    public void setDropDownBackgroundResource(int id) { mDropDownBg = id == 0 ? null : getContext().getDrawable(id); }
    public void setDropDownVerticalOffset(int o) { mVOff = o; } public int getDropDownVerticalOffset() { return mVOff; }
    public void setDropDownHorizontalOffset(int o) { mHOff = o; } public int getDropDownHorizontalOffset() { return mHOff; }
    public int getThreshold() { return mThreshold; } public void setThreshold(int t) { mThreshold = t <= 0 ? 1 : t; }
    public void setOnItemClickListener(AdapterView.OnItemClickListener l) { mItemClick = l; } public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener l) { mItemSelected = l; }
    public AdapterView.OnItemClickListener getItemClickListener() { return mItemClick; } public AdapterView.OnItemSelectedListener getItemSelectedListener() { return mItemSelected; }
    public AdapterView.OnItemClickListener getOnItemClickListener() { return mItemClick; } public AdapterView.OnItemSelectedListener getOnItemSelectedListener() { return mItemSelected; }
    public void setOnDismissListener(OnDismissListener l) { mOnDismiss = l; }
    public ListAdapter getAdapter() { return mAdapter; }
    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) { mAdapter = adapter; mFilter = adapter == null ? null : adapter.getFilter(); }
    public boolean onKeyPreIme(int keyCode, android.view.KeyEvent event) { if (keyCode == 4 && isPopupShowing()) { dismissDropDown(); return true; } return false; }
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) { if (isPopupShowing() && mListSelection >= 0 && (keyCode == 66 || keyCode == 23)) { performCompletion(); return true; } return super.onKeyUp(keyCode, event); }
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) { if (isPopupShowing() && mAdapter != null) { int n = mAdapter.getCount(); if (keyCode == 20 && n > 0) { setListSelection(Math.min(n - 1, mListSelection + 1)); return true; } if (keyCode == 19 && n > 0) { setListSelection(Math.max(0, mListSelection - 1)); return true; } } return super.onKeyDown(keyCode, event); }
    public boolean enoughToFilter() { return getText().length() >= mThreshold; }
    public final void refreshAutoCompleteResults() { if (enoughToFilter()) { if (mFilter != null) { mPopupShowing |= false; performFiltering(getText(), 0); } } else { if (!mDropDownDismissedOnCompletion) {} dismissDropDown(); if (mFilter != null) mFilter.filter(null); } }
    protected void performFiltering(CharSequence text, int keyCode) { mFilter.filter(text, this); }
    public boolean isPopupShowing() { return mPopupShowing; }
    protected CharSequence convertSelectionToString(Object selectedItem) { return mFilter != null ? mFilter.convertResultToString(selectedItem) : String.valueOf(selectedItem); }
    public void clearListSelection() { mListSelection = -1; } public void setListSelection(int p) { if (isPopupShowing() && p >= 0) mListSelection = p; } public int getListSelection() { return mListSelection; }
    public void performCompletion() {
        if (!isPopupShowing() || mAdapter == null) return;
        int pos = mListSelection < 0 ? -1 : mListSelection; if (pos < 0 && mAdapter.getCount() > 0) return;
        Object item = mAdapter.getItem(pos);
        mPerformingCompletion = true; try { replaceText(convertSelectionToString(item)); } finally { mPerformingCompletion = false; }
        if (mItemClick != null) mItemClick.onItemClick(null, null, pos, mAdapter.getItemId(pos));
        if (mDropDownDismissedOnCompletion) dismissDropDown();
    }
    public void onCommitCompletion(android.view.inputmethod.CompletionInfo completion) { if (isPopupShowing()) { mListSelection = completion.getPosition(); performCompletion(); } }
    public boolean isPerformingCompletion() { return mPerformingCompletion; }
    public void setText(CharSequence text, boolean filter) { if (filter) setText(text); else { mBlockCompletion = true; try { setText(text); } finally { mBlockCompletion = false; } } }
    protected void replaceText(CharSequence text) { clearComposingText(); setText(text); setSelection(getText().length()); }
    public void onFilterComplete(int count) { if (!enoughToFilter()) return; if (count > 0 || mAdapter != null && mAdapter.getCount() > 0) { if (hasFocus() || isFocused()) showDropDown(); } else dismissDropDown(); }
    public void onWindowFocusChanged(boolean hasWindowFocus) { if (!hasWindowFocus) dismissDropDown(); }
    public void dismissDropDown() { if (!mPopupShowing) return; mPopupShowing = false; mListSelection = -1; if (mOnDismiss != null) mOnDismiss.onDismiss(); }
    public int getInputMethodMode() { return mInputMethodMode; } public void setInputMethodMode(int m) { mInputMethodMode = m; }
    public void showDropDown() { if (mAdapter == null) return; mPopupShowing = true; }
    public void setValidator(Validator v) { mValidator = v; } public Validator getValidator() { return mValidator; }
    public void performValidation() { if (mValidator == null) return; CharSequence t = getText(); if (!android.text.TextUtils.isEmpty(t) && !mValidator.isValid(t)) setText(mValidator.fixText(t)); }
    @Override public CharSequence getAccessibilityClassName() { return AutoCompleteTextView.class.getName(); }
}
