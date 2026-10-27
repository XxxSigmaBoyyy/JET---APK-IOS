package android.telephony;
import android.content.Context;
import java.util.Collections;
import java.util.List;
public class SubscriptionManager {
    public static SubscriptionManager from(Context c) { return new SubscriptionManager(); }
    public List<SubscriptionInfo> getActiveSubscriptionInfoList() { return Collections.emptyList(); }
    public int getActiveSubscriptionInfoCount() { return 0; }
    public static int getDefaultSubscriptionId() { return -1; }
}
