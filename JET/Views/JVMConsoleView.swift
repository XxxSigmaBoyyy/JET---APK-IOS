import SwiftUI

/// Plain console showing what the embedded JVM printed / returned (smoke test + diagnostics).
struct JVMConsoleView: View {
    let title: String
    let text: String
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text(title).font(.system(.headline, design: .monospaced)).foregroundStyle(.green)
                    Text(text).font(.system(size: 12, design: .monospaced)).foregroundStyle(.white)
                        .textSelection(.enabled)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
            }
        }
    }
}
