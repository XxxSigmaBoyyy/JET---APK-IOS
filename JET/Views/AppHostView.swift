import SwiftUI
import UIKit

/// UIKit surface the JET runtime renders into; forwards touches to the bridge in pixel coordinates.
final class JetSurfaceView: UIView {
    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        backgroundColor = UIColor(red: 0.55, green: 0.82, blue: 1.00, alpha: 1)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    private func send(_ touches: Set<UITouch>, _ action: Int32) {
        let scale = Float(contentScaleFactor)
        for t in touches {
            let p = t.location(in: self)
            JetBridge.touch(id: Int32(truncatingIfNeeded: ObjectIdentifier(t).hashValue), action: action,
                            x: Float(p.x) * scale, y: Float(p.y) * scale)
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, 0) }
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, 1) }
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, 2) }
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { send(touches, 3) }
}

/// JetBridge host: starts the bundle's Activity in a JetSurfaceView and stops it when dismantled.
struct JetHostView: UIViewRepresentable {
    let bundle: URL
    var activity: String = ""
    @Binding var running: Bool

    func makeUIView(context: Context) -> JetSurfaceView {
        let v = JetSurfaceView(frame: .zero)
        let ok = JetBridge.launch(bundle: bundle, activity: activity, in: Unmanaged.passUnretained(v).toOpaque())
        DispatchQueue.main.async { running = ok }
        return v
    }

    func updateUIView(_ uiView: JetSurfaceView, context: Context) {}

    static func dismantleUIView(_ uiView: JetSurfaceView, coordinator: ()) { JetBridge.stop() }
}

/// Renders the running Activity from jet_runtime_ui_snapshot (ABI v1 runtimes do not draw into the surface):
/// text views as glass rows, buttons as a glass key grid. Taps go to jet_runtime_click.
struct AotActivityView: View {
    let title: String
    @State private var nodes: [JetBridge.Node] = JetBridge.snapshot()
    @State private var lastError: String?

    private var texts: [JetBridge.Node] { nodes.filter { !$0.isButton && !$0.text.isEmpty } }
    private var buttons: [JetBridge.Node] { nodes.filter(\.isButton) }

    var body: some View {
        ZStack {
            GlassBackground()
            ScrollView {
                VStack(spacing: 18) {
                    Text(title)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(JetTheme.ink)
                        .padding(.top, 56)
                    VStack(spacing: 0) {
                        ForEach(texts) { n in
                            Text(n.text)
                                .font(n.selected ? .title2.monospacedDigit().weight(.semibold) : .body.monospacedDigit())
                                .foregroundStyle(n.selected ? AnyShapeStyle(JetTheme.accent) : AnyShapeStyle(JetTheme.ink.opacity(0.9)))
                                .frame(maxWidth: .infinity, alignment: n.viewID == 0 ? .leading : .trailing)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 6)
                        }
                    }
                    .padding(.vertical, 10)
                    .glassCard(cornerRadius: 22, material: .thinMaterial)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3), spacing: 12) {
                        ForEach(buttons) { b in
                            Button { tap(b.text) } label: {
                                Text(b.text)
                                    .font(.title2.weight(.medium))
                                    .foregroundStyle(JetTheme.ink)
                                    .frame(maxWidth: .infinity, minHeight: 58)
                                    .glassCard(cornerRadius: 18)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(b.text)
                        }
                    }
                    if let e = lastError {
                        Text(e).font(.footnote).foregroundStyle(Color(red: 0.80, green: 0.35, blue: 0.05)).multilineTextAlignment(.center)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 30)
                .frame(maxWidth: 520)
                .frame(maxWidth: .infinity)
            }
        }
    }

    private func tap(_ label: String) {
        let rc = JetBridge.click(label)
        lastError = rc == JET_BRIDGE_OK ? nil : JetBridge.status
        withAnimation(.easeOut(duration: 0.12)) { nodes = JetBridge.snapshot() }
    }
}

/// Fullscreen screen for one shortcut. Shows a glass placeholder while the runtime is not linked.
struct AppHostScreen: View {
    let shortcut: AppShortcut
    let icon: UIImage?
    let bundle: URL
    @Environment(\.dismiss) private var dismiss
    @State private var running = false

    var body: some View {
        ZStack {
            JetHostView(bundle: bundle, running: $running)
                .ignoresSafeArea()

            if running {
                AotActivityView(title: shortcut.name)
            } else {
                GlassBackground()
                placeholder
            }
        }
        .overlay(alignment: .topLeading) {
            GlassCircleButton(systemImage: "xmark", size: 38) { dismiss() }
                .padding(.leading, 16)
                .padding(.top, 8)
                .accessibilityLabel("Close")
        }
        .preferredColorScheme(.light)
    }

    private var placeholder: some View {
        VStack(spacing: 16) {
            ShortcutTile(shortcut: shortcut, icon: icon, iconSize: 92)
            Text(!JetBridge.hasRuntime ? "Runtime not linked" : shortcut.isTranslated ? "Could not start" : "No prebuilt module")
                .font(.title3.weight(.semibold))
                .foregroundStyle(JetTheme.ink)
            Text(JetBridge.status)
                .font(.subheadline)
                .foregroundStyle(JetTheme.inkSoft)
                .multilineTextAlignment(.center)
            VStack(alignment: .leading, spacing: 6) {
                detail("doc.zipper", "\(shortcut.sourceFileName)")
                detail("cpu", shortcut.nativeABIs.isEmpty ? "No native libraries" : shortcut.nativeABIs.joined(separator: ", "))
                detail("curlybraces", "\(shortcut.dexCount) dex file\(shortcut.dexCount == 1 ? "" : "s")")
                detail("link", "Bridge \(shortcut.bridgeVersion)")
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassCard(cornerRadius: 18, material: .thinMaterial)
        }
        .padding(26)
        .frame(maxWidth: 420)
        .glassCard(cornerRadius: 30)
        .padding(.horizontal, 24)
    }

    private func detail(_ symbol: String, _ text: String) -> some View {
        Label {
            Text(text).lineLimit(1).truncationMode(.middle)
        } icon: {
            Image(systemName: symbol).foregroundStyle(JetTheme.skyDeep)
        }
        .font(.footnote)
        .foregroundStyle(JetTheme.ink.opacity(0.85))
    }
}
