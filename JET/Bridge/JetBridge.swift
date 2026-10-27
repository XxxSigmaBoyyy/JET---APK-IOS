import Foundation

/// Swift face of the JetBridge C ABI (v1, see docs/RUNTIME_LINK.md).
enum JetBridge {
    enum TranslateResult: Equatable {
        case translated   // a prebuilt AOT module in this binary matches the APK
        case unsupported  // runtime linked, but this binary has no AOT module for the APK's code
        case stub         // stub accepted the APK; nothing was translated

        /// Value stored in AppShortcut.bridgeResult.
        var storedValue: String {
            switch self {
            case .translated: return "translated"
            case .unsupported: return "unsupported"
            case .stub: return "stub"
            }
        }
    }

    struct BridgeError: LocalizedError {
        let code: Int32
        var errorDescription: String? {
            switch code {
            case JET_BRIDGE_ERR_INPUT: return "This file is not a usable APK."
            case JET_BRIDGE_ERR_IO: return "The bridge could not read the APK or write the bundle."
            case JET_BRIDGE_ERR_RUNTIME: return "The translated app stopped with a runtime error."
            default: return "Bridge error \(code)."
            }
        }
    }

    /// One node of the running Activity's view tree (jet_runtime_ui_snapshot line).
    struct Node: Identifiable, Hashable {
        let id: Int          // line index (stable for one snapshot)
        let depth: Int
        let isButton: Bool
        let viewID: UInt32
        let selected: Bool
        let text: String
    }

    static var abiVersion: Int32 { jet_bridge_abi_version() }
    static var version: String { String(cString: jet_bridge_version()) }
    static var hasRuntime: Bool { jet_bridge_has_runtime() != 0 }
    static var status: String { String(cString: jet_runtime_status()) }
    /// Number of AOT app modules compiled into this binary.
    static var moduleCount: Int { String(cString: jet_bridge_modules()).split(separator: "\n").count }

    static func translate(apk: URL, into bundle: URL) throws -> TranslateResult {
        let rc = jet_translate_apk(apk.path, bundle.path, nil, nil)
        switch rc {
        case JET_BRIDGE_OK: return .translated
        case JET_BRIDGE_ERR_UNSUPPORTED: return .unsupported
        case JET_BRIDGE_STUB: return .stub
        default: throw BridgeError(code: rc)
        }
    }

    /// Starts the Activity (main thread). Returns true when the runtime is running.
    static func launch(bundle: URL, activity: String, in view: UnsafeMutableRawPointer?) -> Bool {
        jet_runtime_main(bundle.path, activity, view) == JET_BRIDGE_OK
    }

    /// Current view tree of the running Activity (main thread). Empty when nothing runs.
    static func snapshot() -> [Node] {
        let need = jet_runtime_ui_snapshot(nil, 0)
        guard need > 0 else { return [] }
        var buf = [CChar](repeating: 0, count: Int(need) + 1)
        _ = buf.withUnsafeMutableBufferPointer { jet_runtime_ui_snapshot($0.baseAddress, Int32($0.count)) }
        let text = String(cString: buf)
        return text.split(separator: "\n", omittingEmptySubsequences: true).enumerated().compactMap { i, line in
            let f = line.split(separator: "\t", maxSplits: 4, omittingEmptySubsequences: false)
            guard f.count == 5, let d = Int(f[0]) else { return nil }
            return Node(id: i, depth: d, isButton: f[1] == "button", viewID: UInt32(f[2], radix: 16) ?? 0,
                        selected: f[3] == "s", text: String(f[4]))
        }
    }

    @discardableResult
    static func click(_ label: String) -> Int32 { jet_runtime_click(label) }

    static func touch(id: Int32, action: Int32, x: Float, y: Float) { jet_runtime_touch(id, action, x, y) }
    static func stop() { jet_runtime_stop() }
}
