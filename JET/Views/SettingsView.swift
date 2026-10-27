import SwiftUI

@MainActor
struct SettingsView: View {
    @EnvironmentObject private var store: ShortcutStore
    @Environment(\.dismiss) private var dismiss

    @State private var cacheBytes: Int64 = 0
    @State private var appsBytes: Int64 = 0
    @State private var confirmClear = false
    @State private var confirmRemoveAll = false
    @State private var lastFreed: Int64?

    private var version: String {
        let v = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let b = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        return "\(v) (\(b))"
    }

    var body: some View {
        NavigationStack {
            ZStack {
                GlassBackground()
                ScrollView {
                    VStack(spacing: 18) {
                        about
                        storage
                        notes
                    }
                    .padding(16)
                    .padding(.bottom, 30)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }.fontWeight(.semibold)
                }
            }
        }
        .presentationBackground(.clear)
        .onAppear(perform: refreshSizes)
        .confirmationDialog("Clear cache?", isPresented: $confirmClear, titleVisibility: .visible) {
            Button("Clear cache", role: .destructive) {
                lastFreed = store.clearCache()
                refreshSizes()
            }
        } message: {
            Text("Removes import staging and temporary files. Your shortcuts stay.")
        }
        .confirmationDialog("Remove all shortcuts?", isPresented: $confirmRemoveAll, titleVisibility: .visible) {
            Button("Remove all", role: .destructive) {
                store.removeAll()
                refreshSizes()
            }
        } message: {
            Text("Deletes every imported APK and its bundle from this device.")
        }
    }

    private var about: some View {
        VStack(spacing: 14) {
            JetLogo(size: 76)
            Text("JET").font(.system(size: 28, weight: .heavy, design: .rounded)).foregroundStyle(JetTheme.ink)
            Text("Runs Android apps through the JET translation layer.")
                .font(.subheadline)
                .foregroundStyle(JetTheme.inkSoft)
                .multilineTextAlignment(.center)
            VStack(spacing: 0) {
                row("Version", version)
                Divider().overlay(JetTheme.ink.opacity(0.10))
                row("Bridge", JetBridge.version)
                Divider().overlay(JetTheme.ink.opacity(0.10))
                row("Runtime", JetBridge.hasRuntime ? "Linked · AOT, \(JetBridge.moduleCount) app module\(JetBridge.moduleCount == 1 ? "" : "s")" : "Not linked (preview)")
                Divider().overlay(JetTheme.ink.opacity(0.10))
                row("Bridge ABI", "v\(JetBridge.abiVersion)")
            }
            .glassCard(cornerRadius: 16, material: .thinMaterial)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .glassCard()
    }

    private var storage: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Storage", systemImage: "internaldrive").font(.headline).foregroundStyle(JetTheme.ink)
            VStack(spacing: 0) {
                row("Shortcuts", "\(store.shortcuts.count)")
                Divider().overlay(JetTheme.ink.opacity(0.10))
                row("Apps", bytes(appsBytes))
                Divider().overlay(JetTheme.ink.opacity(0.10))
                row("Cache", bytes(cacheBytes))
            }
            .glassCard(cornerRadius: 16, material: .thinMaterial)

            Button { confirmClear = true } label: {
                Label("Clear cache", systemImage: "sparkles")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(JetTheme.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(.white)
                    .shadow(color: JetTheme.skyDeep.opacity(0.35), radius: 10, x: 0, y: 4)
            }
            .buttonStyle(.plain)

            if let freed = lastFreed {
                Text("Freed \(bytes(freed)).").font(.caption).foregroundStyle(JetTheme.inkSoft)
            }

            Button(role: .destructive) { confirmRemoveAll = true } label: {
                Label("Remove all shortcuts", systemImage: "trash")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .foregroundStyle(.red)
            }
            .buttonStyle(.plain)
            .disabled(store.shortcuts.isEmpty)
        }
        .padding(20)
        .glassCard()
    }

    private var notes: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("About imports", systemImage: "info.circle").font(.headline).foregroundStyle(JetTheme.ink)
            Text("APKs stay on this device inside JET's sandbox. Only import apps you have the right to use. "
                 + "This build ships the preview bridge: apps are inspected and pinned, but not yet executed.")
                .font(.footnote)
                .foregroundStyle(JetTheme.inkSoft)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard()
    }

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).foregroundStyle(JetTheme.inkSoft)
            Spacer()
            Text(value).foregroundStyle(JetTheme.ink).monospacedDigit()
        }
        .font(.subheadline)
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
    }

    private func bytes(_ n: Int64) -> String { ByteCountFormatter.string(fromByteCount: n, countStyle: .file) }

    private func refreshSizes() {
        cacheBytes = store.cacheSize
        appsBytes = store.appsSize
    }
}
