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
                              (fontOptions() + options).joined(separator: "\n"), log?.path, &err, Int32(err.count))
        return (rc == 0, jet_jvm_boot_ms(), String(cString: err))
    }

    /// Java2D text needs a logical-font config. stage_jvm.sh bundles DejaVu in jre/lib/fonts plus
    /// lib/fontconfig.properties.in; FontConfiguration wants absolute file names, and the bundle path is only known
    /// now, so write the resolved file to Caches and point sun.awt.fontconfig at it.
    static func fontOptions() -> [String] {
        let fonts = javaHome.appendingPathComponent("lib/fonts", isDirectory: true)
        let tmpl = javaHome.appendingPathComponent("lib/fontconfig.properties.in")
        guard let text = try? String(contentsOf: tmpl, encoding: .utf8) else { return [] }
        let out = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("jet-fontconfig.properties")
        do { try text.replacingOccurrences(of: "$JRE_LIB_FONTS", with: fonts.path).write(to: out, atomically: true, encoding: .utf8) }
        catch { return [] }
        return ["-Dsun.awt.fontconfig=\(out.path)", "-Dsun.java2d.fontpath=append:\(fonts.path)"]
    }

    /// Call `static String method(String)` on `cls` ("dev/jet/Hello").
    static func callString(_ cls: String, _ method: String, _ arg: String = "") -> (ok: Bool, out: String) {
        var out = [CChar](repeating: 0, count: 64 * 1024)
        let rc = jet_jvm_call_string(cls, method, arg, &out, Int32(out.count))
        return (rc == 0, String(cString: out))
    }
}
