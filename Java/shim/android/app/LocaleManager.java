package android.app;
import android.os.LocaleList;
public class LocaleManager {
    public LocaleList getApplicationLocales() { return LocaleList.getEmptyLocaleList(); }
    public void setApplicationLocales(LocaleList locales) {}
    public LocaleList getSystemLocales() { return LocaleList.getDefault(); }
}
