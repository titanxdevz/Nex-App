package com.nexchat.data.remote.api

import com.nexchat.data.remote.dto.*
import com.nexchat.domain.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<ApiResponse<Unit>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Unit>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<ApiResponse<AuthResponse>>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<ApiResponse<Unit>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Unit>>

    @PUT("auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>
}

interface UsersApi {
    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<User>>

    @PUT("users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<User>>

    @GET("users/search")
    suspend fun search(@Query("q") query: String): Response<ApiResponse<List<User>>>

    @GET("users/by-username/{username}")
    suspend fun getByUsername(@Path("username") username: String): Response<ApiResponse<User>>

    @POST("users/fcm-token")
    suspend fun saveFcmToken(@Body request: FcmTokenRequest): Response<ApiResponse<Unit>>

    @GET("users/blocked")
    suspend fun getBlocked(): Response<ApiResponse<List<User>>>

    @POST("users/block")
    suspend fun block(@Body request: BlockRequest): Response<ApiResponse<Unit>>

    @POST("users/unblock")
    suspend fun block(@Body request: BlockRequest): Response<ApiResponse<Unit>>

    @GET("users/sessions")
    suspend fun getSessions(): Response<ApiResponse<List<Unit>>>

    @DELETE("users/sessions/{id}")
    suspend fun revokeSession(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("users/sessions/revoke-others")
    suspend fun revokeOtherSessions(): Response<ApiResponse<Map<String, Int>>>

    @PUT("users/username")
    suspend fun changeUsername(@Body request: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("users/email/change-send-otp")
    suspend fun sendEmailChangeOtp(@Body request: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("users/email/change-confirm")
    suspend fun confirmEmailChange(@Body request: Map<String, String>): Response<ApiResponse<Unit>>

    @HTTP(method = "DELETE", path = "users/me", hasBody = true)
    suspend fun deleteAccount(@Body request: Map<String, String>): Response<ApiResponse<Unit>>
}

interface ConversationsApi {
    @GET("conversations")
    suspend fun list(): Response<ApiResponse<List<Conversation>>>

    @POST("conversations")
    suspend fun create(@Body request: CreateConversationRequest): Response<ApiResponse<Conversation>>

    @DELETE("conversations/{id}")
    suspend fun delete(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("conversations/{id}/clear")
    suspend fun clear(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("conversations/{id}/pin")
    suspend fun togglePin(@Path("id") id: String): Response<ApiResponse<Map<String, Any?>>>

    @POST("conversations/{id}/mute")
    suspend fun mute(@Path("id") id: String, @Body request: MuteRequest): Response<ApiResponse<Map<String, Any?>>>

    @POST("conversations/{id}/archive")
    suspend fun archive(@Path("id") id: String, @Body request: ArchiveRequest): Response<ApiResponse<Map<String, Any?>>>

    @POST("conversations/{id}/disappearing")
    suspend fun setDisappearing(@Path("id") id: String, @Body request: DisappearingRequest): Response<ApiResponse<Map<String, Any?>>>

    @PUT("conversations/{id}/group")
    suspend fun updateGroup(@Path("id") id: String, @Body request: Map<String, Any?>): Response<ApiResponse<Conversation>>

    @POST("conversations/{id}/participants")
    suspend fun addParticipants(@Path("id") id: String, @Body request: AddParticipantsRequest): Response<ApiResponse<Conversation>>

    @DELETE("conversations/{id}/participants/{userId}")
    suspend fun removeParticipant(@Path("id") id: String, @Path("userId") userId: String): Response<ApiResponse<Unit>>

    @PUT("conversations/{id}/participants/{userId}/role")
    suspend fun updateParticipantRole(@Path("id") id: String, @Path("userId") userId: String, @Body request: RoleRequest): Response<ApiResponse<Conversation>>

    @GET("conversations/{id}/participants")
    suspend fun listParticipants(@Path("id") id: String, @Query("offset") offset: Int = 0, @Query("limit") limit: Int = 30): Response<ApiResponse<Map<String, Any>>>

    @POST("conversations/{id}/invites")
    suspend fun createInvite(@Path("id") id: String, @Body request: Map<String, Any?> = emptyMap()): Response<ApiResponse<Unit>>

    @GET("conversations/{id}/invites")
    suspend fun listInvites(@Path("id") id: String): Response<ApiResponse<List<Unit>>>

    @GET("conversations/{id}/join-requests")
    suspend fun listJoinRequests(@Path("id") id: String): Response<ApiResponse<List<Unit>>>

    @POST("conversations/{id}/join-requests/{requestId}/resolve")
    suspend fun resolveJoinRequest(@Path("id") id: String, @Path("requestId") requestId: String, @Body request: Map<String, String>): Response<ApiResponse<Unit>>

    @GET("conversations/{id}/audit-log")
    suspend fun listAuditLogs(@Path("id") id: String): Response<ApiResponse<List<Unit>>>

    @POST("conversations/{id}/notification-preference")
    suspend fun updateNotificationPreference(@Path("id") id: String, @Body request: NotificationPreferenceRequest): Response<ApiResponse<Unit>>
}

interface MessagesApi {
    @GET("messages/{conversationId}")
    suspend fun list(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 25
    ): Response<ApiResponse<PaginatedMessages>>

    @GET("messages/{conversationId}/search")
    suspend fun search(
        @Path("conversationId") conversationId: String,
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null
    ): Response<ApiResponse<PaginatedMessages>>

    @GET("messages/global-search")
    suspend fun globalSearch(
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null
    ): Response<ApiResponse<PaginatedMessages>>

    @POST("messages")
    suspend fun create(@Body request: CreateMessageRequest): Response<ApiResponse<Message>>

    @PATCH("messages/{id}")
    suspend fun edit(@Path("id") id: String, @Body request: Map<String, String>): Response<ApiResponse<Message>>

    @DELETE("messages/{id}")
    suspend fun delete(@Path("id") id: String, @Body request: MessageDeleteRequest): Response<ApiResponse<Unit>>

    @POST("messages/{id}/star")
    suspend fun toggleStar(@Path("id") id: String): Response<ApiResponse<Map<String, Boolean>>>

    @GET("messages/starred")
    suspend fun getStarred(@Query("cursor") cursor: String? = null): Response<ApiResponse<PaginatedMessages>>

    @POST("messages/{id}/reactions")
    suspend fun toggleReaction(@Path("id") id: String, @Body request: ReactionRequest): Response<ApiResponse<ToggleReactionResponse>>

    @GET("messages/{id}/reactions")
    suspend fun getReactions(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @POST("messages/{id}/pin")
    suspend fun togglePin(@Path("id") id: String): Response<ApiResponse<Map<String, Boolean>>>

    @GET("messages/{conversationId}/pinned")
    suspend fun listPinned(@Path("conversationId") conversationId: String): Response<ApiResponse<List<Message>>>

    @POST("messages/{id}/poll-vote")
    suspend fun pollVote(@Path("id") id: String, @Body request: PollVoteRequest): Response<ApiResponse<Map<String, Any?>>>

    @GET("messages/{id}/poll-votes")
    suspend fun getPollVotes(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @GET("messages/{id}/read-by")
    suspend fun readBy(@Path("id") id: String): Response<ApiResponse<Map<String, List<User>>>>

    @POST("messages/read/{conversationId}")
    suspend fun markAsRead(@Path("conversationId") conversationId: String): Response<ApiResponse<Unit>>

    @GET("messages/{conversationId}/stats")
    suspend fun getStats(@Path("conversationId") conversationId: String): Response<ApiResponse<Map<String, Int>>>

    @GET("messages/{conversationId}/media")
    suspend fun listMedia(
        @Path("conversationId") conversationId: String,
        @Query("category") category: String = "MEDIA",
        @Query("cursor") cursor: String? = null
    ): Response<ApiResponse<PaginatedMessages>>

    @POST("messages/forward")
    suspend fun forward(@Body request: ForwardRequest): Response<ApiResponse<Message>>

    @GET("messages/{conversationId}/scheduled")
    suspend fun getScheduled(@Path("conversationId") conversationId: String): Response<ApiResponse<Map<String, List<Message>>>>

    @DELETE("messages/scheduled/{id}")
    suspend fun cancelScheduled(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>
}

interface MediaApi {
    @POST("media/presigned-url")
    suspend fun getPresignedUrl(@Body request: PresignedUrlRequest): Response<ApiResponse<PresignedUrlResponse>>

    @POST("media/link-preview")
    suspend fun linkPreview(@Body request: LinkPreviewRequest): Response<ApiResponse<LinkPreviewData?>>

    @POST("media/translate")
    suspend fun translate(@Body request: TranslateRequest): Response<ApiResponse<Map<String, String>>>
}

interface StoriesApi {
    @POST("stories")
    suspend fun create(@Body request: CreateStoryRequest): Response<ApiResponse<Story>>

    @GET("stories/feed")
    suspend fun feed(): Response<ApiResponse<List<StoryFeedGroup>>>

    @POST("stories/{id}/view")
    suspend fun markViewed(@Path("id") id: String): Response<ApiResponse<Unit>>

    @GET("stories/{id}/views")
    suspend fun getViews(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @POST("stories/{id}/react")
    suspend fun react(@Path("id") id: String, @Body request: StoryReactRequest): Response<ApiResponse<Unit>>

    @DELETE("stories/{id}")
    suspend fun delete(@Path("id") id: String): Response<ApiResponse<Unit>>
}

interface FriendsApi {
    @GET("friends")
    suspend fun list(): Response<ApiResponse<List<Friend>>>

    @GET("friends/presence")
    suspend fun listWithPresence(): Response<ApiResponse<List<Friend>>>

    @POST("friends/request")
    suspend fun sendRequest(@Body request: SendFriendRequest): Response<ApiResponse<Unit>>

    @POST("friends/accept/{id}")
    suspend fun acceptRequest(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("friends/reject/{id}")
    suspend fun rejectRequest(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("friends/cancel/{id}")
    suspend fun cancelRequest(@Path("id") id: String): Response<ApiResponse<Unit>>

    @DELETE("friends/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: String): Response<ApiResponse<Unit>>

    @GET("friends/pending/received")
    suspend fun pendingReceived(): Response<ApiResponse<List<FriendRequest>>>

    @GET("friends/pending/sent")
    suspend fun pendingSent(): Response<ApiResponse<List<FriendRequest>>>
}

interface CallsApi {
    @POST("calls/initiate")
    suspend fun initiate(@Body request: InitiateCallRequest): Response<ApiResponse<Map<String, String>>>

    @POST("calls/{id}/accept")
    suspend fun accept(@Path("id") id: String): Response<ApiResponse<CallRecord>>

    @POST("calls/{id}/reject")
    suspend fun reject(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("calls/{id}/end")
    suspend fun end(@Path("id") id: String): Response<ApiResponse<Map<String, Int>>>

    @POST("calls/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): Response<ApiResponse<Unit>>

    @GET("calls/{id}/token")
    suspend fun getToken(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @GET("calls")
    suspend fun history(): Response<ApiResponse<List<CallRecord>>>
}

interface InvitesApi {
    @GET("invites/{token}")
    suspend fun preview(@Path("token") token: String): Response<ApiResponse<Map<String, Any>>>

    @POST("invites/{token}/join")
    suspend fun join(@Path("token") token: String): Response<ApiResponse<Conversation>>

    @DELETE("invites/{token}")
    suspend fun revoke(@Path("token") token: String): Response<ApiResponse<Unit>>
}
