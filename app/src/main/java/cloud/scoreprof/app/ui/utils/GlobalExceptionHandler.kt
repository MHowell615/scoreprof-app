package cloud.scoreprof.app.ui.utils

import android.content.Context
import android.util.Log
import cloud.scoreprof.app.data.SetupRepository

class GlobalExceptionHandler(
    private val context: Context,
    private val setupRepository: SetupRepository,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            // We use a separate Thread because the Main thread is crashing
            val stackTrace = Log.getStackTraceString(throwable)
            val message = throwable.message ?: "Unknown Global Crash"

            // Fire and forget to the Droplet
            // Note: Global handlers are tricky with coroutines,
            // usually we use a simple blocking network call or a Service.

            // After logging, let the system handle the death
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }