import SwiftUI
import UIKit

/// Host shell for one translated APK. The Android Activity is attached to `JetSurfaceView`.
@main
struct JetHostApp: App {
    var body: some Scene {
        WindowGroup { ContentView().ignoresSafeArea() }
    }
}

struct AppInfo {
    let props: [String: String]
    init() {
        var p: [String: String] = [:]
        if let url = Bundle.main.url(forResource: "app", withExtension: "properties", subdirectory: "JetBundle"),
           let text = try? String(contentsOf: url, encoding: .utf8) {
            for line in text.split(separator: "\n") {
                let kv = line.split(separator: "=", maxSplits: 1).map(String.init)
                if kv.count == 2 { p[kv[0]] = kv[1] }
            }
        }
        props = p
    }
    var bundlePath: String { (Bundle.main.resourcePath ?? "") + "/JetBundle" }
}

/// UIKit view the runtime renders into (CALayer/Metal in the real runtime) and that forwards touches.
final class JetSurfaceView: UIView {
    override init(frame: CGRect) { super.init(frame: frame); isMultipleTouchEnabled = true; backgroundColor = .black }
    required init?(coder: NSCoder) { fatalError() }
    private func send(_ touches: Set<UITouch>, _ action: Int32) {
        let scale = Float(contentScaleFactor)   // UIKit points -> Android px
        for t in touches {
            let p = t.location(in: self)
            jet_runtime_touch(Int32(truncatingIfNeeded: t.hash), action, Float(p.x) * scale, Float(p.y) * scale)
        }
    }
    override func touchesBegan(_ t: Set<UITouch>, with e: UIEvent?) { send(t, 0) }
    override func touchesEnded(_ t: Set<UITouch>, with e: UIEvent?) { send(t, 1) }
    override func touchesMoved(_ t: Set<UITouch>, with e: UIEvent?) { send(t, 2) }
    override func touchesCancelled(_ t: Set<UITouch>, with e: UIEvent?) { send(t, 3) }
}

struct SurfaceRepresentable: UIViewRepresentable {
    let info: AppInfo
    @Binding var status: String
    func makeUIView(context: Context) -> JetSurfaceView {
        let v = JetSurfaceView(frame: .zero)
        let rc = jet_runtime_main(info.bundlePath, info.props["mainActivity"] ?? "", Unmanaged.passUnretained(v).toOpaque())
        DispatchQueue.main.async { status = rc == 0 ? "" : String(cString: jet_runtime_status()) }
        return v
    }
    func updateUIView(_ v: JetSurfaceView, context: Context) {}
}

struct ContentView: View {
    private let info = AppInfo()
    @State private var status = ""
    var body: some View {
        ZStack {
            SurfaceRepresentable(info: info, status: $status)
            if !status.isEmpty {
                VStack(spacing: 12) {
                    Text(info.props["label"] ?? "?").font(.title).bold()
                    Text(info.props["package"] ?? "").font(.footnote.monospaced())
                    Text(status).font(.callout).multilineTextAlignment(.center)
                }
                .foregroundStyle(.white).padding()
            }
        }
        .onOpenURL { url in
            // jet://launch?pkg=<android package>  (home-screen shortcut / web clip entry point)
            _ = url
        }
    }
}
