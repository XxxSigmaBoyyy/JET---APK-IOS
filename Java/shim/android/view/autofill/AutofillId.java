package android.view.autofill;
/** JET: identifies a view for autofill (no autofill service on the harness; iOS maps to UITextContentType). */
public final class AutofillId implements android.os.Parcelable {
    private static final java.util.concurrent.atomic.AtomicInteger NEXT = new java.util.concurrent.atomic.AtomicInteger(1000);
    private final int id; private final long virtual;
    public AutofillId() { this(NEXT.incrementAndGet(), -1); }
    private AutofillId(int id, long v) { this.id = id; this.virtual = v; }
    public static AutofillId create(android.view.View host, int virtualId) { return new AutofillId(host.getAutofillId().id, virtualId); }
    @Override public boolean equals(Object o) { return o instanceof AutofillId && ((AutofillId) o).id == id && ((AutofillId) o).virtual == virtual; }
    @Override public int hashCode() { return id * 31 + (int) virtual; }
    @Override public String toString() { return virtual < 0 ? Integer.toString(id) : id + ":" + virtual; }
    public int describeContents() { return 0; }
    public void writeToParcel(android.os.Parcel p, int f) {}
}
