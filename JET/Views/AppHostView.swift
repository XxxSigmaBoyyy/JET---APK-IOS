import SwiftUI
import UIKit

/// UIKit surface the JET runtime renders into (ABI v2). The runtime lays the translated Activity out with Android's
/// measure/layout rules in pixels and returns a display list; this view draws it with CoreGraphics and forwards
/// touches (down / move / up / cancel, plus the 500 ms long-press timeout of android.view.ViewConfiguration).
final class JetSurfaceView: UIView {
    /// Called on the main thread when the running app stops with an error (status text from the bridge).
    var onRuntimeError: ((String) -> Void)?
    /// Called after every completed draw (smoke tests wait on it).
    var onDraw: (() -> Void)?

    private var activeTouch: UITouch?
    private var longPressTimer: Timer?
    private var wakeupTimer: Timer?
    private var lastViewport: (Int32, Int32) = (0, 0)
    private var scale: CGFloat { window?.screen.scale ?? UIScreen.main.scale }

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = false
        isOpaque = true
        contentMode = .redraw
        backgroundColor = .black
        JetBridge.installTextMeasurer()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    deinit { longPressTimer?.invalidate(); wakeupTimer?.invalidate() }

    override func layoutSubviews() {
        super.layoutSubviews()
        let w = Int32((bounds.width * scale).rounded()), h = Int32((bounds.height * scale).rounded())
        guard w > 0, h > 0, (w, h) != lastViewport else { return }
        lastViewport = (w, h)
        // 1 iOS point ~ 1 Android dip (both ~160 per inch), so the Android density is the screen scale.
        JetBridge.setViewport(width: w, height: h, density: Float(scale))
        setNeedsDisplay()
    }

    override func draw(_ rect: CGRect) {
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        let frame = JetBridge.frame()
        let s = scale
        ctx.setFillColor(Self.color(frame.background).cgColor)
        ctx.fill(bounds)
        ctx.saveGState()
        ctx.scaleBy(x: 1 / s, y: 1 / s)       // runtime coordinates are pixels
        for op in frame.ops {
            ctx.saveGState()
            ctx.clip(to: CGRect(x: CGFloat(op.clip_x), y: CGFloat(op.clip_y), width: CGFloat(op.clip_w), height: CGFloat(op.clip_h)))
            let r = CGRect(x: CGFloat(op.x), y: CGFloat(op.y), width: CGFloat(op.w), height: CGFloat(op.h))
            switch op.op {
            case JET_OP_RECT:
                ctx.setFillColor(Self.color(op.argb).cgColor)
                ctx.fill(r)
            case JET_OP_RRECT:
                ctx.setFillColor(Self.color(op.argb).cgColor)
                ctx.addPath(UIBezierPath(roundedRect: r, cornerRadius: CGFloat(op.size)).cgPath)
                ctx.fillPath()
            case JET_OP_TEXT:
                guard let p = op.text else { break }
                Self.drawText(String(cString: p), in: r, size: CGFloat(op.size), bold: op.flags & JET_TEXT_BOLD != 0,
                              color: Self.color(op.argb), gravity: op.gravity)
            default:
                break
            }
            ctx.restoreGState()
        }
        ctx.restoreGState()
        scheduleWakeup()
        onDraw?()
    }

