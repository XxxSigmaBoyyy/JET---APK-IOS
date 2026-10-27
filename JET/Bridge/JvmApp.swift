import Foundation
import UIKit
import SwiftUI

/// Stock-APK path on the embedded no-JIT JVM: the APK's DEX is translated per class at run time (jet.dex.DexClassLoader)
/// and runs against the Android shim; the shim's frame is drawn by `JvmAppSurface`. No per-app code is built in.
enum JvmApp {
    static var dataDir: URL { JetJVM.appJars.appendingPathComponent("data", isDirectory: true) }
    static var appsRoot: URL {
        FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask)[0].appendingPathComponent("jet-apps", isDirectory: true)
    }

    /// JVM options for the shim + run-time DEX loading (no bytecode verification for translated app classes, like ART
    /// trusting dex2oat output; SDK model for framework classes the shim lacks; Java2D software canvas).
    static var options: [String] {
        ["-XX:+UnlockDiagnosticVMOptions", "-XX:-BytecodeVerificationRemote",
         "-Djet.android.model=\(dataDir.appendingPathComponent("android-model.jar").path)",
         "-Djet.gfx=java2d", "-Djet.fw.sdk=28"] + opens
    }

    /// ART has no module system; stock apps (and their desugar runtime, j$.util.DesugarCollections) reflect into
    /// java.base internals, so open the packages they touch to the class-path (unnamed) module.
    static let opens: [String] = ["java.lang", "java.lang.reflect", "java.lang.invoke", "java.util", "java.util.concurrent",
                                  "java.util.concurrent.atomic", "java.io", "java.nio", "java.net", "java.text", "java.time",
                                  "sun.nio.ch"].map { "--add-opens=java.base/\($0)=ALL-UNNAMED" }

    static func boot(log: URL?) -> (ok: Bool, ms: Double, error: String) {
        let jars = (try? FileManager.default.contentsOfDirectory(at: JetJVM.appJars, includingPropertiesForKeys: nil))?
            .filter { $0.pathExtension == "jar" }.sorted { $0.path < $1.path } ?? []
        return JetJVM.boot(classpath: jars, options: options, log: log)
    }

    /// Import: copy + convert resources into appDir/bundle, extract arm64 .so files. Returns the host's key=value text.
    static func importAPK(_ apk: URL, into appDir: URL) -> (ok: Bool, out: String) {
        let r = JetJVM.callString("jet/ios/JetIosHost", "importApk",
                                  [apk.path, appDir.path, dataDir.appendingPathComponent("fw").path].joined(separator: "\n"))
        return (r.ok && r.out.hasPrefix("ok"), r.out)
    }

    /// Start the launcher Activity; blocks until the first frame (or failure/timeout). Returns the host report.
    static func launch(_ appDir: URL, width: Int, height: Int, timeoutMs: Int = 600_000) -> (ok: Bool, out: String) {
        let r = JetJVM.callString("jet/ios/JetIosHost", "launch", "\(appDir.path)\n\(width)\n\(height)\n\(timeoutMs)")
        return (r.ok && r.out.hasPrefix("state=running"), r.out)
    }

    static func screenshot(_ png: URL) -> String { JetJVM.callString("jet/ios/JetIosHost", "screenshot", png.path).out }

    static func value(_ key: String, in report: String) -> String? {
        report.split(separator: "\n").first { $0.hasPrefix(key + "=") }.map { String($0.dropFirst(key.count + 1)) }
    }
}

/// Polls the shim frame on one long-lived JVM-attached thread and delivers touches on the same thread.
final class JvmFramePump: NSObject {
    var onImage: ((CGImage, Int32) -> Void)?
    private var thread: Thread?
    private let lock = NSLock()
    private var pending: [(Int32, Float, Float)] = []
    private var running = false

    func start() {
        guard thread == nil else { return }
        running = true
        let t = Thread { [weak self] in self?.loop() }
        t.name = "jet-frame-pump"
        t.stackSize = 8 << 20
        thread = t
        t.start()
    }
    func stop() { lock.lock(); running = false; lock.unlock() }

    func touch(_ action: Int32, _ p: CGPoint) {
        lock.lock(); pending.append((action, Float(p.x), Float(p.y))); lock.unlock()
    }

