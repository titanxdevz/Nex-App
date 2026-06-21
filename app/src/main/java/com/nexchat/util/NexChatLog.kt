package com.nexchat.util

import android.util.Log
import com.nexchat.BuildConfig
import timber.log.Timber

/**
 * Timber tree setup. Debug tree logs everything in debug builds.
 * Release tree is a no-op — zero log output.
 */
object NexChatLog {

    fun init() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "NexChat:${element.fileName}:${element.lineNumber}"
                }
            })
        }
        // Release: no tree planted = all logs silenced
    }

    // Convenience methods that match Log API
    fun d(msg: String, vararg args: Any) = Timber.d(msg, *args)
    fun i(msg: String, vararg args: Any) = Timber.i(msg, *args)
    fun w(msg: String, vararg args: Any) = Timber.w(msg, *args)
    fun e(msg: String, vararg args: Any) = Timber.e(msg, *args)
    fun e(t: Throwable, msg: String, vararg args: Any) = Timber.e(t, msg, *args)
    fun v(msg: String, vararg args: Any) = Timber.v(msg, *args)

    // Security: never log tokens, passwords, or PII
    fun logTokenEvent(event: String) {
        Timber.d("[AUTH] $event") // No token values logged
    }

    fun logSocketEvent(event: String) {
        Timber.d("[SOCKET] $event")
    }

    fun logError(tag: String, error: String) {
        Timber.e("[$tag] $error")
    }
}