    /// Android gravity inside the text box: horizontal LEFT 3 / CENTER_HORIZONTAL 1 / RIGHT 5 (START/END map to
    /// left/right), vertical TOP 0x30 / CENTER_VERTICAL 0x10 / BOTTOM 0x50.
    private static func drawText(_ text: String, in r: CGRect, size: CGFloat, bold: Bool, color: UIColor, gravity: Int32) {
        let font = JetBridge.font(size: size, bold: bold)
        let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: color]
        let lines = text.split(separator: "\n", omittingEmptySubsequences: false).map(String.init)
        let lineH = font.ascender - font.descender
        let totalH = lineH * CGFloat(lines.count)
        let vg = gravity & 0x70, hg = gravity & 0x07
        var y = r.minY
        if vg == 0x10 { y = r.minY + (r.height - totalH) / 2 } else if vg == 0x50 { y = r.maxY - totalH }
        for line in lines {
            let w = (line as NSString).size(withAttributes: attrs).width
            var x = r.minX
            if hg == 1 { x = r.minX + (r.width - w) / 2 } else if hg == 5 { x = r.maxX - w }
            // NSString draws with the line's top at the point (ascender above the baseline)
            (line as NSString).draw(at: CGPoint(x: x, y: y + (lineH - font.lineHeight) / 2), withAttributes: attrs)
            y += lineH
        }
    }

    private static func color(_ argb: UInt32) -> UIColor {
        UIColor(red: CGFloat((argb >> 16) & 0xff) / 255, green: CGFloat((argb >> 8) & 0xff) / 255,
                blue: CGFloat(argb & 0xff) / 255, alpha: CGFloat(argb >> 24) / 255)
    }

    private func scheduleWakeup() {
        wakeupTimer?.invalidate(); wakeupTimer = nil
        let ms = JetBridge.nextWakeupMs
        guard ms >= 0 else { return }
        wakeupTimer = Timer.scheduledTimer(withTimeInterval: Double(ms) / 1000, repeats: false) { [weak self] _ in
            guard let view = self else { return }
            Task { @MainActor in view.setNeedsDisplay() }
        }
    }

    // MARK: input

    /// Sends one action at a point in view coordinates (points); returns the bridge code.
    @discardableResult
    func send(action: Int32, at p: CGPoint) -> Int32 {
        let rc = JetBridge.touch(id: 0, action: action, x: Float(p.x * scale), y: Float(p.y * scale))
        if rc != 0 { setNeedsDisplay() }
        if rc < 0 { cancelLongPress(); activeTouch = nil; onRuntimeError?(JetBridge.status) }
        return rc
    }

    private func cancelLongPress() { longPressTimer?.invalidate(); longPressTimer = nil }

    func pressBegan(at p: CGPoint) {
        send(action: 0, at: p)
        cancelLongPress()
        longPressTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: false) { [weak self] t in
            guard let view = self else { return }
            let id = ObjectIdentifier(t)
            Task { @MainActor in view.longPressFired(id, at: p) }
        }
    }
    private func longPressFired(_ id: ObjectIdentifier, at p: CGPoint) {
        guard let t = longPressTimer, ObjectIdentifier(t) == id else { return }
        longPressTimer = nil
        send(action: 4, at: p)
    }
    func pressMoved(to p: CGPoint) { send(action: 2, at: p) }
    func pressEnded(at p: CGPoint) { cancelLongPress(); send(action: 1, at: p) }
    func pressCancelled(at p: CGPoint) { cancelLongPress(); send(action: 3, at: p) }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard activeTouch == nil, let t = touches.first else { return }
        activeTouch = t
        pressBegan(at: t.location(in: self))
    }
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = activeTouch, touches.contains(t) else { return }
        pressMoved(to: t.location(in: self))
    }
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = activeTouch, touches.contains(t) else { return }
        activeTouch = nil
        pressEnded(at: t.location(in: self))
    }
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = activeTouch, touches.contains(t) else { return }
        activeTouch = nil
        pressCancelled(at: t.location(in: self))
    }

    /// Point (view coordinates) of the clickable view labelled `label`, for automation.
    func point(forLabel label: String) -> CGPoint? {
        guard let p = JetBridge.find(label: label) else { return nil }
        return CGPoint(x: p.x / scale, y: p.y / scale)
    }
}

/// JetBridge host: starts the bundle's Activity, shows it in a JetSurfaceView and stops it when dismantled.
struct JetHostView: UIViewRepresentable {
    let bundle: URL
    var activity: String = ""
    @Binding var running: Bool
    @Binding var error: String?

    func makeUIView(context: Context) -> JetSurfaceView {
        let v = JetSurfaceView(frame: .zero)
        let ok = JetBridge.launch(bundle: bundle, activity: activity, in: nil)
        let status = JetBridge.status
        v.onRuntimeError = { msg in running = false; error = msg }
        Task { @MainActor in
            running = ok
            error = ok ? nil : status
            if ok { SmokeTest.shared.attach(v) }
        }
        return v
    }

    func updateUIView(_ uiView: JetSurfaceView, context: Context) {}

    static func dismantleUIView(_ uiView: JetSurfaceView, coordinator: ()) { JetBridge.stop() }
}

/// Fullscreen screen for one shortcut: a slim bar (name + close) above the app's own window.
struct AppHostScreen: View {
    let shortcut: AppShortcut
    let icon: UIImage?
    let bundle: URL
    @Environment(\.dismiss) private var dismiss
    @State private var running = false
    @State private var error: String?

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Button { dismiss() } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 34, height: 34)
                        .background(Circle().fill(Color.white.opacity(0.16)))
                }
                .accessibilityLabel("Close")
                if let icon { Image(uiImage: icon).resizable().frame(width: 22, height: 22).clipShape(RoundedRectangle(cornerRadius: 5)) }
                Text(shortcut.name).font(.subheadline.weight(.semibold)).foregroundStyle(.white).lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.black)

            ZStack {
                JetHostView(bundle: bundle, running: $running, error: $error)
                if !running, error != nil || !shortcut.isTranslated {
                    GlassBackground()
                    placeholder
                }
            }
        }
        .background(Color.black.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .statusBarHidden(false)
    }

    private var placeholder: some View {
        VStack(spacing: 16) {
            ShortcutTile(shortcut: shortcut, icon: icon, iconSize: 92)
            Text(shortcut.isTranslated ? "The app stopped" : "No prebuilt module")
                .font(.title3.weight(.semibold))
                .foregroundStyle(JetTheme.ink)
            Text(error ?? JetBridge.status)
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
        .environment(\.colorScheme, .light)
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