    private func loop() {
        var cap = 0
        var buf = [UInt32]()
        var seq: Int32 = 0
        while true {
            lock.lock(); let go = running; let ts = pending; pending.removeAll(); lock.unlock()
            if !go { break }
            for t in ts { _ = jet_jvm_touch(t.0, t.1, t.2) }
            var w: Int32 = 0, h: Int32 = 0, s = seq
            var rc = buf.withUnsafeMutableBufferPointer { jet_jvm_frame($0.baseAddress, Int32(cap), &w, &h, &s) }
            if rc == -7 {
                cap = Int(w) * Int(h); buf = [UInt32](repeating: 0, count: cap)
                rc = buf.withUnsafeMutableBufferPointer { jet_jvm_frame($0.baseAddress, Int32(cap), &w, &h, &s) }
            }
            if rc == 1, let img = Self.image(buf, Int(w), Int(h)) { seq = s; onImage?(img, s) }
            Thread.sleep(forTimeInterval: ts.isEmpty ? 0.016 : 0.004)
        }
    }

    private static func image(_ px: [UInt32], _ w: Int, _ h: Int) -> CGImage? {
        let data = px.withUnsafeBufferPointer { Data(buffer: UnsafeBufferPointer(start: $0.baseAddress, count: w * h)) }
        guard let provider = CGDataProvider(data: data as CFData) else { return nil }
        let info = CGBitmapInfo(rawValue: CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.noneSkipFirst.rawValue)
        return CGImage(width: w, height: h, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: w * 4,
                       space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: info, provider: provider,
                       decode: nil, shouldInterpolate: true, intent: .defaultIntent)
    }
}

/// CGImage view of the shim's frame (aspect-fit); UIKit touches -> MotionEvents in frame pixels.
final class JvmAppSurface: UIView {
    private let pump = JvmFramePump()
    private(set) var frameSize = CGSize.zero
    private(set) var seq: Int32 = 0
    var onFrame: ((Int32) -> Void)?

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isMultipleTouchEnabled = false
        layer.contentsGravity = .resizeAspect
        pump.onImage = { [weak self] img, s in
            DispatchQueue.main.async {
                guard let self else { return }
                self.layer.contents = img
                self.frameSize = CGSize(width: img.width, height: img.height)
                self.seq = s
                self.onFrame?(s)
            }
        }
        pump.start()
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) unused") }
    deinit { pump.stop() }

    /// View point -> frame pixel (inverse of .resizeAspect), nil outside the image.
    func framePoint(_ p: CGPoint) -> CGPoint? {
        guard frameSize.width > 0, bounds.width > 0 else { return nil }
        let k = min(bounds.width / frameSize.width, bounds.height / frameSize.height)
        let ox = (bounds.width - frameSize.width * k) / 2, oy = (bounds.height - frameSize.height * k) / 2
        let x = (p.x - ox) / k, y = (p.y - oy) / k
        guard x >= 0, y >= 0, x < frameSize.width, y < frameSize.height else { return nil }
        return CGPoint(x: x, y: y)
    }

    /// Frame-pixel tap (smoke test): same queue as real touches.
    func tap(framePixel p: CGPoint) { pump.touch(0, p); pump.touch(1, p) }

    private func send(_ action: Int32, _ touches: Set<UITouch>) {
        guard let t = touches.first, let p = framePoint(t.location(in: self)) else { return }
        pump.touch(action, p)
    }
    override func touchesBegan(_ t: Set<UITouch>, with e: UIEvent?) { send(0, t) }
    override func touchesMoved(_ t: Set<UITouch>, with e: UIEvent?) { send(2, t) }
    override func touchesEnded(_ t: Set<UITouch>, with e: UIEvent?) { send(1, t) }
    override func touchesCancelled(_ t: Set<UITouch>, with e: UIEvent?) { send(3, t) }
}

/// SwiftUI host for a running JVM app (full screen).
struct JvmAppScreen: UIViewRepresentable {
    let surface: JvmAppSurface
    func makeUIView(context: Context) -> JvmAppSurface { surface }
    func updateUIView(_ v: JvmAppSurface, context: Context) {}
}

/// Identifiable wrapper so HomeView can present it with .fullScreenCover(item:).
struct JvmAppPresentation: Identifiable {
    let id = UUID()
    let surface: JvmAppSurface
}
