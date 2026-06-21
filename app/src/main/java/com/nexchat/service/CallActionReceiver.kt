package com.nexchat.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.nexchat.NexChatApp
import com.nexchat.R

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("callId") ?: return
        val action = intent.action

        when (action) {
            "ACCEPT_CALL" -> {
                // Emit broadcast that call VM will pick up
                val acceptIntent = Intent("com.nexchat.ACCEPT_CALL").apply {
                    putExtra("callId", callId)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(acceptIntent)
            }
            "REJECT_CALL" -> {
                val rejectIntent = Intent("com.nexchat.REJECT_CALL").apply {
                    putExtra("callId", callId)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(rejectIntent)
            }
        }

        // Dismiss notification
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.cancel(CallNotificationHelper.CALL_NOTIFICATION_ID)
    }
}
