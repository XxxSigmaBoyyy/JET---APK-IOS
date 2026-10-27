package android.app;
public final class PictureInPictureParams { private PictureInPictureParams() {}
    public static class Builder { public Builder() {} public Builder setAspectRatio(android.util.Rational r) { return this; } public Builder setAutoEnterEnabled(boolean b) { return this; } public Builder setSeamlessResizeEnabled(boolean b) { return this; } public Builder setSourceRectHint(android.graphics.Rect r) { return this; } public PictureInPictureParams build() { return new PictureInPictureParams(); } } }
