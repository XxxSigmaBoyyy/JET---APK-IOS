package android.os;
public final class Messenger implements Parcelable { private final Handler h; public Messenger(Handler h) { this.h = h; } public Messenger(IBinder b) { h = null; }
    public void send(Message m) throws RemoteException { if (h != null) h.sendMessage(m); } public IBinder getBinder() { return null; }
    public int describeContents() { return 0; } public void writeToParcel(Parcel p, int f) {} }
