import Foundation
import UIKit

/// What the shell can learn about an APK without the runtime: structure, code size, native ABIs, launcher icon.
struct ApkInfo {
    let entryCount: Int
    let dexCount: Int
    let nativeABIs: [String]
    let iconEntry: ZipEntry?
}

enum ApkInspectorError: LocalizedError {
    case notAnAPK
    var errorDescription: String? { "This file has no AndroidManifest.xml, so it is not an Android APK." }
}

enum ApkInspector {
    static func inspect(_ url: URL) throws -> ApkInfo {
        let entries = try ZipReader.entries(of: url)
        guard entries.contains(where: { $0.name == "AndroidManifest.xml" }) else { throw ApkInspectorError.notAnAPK }
        let dex = entries.filter { $0.name.hasPrefix("classes") && $0.name.hasSuffix(".dex") && !$0.name.contains("/") }.count
        var abis = Set<String>()
        for e in entries where e.name.hasPrefix("lib/") && e.name.hasSuffix(".so") {
            let parts = e.name.split(separator: "/")
            if parts.count >= 3 { abis.insert(String(parts[1])) }
        }
        return ApkInfo(entryCount: entries.count, dexCount: dex, nativeABIs: abis.sorted(), iconEntry: bestIcon(entries))
    }

    /// Picks the highest-density PNG launcher icon by conventional resource name. Apps with shrunk/obfuscated
    /// resource paths or adaptive-only (XML) icons get a generated monogram tile instead.
    static func bestIcon(_ entries: [ZipEntry]) -> ZipEntry? {
        let densities = ["xxxhdpi": 6, "xxhdpi": 5, "xhdpi": 4, "hdpi": 3, "mdpi": 2, "": 1]
        var best: (score: Int, entry: ZipEntry)?
        for e in entries where e.name.hasPrefix("res/") && e.name.hasSuffix(".png") {
            let comps = e.name.split(separator: "/")
            guard comps.count == 3 else { continue }
            let dir = String(comps[1]), file = comps[2].lowercased()
            guard dir.hasPrefix("mipmap") || dir.hasPrefix("drawable") else { continue }
            guard file.hasPrefix("ic_launcher") || file.hasPrefix("icon") || file.hasPrefix("app_icon") else { continue }
            if file.contains("foreground") || file.contains("background") || file.contains("monochrome") { continue }
            var density = 0
            for (k, v) in densities where !k.isEmpty && dir.contains("-\(k)") { density = max(density, v) }
            if density == 0 { density = 1 }
            let score = density * 10 + (dir.hasPrefix("mipmap") ? 2 : 0) + (file.contains("round") ? 0 : 1)
            if best == nil || score > best!.score { best = (score, e) }
        }
        return best?.entry
    }

    /// PNG bytes of the launcher icon, validated by UIImage.
    static func iconPNG(_ info: ApkInfo, apk: URL) -> Data? {
        guard let e = info.iconEntry, let data = try? ZipReader.extract(e, from: apk, maxSize: 4 << 20) else { return nil }
        guard let img = UIImage(data: data), img.size.width >= 16 else { return nil }
        return img.pngData()
    }
}
