package com.nexchat.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nexchat.NexChatApp
import com.nexchat.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallForegroundService : Service() {

    @Inject lateinit var callNotificationHelper: CallNotificationHelper

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callerName = intent?.getStringExtra("callerName") ?: "Unknown"
        val isVideo = intent?.getBooleanExtra("isVideo", false) ?: false

        // Show persistent foreground notification
        callNotificationHelper.showActiveCallNotification(callerName, isVideo)

        // Acquire wake lock to keep CPU alive during call
        acquireWakeLock()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        callNotificationHelper.dismissActiveCallNotification()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "nexchat:call_wakelock"
        ).apply {
            acquire(60 * 60 * 1000L) // Max 1 hour
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    companion object {
        const val CALL_FOREGROUND_ID = 9998
    }
}
