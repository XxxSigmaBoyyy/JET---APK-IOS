import SwiftUI
import UIKit

/// Fighter-jet silhouette (nose up) in a unit square; the same outline is used for the app icon.
struct JetGlyph: Shape {
    /// Outline in a 100 x 100 box.
    private static let outline: [(CGFloat, CGFloat)] = [
        (50, 3), (53.5, 14), (55.2, 27), (56, 38), (64, 43), (91, 63), (91, 70), (58, 64.5),
        (58.5, 77), (73, 88.5), (73, 94), (56.5, 90), (54.5, 97), (45.5, 97), (43.5, 90),
        (27, 94), (27, 88.5), (41.5, 77), (42, 64.5), (9, 70), (9, 63), (36, 43), (44, 38),
        (44.8, 27), (46.5, 14)
    ]
    static let points: [CGPoint] = outline.map { CGPoint(x: $0.0, y: $0.1) }

    func path(in rect: CGRect) -> Path {
        let s = min(rect.width, rect.height) / 100
        let ox = rect.midX - 50 * s, oy = rect.midY - 50 * s
        var p = Path()
        for (i, pt) in Self.points.enumerated() {
            let q = CGPoint(x: ox + pt.x * s, y: oy + pt.y * s)
            if i == 0 { p.move(to: q) } else { p.addLine(to: q) }
        }
        p.closeSubpath()
        return p
    }
}

/// JET logo: the app icon artwork (BrandIcon, synced from Assets/AppIcon-1024.png) as a rounded tile
/// with a soft sky shadow. Falls back to the vector jet on a sky disc if the image is missing.
struct JetLogo: View {
    var size: CGFloat = 56

    var body: some View {
        Group {
            if UIImage(named: "BrandIcon") != nil {
                Image("BrandIcon")
                    .resizable()
                    .interpolation(.high)
                    .scaledToFill()
                    .clipShape(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
            } else {
                ZStack {
                    Circle().fill(JetTheme.accent)
                    JetGlyph().fill(Color.white).padding(size * 0.2).rotationEffect(.degrees(35))
                }
            }
        }
        .frame(width: size, height: size)
        .overlay(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous).strokeBorder(JetTheme.rim, lineWidth: 1))
        .shadow(color: JetTheme.skyDeep.opacity(0.35), radius: size * 0.22, x: 0, y: size * 0.08)
        .accessibilityHidden(true)
    }
}
