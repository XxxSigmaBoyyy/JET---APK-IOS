import SwiftUI
import UIKit

/// Launch-argument driven smoke test (CI simulator job, or a device via Xcode's scheme arguments). It goes through
/// the same path a user does: import the APK -> home shortcut -> open it -> real touch events on the surface.
///
///   -JETSmokeAPK <path>          APK to import (relative paths are resolved against the app's home directory)
///   -JETSmokeTaps "1,2,0,~TC"    labels to tap in order; "~label" = long press (700 ms)
///
///   -JETSmokeJVM hello           boot the embedded no-JIT JVM and run dev.jet.Hello (M2 step 1); no APK needed
///
/// Writes Documents/jet-smoke/{NN_<label>.png, log.txt, result.txt}. Inactive without -JETSmokeAPK / -JETSmokeJVM.
@MainActor
final class SmokeTest {
    static let shared = SmokeTest()

    let apk: URL?
    let taps: [String]
    let jvmMode: String?
    let outDir: URL
    private var started = false
    private var attached = false
    private var log: [String] = []

    private init() {
        let args = UserDefaults.standard
        if let p = args.string(forKey: "JETSmokeAPK"), !p.isEmpty {
            apk = p.hasPrefix("/") ? URL(fileURLWithPath: p) : URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent(p)
        } else {
            apk = nil
        }
        jvmMode = args.string(forKey: "JETSmokeJVM").flatMap { $0.isEmpty ? nil : $0 }
        taps = (args.string(forKey: "JETSmokeTaps") ?? "").split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        outDir = docs.appendingPathComponent("jet-smoke", isDirectory: true)
    }

    var isActive: Bool { apk != nil }
    var isJVMActive: Bool { jvmMode != nil }

    /// JVM smoke: boot the bundled JRE, run dev.jet.Hello, show the output full screen and capture it.
    func runJVM(show: @escaping (String, String) -> Void) async {
        guard let mode = jvmMode, !started else { return }
        started = true
        try? FileManager.default.removeItem(at: outDir)
        try? FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
        let jvmLog = outDir.appendingPathComponent("jvm.log")
        note("jvm mode=\(mode) compiled=\(jet_jvm_compiled()) bundled=\(JetJVM.isBundled) home=\(JetJVM.javaHome.path)")
        let jars = (try? FileManager.default.contentsOfDirectory(at: JetJVM.appJars, includingPropertiesForKeys: nil))?
            .filter { $0.pathExtension == "jar" }.sorted { $0.path < $1.path } ?? []
        note("classpath \(jars.map(\.lastPathComponent))")
        let t0 = ProcessInfo.processInfo.systemUptime
        let boot = await Task.detached { JetJVM.boot(classpath: jars, log: jvmLog) }.value
        note(String(format: "boot ok=%@ JNI_CreateJavaVM=%.1f ms err=%@", boot.ok ? "yes" : "no", boot.ms, boot.error))
        guard boot.ok else {
            show("JVM boot FAILED", boot.error + "\n\n" + ((try? String(contentsOf: jvmLog)) ?? ""))
            try? await Task.sleep(nanoseconds: 1_200_000_000)
            captureKeyWindow("00_jvm"); finish("FAIL boot: \(boot.error)"); return
        }
        let t1 = ProcessInfo.processInfo.systemUptime
        let r = await Task.detached { JetJVM.callString("dev/jet/Hello", "run", "JET smoke \(mode)") }.value
        let t2 = ProcessInfo.processInfo.systemUptime
        note(String(format: "Hello.run ok=%@ in %.1f ms (boot+call %.1f ms)", r.ok ? "yes" : "no", (t2 - t1) * 1000, (t2 - t0) * 1000))
        for line in r.out.split(separator: "\n") { note("java> \(line)") }
        show(r.ok ? String(format: "JVM (Zero, no JIT) — boot %.0f ms", boot.ms) : "JVM call FAILED", r.out)
        try? await Task.sleep(nanoseconds: 1_200_000_000)
        captureKeyWindow("00_jvm_hello")
        finish(r.ok && r.out.contains("Hello from Java") ? "PASS" : "FAIL \(r.out.prefix(200))")
    }

