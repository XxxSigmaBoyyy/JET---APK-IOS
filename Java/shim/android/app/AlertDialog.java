package android.app;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;
public class AlertDialog extends Dialog {
    public AlertDialog(Context context) { super(context); }
    public AlertDialog(Context context, int themeResId) { super(context, themeResId); }
    public CharSequence jetTitle, jetMessage;
    public void setMessage(CharSequence m) { jetMessage = m; }
    private android.widget.Button[] jetButtons;
    void jetButtons(CharSequence[] texts, DialogInterface.OnClickListener[] ls) {
        jetButtons = new android.widget.Button[3];
        final int[] which = { BUTTON_POSITIVE, BUTTON_NEGATIVE, BUTTON_NEUTRAL };
        for (int i = 0; i < 3; i++) {
            android.widget.Button b = new android.widget.Button(ctx);
            b.setText(texts[i] == null ? "" : texts[i]);
            if (texts[i] == null) b.setVisibility(View.GONE);
            final DialogInterface.OnClickListener l = ls[i]; final int w = which[i];
            b.setOnClickListener(v -> { if (l != null) l.onClick(AlertDialog.this, w); dismiss(); });
            jetButtons[i] = b;
        }
    }
    /** Like AlertController: button1=positive, button2=negative, button3=neutral; unset buttons exist but are GONE. */
    public android.widget.Button getButton(int which) {
        if (jetButtons == null) jetButtons(new CharSequence[3], new DialogInterface.OnClickListener[3]);
        switch (which) { case BUTTON_POSITIVE: return jetButtons[0]; case BUTTON_NEGATIVE: return jetButtons[1]; case BUTTON_NEUTRAL: return jetButtons[2]; default: return null; }
    }
    public android.widget.ListView getListView() { return null; }
    public static class Builder {
        private final Context ctx;
        private CharSequence title, message;
        private DialogInterface.OnClickListener pos, neg, neu;
        private CharSequence posT, negT, neuT;
        private View view;
        private boolean cancelable = true;
        private CharSequence[] items; private DialogInterface.OnClickListener itemL;
        private DialogInterface.OnCancelListener cancelL; private DialogInterface.OnDismissListener dismissL;
        public Builder(Context context) { this.ctx = context; }
        public Builder(Context context, int themeResId) { this(context); }
        public Builder setTitle(CharSequence title) { this.title = title; return this; }
        public Builder setTitle(int titleId) { return setTitle(ctx.getString(titleId)); }
        public Builder setMessage(CharSequence message) { this.message = message; return this; }
        public Builder setMessage(int messageId) { return setMessage(ctx.getString(messageId)); }
        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener l) { posT=text; pos=l; return this; }
        public Builder setPositiveButton(int textId, DialogInterface.OnClickListener l) { return setPositiveButton(ctx.getString(textId), l); }
        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener l) { negT=text; neg=l; return this; }
        public Builder setNegativeButton(int textId, DialogInterface.OnClickListener l) { return setNegativeButton(ctx.getString(textId), l); }
        public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener l) { neuT=text; neu=l; return this; }
        public Builder setView(View view) { this.view = view; return this; }
        public Builder setCancelable(boolean cancelable) { this.cancelable = cancelable; return this; }
        public Builder setIcon(Drawable icon) { return this; }
        public Builder setIcon(int iconId) { return this; }
        public Builder setIconAttribute(int attrId) { return this; }
        public Builder setNeutralButton(int textId, DialogInterface.OnClickListener l) { return setNeutralButton(ctx.getString(textId), l); }
        public Builder setCustomTitle(View v) { return this; }
        public Builder setView(int layoutResId) { this.view = android.view.LayoutInflater.from(ctx).inflate(layoutResId, null); return this; }
        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener l) { this.items = items; this.itemL = l; return this; }
        public Builder setItems(int itemsId, DialogInterface.OnClickListener l) { return setItems(ctx.getResources().getTextArray(itemsId), l); }
        public Builder setSingleChoiceItems(CharSequence[] items, int checked, DialogInterface.OnClickListener l) { return setItems(items, l); }
        public Builder setSingleChoiceItems(int itemsId, int checked, DialogInterface.OnClickListener l) { return setItems(itemsId, l); }
        public Builder setSingleChoiceItems(android.widget.ListAdapter a, int checked, DialogInterface.OnClickListener l) { this.itemL = l; return this; }
        public Builder setSingleChoiceItems(android.database.Cursor c, int checked, String label, DialogInterface.OnClickListener l) { this.itemL = l; return this; }
        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checked, DialogInterface.OnMultiChoiceClickListener l) { this.items = items; return this; }
        public Builder setMultiChoiceItems(int itemsId, boolean[] checked, DialogInterface.OnMultiChoiceClickListener l) { this.items = ctx.getResources().getTextArray(itemsId); return this; }
        public Builder setAdapter(android.widget.ListAdapter a, DialogInterface.OnClickListener l) { this.itemL = l; return this; }
        public Builder setOnCancelListener(DialogInterface.OnCancelListener l) { this.cancelL = l; return this; }
        public Builder setOnDismissListener(DialogInterface.OnDismissListener l) { this.dismissL = l; return this; }
        public Builder setOnKeyListener(DialogInterface.OnKeyListener l) { return this; }
        public Builder setOnItemSelectedListener(android.widget.AdapterView.OnItemSelectedListener l) { return this; }
        public Context getContext() { return ctx; }
        public AlertDialog create() {
            AlertDialog d = new AlertDialog(ctx);
            d.setCancelable(cancelable);
            d.jetTitle = title; d.jetMessage = message;
            d.jetButtons(new CharSequence[] { posT, negT, neuT }, new DialogInterface.OnClickListener[] { pos, neg, neu });
            System.err.println("[jet-dialog] AlertDialog title=" + title + " buttons=" + posT + "/" + negT + "/" + neuT);
            if (cancelL != null) d.setOnCancelListener(cancelL);
            if (dismissL != null) d.setOnDismissListener(dismissL);
            return d;
        }
        public AlertDialog show() { AlertDialog d = create(); d.show(); return d; }
    }
}
