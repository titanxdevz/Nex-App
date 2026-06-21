package com.nexchat.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexchat.data.remote.api.UsersApi
import com.nexchat.data.remote.dto.FcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NexChatMessagingService : FirebaseMessagingService() {

    @Inject lateinit var usersApi: UsersApi
    @Inject lateinit var notificationQueue: NotificationQueue

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                usersApi.saveFcmToken(FcmTokenRequest(fcmToken = token))
                Log.d("FCM", "Token saved successfully")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to save token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val conversationId = data["conversationId"] ?: return
        val senderId = data["senderId"] ?: return
        val senderName = data["senderName"] ?: "New message"
        val senderAvatar = data["senderAvatar"]
        val content = message.notification?.body ?: data["body"] ?: ""
        val messageType = data["messageType"] ?: "TEXT"

        CoroutineScope(Dispatchers.IO).launch {
            notificationQueue.enqueue(
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                content = content,
                messageType = messageType
            )
        }
    }
}
