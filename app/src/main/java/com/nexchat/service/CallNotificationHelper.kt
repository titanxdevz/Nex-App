package com.nexchat.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nexchat.NexChatApp
import com.nexchat.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showIncomingCallNotification(
        callId: String,
        callerName: String,
        callerAvatar: String?,
        isVideo: Boolean
    ) {
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("callId", callId)
        }
        val rejectIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "REJECT_CALL"
            putExtra("callId", callId)
        }

        val acceptPending = PendingIntent.getBroadcast(
            context, callId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectPending = PendingIntent.getBroadcast(
            context, callId.hashCode() + 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callType = if (isVideo) "Video" else "Voice"

        val notification = NotificationCompat.Builder(context, NexChatApp.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle("Incoming $callType Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(null, true)
            .setAutoCancel(true)
            .addAction(0, "Accept", acceptPending)
            .addAction(0, "Decline", rejectPending)
            .setOngoing(true)
            .setTimeoutAfter(30_000) // Auto-dismiss after 30s
            .build()

        notificationManager?.notify(CALL_NOTIFICATION_ID, notification)
    }

    fun showActiveCallNotification(callerName: String, isVideo: Boolean) {
        val notification = NotificationCompat.Builder(context, NexChatApp.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Active ${if (isVideo) "Video" else "Voice"} Call")
            .setContentText(callerName)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager?.notify(ACTIVE_CALL_NOTIFICATION_ID, notification)
    }

    fun cancelCallNotification() {
        notificationManager?.cancel(CALL_NOTIFICATION_ID)
        notificationManager?.cancel(ACTIVE_CALL_NOTIFICATION_ID)
    }

    fun dismissActiveCallNotification() {
        notificationManager?.cancel(ACTIVE_CALL_NOTIFICATION_ID)
    }

    companion object {
        const val CALL_NOTIFICATION_ID = 9999
        const val ACTIVE_CALL_NOTIFICATION_ID = 9998
    }
}
