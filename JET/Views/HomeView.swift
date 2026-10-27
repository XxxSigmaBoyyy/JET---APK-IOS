import SwiftUI
import UniformTypeIdentifiers

@MainActor
struct HomeView: View {
    @EnvironmentObject private var store: ShortcutStore
    @EnvironmentObject private var importer: ImportCoordinator

    @State private var showPicker = false
    @State private var showSettings = false
    @State private var launching: AppShortcut?
    @State private var renaming: AppShortcut?
    @State private var renameText = ""
    @State private var pickerError: String?

    private let columns = [GridItem(.adaptive(minimum: 86, maximum: 110), spacing: 18, alignment: .top)]

    var body: some View {
        NavigationStack {
            ZStack {
                GlassBackground()
                ScrollView {
                    VStack(spacing: 24) {
                        header
                        if store.shortcuts.isEmpty {
                            EmptyStateCard { showPicker = true }
                                .padding(.top, 40)
                        } else {
                            grid
                        }
                    }
                    .padding(.bottom, 140)
                }
                .scrollIndicators(.hidden)
            }
            .overlay(alignment: .bottom) {
                if !importer.jobs.isEmpty {
                    ImportProgressCard(coordinator: importer)
                        .padding(.bottom, 12)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.spring(response: 0.45, dampingFraction: 0.85), value: importer.jobs.count)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    GlassCircleButton(systemImage: "gearshape.fill", size: 36) { showSettings = true }
                        .accessibilityLabel("Settings")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    GlassCircleButton(systemImage: "plus", size: 36) { showPicker = true }
                        .accessibilityLabel("Import APK")
                }
            }
            .toolbarBackground(.hidden, for: .navigationBar)
        }
        .fileImporter(isPresented: $showPicker, allowedContentTypes: [.apk], allowsMultipleSelection: true) { result in
            switch result {
            case .success(let urls): importer.importAPKs(urls, into: store)
            case .failure(let error): pickerError = error.localizedDescription
            }
        }
        .sheet(isPresented: $showSettings) { SettingsView().environmentObject(store) }
        .fullScreenCover(item: $launching) { s in
            AppHostScreen(shortcut: s, icon: store.icon(for: s), bundle: store.bundleURL(for: s.id))
        }
        .alert("Rename", isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField("Name", text: $renameText)
            Button("Save") { if let r = renaming { store.rename(r.id, to: renameText) }; renaming = nil }
            Button("Cancel", role: .cancel) { renaming = nil }
        }
        .alert("Import failed", isPresented: Binding(get: { pickerError != nil }, set: { if !$0 { pickerError = nil } })) {
            Button("OK", role: .cancel) { pickerError = nil }
        } message: {
            Text(pickerError ?? "")
        }
        .onOpenURL { url in handle(url) }
    }

    private var header: some View {
        HStack(spacing: 14) {
            JetLogo(size: 54)
            VStack(alignment: .leading, spacing: 2) {
                Text("JET")
                    .font(.system(size: 34, weight: .heavy, design: .rounded))
                    .foregroundStyle(LinearGradient(colors: [JetTheme.ink, JetTheme.skyDeep], startPoint: .leading, endPoint: .trailing))
                Text("Android apps, cleared for take-off")
                    .font(.subheadline)
                    .foregroundStyle(JetTheme.inkSoft)
            }
            Spacer()
        }
        .padding(.horizontal, 22)
        .padding(.top, 4)
    }

    private var grid: some View {
        LazyVGrid(columns: columns, spacing: 22) {
            ForEach(store.shortcuts) { s in
                Button { launching = s } label: {
                    ShortcutTile(shortcut: s, icon: store.icon(for: s))
                }
                .buttonStyle(.plain)
                .contextMenu {
                    Button { launching = s } label: { Label("Open", systemImage: "play.fill") }
                    Button { renameText = s.name; renaming = s } label: { Label("Rename", systemImage: "pencil") }
                    Button(role: .destructive) { withAnimation { store.remove(s.id) } } label: { Label("Remove", systemImage: "trash") }
                }
            }
        }
        .padding(20)
        .glassCard(cornerRadius: 30)
        .padding(.horizontal, 16)
    }

    /// file:// (Open in JET from Files / share sheet) imports; jet://launch?id=<uuid> opens a shortcut.
    private func handle(_ url: URL) {
        if url.isFileURL {
            importer.importAPKs([url], into: store)
        } else if url.scheme == "jet", url.host == "launch",
                  let idString = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "id" })?.value,
                  let id = UUID(uuidString: idString),
                  let s = store.shortcuts.first(where: { $0.id == id }) {
            launching = s
        }
    }
}
