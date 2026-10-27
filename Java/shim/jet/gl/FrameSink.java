package jet.gl;
/** Receives presented GL frames (top-down ARGB). Implemented by SurfaceView. */
public interface FrameSink { void jetPostFrame(int[] argb, int w, int h); int jetSinkWidth(); int jetSinkHeight(); }
