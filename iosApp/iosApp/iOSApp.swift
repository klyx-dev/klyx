import SwiftUI
import Klyx

@main
struct iOSApp: App {
    init() {
        CrashHandlerKt.initCrashHandler()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
