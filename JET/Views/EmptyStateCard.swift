import SwiftUI

/// Shown on Home when no APK has been imported yet.
struct EmptyStateCard: View {
    let onImport: () -> Void

    var body: some View {
        VStack(spacing: 18) {
            JetLogo(size: 84)
            VStack(spacing: 6) {
                Text("No apps yet")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(JetTheme.ink)
                Text("Import an Android .apk from Files. JET prepares it and pins a shortcut here.")
                    .font(.subheadline)
                    .foregroundStyle(JetTheme.inkSoft)
                    .multilineTextAlignment(.center)
            }
            Button(action: onImport) {
                Label("Import APK", systemImage: "plus")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 22)
                    .padding(.vertical, 12)
                    .background(JetTheme.accent, in: Capsule())
                    .overlay(Capsule().strokeBorder(.white.opacity(0.6), lineWidth: 1))
                    .shadow(color: JetTheme.skyDeep.opacity(0.40), radius: 14, x: 0, y: 6)
            }
            .buttonStyle(.plain)
            Text("Only import apps you have the right to use.")
                .font(.footnote)
                .foregroundStyle(JetTheme.inkSoft.opacity(0.8))
        }
        .padding(28)
        .frame(maxWidth: 420)
        .glassCard(cornerRadius: 30)
        .padding(.horizontal, 24)
    }
}
