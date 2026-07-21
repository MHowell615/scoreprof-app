package cloud.scoreprof.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class FirebaseManager(
    private val setupRepository: SetupRepository,
    private val platform: cloud.scoreprof.app.Platform
) {
    fun initialize(onKeyFetched: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val key = setupRepository.getFirebaseKey()
            if (key != null) {
                onKeyFetched(key)
            }
        }
    }
}
