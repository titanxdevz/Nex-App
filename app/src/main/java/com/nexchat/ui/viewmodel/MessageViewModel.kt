package com.nexchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexchat.data.local.entity.MessageEntity
import com.nexchat.data.repository.MessageRepository
import com.nexchat.service.SocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageUiState(
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val typingUsers: Map<String, String> = emptyMap(), // userId -> displayName
    val replyTo: MessageEntity? = null,
    val editingMessage: MessageEntity? = null,
    val scrollToMessageId: String? = null
)

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val socketService: SocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var currentConversationId: String? = null
    private var cursor: String? = null

    fun loadMessages(conversationId: String) {
        currentConversationId = conversationId
        cursor = null
        _uiState.value = MessageUiState()

        // Join socket room
        socketService.joinConversation(conversationId)

        // Observe from Room
        viewModelScope.launch {
            messageRepository.observeMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }

        // Fetch from server
        viewModelScope.launch {
            try {
                val result = messageRepository.fetchMessages(conversationId)
                val data = result.body()?.data
                cursor = data?.nextCursor
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasMore = data?.nextCursor != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }

        // Listen for new messages
        viewModelScope.launch {
            socketService.newMessage.collect { json ->
                val msgConvId = json.optString("conversationId")
                if (msgConvId == conversationId) {
                    // Message will be picked up by Room observer
                    messageRepository.markAsRead(conversationId)
                }
            }
        }

        // Listen for typing
        viewModelScope.launch {
            socketService.typingStart.collect { json ->
                val convId = json.optString("conversationId")
                val userId = json.optString("userId")
                val displayName = json.optString("displayName")
                if (convId == conversationId) {
                    _uiState.value = _uiState.value.copy(
                        typingUsers = _uiState.value.typingUsers + (userId to displayName)
                    )
                }
            }
        }

        viewModelScope.launch {
            socketService.typingStop.collect { json ->
                val convId = json.optString("conversationId")
                val userId = json.optString("userId")
                if (convId == conversationId) {
                    _uiState.value = _uiState.value.copy(
                        typingUsers = _uiState.value.typingUsers - userId
                    )
                }
            }
        }

        // Mark as read
        viewModelScope.launch {
            messageRepository.markAsRead(conversationId)
        }
    }

    fun loadMore() {
        val convId = currentConversationId ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val result = messageRepository.fetchMessages(convId, cursor)
                val data = result.body()?.data
                cursor = data?.nextCursor
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    hasMore = data?.nextCursor != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun sendMessage(content: String, type: String = "TEXT", replyToId: String? = null) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                val request = com.nexchat.data.remote.dto.CreateMessageRequest(
                    conversationId = convId,
                    content = content,
                    type = type,
                    replyToId = replyToId
                )
                messageRepository.sendMessage(request)
                _uiState.value = _uiState.value.copy(replyTo = null, editingMessage = null)
            } catch (e: Exception) {
                // Queue for retry
                val tempMessage = MessageEntity(
                    id = "temp_${System.currentTimeMillis()}",
                    conversationId = convId,
                    senderId = "self",
                    content = content,
                    type = type,
                    status = "PENDING",
                    createdAt = java.time.Instant.now().toString()
                )
                messageRepository.insertPending(tempMessage)
            }
        }
    }

    fun editMessage(messageId: String, content: String) {
        viewModelScope.launch {
            try {
                messageRepository.editMessage(messageId, content)
                _uiState.value = _uiState.value.copy(editingMessage = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteMessage(messageId: String, type: String = "ME") {
        viewModelScope.launch {
            try { messageRepository.deleteMessage(messageId, type) } catch (_: Exception) {}
        }
    }

    fun toggleStar(messageId: String) {
        viewModelScope.launch {
            try { messageRepository.toggleStar(messageId) } catch (_: Exception) {}
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            try { messageRepository.toggleReaction(messageId, emoji) } catch (_: Exception) {}
        }
    }

    fun togglePin(messageId: String) {
        viewModelScope.launch {
            try { messageRepository.togglePin(messageId) } catch (_: Exception) {}
        }
    }

    fun setReplyTo(message: MessageEntity?) { _uiState.value = _uiState.value.copy(replyTo = message) }
    fun setEditingMessage(message: MessageEntity?) { _uiState.value = _uiState.value.copy(editingMessage = message) }
    fun scrollToMessage(id: String) { _uiState.value = _uiState.value.copy(scrollToMessageId = id) }
    fun clearScrollToMessage() { _uiState.value = _uiState.value.copy(scrollToMessageId = null) }

    fun startTyping(userId: String, displayName: String) {
        currentConversationId?.let { socketService.startTyping(it, userId, displayName) }
    }

    fun stopTyping(userId: String) {
        currentConversationId?.let { socketService.stopTyping(it, userId) }
    }

    override fun onCleared() {
        super.onCleared()
        currentConversationId?.let { socketService.leaveConversation(it) }
    }
}
