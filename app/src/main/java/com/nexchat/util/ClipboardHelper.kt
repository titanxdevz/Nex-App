package com.nexchat.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clipboard manager with auto-clear for security.
 * Copied messages are cleared after [CLEAR_DELAY_MS] to prevent
 * sensitive content from lingering in clipboard.
 */
@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null

    /**
     * Copy text to clipboard with auto-clear after delay.
     */
    fun copy(text: String, label: String = "NexChat", showToast: Boolean = true) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        if (showToast) {
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }

        // Schedule auto-clear
        scheduleAutoClear()
    }

    /**
     * Get current clipboard content (for paste operations).
     */
    fun paste(): String? {
        val clip = clipboard.primaryClip ?: return null
        return clip.getItemAt(0)?.text?.toString()
    }

    /**
     * Clear clipboard immediately.
     */
    fun clear() {
        handler.removeCallbacksAndMessages(null)
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    private fun scheduleAutoClear() {
        clearRunnable?.let { handler.removeCallbacks(it) }
        clearRunnable = Runnable { clear() }
        handler.postDelayed(clearRunnable!!, CLEAR_DELAY_MS)
    }

    companion object {
        private const val CLEAR_DELAY_MS = 30_000L // 30 seconds
    }
}
