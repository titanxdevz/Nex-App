package com.nexchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String? = null,
    val phone: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val lastSeenVisibility: String? = "EVERYONE",
    val showEmail: Boolean = false,
    val isPublic: Boolean = false,
    val readReceiptsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val role: String? = "MEMBER",
    val createdAt: String? = null
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String = "DIRECT",
    val name: String? = null,
    val avatar: String? = null,
    val lastMessageId: String? = null,
    val lastMessageContent: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageCreatedAt: String? = null,
    val lastMessageType: String? = null,
    val unreadCount: Int = 0,
    val updatedAt: String = "",
    val pinnedAt: String? = null,
    val mutedUntil: String? = null,
    val archivedAt: String? = null,
    val disappearingTtlSeconds: Int? = null,
    val description: String? = null,
    val isAnnouncementMode: Boolean = false,
    val requiresApproval: Boolean = false,
    val isPublic: Boolean = false,
    val invitePermission: String = "EVERYONE",
    val messagePermission: String = "EVERYONE",
    val editPermission: String = "ADMINS",
    val notificationPreference: String = "ALL"
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderDisplayName: String? = null,
    val senderAvatar: String? = null,
    val content: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val status: String = "SENT",
    val createdAt: String = "",
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderId: String? = null,
    val isDeleted: Boolean = false,
    val forwardedFromId: String? = null,
    val editedAt: String? = null,
    val starred: Boolean = false,
    val mentionedUserIds: String? = null,
    val expiresAt: String? = null,
    val scheduledAt: String? = null,
    val pinnedAt: String? = null,
    val pinnedById: String? = null,
    val pollOptionCount: Int? = null,
    val reactionsJson: String? = null,
    val pollVotesJson: String? = null
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val conversationId: String,
    val role: String = "MEMBER",
    val joinedAt: String = ""
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val username: String? = null,
    val isOnline: Boolean = false
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val id: String,
    val callerId: String,
    val calleeId: String,
    val roomName: String,
    val status: String,
    val isVideo: Boolean = false,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val duration: Int? = null,
    val createdAt: String = "",
    val callerDisplayName: String? = null,
    val callerAvatar: String? = null,
    val calleeDisplayName: String? = null,
    val calleeAvatar: String? = null
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val caption: String? = null,
    val bgColor: String? = null,
    val fontStyle: String? = null,
    val createdAt: String = "",
    val expiresAt: String = "",
    val viewed: Boolean = false
)

@Entity(tableName = "notification_queue")
data class NotificationQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val content: String,
    val messageType: String = "TEXT",
    val timestamp: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
    val batchKey: String
)
