package com.nexchat.data.repository

import com.nexchat.data.local.TokenStorage
import com.nexchat.data.local.dao.*
import com.nexchat.data.local.entity.*
import com.nexchat.data.remote.api.*
import com.nexchat.data.remote.dto.*
import com.nexchat.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val userDao: UserDao
) {
    suspend fun sendOtp(email: String? = null, phone: String? = null) = withContext(Dispatchers.IO) {
        authApi.sendOtp(SendOtpRequest(email, phone))
    }

    suspend fun verifyOtp(email: String? = null, phone: String? = null, code: String) = withContext(Dispatchers.IO) {
        authApi.verifyOtp(VerifyOtpRequest(email, phone, code))
    }

    suspend fun register(
        displayName: String,
        email: String? = null,
        phone: String? = null,
        password: String? = null,
        username: String? = null
    ) = withContext(Dispatchers.IO) {
        val result = authApi.register(RegisterRequest(email, phone, password, displayName, username))
        result.body()?.data?.let { auth ->
            tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
            tokenStorage.saveUser(auth.user)
            userDao.upsert(auth.user.toEntity())
        }
        result
    }

    suspend fun login(email: String? = null, phone: String? = null, password: String? = null, code: String? = null) = withContext(Dispatchers.IO) {
        val result = authApi.login(LoginRequest(email, phone, password, code))
        result.body()?.data?.let { auth ->
            tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
            tokenStorage.saveUser(auth.user)
            userDao.upsert(auth.user.toEntity())
        }
        result
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val rt = tokenStorage.getRefreshToken()
        if (rt != null) {
            try { authApi.logout(RefreshRequest(rt)) } catch (_: Exception) {}
        }
        tokenStorage.clearAll()
        userDao.deleteById(tokenStorage.getUser()?.id ?: "")
    }

    suspend fun forgotPassword(email: String) = withContext(Dispatchers.IO) {
        authApi.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun resetPassword(token: String, newPassword: String) = withContext(Dispatchers.IO) {
        authApi.resetPassword(ResetPasswordRequest(token, newPassword))
    }

    suspend fun isLoggedIn(): Boolean = tokenStorage.getAccessToken() != null

    private fun User.toEntity() = UserEntity(
        id = id, email = email, phone = phone, username = username,
        displayName = displayName, avatar = avatar, bio = bio,
        isOnline = isOnline, lastSeen = lastSeen, lastSeenVisibility = lastSeenVisibility,
        showEmail = showEmail, isPublic = isPublic, readReceiptsEnabled = readReceiptsEnabled,
        notificationsEnabled = notificationsEnabled, notificationSound = notificationSound,
        role = role, createdAt = createdAt
    )
}

@Singleton
class UserRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val userDao: UserDao,
    private val tokenStorage: TokenStorage
) {
    suspend fun getMe() = withContext(Dispatchers.IO) {
        val result = usersApi.getMe()
        result.body()?.data?.let { user ->
            tokenStorage.saveUser(user)
            userDao.upsert(user.toEntity())
        }
        result
    }

    suspend fun updateProfile(request: UpdateProfileRequest) = withContext(Dispatchers.IO) {
        val result = usersApi.updateProfile(request)
        result.body()?.data?.let { user ->
            tokenStorage.saveUser(user)
            userDao.upsert(user.toEntity())
        }
        result
    }

    suspend fun search(query: String) = withContext(Dispatchers.IO) {
        usersApi.search(query)
    }

    suspend fun getByUsername(username: String) = withContext(Dispatchers.IO) {
        usersApi.getByUsername(username)
    }

    suspend fun saveFcmToken(fcmToken: String) = withContext(Dispatchers.IO) {
        usersApi.saveFcmToken(FcmTokenRequest(fcmToken))
    }

    suspend fun getBlocked() = withContext(Dispatchers.IO) { usersApi.getBlocked() }
    suspend fun block(userId: String) = withContext(Dispatchers.IO) { usersApi.block(BlockRequest(userId)) }
    suspend fun unblock(userId: String) = withContext(Dispatchers.IO) { usersApi.block(BlockRequest(userId)) }

    fun observeUser(id: String): Flow<UserEntity?> = userDao.observeById(id)

    private fun User.toEntity() = UserEntity(
        id = id, email = email, phone = phone, username = username,
        displayName = displayName, avatar = avatar, bio = bio,
        isOnline = isOnline, lastSeen = lastSeen, lastSeenVisibility = lastSeenVisibility,
        showEmail = showEmail, isPublic = isPublic, readReceiptsEnabled = readReceiptsEnabled,
        notificationsEnabled = notificationsEnabled, notificationSound = notificationSound,
        role = role, createdAt = createdAt
    )
}

