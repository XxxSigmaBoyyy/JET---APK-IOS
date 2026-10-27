Android framework shim (from proto/shim). Compiled into jetshim.jar by scripts/stage_jvm.sh.

iOS stage defaults to JetJava2DGfx (-Djet.gfx=java2d). JetSkia*.java / SkiaInit.java are
kept in tree but excluded from the iOS javac until a Metal/CoreGraphics Skia build is wired.
Image encode/decode uses JetImageIOCodec (javax.imageio).

System.loadLibrary in APK classes is rewritten to dev.jet.nativeapi.Natives by JetAppClassLoader.
