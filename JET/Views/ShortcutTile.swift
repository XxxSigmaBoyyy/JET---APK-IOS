import SwiftUI

/// Home-grid tile: APK icon (or generated monogram) on glass, name below.
struct ShortcutTile: View {
    let shortcut: AppShortcut
    let icon: UIImage?
    var iconSize: CGFloat = 68

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                if let icon {
                    Image(uiImage: icon)
                        .resizable()
                        .interpolation(.high)
                        .scaledToFill()
                } else {
                    MonogramIcon(name: shortcut.name)
                }
            }
            .frame(width: iconSize, height: iconSize)
            .clipShape(RoundedRectangle(cornerRadius: iconSize * 0.24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: iconSize * 0.24, style: .continuous)
                    .strokeBorder(JetTheme.rim, lineWidth: 1)
            )
            .shadow(color: JetTheme.skyDeep.opacity(0.30), radius: 12, x: 0, y: 6)
            .overlay(alignment: .bottomTrailing) {
                if !shortcut.isTranslated {
                    Image(systemName: "hammer.circle.fill")
                        .font(.system(size: 18))
                        .symbolRenderingMode(.palette)
                        .foregroundStyle(.white, JetTheme.skyDeep)
                        .offset(x: 5, y: 5)
                        .accessibilityLabel(shortcut.isUnsupported ? "No prebuilt module in this build" : "Preview — runtime not linked")
                }
            }

            Text(shortcut.name)
                .font(.caption.weight(.medium))
                .foregroundStyle(JetTheme.ink)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: iconSize + 18)
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

/// Fallback icon: first letter on a sky-blue glass tile, hue derived from the name.
struct MonogramIcon: View {
    let name: String

    private var hueShift: Double {
        let sum = name.unicodeScalars.reduce(0) { ($0 &* 31 &+ Int($1.value)) & 0xFFFF }
        return Double(sum % 60) - 30
    }

    var body: some View {
        ZStack {
            JetTheme.accent.hueRotation(.degrees(hueShift))
            Rectangle().fill(.ultraThinMaterial).opacity(0.35)
            Text(String(name.prefix(1)).uppercased())
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .shadow(color: .black.opacity(0.25), radius: 3, y: 2)
        }
    }
}
