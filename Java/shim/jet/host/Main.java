package jet.host;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import jet.runtime.ResTable;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * Headless JET host for Linux: boots the translated app's launcher Activity, lays it out
 * on a 375x667 pt @2x surface (iPhone SE/8 size), runs a scripted session and writes PNGs.
 * Script commands: shot:NAME | tap:ID_NAME | longtap:ID_NAME | tapxy:X,Y | recreate
 * The iOS host replaces this with UIKit (touch events -> hitTest, CALayer/Metal drawing).
 */
public final class Main {
    static final int W = 750, H = 1334; static final float DENSITY = 2f;
    static Activity act; static ResTable table; static String mainActivity; static File out;

    public static void main(String[] args) throws Exception {
        File bundle = new File(args[0]); out = new File(args[1]); out.mkdirs();
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(new File(bundle, "app.properties")), "UTF-8")) { p.load(r); }
        mainActivity = p.getProperty("mainActivity");
        System.out.println("JET host: " + p.getProperty("label") + " (" + p.getProperty("package") + ") -> " + mainActivity);
        long t0 = System.nanoTime();
        table = new ResTable(bundle, DENSITY);
        launch(null);
        System.out.printf("cold start (resources + onCreate + first layout): %.1f ms%n", (System.nanoTime() - t0) / 1e6);
        for (int i = 2; i < args.length; i++) run(args[i]);
    }

    static void launch(Bundle saved) throws Exception {
        act = (Activity) Class.forName(mainActivity).getConstructor().newInstance();
        act.jetAttach(new Resources(table));
        act.jetPerformCreate(saved);
    }

    static void run(String cmd) throws Exception {
        String[] kv = cmd.split(":", 2);
        View root = act.jetContent();
        switch (kv[0]) {
            case "shot": render(kv[1]); break;
            case "tap": case "longtap": {
                Integer id = table.idByName("id", kv[1]);
                View v = id == null ? null : root.findViewById(id);
                if (v == null) throw new RuntimeException("no view " + kv[1]);
                boolean ok = kv[0].equals("tap") ? v.performClick() : v.performLongClick();
                System.out.println(kv[0] + " " + kv[1] + " handled=" + ok);
                break;
            }
            case "tapxy": {
                String[] xy = kv[1].split(",");
                View v = ((ViewGroup) root).hitTest(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                System.out.println("tapxy " + kv[1] + " -> " + (v == null ? "nothing" : table.name(v.getId())) + (v != null && v.performClick() ? " handled" : ""));
                break;
            }
            case "recreate": {   // configuration change: save state, destroy, recreate with Bundle
                Bundle b = act.jetPerformSave();
                System.out.println("onSaveInstanceState -> " + b);
                Toast.current = null;
                launch(b);
                break;
            }
            default: throw new IllegalArgumentException(cmd);
        }
    }

    static void render(String name) throws Exception {
        View root = act.jetContent();
        root.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, W, H);
        jet.gfx.PixelBuffer img = new jet.gfx.PixelBuffer(W, H); img.opaque = true;
        jet.gfx.Gfx gx = android.graphics.JetGfx.forFrame(img);
        android.graphics.Canvas cv = new android.graphics.Canvas(gx);
        cv.drawColor(0xFF000000);   // Theme.NoTitleBar window background
        root.draw(cv);
        if (Toast.current != null) {
            android.graphics.Paint tp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG); tp.setTextSize(28);
            int tw = Math.round(tp.measureText(Toast.current)) + 48;
            tp.setColor(0xE0333333); cv.drawRoundRect((W - tw) / 2f, H - 200, (W + tw) / 2f, H - 136, 32, 32, tp);
            tp.setColor(0xFFFFFFFF); cv.drawText(Toast.current, (W - tw) / 2f + 24, H - 158, tp);
        }
        gx.dispose();
        File f = new File(out, name + ".png");
        android.graphics.Bitmap bm = android.graphics.Bitmap.jetWrap(img, false);
        try (java.io.FileOutputStream fo = new java.io.FileOutputStream(f)) { bm.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo); }
        System.out.println("frame -> " + f);
    }
}
