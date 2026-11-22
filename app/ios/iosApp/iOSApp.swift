import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitIosKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in MainViewControllerKt.openKlyxURL(url: url.absoluteString) }
        }
    }
}