package cloud.scoreprof.app.ui.utils

import cloud.scoreprof.app.data.SetupRepository

class GlobalExceptionHandler(
    private val setupRepository: SetupRepository
) {
    fun handleException(throwable: Throwable) {
        val stackTrace = throwable.stackTraceToString()
        val message = throwable.message ?: "Unknown Global Crash"
        // setupRepository.logError(...) // Can't easily use coroutines here
    }
}