@Singleton
class ConversationRepository @Inject constructor(
    private val conversationsApi: ConversationsApi,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao,
    private val userDao: UserDao
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeActive()
    fun observeArchived(): Flow<List<ConversationEntity>> = conversationDao.observeArchived()
    fun observeById(id: String): Flow<ConversationEntity?> = conversationDao.observeById(id)

    suspend fun fetchConversations() = withContext(Dispatchers.IO) {
        val result = conversationsApi.list()
        result.body()?.data?.let { conversations ->
            val entities = conversations.map { it.toEntity() }
            conversationDao.upsertAll(entities)
            // Cache participants and users
            for (conv in conversations) {
                val participants = conv.participants.map { user ->
                    ParticipantEntity(
                        id = "${conv.id}:${user.id}",
                        userId = user.id,
                        conversationId = conv.id,
                        role = user.role ?: "MEMBER",
                        joinedAt = user.createdAt ?: ""
                    )
                }
                participantDao.upsertAll(participants)
                userDao.upsertAll(conv.participants.map { it.toEntity() })
            }
        }
        result
    }

    suspend fun create(request: CreateConversationRequest) = withContext(Dispatchers.IO) {
        val result = conversationsApi.create(request)
        result.body()?.data?.let { conv -> conversationDao.upsert(conv.toEntity()) }
        result
    }

    suspend fun togglePin(id: String) = withContext(Dispatchers.IO) {
        val result = conversationsApi.togglePin(id)
        val pinnedAt = result.body()?.data?.get("pinnedAt") as? String
        val conv = conversationDao.getById(id)
        conv?.let { conversationDao.upsert(it.copy(pinnedAt = pinnedAt)) }
        result
    }

    suspend fun mute(id: String, duration: String) = withContext(Dispatchers.IO) {
        conversationsApi.mute(id, MuteRequest(duration))
    }

    suspend fun archive(id: String, archived: Boolean) = withContext(Dispatchers.IO) {
        conversationsApi.archive(id, ArchiveRequest(archived))
    }

    suspend fun updateLastMessage(conversationId: String, message: MessageEntity) {
        conversationDao.updateLastMessage(
            conversationId, message.id, message.content,
            message.senderId, message.createdAt, message.type
        )
    }

    suspend fun incrementUnread(conversationId: String) {
        val conv = conversationDao.getById(conversationId) ?: return
        conversationDao.updateUnreadCount(conversationId, conv.unreadCount + 1)
    }

    suspend fun clearUnread(conversationId: String) {
        conversationDao.updateUnreadCount(conversationId, 0)
    }

    suspend fun getParticipants(conversationId: String): List<UserEntity> = withContext(Dispatchers.IO) {
        val participants = participantDao.getByConversation(conversationId)
        participants.mapNotNull { userDao.getById(it.userId) }
    }

    private fun Conversation.toEntity() = ConversationEntity(
        id = id, type = type, name = name, avatar = avatar,
        lastMessageId = lastMessage?.id, lastMessageContent = lastMessage?.content,
        lastMessageSenderId = lastMessage?.senderId, lastMessageCreatedAt = lastMessage?.createdAt,
        lastMessageType = lastMessage?.type, unreadCount = unreadCount, updatedAt = updatedAt,
        pinnedAt = pinnedAt, mutedUntil = mutedUntil, archivedAt = archivedAt,
        disappearingTtlSeconds = disappearingTtlSeconds, description = description,
        isAnnouncementMode = isAnnouncementMode, requiresApproval = requiresApproval,
        isPublic = isPublic, invitePermission = invitePermission,
        messagePermission = messagePermission, editPermission = editPermission,
        notificationPreference = notificationPreference
    )

    private fun User.toEntity() = UserEntity(
        id = id, email = email, phone = phone, username = username,
        displayName = displayName, avatar = avatar, bio = bio,
        isOnline = isOnline, lastSeen = lastSeen, role = role, createdAt = createdAt
    )
}

