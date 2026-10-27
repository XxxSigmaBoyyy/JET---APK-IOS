package android.graphics.drawable;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import org.w3c.dom.*;
/** android.graphics.drawable.VectorDrawable: <vector><path pathData fillColor strokeColor/></vector> rendered with Java2D. */
public class VectorDrawable extends Drawable {
    static final class P { Path2D.Float path; int fill, stroke; float strokeW; float fillAlpha = 1; }
    float vw = 24, vh = 24; int iw = 48, ih = 48;
    final ArrayList<P> paths = new ArrayList<>();
    public VectorDrawable() {}
    @Override public int getIntrinsicWidth() { return iw; }
    @Override public int getIntrinsicHeight() { return ih; }
    @Override public int getOpacity() { return -3; }
    @Override public void jetDraw(android.graphics.Canvas c, int l, int t, int r, int b, int state) {
        if (r <= l || b <= t) return;
        c.save();
        try {
            c.translate(l, t); c.scale((r - l) / vw, (b - t) / vh);
            android.graphics.Paint q = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            for (P p : paths) {
                int fill = mHasTint ? mTint : p.fill;
                if (mColorFilter instanceof android.graphics.PorterDuffColorFilter) fill = ((android.graphics.PorterDuffColorFilter) mColorFilter).getColor();
                else if (mColorFilter instanceof android.graphics.BlendModeColorFilter) fill = ((android.graphics.BlendModeColorFilter) mColorFilter).getColor();
                if (fill != 0) { q.setStyle(android.graphics.Paint.Style.FILL); q.setColor(fill); c.jetFill(p.path, q); }
                if (p.stroke != 0 && p.strokeW > 0) { q.setStyle(android.graphics.Paint.Style.STROKE); q.setColor(p.stroke); q.setStrokeWidth(p.strokeW); c.jetStroke(p.path, q); }
            }
        } finally { c.restore(); }
    }
    public static VectorDrawable parse(Element root, jet.runtime.ResTable t) {
        VectorDrawable v = new VectorDrawable();
        v.vw = num(root.getAttribute("android:viewportWidth"), 24); v.vh = num(root.getAttribute("android:viewportHeight"), 24);
        v.iw = (int) dimen(root.getAttribute("android:width"), t, 24); v.ih = (int) dimen(root.getAttribute("android:height"), t, 24);
        NodeList nl = root.getElementsByTagName("path");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            P p = new P();
            try { p.path = PathParser.parse(e.getAttribute("android:pathData")); } catch (Exception ex) { continue; }
            p.fill = color(e.getAttribute("android:fillColor"), t);
            p.stroke = color(e.getAttribute("android:strokeColor"), t);
            p.strokeW = num(e.getAttribute("android:strokeWidth"), 0);
            String ft = e.getAttribute("android:fillType");
            // AOSP: evenOdd|nonZero; aapt2 / androguard often emit 0=nonZero, 1=evenOdd.
            if ("evenOdd".equals(ft) || "1".equals(ft)) p.path.setWindingRule(Path2D.WIND_EVEN_ODD);
            v.paths.add(p);
        }
        return v;
    }
    static float num(String s, float d) { try { return s == null || s.isEmpty() ? d : Float.parseFloat(s); } catch (Exception e) { return d; } }
    static float dimen(String s, jet.runtime.ResTable t, float d) { try { return s == null || s.isEmpty() ? d * t.density : t.dimen(s); } catch (Exception e) { return d * t.density; } }
    static int color(String s, jet.runtime.ResTable t) {
        if (s == null || s.isEmpty()) return 0;
        try {
            if (s.startsWith("#")) return jet.runtime.ResTable.parseColor(s);
            if (s.startsWith("@")) { String r = t.resolve("@" + Integer.toHexString(jet.runtime.ResTable.refId(s))); if (r != null && r.startsWith("#")) return jet.runtime.ResTable.parseColor(r); }
            if (s.startsWith("?")) return 0xFF757575; // theme attr: neutral grey until theme-aware inflation
        } catch (Exception ignored) {}
        return 0xFF757575;
    }

    /** SVG path data subset used by Android vectors (M L H V C S Q T A Z, relative + absolute). */
    static final class PathParser {
        static Path2D.Float parse(String d) {
            Path2D.Float p = new Path2D.Float();
            ArrayList<String> tok = new ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Za-z]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").matcher(d);
            while (m.find()) tok.add(m.group());
            int i = 0; char cmd = 'M'; float x = 0, y = 0, sx = 0, sy = 0, cx = 0, cy = 0; char prev = ' ';
            while (i < tok.size()) {
                String s = tok.get(i);
                if (Character.isLetter(s.charAt(0))) { cmd = s.charAt(0); i++; if (cmd == 'Z' || cmd == 'z') { p.closePath(); x = sx; y = sy; prev = cmd; continue; } }
                boolean rel = Character.isLowerCase(cmd);
                float ox = rel ? x : 0, oy = rel ? y : 0;
                switch (Character.toUpperCase(cmd)) {
                    case 'M': x = ox + f(tok, i++); y = oy + f(tok, i++); p.moveTo(x, y); sx = x; sy = y; cmd = rel ? 'l' : 'L'; break;
                    case 'L': x = ox + f(tok, i++); y = oy + f(tok, i++); p.lineTo(x, y); break;
                    case 'H': x = ox + f(tok, i++); p.lineTo(x, y); break;
                    case 'V': y = (rel ? y : 0) + f(tok, i++); p.lineTo(x, y); break;
                    case 'C': { float x1 = ox + f(tok, i++), y1 = oy + f(tok, i++); cx = ox + f(tok, i++); cy = oy + f(tok, i++); x = ox + f(tok, i++); y = oy + f(tok, i++); p.curveTo(x1, y1, cx, cy, x, y); break; }
                    case 'S': { float x1 = "CcSs".indexOf(prev) >= 0 ? 2 * x - cx : x, y1 = "CcSs".indexOf(prev) >= 0 ? 2 * y - cy : y; cx = ox + f(tok, i++); cy = oy + f(tok, i++); x = ox + f(tok, i++); y = oy + f(tok, i++); p.curveTo(x1, y1, cx, cy, x, y); break; }
                    case 'Q': { cx = ox + f(tok, i++); cy = oy + f(tok, i++); x = ox + f(tok, i++); y = oy + f(tok, i++); p.quadTo(cx, cy, x, y); break; }
                    case 'T': { cx = "QqTt".indexOf(prev) >= 0 ? 2 * x - cx : x; cy = "QqTt".indexOf(prev) >= 0 ? 2 * y - cy : y; x = ox + f(tok, i++); y = oy + f(tok, i++); p.quadTo(cx, cy, x, y); break; }
                    case 'A': {
                        float rx = f(tok, i++), ry = f(tok, i++), rot = f(tok, i++); boolean large = f(tok, i++) != 0, sweep = f(tok, i++) != 0;
                        float nx = ox + f(tok, i++), ny = oy + f(tok, i++);
                        arc(p, x, y, rx, ry, rot, large, sweep, nx, ny); x = nx; y = ny; break;
                    }
                    default: i++;
                }
                prev = cmd;
            }
            return p;
        }
        static float f(ArrayList<String> t, int i) { return Float.parseFloat(t.get(i)); }
        /** Endpoint -> center parameterisation (SVG F.6.5), appended as cubic segments via Arc2D. */
        static void arc(Path2D.Float p, float x0, float y0, float rx, float ry, float rotDeg, boolean large, boolean sweep, float x, float y) {
            if (rx == 0 || ry == 0) { p.lineTo(x, y); return; }
            double phi = Math.toRadians(rotDeg), c = Math.cos(phi), s = Math.sin(phi);
            double dx = (x0 - x) / 2.0, dy = (y0 - y) / 2.0;
            double x1 = c * dx + s * dy, y1 = -s * dx + c * dy;
            rx = Math.abs(rx); ry = Math.abs(ry);
            double lam = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry);
            if (lam > 1) { rx *= Math.sqrt(lam); ry *= Math.sqrt(lam); }
            double num = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1, den = rx * rx * y1 * y1 + ry * ry * x1 * x1;
            double co = (large == sweep ? -1 : 1) * Math.sqrt(Math.max(0, num / den));
            double cxp = co * rx * y1 / ry, cyp = -co * ry * x1 / rx;
            double cxx = c * cxp - s * cyp + (x0 + x) / 2.0, cyy = s * cxp + c * cyp + (y0 + y) / 2.0;
            double th1 = Math.atan2((y1 - cyp) / ry, (x1 - cxp) / rx), th2 = Math.atan2((-y1 - cyp) / ry, (-x1 - cxp) / rx);
            double dth = th2 - th1;
            if (sweep && dth < 0) dth += 2 * Math.PI; else if (!sweep && dth > 0) dth -= 2 * Math.PI;
            Arc2D.Double a = new Arc2D.Double(-rx, -ry, 2 * rx, 2 * ry, -Math.toDegrees(th1), -Math.toDegrees(dth), Arc2D.OPEN);
            AffineTransform at = new AffineTransform(); at.translate(cxx, cyy); at.rotate(phi);
            p.append(at.createTransformedShape(a).getPathIterator(null), true);
        }
    }
}
