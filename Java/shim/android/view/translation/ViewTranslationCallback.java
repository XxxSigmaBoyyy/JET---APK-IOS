package android.view.translation;
public interface ViewTranslationCallback {
    boolean onShowTranslation(android.view.View view);
    default void enableContentPadding() {}
    default void setAnimationDurationMillis(int durationMillis) {}
    boolean onHideTranslation(android.view.View view);
    boolean onClearTranslation(android.view.View view);
}
