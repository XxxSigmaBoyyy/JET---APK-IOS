import Foundation

import UIKit

/// Swift face of the JetBridge C ABI (v2, see docs/RUNTIME_LINK.md).
enum JetBridge {
    enum TranslateResult: Equatable {
        case translated   // a prebuilt AOT module in this binary matches the APK
        case unsupported  // runtime linked, but this binary has no AOT module for the APK's code
        case stub         // legacy value from ABI v1 shell-preview builds (kept for stored shortcuts)

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

    /// Touch in viewport pixels; returns the bridge code (1 = redraw, 0 = nothing changed, < 0 = error).
    @discardableResult
    static func touch(id: Int32, action: Int32, x: Float, y: Float) -> Int32 { jet_runtime_touch(id, action, x, y) }
    static func stop() { jet_runtime_stop() }

    // MARK: ABI v2 rendering

    /// Installs the UIKit text measurer (system font, the same one JetSurfaceView draws with). Idempotent.
    static func installTextMeasurer() {
        jet_runtime_set_text_measurer { utf8, size, bold, width, ascent, descent in
            let font = JetBridge.font(size: CGFloat(size), bold: bold != 0)
            let text = utf8.map { String(cString: $0) } ?? ""
            width?.pointee = Float(ceil((text as NSString).size(withAttributes: [.font: font]).width))
            ascent?.pointee = Float(font.ascender)
            descent?.pointee = Float(-font.descender)
        }
    }

    private static var fontCache: [Int: UIFont] = [:]
    /// System font at a pixel size (the surface draws in a pixel-scaled context).
    static func font(size: CGFloat, bold: Bool) -> UIFont {
        let key = Int(size * 16) * 2 + (bold ? 1 : 0)
        if let f = fontCache[key] { return f }
        let f = UIFont.systemFont(ofSize: max(size, 1), weight: bold ? .bold : .regular)
        fontCache[key] = f
        return f
    }

    static func setViewport(width: Int32, height: Int32, density: Float) { jet_runtime_set_viewport(width, height, density) }

    /// Current display list (valid until the next runtime call) and window background colour.
    static func frame() -> (ops: UnsafeBufferPointer<JetDrawOp>, background: UInt32) {
        var ptr: UnsafePointer<JetDrawOp>?
        var bg: UInt32 = 0xff000000
        let n = jet_runtime_frame(&ptr, &bg)
        return (UnsafeBufferPointer(start: n > 0 ? ptr : nil, count: Int(max(n, 0))), bg)
    }

    static var nextWakeupMs: Int32 { jet_runtime_next_wakeup_ms() }

    /// Centre (pixels) of the first clickable view labelled `label` (automation / smoke tests).
    static func find(label: String) -> CGPoint? {
        var x: Float = 0, y: Float = 0
        return jet_runtime_find_label(label, &x, &y) == JET_BRIDGE_OK ? CGPoint(x: CGFloat(x), y: CGFloat(y)) : nil
    }
}
