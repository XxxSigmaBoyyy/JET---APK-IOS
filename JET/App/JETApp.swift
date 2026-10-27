import SwiftUI

@main
@MainActor
struct JETApp: App {
    @StateObject private var store = ShortcutStore()
    @StateObject private var importer = ImportCoordinator()

    var body: some Scene {
        WindowGroup {
            HomeView()
                .environmentObject(store)
                .environmentObject(importer)
                .preferredColorScheme(.light)
                .tint(JetTheme.skyDeep)
        }
    }
}
