package com.nexchat.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOtpRequest(val email: String? = null, val phone: String? = null)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(val email: String? = null, val phone: String? = null, val code: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val displayName: String,
    val username: String? = null,
    val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(val token: String, val newPassword: String)

@JsonClass(generateAdapter = true)
data class CreateMessageRequest(
    val conversationId: String,
    val content: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val replyToId: String? = null,
    val mentionedUserIds: List<String>? = null,
    val mentionEveryone: Boolean? = null,
    val scheduledAt: String? = null,
    val pollOptionCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreateConversationRequest(
    val type: String,
    val name: String? = null,
    val avatar: String? = null,
    val description: String? = null,
    val participantIds: List<String>
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val username: String? = null,
    val lastSeenVisibility: String? = null,
    val readReceiptsEnabled: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val notificationSound: Boolean? = null,
    val isPublic: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FcmTokenRequest(val fcmToken: String)

@JsonClass(generateAdapter = true)
data class BlockRequest(val blockedId: String)

@JsonClass(generateAdapter = true)
data class PresignedUrlRequest(val fileName: String, val fileType: String, val fileSize: Long)

@JsonClass(generateAdapter = true)
data class PresignedUrlResponse(val uploadUrl: String, val publicUrl: String)

@JsonClass(generateAdapter = true)
data class LinkPreviewRequest(val url: String)

@JsonClass(generateAdapter = true)
data class TranslateRequest(val text: String, val target: String)

@JsonClass(generateAdapter = true)
data class CreateStoryRequest(
    val type: String,
    val mediaUrl: String? = null,
    val caption: String? = null,
    val bgColor: String? = null,
    val fontStyle: String? = null
)

@JsonClass(generateAdapter = true)
data class StoryReactRequest(val emoji: String)

@JsonClass(generateAdapter = true)
data class SendFriendRequest(val userId: String)

@JsonClass(generateAdapter = true)
data class ForwardRequest(val messageId: String, val targetConversationId: String)

@JsonClass(generateAdapter = true)
data class PollVoteRequest(val optionIndex: Int)

@JsonClass(generateAdapter = true)
data class ReactionRequest(val emoji: String)

@JsonClass(generateAdapter = true)
data class ToggleReactionResponse(val action: String, val emoji: String)

@JsonClass(generateAdapter = true)
data class MuteRequest(val duration: String)

@JsonClass(generateAdapter = true)
data class ArchiveRequest(val archived: Boolean)

@JsonClass(generateAdapter = true)
data class DisappearingRequest(val ttlSeconds: Int?)

@JsonClass(generateAdapter = true)
data class NotificationPreferenceRequest(val preference: String)

@JsonClass(generateAdapter = true)
data class AddParticipantsRequest(val userIds: List<String>)

@JsonClass(generateAdapter = true)
data class RoleRequest(val role: String)

@JsonClass(generateAdapter = true)
data class InitiateCallRequest(val userId: String, val isVideo: Boolean = false)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(val data: T?)

@JsonClass(generateAdapter = true)
data class MessageDeleteRequest(val type: String)
