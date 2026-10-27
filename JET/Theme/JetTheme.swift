import SwiftUI

/// Light sky / cloud colours shared by every screen (matches the JET app icon).
enum JetTheme {
    static let skyTop = Color(red: 0.72, green: 0.88, blue: 1.00)  // soft cloud
    static let skyMid = Color(red: 0.039, green: 0.482, blue: 0.992)  // #0A7BFD
    static let skyDeep = Color(red: 0.004, green: 0.322, blue: 0.996)  // #0152FE
    static let cloud = Color.white
    static let ink = Color(red: 0.10, green: 0.22, blue: 0.40)
    static let violet = skyDeep
    static let blue = skyMid
    static let cyan = skyTop
    static let inkSoft = Color(red: 0.24, green: 0.38, blue: 0.56)

    static let accent = LinearGradient(colors: [skyTop, skyDeep], startPoint: .topLeading, endPoint: .bottomTrailing)
    static let rim = LinearGradient(colors: [.white.opacity(0.85), .white.opacity(0.25), skyDeep.opacity(0.25)],
                                    startPoint: .topLeading, endPoint: .bottomTrailing)
}

/// Frosted translucent card on a light sky.
struct GlassCard: ViewModifier {
    var cornerRadius: CGFloat = 26
    var material: Material = .ultraThinMaterial

    func body(content: Content) -> some View {
        content
            .background(material, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(JetTheme.rim, lineWidth: 1)
            )
            .shadow(color: JetTheme.skyDeep.opacity(0.18), radius: 18, x: 0, y: 10)
    }
}

extension View {
    func glassCard(cornerRadius: CGFloat = 26, material: Material = .ultraThinMaterial) -> some View {
        modifier(GlassCard(cornerRadius: cornerRadius, material: material))
    }
}

struct GlassCircleButton: View {
    let systemImage: String
    var size: CGFloat = 40
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(JetTheme.ink)
                .frame(width: size, height: size)
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().strokeBorder(JetTheme.rim, lineWidth: 1))
                .shadow(color: JetTheme.skyDeep.opacity(0.22), radius: 10, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

/// Soft sky with drifting cloud blobs.
struct GlassBackground: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var drift = false

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            ZStack {
                LinearGradient(colors: [JetTheme.skyTop, JetTheme.skyMid, JetTheme.skyDeep.opacity(0.85)],
                               startPoint: .top, endPoint: .bottom)
                Circle().fill(Color.white.opacity(0.55))
                    .frame(width: w * 0.9).blur(radius: 60)
                    .offset(x: drift ? -w * 0.25 : -w * 0.08, y: drift ? -h * 0.18 : -h * 0.10)
                Circle().fill(Color.white.opacity(0.45))
                    .frame(width: w * 0.75).blur(radius: 70)
                    .offset(x: drift ? w * 0.30 : w * 0.15, y: drift ? h * 0.05 : h * 0.15)
                Circle().fill(Color.white.opacity(0.35))
                    .frame(width: w * 0.55).blur(radius: 55)
                    .offset(x: drift ? w * 0.05 : -w * 0.10, y: drift ? h * 0.38 : h * 0.30)
            }
            .frame(width: w, height: h)
        }
        .ignoresSafeArea()
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 10).repeatForever(autoreverses: true)) { drift = true }
        }
    }
}
