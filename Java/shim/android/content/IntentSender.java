package android.content;
public class IntentSender {
    public static class SendIntentException extends android.util.AndroidException {
        public SendIntentException() {}
        public SendIntentException(String name) { super(name); }
        public SendIntentException(String name, Exception cause) { super(name); }
        public SendIntentException(Exception cause) { super(cause != null ? cause.getMessage() : null); }
    }
}
