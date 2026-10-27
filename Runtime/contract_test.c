// JetBridge ABI v2 contract test: links the runtime sources (Runtime/*.c + one module) and checks the return-code /
// ownership rules of docs/RUNTIME_LINK.md, including the v2 render/touch path.
// Usage: contract_test WORKDIR [MATCHING_APK [CLICK...]]. Exit 0 = all checks passed. Run by scripts/test_runtime.sh.
#include "JetBridge.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

static int fails;
#define CHECK(c, ...) do { if (c) printf("  ok   " __VA_ARGS__); else { printf("  FAIL " __VA_ARGS__); fails++; } printf("\n"); } while (0)
static void prog(float f, const char *s, void *ctx) { (*(int *) ctx)++; (void) f; (void) s; }
static int has(const char *file, const char *needle) {
    FILE *f = fopen(file, "r"); if (!f) return 0; char b[8192]; size_t n = fread(b, 1, sizeof b - 1, f); b[n] = 0; fclose(f);
    return strstr(b, needle) != NULL;
}

int main(int argc, char **argv) {
    if (argc < 2) { fprintf(stderr, "usage: %s WORKDIR [MATCHING_APK [CLICK...]]\n", argv[0]); return 2; }
    const char *w = argv[1]; char bundle[4096], props[4200], junk[4200], other[4200];
    snprintf(bundle, sizeof bundle, "%s/bundle", w); mkdir(bundle, 0755);
    snprintf(props, sizeof props, "%s/app.properties", bundle);
    int rt = jet_bridge_has_runtime();
    printf("bridge %s abi %d runtime %d\n", jet_bridge_version(), jet_bridge_abi_version(), rt);
    CHECK(jet_bridge_abi_version() == JET_BRIDGE_ABI_VERSION, "abi version == %d", JET_BRIDGE_ABI_VERSION);
    CHECK(jet_bridge_modules() != NULL, "modules list is non-NULL");
    CHECK(jet_translate_apk(NULL, bundle, NULL, NULL) == JET_BRIDGE_ERR_INPUT, "NULL apk -> ERR_INPUT");
    CHECK(jet_translate_apk("x.apk", "", NULL, NULL) == JET_BRIDGE_ERR_INPUT, "empty bundle_dir -> ERR_INPUT");
    snprintf(junk, sizeof junk, "%s/not-an-apk.apk", w); FILE *f = fopen(junk, "w"); fputs("hello, not a zip\n", f); fclose(f);
    int32_t r = jet_translate_apk(junk, bundle, NULL, NULL);
    CHECK(rt && r == JET_BRIDGE_ERR_INPUT, "non-zip -> ERR_INPUT (got %d)", r);
    r = jet_translate_apk("/nonexistent/dir/a.apk", bundle, NULL, NULL);
    CHECK(r == JET_BRIDGE_ERR_IO, "missing apk -> ERR_IO (got %d)", r);
    snprintf(other, sizeof other, "%s/no-such-bundle-dir", w);
    if (argc > 2) {
        r = jet_translate_apk(argv[2], other, NULL, NULL);
        CHECK(r == JET_BRIDGE_ERR_IO, "unwritable bundle_dir -> ERR_IO (got %d)", r);
        int calls = 0; r = jet_translate_apk(argv[2], bundle, prog, &calls);
        const char *mods = jet_bridge_modules();
        printf("  translate -> %d (%s)\n", r, jet_runtime_status());
        CHECK(r == (*mods ? JET_BRIDGE_OK : JET_BRIDGE_ERR_UNSUPPORTED), "APK translate code");
        CHECK(calls >= 2, "progress called %d times", calls);
        CHECK(has(props, "jet.abi=2"), "app.properties written with jet.abi=2");
        r = jet_runtime_main(bundle, NULL, NULL);
        printf("  main -> %d (%s)\n", r, jet_runtime_status());
        if (!*mods) { CHECK(r == JET_BRIDGE_ERR_UNSUPPORTED, "runtime-only main -> ERR_UNSUPPORTED"); }
        else {
            CHECK(r == JET_BRIDGE_OK, "main -> OK");
            char small[8]; int32_t need = jet_runtime_ui_snapshot(small, sizeof small);
            CHECK(need > 0 && (int32_t) strlen(small) == (need < 7 ? need : 7), "snapshot truncates and reports full length (%d)", need);
            char *ui = malloc((size_t) need + 1); jet_runtime_ui_snapshot(ui, need + 1);
            CHECK(strstr(ui, "\tbutton\t") != NULL, "snapshot lists buttons");
            for (int i = 3; i < argc; i++) { r = jet_runtime_click(argv[i]); CHECK(r == JET_BRIDGE_OK, "click \"%s\" -> OK (%s)", argv[i], jet_runtime_status()); }
            CHECK(jet_runtime_click("\x01no-such-label") == JET_BRIDGE_ERR_INPUT, "click unknown label -> ERR_INPUT");
            need = jet_runtime_ui_snapshot(NULL, 0); free(ui); ui = malloc((size_t) need + 1); jet_runtime_ui_snapshot(ui, need + 1);
            printf("  --- view tree after clicks ---\n");
            for (char *l = strtok(ui, "\n"); l; l = strtok(NULL, "\n")) if (!strstr(l, "\tbutton\t")) printf("  | %s\n", l);
            free(ui);
            /* ABI v2: display list + real touch dispatch */
            jet_runtime_set_viewport(1179, 2400, 3.0f);
            const JetDrawOp *ops = NULL; uint32_t bg = 0; int32_t n = jet_runtime_frame(&ops, &bg);
            int texts = 0, rects = 0; for (int i = 0; i < n; i++) { texts += ops[i].op == JET_OP_TEXT; rects += ops[i].op == JET_OP_RECT; }
            CHECK(n > 0 && ops && texts > 0 && rects > 0, "frame -> %d ops (%d text, %d rect), window bg %08x", n, texts, rects, bg);
            int inside = 1; for (int i = 0; i < n; i++) if (ops[i].x < -1 || ops[i].y < -1 || ops[i].x + ops[i].w > 1180 || ops[i].y + ops[i].h > 2401) inside = 0;
            CHECK(inside, "all ops inside the viewport");
            float x = 0, y = 0; r = jet_runtime_find_label("CE", &x, &y);
            CHECK(r == JET_BRIDGE_OK && x > 0 && y > 0, "find_label(CE) -> %.0f,%.0f", x, y);
            CHECK(jet_runtime_touch(0, 0, x, y) == 1, "touch down on a button -> redraw");
            CHECK(jet_runtime_touch(0, 1, x, y) == 1, "touch up -> click + redraw (%s)", jet_runtime_status());
            CHECK(jet_runtime_touch(0, 0, 1, 1) >= 0 && jet_runtime_touch(0, 3, 1, 1) >= 0, "touch outside + cancel");
            CHECK(jet_runtime_find_label("\x01none", &x, &y) == JET_BRIDGE_ERR_INPUT, "find_label unknown -> ERR_INPUT");
            r = jet_runtime_main(bundle, "no.such.Activity", NULL);
            CHECK(r == JET_BRIDGE_ERR_INPUT, "unknown activity -> ERR_INPUT (got %d)", r);
            jet_runtime_stop(); jet_runtime_stop();
            CHECK(jet_runtime_click("7") == JET_BRIDGE_ERR_STATE, "click after stop -> ERR_STATE");
            CHECK(jet_runtime_ui_snapshot(NULL, 0) == 0, "snapshot after stop -> 0");
            r = jet_runtime_main(bundle, NULL, NULL);
            CHECK(r == JET_BRIDGE_OK, "restart main -> OK");
            jet_runtime_stop();
        }
    }
    CHECK(jet_runtime_touch(0, 0, 1, 1) == JET_BRIDGE_ERR_STATE, "touch with nothing running -> ERR_STATE");
    CHECK(jet_runtime_frame(NULL, NULL) == 0, "frame with nothing running -> 0");
    CHECK(jet_runtime_status() && *jet_runtime_status(), "status non-empty");
    printf("%s: %d failure(s)\n", fails ? "CONTRACT FAILED" : "CONTRACT OK", fails);
    return fails ? 1 : 0;
}
