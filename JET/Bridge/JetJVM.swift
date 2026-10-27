import Foundation

/// Swift face of the embedded no-JIT JVM (Runtime/jvm/jetjvm.c). JRE lives at JET.app/jre, app jars at JET.app/jvm-app.
enum JetJVM {
    static var javaHome: URL { Bundle.main.bundleURL.appendingPathComponent("jre", isDirectory: true) }
    static var appJars: URL { Bundle.main.bundleURL.appendingPathComponent("jvm-app", isDirectory: true) }

    static var isBundled: Bool {
        let fm = FileManager.default
        return jet_jvm_compiled() == 1 && ["zero", "server"].contains {
            fm.fileExists(atPath: javaHome.appendingPathComponent("lib/\($0)/libjvm.dylib").path)
        }
    }

    /// Boot (idempotent). Blocking — call from a background task.
    static func boot(classpath: [URL], options: [String] = [], log: URL?) -> (ok: Bool, ms: Double, error: String) {
        var err = [CChar](repeating: 0, count: 1024)
        let rc = jet_jvm_boot(javaHome.path, classpath.map(\.path).joined(separator: ":"),
                              options.joined(separator: "\n"), log?.path, &err, Int32(err.count))
        return (rc == 0, jet_jvm_boot_ms(), String(cString: err))
    }

    /// Call `static String method(String)` on `cls` ("dev/jet/Hello").
    static func callString(_ cls: String, _ method: String, _ arg: String = "") -> (ok: Bool, out: String) {
        var out = [CChar](repeating: 0, count: 64 * 1024)
        let rc = jet_jvm_call_string(cls, method, arg, &out, Int32(out.count))
        return (rc == 0, String(cString: out))
    }
}
