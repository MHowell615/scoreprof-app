import SwiftUI
import Shared
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
