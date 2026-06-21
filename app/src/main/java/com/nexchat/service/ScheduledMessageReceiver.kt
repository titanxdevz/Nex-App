package com.nexchat.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduledMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra("messageId") ?: return
        val conversationId = intent.getStringExtra("conversationId") ?: return
        // Trigger sending scheduled message
        val sendIntent = Intent("com.nexchat.SEND_SCHEDULED").apply {
            putExtra("messageId", messageId)
            putExtra("conversationId", conversationId)
            setPackage(context.packageName)
        }
        context.sendBroadcast(sendIntent)
    }
}
