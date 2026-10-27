import SwiftUI
import UIKit

/// Launch-argument driven smoke test (CI simulator job, or a device via Xcode's scheme arguments). It goes through
/// the same path a user does: import the APK -> home shortcut -> open it -> real touch events on the surface.
///
///   -JETSmokeAPK <path>          APK to import (relative paths are resolved against the app's home directory)
///   -JETSmokeTaps "1,2,0,~TC"    labels to tap in order; "~label" = long press (700 ms)
///
/// Writes Documents/jet-smoke/{NN_<label>.png, log.txt, result.txt}. Inactive without -JETSmokeAPK.
@MainActor
final class SmokeTest {
    static let shared = SmokeTest()

    let apk: URL?
    let taps: [String]
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
        taps = (args.string(forKey: "JETSmokeTaps") ?? "").split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        outDir = docs.appendingPathComponent("jet-smoke", isDirectory: true)
    }

    var isActive: Bool { apk != nil }

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
        guard let window = view.window else { note("capture \(name): no window"); return }
        let img = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        if let png = img.pngData() {
            try? png.write(to: outDir.appendingPathComponent("\(name).png"))
            note("captured \(name).png")
        }
    }
}
