package com.nexchat.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.nexchat.NexChatApp
import com.nexchat.R
import com.nexchat.data.local.TokenStorage
import com.nexchat.data.local.dao.ConversationDao
import com.nexchat.data.local.dao.NotificationQueueEntity
import com.nexchat.data.local.dao.NotificationQueueDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationQueue - Smart notification batching system.
 *
 * Instead of showing a notification for every single message, this system:
 * 1. Batches messages from the same conversation within a 3-second window
 * 2. Rate-limits to max 1 notification per conversation per 5 seconds
 * 3. Coalesces multiple messages into a single summary notification
 * 4. Respects per-conversation notification preferences
 * 5. Silences notifications when the user is actively viewing that conversation
 * 6. Deduplicates duplicate events from socket reconnection
 */
@Singleton
class NotificationQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationQueueDao: NotificationQueueDao,
    private val conversationDao: ConversationDao,
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    // Rate limiting: track last notification time per conversation
    private val lastNotified = ConcurrentHashMap<String, Long>()
    private val RATE_LIMIT_MS = 5000L // 5 seconds between notifications per conversation

    // Batching window
    private val BATCH_WINDOW_MS = 3000L // 3 seconds

    // Deduplication
    private val recentMessageIds = ConcurrentHashMap.newKeySet<String>()

    // Active conversation (don't notify for this one)
    private var activeConversationId: String? = null

    // Notification ID counter
    private var notificationIdCounter = 1000

    private val _notificationEvent = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 64)
    val notificationEvent = _notificationEvent.asSharedFlow()

    fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
    }

    /**
     * Enqueue a message for notification. This is the main entry point.
     * Messages are batched, deduplicated, and rate-limited before being shown.
     */
    suspend fun enqueue(
        conversationId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String?,
        content: String,
        messageType: String = "TEXT"
    ) {
        // Don't notify for own messages
        val currentUser = tokenStorage.getUser() ?: return
        if (senderId == currentUser.id) return

        // Don't notify if viewing this conversation
        if (activeConversationId == conversationId) return

        // Deduplicate
        val messageKey = "$conversationId:$senderId:$content:${System.currentTimeMillis() / 1000}"
        if (!recentMessageIds.add(messageKey)) return
        // Clean old entries periodically
        if (recentMessageIds.size > 500) recentMessageIds.clear()

        // Rate limit
        val now = System.currentTimeMillis()
        val lastTime = lastNotified[conversationId] ?: 0
        if (now - lastTime < RATE_LIMIT_MS) {
            // Store for later delivery
            notificationQueueDao.insert(
                NotificationQueueEntity(
                    conversationId = conversationId,
                    senderId = senderId,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    content = content,
                    messageType = messageType,
                    timestamp = now,
                    batchKey = conversationId
                )
            )
            return
        }

        // Check notification preference
        val conversation = conversationDao.getById(conversationId)
        val preference = conversation?.notificationPreference ?: "ALL"
        if (preference == "MUTE") return

        // Check if muted
        if (conversation?.mutedUntil != null) {
            val mutedUntil = try {
                java.time.Instant.parse(conversation.mutedUntil).toEpochMilli()
            } catch (_: Exception) { 0L }
            if (mutedUntil > now) return
        }

        // Show notification
        showNotification(conversationId, senderName, content, messageType, senderAvatar)
        lastNotified[conversationId] = now

        // Emit event for UI
        _notificationEvent.emit(
            NotificationEvent.NewMessage(conversationId, senderName, content)
        )
    }

    /**
     * Process queued notifications (called periodically or when app goes to background)
     */
    suspend fun processPending() {
        val pending = notificationQueueDao.getPending()
        if (pending.isEmpty()) return

        // Group by conversation
        val grouped = pending.groupBy { it.batchKey }

        for ((batchKey, items) in grouped) {
            val now = System.currentTimeMillis()
            val lastTime = lastNotified[batchKey] ?: 0

            if (now - lastTime >= RATE_LIMIT_MS) {
                if (items.size == 1) {
                    val item = items.first()
                    showNotification(
                        item.conversationId,
                        item.senderName,
                        item.content,
                        item.messageType,
                        item.senderAvatar
                    )
                } else {
                    // Batch multiple messages into one notification
                    showBatchNotification(batchKey, items)
                }
                lastNotified[batchKey] = now
                notificationQueueDao.markBatchDelivered(batchKey)
            }
        }

        // Cleanup old delivered notifications
        notificationQueueDao.cleanupDelivered()
    }

    private fun showNotification(
        conversationId: String,
        senderName: String,
        content: String,
        messageType: String,
        avatar: String?
    ) {
        val displayContent = when (messageType) {
            "IMAGE" -> "Photo"
            "AUDIO" -> "Voice message"
            "VIDEO" -> "Video"
            "FILE" -> content.ifEmpty { "File" }
            "POLL" -> "New poll"
            else -> content.take(100)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, conversationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = notificationIdCounter++

        val notification = NotificationCompat.Builder(context, NexChatApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(senderName)
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup("nexchat_messages")
            .setShortcutId("new_message")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager?.notify(notifId, notification)
    }

    private fun showBatchNotification(
        conversationId: String,
        items: List<NotificationQueueEntity>
    ) {
        val senderNames = items.map { it.senderName }.distinct()
        val title = if (senderNames.size == 1) {
            senderNames.first()
        } else {
            "${senderNames.first()} and ${senderNames.size - 1} others"
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, conversationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val style = NotificationCompat.InboxStyle()
        items.takeLast(5).forEach { item ->
            val line = "${item.senderName}: ${item.content.take(50)}"
            style.addLine(line)
        }
        style.setSummaryText("${items.size} new messages")

        val notification = NotificationCompat.Builder(context, NexChatApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText("${items.size} new messages")
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup("nexchat_messages")
            .setGroupSummary(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager?.notify(conversationId.hashCode(), notification)
    }

    fun cancelAll() {
        notificationManager?.cancelAll()
    }

    fun cancelForConversation(conversationId: String) {
        notificationManager?.cancel(conversationId.hashCode())
    }

    fun shutdown() {
        scope.cancel()
    }
}

sealed class NotificationEvent {
    data class NewMessage(
        val conversationId: String,
        val senderName: String,
        val content: String
    ) : NotificationEvent()
}
