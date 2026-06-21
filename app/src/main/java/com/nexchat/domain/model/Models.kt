package com.nexchat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
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

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val status: String = "SENT",
    val createdAt: String = "",
    val replyTo: Message? = null,
    val isDeleted: Boolean = false,
    val forwardedFromId: String? = null,
    val reactions: List<MessageReaction>? = null,
    val editedAt: String? = null,
    val starred: Boolean = false,
    val mentionedUserIds: List<String>? = null,
    val expiresAt: String? = null,
    val scheduledAt: String? = null,
    val pinnedAt: String? = null,
    val pinnedById: String? = null,
    val pollOptionCount: Int? = null,
    val pollVotes: List<PollVote>? = null,
    val sender: SenderInfo? = null
)

@Serializable
data class SenderInfo(
    val id: String,
    val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class MessageReaction(
    val emoji: String,
    val userId: String
)

@Serializable
data class PollVote(
    val userId: String,
    val optionIndex: Int
)

@Serializable
data class Conversation(
    val id: String,
    val type: String = "DIRECT",
    val name: String? = null,
    val avatar: String? = null,
    val participants: List<User> = emptyList(),
    val lastMessage: Message? = null,
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

@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class CallRecord(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val roomName: String,
    val status: String,
    val isVideo: Boolean = false,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val duration: Int? = null,
    val createdAt: String = "",
    val caller: SenderInfo? = null,
    val callee: SenderInfo? = null
)

@Serializable
data class Story(
    val id: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val caption: String? = null,
    val bgColor: String? = null,
    val fontStyle: String? = null,
    val createdAt: String = "",
    val expiresAt: String = "",
    val viewed: Boolean = false
)

@Serializable
data class StoryFeedGroup(
    val userId: String,
    val user: SenderInfo,
    val stories: List<Story>
)

@Serializable
data class Friend(
    val id: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val username: String? = null,
    val isOnline: Boolean = false
)

@Serializable
data class FriendRequest(
    val id: String,
    val sender: SenderInfo,
    val receiver: SenderInfo? = null,
    val createdAt: String = ""
)

@Serializable
data class LinkPreviewData(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null
)

@Serializable
data class PaginatedMessages(
    val messages: List<Message>,
    val nextCursor: String? = null
)
