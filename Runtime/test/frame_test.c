/* Host test for the ABI v2 render/input path: translate + start the module, dump display lists to text files
 * (rendered to PNG by Runtime/test/render_ops.py), and drive real touch sequences by label.
 *   frame_test BUNDLE_DIR APK W H DENSITY OUTPREFIX [label|~label(long press)]...  */
#include "JetBridge.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
static void dump(const char *path) {
    const JetDrawOp *ops; uint32_t bg; int n = jet_runtime_frame(&ops, &bg);
    FILE *f = fopen(path, "w"); if (!f) { perror(path); exit(2); }
    fprintf(f, "bg %08x\n", bg);
    for (int i = 0; i < n; i++) {
        const JetDrawOp *o = &ops[i];
        fprintf(f, "%d %.1f %.1f %.1f %.1f %08x %.2f %d %d %.1f %.1f %.1f %.1f\t%s\n", o->op, o->x, o->y, o->w, o->h, o->argb, o->size,
                o->flags, o->gravity, o->clip_x, o->clip_y, o->clip_w, o->clip_h, o->text ? o->text : "");
    }
    fclose(f); printf("frame %s: %d ops\n", path, n);
}
/* optional metrics table (Runtime/test/render_ops.py --metrics): "asc desc" then 2x256 advances per 1 px of font
 * size (regular, bold) for Latin-1 code points, so layout matches the PIL renderer's font */
static float g_adv[2][256], g_asc = 0.93f, g_desc = 0.24f; static int g_have;
static void measure(const char *s, float px, int32_t bold, float *w, float *asc, float *desc) {
    float t = 0; const unsigned char *p = (const unsigned char *) s;
    while (*p) { unsigned c = *p++; if (c >= 0xc0 && (*p & 0xc0) == 0x80) { c = ((c & 0x1f) << 6) | (*p++ & 0x3f); while ((*p & 0xc0) == 0x80) p++; }
        t += c < 256 ? g_adv[bold ? 1 : 0][c] : 0.6f; }
    *w = t * px; *asc = g_asc * px; *desc = g_desc * px;
}
int main(int argc, char **argv) {
    const char *mf = getenv("JET_TEST_METRICS");
    if (mf) { FILE *f = fopen(mf, "r"); if (f && fscanf(f, "%f %f", &g_asc, &g_desc) == 2) { g_have = 1;
        for (int b = 0; b < 2; b++) for (int i = 0; i < 256; i++) if (fscanf(f, "%f", &g_adv[b][i]) != 1) g_have = 0; }
        if (f) fclose(f); if (g_have) jet_runtime_set_text_measurer(measure); }
    if (argc < 7) { fprintf(stderr, "usage: frame_test BUNDLE APK W H DENSITY OUT [labels]\n"); return 2; }
    printf("bridge %s abi %d\n", jet_bridge_version(), jet_bridge_abi_version());
    int rc = jet_translate_apk(argv[2], argv[1], NULL, NULL); printf("translate %d: %s\n", rc, jet_runtime_status()); if (rc) return 1;
    jet_runtime_set_viewport(atoi(argv[3]), atoi(argv[4]), (float) atof(argv[5]));
    rc = jet_runtime_main(argv[1], NULL, NULL); printf("main %d: %s\n", rc, jet_runtime_status()); if (rc) return 1;
    char p[4096]; snprintf(p, sizeof p, "%s_00.ops", argv[6]); dump(p);
    for (int i = 7; i < argc; i++) {
        const char *l = argv[i]; int lng = l[0] == '~'; if (lng) l++;
        float x, y; if (jet_runtime_find_label(l, &x, &y)) { printf("no view labelled %s\n", l); return 1; }
        int r0 = jet_runtime_touch(0, 0, x, y);
        if (i == argc - 1 && !lng) { snprintf(p, sizeof p, "%s_%02d_pressed.ops", argv[6], i - 6); dump(p); }
        int r4 = lng ? jet_runtime_touch(0, 4, x, y) : 0;
        int r1 = jet_runtime_touch(0, 1, x, y);
        printf("tap%s %s @%.0f,%.0f -> %d %d %d | %s\n", lng ? "(long)" : "", l, x, y, r0, r4, r1, jet_runtime_status());
        if (r1 < 0) return 1;
        snprintf(p, sizeof p, "%s_%02d.ops", argv[6], i - 6); dump(p);
    }
    char snap[8192]; jet_runtime_ui_snapshot(snap, sizeof snap); printf("--- snapshot\n%s", snap);
    return 0;
}
