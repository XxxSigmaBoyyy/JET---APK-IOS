package android.telephony;
/** Generic phone TelephonyManager: GSM + SIM ready (WhatsApp device class / registration). */
public class TelephonyManager {
    public static final int PHONE_TYPE_NONE = 0, PHONE_TYPE_GSM = 1, PHONE_TYPE_CDMA = 2, PHONE_TYPE_SIP = 3;
    public static final int SIM_STATE_UNKNOWN = 0, SIM_STATE_ABSENT = 1, SIM_STATE_PIN_REQUIRED = 2,
        SIM_STATE_PUK_REQUIRED = 3, SIM_STATE_NETWORK_LOCKED = 4, SIM_STATE_READY = 5;
    public static final int NETWORK_TYPE_UNKNOWN = 0, NETWORK_TYPE_LTE = 13, NETWORK_TYPE_NR = 20;
    public static final int DATA_CONNECTED = 2;
    public int getPhoneType() { return PHONE_TYPE_GSM; }
    public int getSimState() { return SIM_STATE_READY; }
    public int getSimState(int slotIndex) { return SIM_STATE_READY; }
    public String getSimOperator() { return "310260"; }
    public String getSimOperatorName() { return "Jet"; }
    public String getSimCountryIso() { return "us"; }
    public String getNetworkOperator() { return "310260"; }
    public String getNetworkOperatorName() { return "Jet"; }
    public String getNetworkCountryIso() { return "us"; }
    public int getNetworkType() { return NETWORK_TYPE_LTE; }
    public int getDataNetworkType() { return NETWORK_TYPE_LTE; }
    public int getVoiceNetworkType() { return NETWORK_TYPE_LTE; }
    public String getLine1Number() { return ""; }
    public String getDeviceId() { return null; }
    public String getImei() { return null; }
    public String getMeid() { return null; }
    public String getSubscriberId() { return null; }
    public boolean isNetworkRoaming() { return false; }
    public boolean isDataEnabled() { return true; }
    public boolean isDataCapable() { return true; }
    public boolean isVoiceCapable() { return true; }
    public boolean isSmsCapable() { return true; }
    public int getPhoneCount() { return 1; }
    public int getActiveModemCount() { return 1; }
    public int getDataState() { return DATA_CONNECTED; }
    public void listen(PhoneStateListener l, int events) {}
    public void listen(PhoneStateListener l, int events, int flags) {}
}
