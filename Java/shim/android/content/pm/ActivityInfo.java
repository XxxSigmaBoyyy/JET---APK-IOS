package android.content.pm;
public class ActivityInfo extends ComponentInfo {
    public int configChanges;
    public int flags;
    public int launchMode;
    public int screenOrientation = -1;
    public int theme;
    public String permission;
    public String taskAffinity;
    public String parentActivityName;
    public String targetActivity;
    public int softInputMode;
    public int uiOptions;
}
