import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // 1. Start with the placeholder key from your .plist file
        FirebaseApp.configure()

        // 2. Initialize Koin
        KoinHelperKt.doInitKoin()

        // 3. Fetch the real key from the server and swap if needed
        let firebaseManager = KoinHelperKt.getFirebaseManager()
        firebaseManager.initialize { apiKeyFromServer in
            guard let key = apiKeyFromServer, !key.isEmpty else { return }

            // If the server key is different from the one we started with
            if FirebaseApp.app()?.options.apiKey != key {
                FirebaseApp.app()?.delete { success in
                    if success {
                        // Re-initialize with the server key
                        let plistPath = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist")!
                        if let options = FirebaseOptions(contentsOfFile: plistPath) {
                            options.apiKey = key
                            FirebaseApp.configure(options: options)
                            print("Firebase key swapped successfully on iOS")
                        }
                    }
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
