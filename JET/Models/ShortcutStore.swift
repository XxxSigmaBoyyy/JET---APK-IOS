import Foundation
import SwiftUI
import UIKit

/// Persists shortcuts and owns the on-disk layout:
///   Application Support/JET/shortcuts.json
///   Application Support/JET/Apps/<id>/{app.apk, icon.png, bundle/}
///   Caches/JET/           (import staging; safe to clear)
@MainActor
final class ShortcutStore: ObservableObject {
    @Published private(set) var shortcuts: [AppShortcut] = []

    let root: URL
    let appsRoot: URL
    let cacheRoot: URL
    private let indexURL: URL
    private let fm = FileManager.default
    private var iconCache: [UUID: UIImage] = [:]

    init() {
        let support = (try? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true))
            ?? FileManager.default.temporaryDirectory
        let caches = (try? FileManager.default.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true))
            ?? FileManager.default.temporaryDirectory
        root = support.appendingPathComponent("JET", isDirectory: true)
        appsRoot = root.appendingPathComponent("Apps", isDirectory: true)
        cacheRoot = caches.appendingPathComponent("JET", isDirectory: true)
        indexURL = root.appendingPathComponent("shortcuts.json")
        try? fm.createDirectory(at: appsRoot, withIntermediateDirectories: true)
        try? fm.createDirectory(at: cacheRoot, withIntermediateDirectories: true)
        load()
    }

    // MARK: paths

    func directory(for id: UUID) -> URL { appsRoot.appendingPathComponent(id.uuidString, isDirectory: true) }
    func apkURL(for id: UUID) -> URL { directory(for: id).appendingPathComponent("app.apk") }
    func bundleURL(for id: UUID) -> URL { directory(for: id).appendingPathComponent("bundle", isDirectory: true) }
    func iconURL(for id: UUID) -> URL { directory(for: id).appendingPathComponent("icon.png") }

    func icon(for s: AppShortcut) -> UIImage? {
        guard s.hasIcon else { return nil }
        if let img = iconCache[s.id] { return img }
        guard let img = UIImage(contentsOfFile: iconURL(for: s.id).path) else { return nil }
        iconCache[s.id] = img
        return img
    }

    // MARK: mutations

    func add(_ s: AppShortcut) {
        shortcuts.removeAll { $0.id == s.id }
        shortcuts.append(s)
        save()
    }

    func rename(_ id: UUID, to name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, let i = shortcuts.firstIndex(where: { $0.id == id }) else { return }
        shortcuts[i].name = trimmed
        save()
    }

    func remove(_ id: UUID) {
        shortcuts.removeAll { $0.id == id }
        iconCache[id] = nil
        try? fm.removeItem(at: directory(for: id))
        save()
    }

    func removeAll() {
        for s in shortcuts { try? fm.removeItem(at: directory(for: s.id)) }
        shortcuts = []
        iconCache = [:]
        save()
    }

    /// Deletes import staging data and temporary files. Shortcuts and their APKs stay.
    @discardableResult
    func clearCache() -> Int64 {
        let freed = Self.size(of: cacheRoot) + Self.size(of: fm.temporaryDirectory)
        for dir in [cacheRoot, fm.temporaryDirectory] {
            for item in (try? fm.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil)) ?? [] {
                try? fm.removeItem(at: item)
            }
        }
        try? fm.createDirectory(at: cacheRoot, withIntermediateDirectories: true)
        iconCache = [:]
        return freed
    }

    var cacheSize: Int64 { Self.size(of: cacheRoot) + Self.size(of: fm.temporaryDirectory) }
    var appsSize: Int64 { Self.size(of: appsRoot) }

    // MARK: persistence

    private func load() {
        guard let data = try? Data(contentsOf: indexURL) else { return }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        shortcuts = ((try? decoder.decode([AppShortcut].self, from: data)) ?? [])
            .filter { fm.fileExists(atPath: directory(for: $0.id).path) }
    }

    private func save() {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(shortcuts) else { return }
        try? data.write(to: indexURL, options: .atomic)
    }

    nonisolated static func size(of url: URL) -> Int64 {
        guard let e = FileManager.default.enumerator(at: url, includingPropertiesForKeys: [.totalFileAllocatedSizeKey, .isRegularFileKey]) else { return 0 }
        var total: Int64 = 0
        for case let f as URL in e {
            let v = try? f.resourceValues(forKeys: [.totalFileAllocatedSizeKey, .isRegularFileKey])
            if v?.isRegularFile == true { total += Int64(v?.totalFileAllocatedSize ?? 0) }
        }
        return total
    }
}
