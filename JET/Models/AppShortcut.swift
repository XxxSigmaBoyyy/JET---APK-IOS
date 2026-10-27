import Foundation
import UniformTypeIdentifiers

/// One imported APK shown as a shortcut on the home grid.
struct AppShortcut: Identifiable, Codable, Hashable {
    var id: UUID
    var name: String
    var sourceFileName: String
    var importedAt: Date
    var apkSize: Int64
    var dexCount: Int
    var nativeABIs: [String]
    var hasIcon: Bool
    /// "translated" when a prebuilt AOT module matched, "unsupported" when the runtime is linked but has no module
    /// for this APK, "stub" for the shell-preview bridge.
    var bridgeResult: String
    var bridgeVersion: String

    var isTranslated: Bool { bridgeResult == "translated" }
    var isUnsupported: Bool { bridgeResult == "unsupported" }
}

extension UTType {
    /// Android package (.apk). Declared in Info.plist under UTImportedTypeDeclarations.
    static let apk = UTType(importedAs: "com.android.package-archive", conformingTo: .data)
}
