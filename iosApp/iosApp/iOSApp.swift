import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // 1. Initial configuration with placeholder from plist
        FirebaseApp.configure()

        // 2. Start Koin
        KoinHelperKt.doInitKoin()

        // 3. Fetch the real key and re-configure if different
        let firebaseManager = KoinHelperKt.getFirebaseManager()
        firebaseManager.initialize { apiKeyFromServer in
            // apiKeyFromServer is a non-optional String from Kotlin
            guard !apiKeyFromServer.isEmpty else { return }

            let key = apiKeyFromServer

            // Execute on main thread for Firebase operations
            DispatchQueue.main.async {
                if let app = FirebaseApp.app(), app.options.apiKey != key {
                    app.delete(completion: { success in
                        if success {
                            if let plistPath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
                               let options = FirebaseOptions(contentsOfFile: plistPath) {
                                options.apiKey = key
                                FirebaseApp.configure(options: options)
                                print("Firebase key swapped to: \(key)")
                            }
                        }
                    })
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
