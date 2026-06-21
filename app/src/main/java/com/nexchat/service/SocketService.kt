package com.nexchat.service

import com.nexchat.BuildConfig
import com.nexchat.data.local.TokenStorage
import com.nexchat.util.NexChatLog
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket.IO service with:
 * - Thread-safe connection management
 * - Exponential backoff reconnection (capped)
 * - Max reconnect attempts before giving up
 * - Heartbeat every 25s (server TTL is 30s)
 * - Structured logging (no sensitive data)
 */
@Singleton
class SocketService @Inject constructor(
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _connectionState = MutableStateFlow<SocketState>(SocketState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Event flows
    private val _newMessage = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val newMessage = _newMessage.asSharedFlow()

    private val _messageUpdated = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val messageUpdated = _messageUpdated.asSharedFlow()

    private val _messagesDelivered = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val messagesDelivered = _messagesDelivered.asSharedFlow()

    private val _typingStart = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val typingStart = _typingStart.asSharedFlow()

    private val _typingStop = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val typingStop = _typingStop.asSharedFlow()

    private val _userOnline = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val userOnline = _userOnline.asSharedFlow()

    private val _userOffline = MutableSharedFlow<JSONObject>(extraBufferCapacity = 32)
    val userOffline = _userOffline.asSharedFlow()

    private val _callRinging = MutableSharedFlow<JSONObject>(extraBufferCapacity = 8)
    val callRinging = _callRinging.asSharedFlow()

    private val _callAccepted = MutableSharedFlow<JSONObject>(extraBufferCapacity = 8)
    val callAccepted = _callAccepted.asSharedFlow()

    private val _callRejected = MutableSharedFlow<JSONObject>(extraBufferCapacity = 8)
    val callRejected = _callRejected.asSharedFlow()

    private val _callEnded = MutableSharedFlow<JSONObject>(extraBufferCapacity = 8)
    val callEnded = _callEnded.asSharedFlow()

    private val _callCancelled = MutableSharedFlow<JSONObject>(extraBufferCapacity = 8)
    val callCancelled = _callCancelled.asSharedFlow()

    private val _mqttMessage = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val mqttMessage = _mqttMessage.asSharedFlow()

    fun connect() {
        val token = scope.runCatching { tokenStorage.getAccessToken() }.getOrNull() ?: return
        if (socket?.connected() == true) return

        _connectionState.value = SocketState.Connecting

        val opts = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setReconnection(true)
            .setReconnectionDelay(INITIAL_RECONNECT_DELAY)
            .setReconnectionDelayMax(MAX_RECONNECT_DELAY)
            .setReconnectionAttempts(MAX_RECONNECT_ATTEMPTS)
            .setForceNew(true)
            .setUpgrade(true)
            .build()

        socket = IO.socket(URI.create(BuildConfig.SOCKET_URL), opts)

        socket?.on(Socket.EVENT_CONNECT) {
            NexChatLog.logSocketEvent("Connected")
            _isConnected.value = true
            _connectionState.value = SocketState.Connected
            reconnectAttempts = 0
            startHeartbeat()
        }

        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            NexChatLog.logSocketEvent("Disconnected: $reason")
            _isConnected.value = false
            _connectionState.value = SocketState.Disconnected
            stopHeartbeat()
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()?.toString() ?: "unknown"
            NexChatLog.logError("SOCKET", "Connect error: $error")
            _isConnected.value = false
            reconnectAttempts++
            _connectionState.value = SocketState.Error(error)
        }

        // Register all event listeners
        registerEventListeners()

        socket?.connect()
    }

    private fun registerEventListeners() {
        socket?.on("message:new") { emitSafe(_newMessage, it) }
        socket?.on("message:updated") { emitSafe(_messageUpdated, it) }
        socket?.on("messages:delivered") { emitSafe(_messagesDelivered, it) }
        socket?.on("typing:start") { emitSafe(_typingStart, it) }
        socket?.on("typing:stop") { emitSafe(_typingStop, it) }
        socket?.on("user:online") { emitSafe(_userOnline, it) }
        socket?.on("user:offline") { emitSafe(_userOffline, it) }
        socket?.on("call:ringing") { emitSafe(_callRinging, it) }
        socket?.on("call:accepted") { emitSafe(_callAccepted, it) }
        socket?.on("call:rejected") { emitSafe(_callRejected, it) }
        socket?.on("call:ended") { emitSafe(_callEnded, it) }
        socket?.on("call:cancelled") { emitSafe(_callCancelled, it) }
        socket?.on("mqtt:message") { emitSafe(_mqttMessage, it) }
    }

    private fun emitSafe(flow: MutableSharedFlow<JSONObject>, args: Array<Any>) {
        (args.firstOrNull() as? JSONObject)?.let { json ->
            scope.launch { flow.emit(json) }
        }
    }

    fun disconnect() {
        stopHeartbeat()
        socket?.disconnect()
        socket?.off()
        socket = null
        _isConnected.value = false
        _connectionState.value = SocketState.Disconnected
        reconnectAttempts = 0
    }

    fun joinConversation(id: String) { socket?.emit("join_conversation", id) }
    fun leaveConversation(id: String) { socket?.emit("leave_conversation", id) }

    fun startTyping(conversationId: String, userId: String, displayName: String) {
        mqttPublish("typing/$conversationId", JSONObject().apply {
            put("userId", userId)
            put("displayName", displayName)
            put("action", "start")
        })
    }

    fun stopTyping(conversationId: String, userId: String) {
        mqttPublish("typing/$conversationId", JSONObject().apply {
            put("userId", userId)
            put("action", "stop")
        })
    }

    fun mqttSubscribe(topic: String) { socket?.emit("mqtt:subscribe", topic) }
    fun mqttUnsubscribe(topic: String) { socket?.emit("mqtt:unsubscribe", topic) }

    private fun mqttPublish(topic: String, payload: JSONObject) {
        socket?.emit("mqtt:publish", JSONObject().apply {
            put("topic", topic)
            put("payload", payload)
        })
    }

    fun joinCommunity(id: String) { socket?.emit("community:join", id) }
    fun leaveCommunity(id: String) { socket?.emit("community:leave", id) }

    fun acceptCall(callId: String) { socket?.emit("call:accept", JSONObject().put("callId", callId)) }
    fun rejectCall(callId: String) { socket?.emit("call:reject", JSONObject().put("callId", callId)) }
    fun endCall(callId: String) { socket?.emit("call:end", JSONObject().put("callId", callId)) }
    fun cancelCall(callId: String) { socket?.emit("call:cancel", JSONObject().put("callId", callId)) }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (socket?.connected() == true) {
                    socket?.emit("heartbeat")
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 25_000L // 25s (server TTL is 30s)
        private const val INITIAL_RECONNECT_DELAY = 1_000L
        private const val MAX_RECONNECT_DELAY = 30_000L // Cap at 30s
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }
}

sealed class SocketState {
    data object Disconnected : SocketState()
    data object Connecting : SocketState()
    data object Connected : SocketState()
    data class Error(val message: String) : SocketState()
}
