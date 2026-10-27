import Foundation
import SwiftUI

/// One running or finished import, shown on the glass progress card.
@MainActor
final class ImportJob: ObservableObject, Identifiable {
    enum Stage: Int, CaseIterable {
        case copying, inspecting, translating, bundling, done
        var title: String {
            switch self {
            case .copying: return "Copying APK"
            case .inspecting: return "Inspecting package"
            case .translating: return "Translating (JetBridge)"
            case .bundling: return "Building bundle"
            case .done: return "Added to Home"
            }
        }
    }

    let id = UUID()
    let fileName: String
    @Published var stage: Stage = .copying
    @Published var error: String?
    @Published var result: AppShortcut?

    init(fileName: String) { self.fileName = fileName }

    var progress: Double { Double(stage.rawValue) / Double(Stage.done.rawValue) }
    var isFinished: Bool { stage == .done || error != nil }
}

/// Import flow: copy the picked .apk into the sandbox, inspect it, hand it to JetBridge for translation,
/// assemble the app folder and add a home shortcut. The bridge matches the APK's classes.dex against the AOT modules
/// compiled into this binary (iOS allows no JIT / code loading); unmatched APKs are kept but marked unsupported.
@MainActor
final class ImportCoordinator: ObservableObject {
    @Published private(set) var jobs: [ImportJob] = []

    var activeJobs: [ImportJob] { jobs.filter { !$0.isFinished } }

    func importAPKs(_ urls: [URL], into store: ShortcutStore) {
        for url in urls {
            let job = ImportJob(fileName: url.lastPathComponent)
            jobs.append(job)
            Task { await run(job, source: url, store: store) }
        }
    }

    func dismiss(_ job: ImportJob) { jobs.removeAll { $0.id == job.id } }
    func dismissFinished() { jobs.removeAll { $0.isFinished } }

    private func run(_ job: ImportJob, source: URL, store: ShortcutStore) async {
        let id = UUID()
        let appDir = store.directory(for: id)
        let apk = store.apkURL(for: id)
        let bundle = store.bundleURL(for: id)
        let staging = store.cacheRoot.appendingPathComponent("import-\(id.uuidString).apk")
        do {
            // 1. copy out of the picker's security scope
            job.stage = .copying
            try await Task.detached(priority: .userInitiated) {
                let scoped = source.startAccessingSecurityScopedResource()
                defer { if scoped { source.stopAccessingSecurityScopedResource() } }
                let fm = FileManager.default
                try? fm.removeItem(at: staging)
                try fm.copyItem(at: source, to: staging)
            }.value

            // 2. inspect
            job.stage = .inspecting
            let info = try await Task.detached(priority: .userInitiated) { try ApkInspector.inspect(staging) }.value
            let size = Int64((try? staging.resourceValues(forKeys: [.fileSizeKey]))?.fileSize ?? 0)

            // 3. translate through the bridge
            job.stage = .translating
            try FileManager.default.createDirectory(at: bundle, withIntermediateDirectories: true)
            let translated = try await Task.detached(priority: .userInitiated) { try JetBridge.translate(apk: staging, into: bundle) }.value

            // 4. bundle: move the APK in, extract the icon, write metadata
            job.stage = .bundling
            let hasIcon = try await Task.detached(priority: .userInitiated) { () -> Bool in
                let fm = FileManager.default
                try? fm.removeItem(at: apk)
                try fm.moveItem(at: staging, to: apk)
                guard let png = ApkInspector.iconPNG(info, apk: apk) else { return false }
                try png.write(to: appDir.appendingPathComponent("icon.png"), options: .atomic)
                return true
            }.value

            let shortcut = AppShortcut(
                id: id,
                name: Self.displayName(for: source),
                sourceFileName: source.lastPathComponent,
                importedAt: Date(),
                apkSize: size,
                dexCount: info.dexCount,
                nativeABIs: info.nativeABIs,
                hasIcon: hasIcon,
                bridgeResult: translated.storedValue,
                bridgeVersion: JetBridge.version
            )
            store.add(shortcut)
            job.result = shortcut
            job.stage = .done
        } catch {
            try? FileManager.default.removeItem(at: staging)
            try? FileManager.default.removeItem(at: appDir)
            job.error = error.localizedDescription
        }
    }

    /// "org.example.app_1234.apk" / "My App-v2.1.apk" -> readable shortcut name (editable later).
    static func displayName(for url: URL) -> String {
        var s = url.deletingPathExtension().lastPathComponent
        if let r = s.range(of: #"[-_ ]v?\d[\w.\-]*$"#, options: .regularExpression) { s.removeSubrange(r) }
        if s.contains("."), !s.contains(" "), let last = s.split(separator: ".").last { s = String(last) }
        s = s.replacingOccurrences(of: "_", with: " ").replacingOccurrences(of: "-", with: " ")
        s = s.trimmingCharacters(in: .whitespaces)
        if s.isEmpty { return "App" }
        return s.prefix(1).uppercased() + s.dropFirst()
    }
}