@Singleton
class MessageRepository @Inject constructor(
    private val messagesApi: MessagesApi,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(conversationId)

    suspend fun fetchMessages(conversationId: String, cursor: String? = null) = withContext(Dispatchers.IO) {
        val result = messagesApi.list(conversationId, cursor)
        result.body()?.data?.let { data ->
            val entities = data.messages.map { it.toEntity() }
            messageDao.upsertAll(entities)
        }
        result
    }

    suspend fun sendMessage(request: CreateMessageRequest) = withContext(Dispatchers.IO) {
        messagesApi.create(request)
    }

    suspend fun editMessage(id: String, content: String) = withContext(Dispatchers.IO) {
        val result = messagesApi.edit(id, mapOf("content" to content))
        result.body()?.data?.let { msg ->
            messageDao.updateContent(id, content, msg.editedAt ?: "")
        }
        result
    }

    suspend fun deleteMessage(id: String, type: String = "ME") = withContext(Dispatchers.IO) {
        messagesApi.delete(id, MessageDeleteRequest(type))
    }

    suspend fun toggleStar(id: String) = withContext(Dispatchers.IO) {
        val result = messagesApi.toggleStar(id)
        val starred = result.body()?.data?.get("starred") as? Boolean
        starred?.let { messageDao.updateStarred(id, it) }
        result
    }

    suspend fun toggleReaction(id: String, emoji: String) = withContext(Dispatchers.IO) {
        messagesApi.toggleReaction(id, ReactionRequest(emoji))
    }

    suspend fun togglePin(id: String) = withContext(Dispatchers.IO) {
        val result = messagesApi.togglePin(id)
        val pinned = result.body()?.data?.get("pinned") as? Boolean
        if (pinned == true) {
            messageDao.updatePinned(id, java.time.Instant.now().toString(), null)
        } else {
            messageDao.updatePinned(id, null, null)
        }
        result
    }

    suspend fun markAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        messagesApi.markAsRead(conversationId)
        conversationDao.updateUnreadCount(conversationId, 0)
    }

    suspend fun updateMessageStatus(id: String, status: String) {
        messageDao.updateStatus(id, status)
    }

    // Optimistic insert for offline support
    suspend fun insertPending(message: MessageEntity) {
        messageDao.upsert(message)
    }

    suspend fun getPendingMessages() = messageDao.getPendingMessages()

    suspend fun retryMessage(id: String, request: CreateMessageRequest): Boolean {
        return try {
            val result = sendMessage(request)
            val serverMsg = result.body()?.data
            if (serverMsg != null) {
                messageDao.deleteById(id)
                messageDao.upsert(serverMsg.toEntity())
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun Message.toEntity() = MessageEntity(
        id = id, conversationId = conversationId, senderId = senderId,
        senderDisplayName = sender?.displayName, senderAvatar = sender?.avatar,
        content = content, type = type, mediaUrl = mediaUrl, status = status,
        createdAt = createdAt, replyToId = replyTo?.id, replyToContent = replyTo?.content,
        replyToSenderId = replyTo?.senderId, isDeleted = isDeleted,
        forwardedFromId = forwardedFromId, editedAt = editedAt,
        starred = starred, mentionedUserIds = mentionedUserIds?.joinToString(","),
        expiresAt = expiresAt, scheduledAt = scheduledAt,
        pinnedAt = pinnedAt, pinnedById = pinnedById,
        pollOptionCount = pollOptionCount
    )
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaApi: MediaApi
) {
    suspend fun getPresignedUrl(fileName: String, fileType: String, fileSize: Long) = withContext(Dispatchers.IO) {
        mediaApi.getPresignedUrl(PresignedUrlRequest(fileName, fileType, fileSize))
    }

    suspend fun getLinkPreview(url: String) = withContext(Dispatchers.IO) {
        mediaApi.linkPreview(LinkPreviewRequest(url))
    }

    suspend fun translate(text: String, target: String) = withContext(Dispatchers.IO) {
        mediaApi.translate(TranslateRequest(text, target))
    }
}

@Singleton
class StoryRepository @Inject constructor(
    private val storiesApi: StoriesApi
) {
    suspend fun create(type: String, mediaUrl: String? = null, caption: String? = null, bgColor: String? = null, fontStyle: String? = null) = withContext(Dispatchers.IO) {
        storiesApi.create(CreateStoryRequest(type, mediaUrl, caption, bgColor, fontStyle))
    }

    suspend fun feed() = withContext(Dispatchers.IO) { storiesApi.feed() }

    suspend fun markViewed(id: String) = withContext(Dispatchers.IO) { storiesApi.markViewed(id) }

    suspend fun react(id: String, emoji: String) = withContext(Dispatchers.IO) {
        storiesApi.react(id, StoryReactRequest(emoji))
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { storiesApi.delete(id) }
}

@Singleton
class FriendRepository @Inject constructor(
    private val friendsApi: FriendsApi,
    private val friendDao: FriendDao
) {
    fun observeFriends(): Flow<List<FriendEntity>> = friendDao.observeAll()

    suspend fun fetchFriends() = withContext(Dispatchers.IO) {
        val result = friendsApi.listWithPresence()
        result.body()?.data?.let { friends ->
            friendDao.upsertAll(friends.map {
                FriendEntity(it.id, it.displayName, it.avatar, it.username, it.isOnline)
            })
        }
        result
    }

    suspend fun sendRequest(userId: String) = withContext(Dispatchers.IO) { friendsApi.sendRequest(SendFriendRequest(userId)) }
    suspend fun acceptRequest(id: String) = withContext(Dispatchers.IO) { friendsApi.acceptRequest(id) }
    suspend fun rejectRequest(id: String) = withContext(Dispatchers.IO) { friendsApi.rejectRequest(id) }
    suspend fun cancelRequest(id: String) = withContext(Dispatchers.IO) { friendsApi.cancelRequest(id) }
    suspend fun removeFriend(friendId: String) = withContext(Dispatchers.IO) { friendsApi.removeFriend(friendId) }
    suspend fun pendingReceived() = withContext(Dispatchers.IO) { friendsApi.pendingReceived() }
    suspend fun pendingSent() = withContext(Dispatchers.IO) { friendsApi.pendingSent() }
}

@Singleton
class CallRepository @Inject constructor(
    private val callsApi: CallsApi,
    private val callDao: CallDao
) {
    suspend fun fetchHistory() = withContext(Dispatchers.IO) {
        val result = callsApi.history()
        result.body()?.data?.let { calls ->
            callDao.upsertAll(calls.map { it.toEntity() })
        }
        result
    }

    fun observeHistory(): Flow<List<CallRecordEntity>> = callDao.observeAll()

    suspend fun initiate(userId: String, isVideo: Boolean = false) = withContext(Dispatchers.IO) {
        callsApi.initiate(InitiateCallRequest(userId, isVideo))
    }

    suspend fun accept(callId: String) = withContext(Dispatchers.IO) { callsApi.accept(callId) }
    suspend fun reject(callId: String) = withContext(Dispatchers.IO) { callsApi.reject(callId) }
    suspend fun end(callId: String) = withContext(Dispatchers.IO) { callsApi.end(callId) }
    suspend fun cancel(callId: String) = withContext(Dispatchers.IO) { callsApi.cancel(callId) }
    suspend fun getToken(callId: String) = withContext(Dispatchers.IO) { callsApi.getToken(callId) }

    private fun CallRecord.toEntity() = CallRecordEntity(
        id = id, callerId = callerId, calleeId = calleeId, roomName = roomName,
        status = status, isVideo = isVideo, startedAt = startedAt, endedAt = endedAt,
        duration = duration, createdAt = createdAt,
        callerDisplayName = caller?.displayName, callerAvatar = caller?.avatar,
        calleeDisplayName = callee?.displayName, calleeAvatar = callee?.avatar
    )
}

@Singleton
class InviteRepository @Inject constructor(
    private val invitesApi: InvitesApi
) {
    suspend fun preview(token: String) = withContext(Dispatchers.IO) { invitesApi.preview(token) }
    suspend fun join(token: String) = withContext(Dispatchers.IO) { invitesApi.join(token) }
    suspend fun revoke(token: String) = withContext(Dispatchers.IO) { invitesApi.revoke(token) }
}
