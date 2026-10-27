package android.widget;
import android.view.View;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
/** Headless RemoteViews stub for Notification.contentView etc. */
public class RemoteViews implements Parcelable {
    public RemoteViews(String packageName, int layoutId) {}
    public RemoteViews(RemoteViews src) {}
    public RemoteViews(Parcel p) {}
    public void writeToParcel(Parcel dest, int flags) {}
    public int describeContents() { return 0; }
    public static final Parcelable.Creator<RemoteViews> CREATOR = new Parcelable.Creator<RemoteViews>() {
        public RemoteViews createFromParcel(Parcel p) { return new RemoteViews(p); }
        public RemoteViews[] newArray(int n) { return new RemoteViews[n]; }
    };
    public View apply(Context context, android.view.ViewGroup parent) { return new FrameLayout(context); }
    public void setTextViewText(int viewId, CharSequence text) {}
    public void setImageViewResource(int viewId, int srcId) {}
    public void setOnClickPendingIntent(int viewId, android.app.PendingIntent pi) {}
    public void setViewVisibility(int viewId, int visibility) {}
}
