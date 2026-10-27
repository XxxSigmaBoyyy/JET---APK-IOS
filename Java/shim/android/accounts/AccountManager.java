package android.accounts;
import java.util.*;
/** Device with no accounts: accounts the app adds itself are kept in-process (addAccountExplicitly), nothing is synced. */
public class AccountManager {
    public static final String ACTION_ACCOUNT_REMOVED = "android.accounts.action.ACCOUNT_REMOVED";
    public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
    public static final String AUTHENTICATOR_ATTRIBUTES_NAME = "account-authenticator";
    public static final String AUTHENTICATOR_META_DATA_NAME = "android.accounts.AccountAuthenticator";
    public static final int ERROR_CODE_BAD_ARGUMENTS = 7;
    public static final int ERROR_CODE_BAD_AUTHENTICATION = 9;
    public static final int ERROR_CODE_BAD_REQUEST = 8;
    public static final int ERROR_CODE_CANCELED = 4;
    public static final int ERROR_CODE_INVALID_RESPONSE = 5;
    public static final int ERROR_CODE_NETWORK_ERROR = 3;
    public static final int ERROR_CODE_REMOTE_EXCEPTION = 1;
    public static final int ERROR_CODE_UNSUPPORTED_OPERATION = 6;
    public static final String KEY_ACCOUNTS = "accounts";
    public static final String KEY_ACCOUNT_AUTHENTICATOR_RESPONSE = "accountAuthenticatorResponse";
    public static final String KEY_ACCOUNT_MANAGER_RESPONSE = "accountManagerResponse";
    public static final String KEY_ACCOUNT_NAME = "authAccount";
    public static final String KEY_ACCOUNT_SESSION_BUNDLE = "accountSessionBundle";
    public static final String KEY_ACCOUNT_STATUS_TOKEN = "accountStatusToken";
    public static final String KEY_ACCOUNT_TYPE = "accountType";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_AUTHENTICATOR_TYPES = "authenticator_types";
    public static final String KEY_AUTHTOKEN = "authtoken";
    public static final String KEY_AUTH_FAILED_MESSAGE = "authFailedMessage";
    public static final String KEY_AUTH_TOKEN_LABEL = "authTokenLabelKey";
    public static final String KEY_BOOLEAN_RESULT = "booleanResult";
    public static final String KEY_CALLER_PID = "callerPid";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_ERROR_MESSAGE = "errorMessage";
    public static final String KEY_INTENT = "intent";
    public static final String KEY_LAST_AUTHENTICATED_TIME = "lastAuthenticatedTime";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USERDATA = "userdata";
    public static final String LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED";
    public static final String PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE = "android:accounts:key_legacy_not_visible";
    public static final String PACKAGE_NAME_KEY_LEGACY_VISIBLE = "android:accounts:key_legacy_visible";
    public static final int VISIBILITY_NOT_VISIBLE = 3;
    public static final int VISIBILITY_UNDEFINED = 0;
    public static final int VISIBILITY_USER_MANAGED_NOT_VISIBLE = 4;
    public static final int VISIBILITY_USER_MANAGED_VISIBLE = 2;
    public static final int VISIBILITY_VISIBLE = 1;
    private static final AccountManager INSTANCE = new AccountManager();
    private final Map<Account, String> mAccounts = new LinkedHashMap<>(); private final Map<String, String> mData = new HashMap<>();
    private final List<OnAccountsUpdateListener> mListeners = new ArrayList<>();
    AccountManager() {}
    public static AccountManager get(android.content.Context c) { return INSTANCE; }
    public Account[] getAccounts() { synchronized (mAccounts) { return mAccounts.keySet().toArray(new Account[0]); } }
    public Account[] getAccountsByType(String type) { List<Account> r = new ArrayList<>(); for (Account a : getAccounts()) if (type == null || a.type.equals(type)) r.add(a); return r.toArray(new Account[0]); }
    public Account[] getAccountsByTypeForPackage(String type, String pkg) { return getAccountsByType(type); }
    public boolean addAccountExplicitly(Account a, String password, android.os.Bundle userdata) { synchronized (mAccounts) { if (mAccounts.containsKey(a)) return false; mAccounts.put(a, password); } if (userdata != null) for (String k : userdata.keySet()) setUserData(a, k, userdata.getString(k)); fire(); return true; }
    public boolean removeAccountExplicitly(Account a) { boolean r; synchronized (mAccounts) { r = mAccounts.remove(a) != null; } if (r) fire(); return r; }
    public String getPassword(Account a) { synchronized (mAccounts) { return mAccounts.get(a); } }
    public void setPassword(Account a, String p) { synchronized (mAccounts) { if (mAccounts.containsKey(a)) mAccounts.put(a, p); } }
    public void clearPassword(Account a) { setPassword(a, null); }
    public String getUserData(Account a, String k) { synchronized (mData) { return mData.get(a + "\u0000" + k); } }
    public void setUserData(Account a, String k, String v) { synchronized (mData) { mData.put(a + "\u0000" + k, v); } }
    public AuthenticatorDescription[] getAuthenticatorTypes() { return new AuthenticatorDescription[0]; }
    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener l, android.os.Handler h, boolean now) { synchronized (mListeners) { mListeners.add(l); } if (now) l.onAccountsUpdated(getAccounts()); }
    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener l, android.os.Handler h, boolean now, String[] types) { addOnAccountsUpdatedListener(l, h, now); }
    public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener l) { synchronized (mListeners) { mListeners.remove(l); } }
    private void fire() { List<OnAccountsUpdateListener> ls; synchronized (mListeners) { ls = new ArrayList<>(mListeners); } Account[] a = getAccounts(); for (OnAccountsUpdateListener l : ls) l.onAccountsUpdated(a); }
    public String peekAuthToken(Account a, String type) { return null; }
    public void setAuthToken(Account a, String type, String token) {}
    public void invalidateAuthToken(String accountType, String token) {}
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public boolean addAccountExplicitly(android.accounts.Account p0, java.lang.String p1, android.os.Bundle p2, java.util.Map p3) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.addAccountExplicitly(Landroid/accounts/Account;Ljava/lang/String;Landroid/os/Bundle;Ljava/util/Map;)Z"); return false; }
    public java.util.Map getPackagesAndVisibilityForAccount(android.accounts.Account p0) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.getPackagesAndVisibilityForAccount(Landroid/accounts/Account;)Ljava/util/Map;"); return null; }
    public java.util.Map getAccountsAndVisibilityForPackage(java.lang.String p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.getAccountsAndVisibilityForPackage(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Map;"); return null; }
    public boolean setAccountVisibility(android.accounts.Account p0, java.lang.String p1, int p2) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.setAccountVisibility(Landroid/accounts/Account;Ljava/lang/String;I)Z"); return false; }
    public int getAccountVisibility(android.accounts.Account p0, java.lang.String p1) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.getAccountVisibility(Landroid/accounts/Account;Ljava/lang/String;)I"); return 0; }
    public boolean notifyAccountAuthenticated(android.accounts.Account p0) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.notifyAccountAuthenticated(Landroid/accounts/Account;)Z"); return false; }
    public java.lang.String getPreviousName(android.accounts.Account p0) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.getPreviousName(Landroid/accounts/Account;)Ljava/lang/String;"); return null; }
    public java.lang.String blockingGetAuthToken(android.accounts.Account p0, java.lang.String p1, boolean p2) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.blockingGetAuthToken(Landroid/accounts/Account;Ljava/lang/String;Z)Ljava/lang/String;"); return null; }
    public static android.content.Intent newChooseAccountIntent(android.accounts.Account p0, java.util.ArrayList p1, java.lang.String[] p2, boolean p3, java.lang.String p4, java.lang.String p5, java.lang.String[] p6, android.os.Bundle p7) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.newChooseAccountIntent(Landroid/accounts/Account;Ljava/util/ArrayList;[Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;[Ljava/lang/String;Landroid/os/Bundle;)Landroid/content/Intent;"); return null; }
    public static android.content.Intent newChooseAccountIntent(android.accounts.Account p0, java.util.List p1, java.lang.String[] p2, java.lang.String p3, java.lang.String p4, java.lang.String[] p5, android.os.Bundle p6) { jet.runtime.ShimStubs.hit("android.accounts.AccountManager.newChooseAccountIntent(Landroid/accounts/Account;Ljava/util/List;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Landroid/os/Bundle;)Landroid/content/Intent;"); return null; }
    // ---- END generated SDK surface ----
}
