package jet.runtime;

import android.database.Cursor;
import android.database.MatrixCursor;
import java.io.File;
import java.util.*;

/** MediaStore over the sandbox's emulated external storage (Environment.getExternalStorageDirectory()).
 *  Serves content://media/<volume>/{file,images/media,video/media,audio/media}[/id] with the standard columns,
 *  a small SQL-WHERE evaluator (=, !=, <>, <, <=, >, >=, LIKE, NOT LIKE, IN, IS [NOT] NULL, AND/OR, parentheses, ? args)
 *  and ORDER BY <col> [ASC|DESC] [LIMIT n]. Rows are computed by walking the directory tree (no indexer process). */
public final class JetMediaProvider {
    private JetMediaProvider() {}
    public static boolean handles(android.net.Uri u) { return u != null && "content".equals(u.getScheme()) && "media".equals(u.getAuthority()); }
    static final String[] ALL = {"_id", "_data", "_display_name", "title", "mime_type", "_size", "date_modified", "date_added", "datetaken", "width", "height",
        "duration", "orientation", "bucket_id", "bucket_display_name", "relative_path", "media_type", "parent", "volume_name", "is_favorite", "is_trashed", "is_pending", "is_drm", "owner_package_name", "album", "artist", "resolution"};

    public static Cursor query(android.net.Uri u, String[] proj, String sel, String[] args, String order) {
        List<String> seg = u.getPathSegments();
        String kind = seg.size() >= 2 ? seg.get(1) : "file";   // file | images | video | audio
        Long onlyId = null; if (!seg.isEmpty()) { String last = seg.get(seg.size() - 1); if (last.matches("\\d+")) onlyId = Long.parseLong(last); }
        File root = android.os.Environment.getExternalStorageDirectory();
        List<Map<String, Object>> rows = new ArrayList<>();
        walk(root, root, rows, 0);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int mt = (Integer) r.get("media_type");
            if (kind.equals("images") && mt != 1 || kind.equals("video") && mt != 3 || kind.equals("audio") && mt != 2) continue;
            if (onlyId != null && !onlyId.equals(r.get("_id"))) continue;
            try { if (sel != null && !sel.trim().isEmpty() && !new Where(sel, args).eval(r)) continue; } catch (RuntimeException e) { /* unparsable clause: keep row */ }
            out.add(r);
        }
        String lim = null;
        if (order != null && !order.trim().isEmpty()) {
            String o = order.trim(); int li = o.toUpperCase(Locale.ROOT).indexOf(" LIMIT "); if (li >= 0) { lim = o.substring(li + 7).trim(); o = o.substring(0, li); }
            String[] parts = o.split(",")[0].trim().split("\\s+"); final String col = parts[0]; final boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("DESC");
            out.sort((a, b) -> { int c = cmp(a.get(col), b.get(col)); return desc ? -c : c; });
        }
        if (lim != null) try { int n = Integer.parseInt(lim.split("[ ,]")[0]); if (out.size() > n) out = new ArrayList<>(out.subList(0, n)); } catch (NumberFormatException ignore) {}
        String[] cols = proj == null || proj.length == 0 ? ALL : proj;
        MatrixCursor c = new MatrixCursor(cols);
        for (Map<String, Object> r : out) {
            Object[] v = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                String k = cols[i].trim(); String kl = k.toLowerCase(Locale.ROOT);
                if (kl.startsWith("count(")) v[i] = out.size(); else if (kl.contains(" as ")) v[i] = r.get(k.substring(kl.lastIndexOf(" as ") + 4).trim()); else v[i] = r.get(k);
            }
            c.addRow(v);
        }
        System.err.println("[jet-media] " + u + " sel=" + sel + " -> " + out.size() + " rows");
        return c;
    }
    private static int cmp(Object a, Object b) {
        if (a == null || b == null) return a == null ? (b == null ? 0 : -1) : 1;
        if (a instanceof Number && b instanceof Number) return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        return a.toString().compareToIgnoreCase(b.toString());
    }
    private static void walk(File root, File dir, List<Map<String, Object>> rows, int depth) {
        File[] fs = dir.listFiles(); if (fs == null || depth > 12) return;
        Arrays.sort(fs);
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { walk(root, f, rows, depth + 1); continue; }
            rows.add(row(root, f));
        }
    }
    static Map<String, Object> row(File root, File f) {
        Map<String, Object> r = new HashMap<>();
        String name = f.getName(), mime = mime(name); File p = f.getParentFile();
        int mt = mime.startsWith("image/") ? 1 : mime.startsWith("video/") ? 3 : mime.startsWith("audio/") ? 2 : 0;
        long id = Math.abs((long) f.getAbsolutePath().hashCode()) + 1;
        String rel = root.toPath().relativize(p.toPath()).toString(); rel = rel.isEmpty() ? "" : rel + "/";
        r.put("_id", id); r.put("_data", f.getAbsolutePath()); r.put("_display_name", name);
        r.put("title", name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name); r.put("mime_type", mime);
        r.put("_size", f.length()); r.put("date_modified", f.lastModified() / 1000); r.put("date_added", f.lastModified() / 1000); r.put("datetaken", f.lastModified());
        r.put("orientation", 0); r.put("duration", 0L); r.put("bucket_id", (long) p.getAbsolutePath().toLowerCase(Locale.ROOT).hashCode());
        r.put("bucket_display_name", p.getName()); r.put("relative_path", rel); r.put("media_type", mt);
        r.put("parent", (long) p.getAbsolutePath().hashCode()); r.put("volume_name", "external_primary");
        r.put("is_favorite", 0); r.put("is_trashed", 0); r.put("is_pending", 0); r.put("is_drm", 0); r.put("owner_package_name", null);
        if (mt == 1) {
            try { byte[] b = java.nio.file.Files.readAllBytes(f.toPath()); int[] wh = android.graphics.BitmapFactory.jetBounds(b); if (wh != null) { r.put("width", wh[0]); r.put("height", wh[1]); r.put("resolution", wh[0] + "\u00d7" + wh[1]); } } catch (Exception ignore) {}
        }
        return r;
    }
    static String mime(String n) {
        String e = n.contains(".") ? n.substring(n.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        switch (e) {
            case "jpg": case "jpeg": return "image/jpeg"; case "png": return "image/png"; case "webp": return "image/webp"; case "gif": return "image/gif"; case "bmp": return "image/bmp";
            case "mp4": return "video/mp4"; case "webm": return "video/webm"; case "mkv": return "video/x-matroska";
            case "mp3": return "audio/mpeg"; case "ogg": return "audio/ogg"; case "flac": return "audio/flac"; case "wav": return "audio/x-wav"; case "m4a": return "audio/mp4";
            case "pdf": return "application/pdf"; case "txt": return "text/plain"; case "md": return "text/markdown"; case "html": return "text/html"; case "zip": return "application/zip";
            case "apk": return "application/vnd.android.package-archive"; case "json": return "application/json"; case "csv": return "text/csv";
            default: return "application/octet-stream";
        }
    }

    /** Recursive-descent WHERE evaluator: expr := and (OR and)* ; and := atom (AND atom)* ; atom := '(' expr ')' | NOT atom | col op value. */
    static final class Where {
        private final List<String> t = new ArrayList<>(); private int i; private final String[] args; private int ai;
        Where(String s, String[] a) {
            args = a == null ? new String[0] : a;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("'(?:[^']|'')*'|\\(|\\)|,|<=|>=|<>|!=|=|<|>|\\?|[^\\s(),=<>!?']+").matcher(s);
            while (m.find()) t.add(m.group());
        }
        boolean eval(Map<String, Object> r) { i = 0; ai = 0; return expr(r); }
        private String peek() { return i < t.size() ? t.get(i) : ""; }
        private boolean kw(String k) { if (peek().equalsIgnoreCase(k)) { i++; return true; } return false; }
        private boolean expr(Map<String, Object> r) { boolean v = and(r); while (kw("OR")) { boolean w = and(r); v = v || w; } return v; }
        private boolean and(Map<String, Object> r) { boolean v = atom(r); while (kw("AND")) { boolean w = atom(r); v = v && w; } return v; }
        private Object val() {
            String s = t.get(i++);
            if (s.equals("?")) return ai < args.length ? args[ai++] : null;
            if (s.startsWith("'")) return s.substring(1, s.length() - 1).replace("''", "'");
            if (s.equalsIgnoreCase("NULL")) return null;
            try { return s.contains(".") ? (Object) Double.parseDouble(s) : (Object) Long.parseLong(s); } catch (NumberFormatException e) { return s; }
        }
        private boolean atom(Map<String, Object> r) {
            if (kw("NOT")) return !atom(r);
            if (kw("(")) { boolean v = expr(r); kw(")"); return v; }
            String col = t.get(i++); Object cv = r.get(col.replace("`", "").replace("\"", ""));
            if (kw("IS")) { boolean not = kw("NOT"); kw("NULL"); return not == (cv != null); }
            boolean not = kw("NOT");
            if (kw("LIKE")) { Object pv = val(); kw("ESCAPE"); boolean m = like(cv, pv); return not != m; }
            if (kw("IN")) { kw("("); boolean m = false; while (!peek().equals(")") && i < t.size()) { if (peek().equals(",")) { i++; continue; } m |= cmp(cv, val()) == 0 && cv != null; } kw(")"); return not != m; }
            if (kw("BETWEEN")) { Object a = val(); kw("AND"); Object b = val(); boolean m = cmp(cv, a) >= 0 && cmp(cv, b) <= 0; return not != m; }
            String op = t.get(i++); Object v = val();
            if (cv == null || v == null) return false;
            Object c1 = cv, c2 = v;
            if (c1 instanceof Number && !(c2 instanceof Number)) try { c2 = Double.parseDouble(c2.toString()); } catch (NumberFormatException ignore) {}
            int c = cmp(c1, c2);
            switch (op) { case "=": return c == 0; case "!=": case "<>": return c != 0; case "<": return c < 0; case "<=": return c <= 0; case ">": return c > 0; case ">=": return c >= 0; default: return true; }
        }
        private static boolean like(Object v, Object p) {
            if (v == null || p == null) return false;
            StringBuilder re = new StringBuilder("(?is)");
            for (char ch : p.toString().toCharArray()) re.append(ch == '%' ? ".*" : ch == '_' ? "." : java.util.regex.Pattern.quote(String.valueOf(ch)));
            return v.toString().matches(re.toString());
        }
    }
}
