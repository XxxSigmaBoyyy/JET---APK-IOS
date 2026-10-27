package android.os;
public final class Message {
    public int what; public int arg1, arg2; public Object obj;
    public Handler target; public Runnable callback;
    public static Message obtain() { return new Message(); }
    public static Message obtain(Handler h) { Message m = new Message(); m.target = h; return m; }
    public static Message obtain(Handler h, int what) { Message m = obtain(h); m.what = what; return m; }
    public static Message obtain(Handler h, Runnable r) { Message m = obtain(h); m.callback = r; return m; }
    public void recycle() {}
    public Messenger replyTo; public int sendingUid = -1; private Bundle data; long when;
    public static Message obtain(Handler h, int what, Object obj) { Message m = obtain(h, what); m.obj = obj; return m; }
    public static Message obtain(Handler h, int what, int a1, int a2) { Message m = obtain(h, what); m.arg1 = a1; m.arg2 = a2; return m; }
    public static Message obtain(Handler h, int what, int a1, int a2, Object obj) { Message m = obtain(h, what, a1, a2); m.obj = obj; return m; }
    public static Message obtain(Message o) { Message m = new Message(); m.copyFrom(o); m.target = o.target; m.callback = o.callback; return m; }
    public void copyFrom(Message o) { what = o.what; arg1 = o.arg1; arg2 = o.arg2; obj = o.obj; replyTo = o.replyTo; data = o.data == null ? null : new Bundle(o.data); }
    public Bundle getData() { if (data == null) data = new Bundle(); return data; } public Bundle peekData() { return data; } public void setData(Bundle b) { data = b; }
    public Handler getTarget() { return target; } public void setTarget(Handler h) { target = h; } public Runnable getCallback() { return callback; } public long getWhen() { return when; }
    public void sendToTarget() { target.sendMessage(this); } public boolean isAsynchronous() { return false; } public void setAsynchronous(boolean a) {}
    @Override public String toString() { return "{ what=" + what + " arg1=" + arg1 + " arg2=" + arg2 + " obj=" + obj + " }"; }
}