    private func captureKeyWindow(_ name: String) {
        let w = UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow }.first
        if let w { capture(w, name) } else { note("capture \(name): no key window") }
    }

    private func note(_ s: String) {
        let line = "\(String(format: "%.2f", ProcessInfo.processInfo.systemUptime)) \(s)"
        log.append(line)
        NSLog("[JETSmoke] %@", s)
        try? (log.joined(separator: "\n") + "\n").write(to: outDir.appendingPathComponent("log.txt"), atomically: true, encoding: .utf8)
    }

    private func finish(_ result: String) {
        note("RESULT \(result)")
        try? (result + "\n").write(to: outDir.appendingPathComponent("result.txt"), atomically: true, encoding: .utf8)
    }

    /// Step 1 (HomeView.task): import the APK and open its shortcut.
    func start(store: ShortcutStore, importer: ImportCoordinator, open: @escaping (AppShortcut) -> Void) async {
        guard let apk, !started else { return }
        started = true
        try? FileManager.default.removeItem(at: outDir)
        try? FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
        note("bridge \(JetBridge.version) abi \(JetBridge.abiVersion) modules \(JetBridge.moduleCount)")
        note("import \(apk.path) exists=\(FileManager.default.fileExists(atPath: apk.path))")
        let before = Set(importer.jobs.map(\.id))
        importer.importAPKs([apk], into: store)
        guard let job = importer.jobs.first(where: { !before.contains($0.id) }) else { finish("FAIL no import job"); return }
        for _ in 0..<600 where !job.isFinished { try? await Task.sleep(nanoseconds: 50_000_000) }
        guard let s = job.result else { finish("FAIL import: \(job.error ?? "timeout")"); return }
        note("imported \(s.name) bridgeResult=\(s.bridgeResult) status=\(JetBridge.status)")
        guard s.isTranslated else { finish("FAIL not translated: \(JetBridge.status)"); return }
        try? await Task.sleep(nanoseconds: 600_000_000)
        open(s)
    }

    /// Step 2 (JetHostView): the runtime is running in `view`; tap through the script and capture the window.
    func attach(_ view: JetSurfaceView) {
        guard isActive, !attached else { return }
        attached = true
        note("running: \(JetBridge.status)")
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            capture(view, "00_first_screen")
            for (i, raw) in taps.enumerated() {
                let long = raw.hasPrefix("~")
                let label = long ? String(raw.dropFirst()) : raw
                guard let p = view.point(forLabel: label) else { finish("FAIL no view labelled \(label)"); return }
                view.pressBegan(at: p)
                try? await Task.sleep(nanoseconds: long ? 700_000_000 : 90_000_000)
                view.pressEnded(at: p)
                try? await Task.sleep(nanoseconds: 450_000_000)
                let tree = JetBridge.snapshot().filter { !$0.isButton && $0.viewID != 0 }.map { ($0.selected ? "*" : "") + $0.text }
                note("tap\(long ? "(long)" : "") \(label) @\(Int(p.x)),\(Int(p.y)) -> values [\(tree.joined(separator: " "))] status=\(JetBridge.status)")
                capture(view, String(format: "%02d_%@", i + 1, label.replacingOccurrences(of: "/", with: "_")))
            }
            finish(JetBridge.status.hasPrefix("Running") ? "PASS" : "FAIL \(JetBridge.status)")
        }
    }

    private func capture(_ view: UIView, _ name: String) {
        guard let window = (view as? UIWindow) ?? view.window else { note("capture \(name): no window"); return }
        let img = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        if let png = img.pngData() {
            try? png.write(to: outDir.appendingPathComponent("\(name).png"))
            note("captured \(name).png")
        }
    }
}
