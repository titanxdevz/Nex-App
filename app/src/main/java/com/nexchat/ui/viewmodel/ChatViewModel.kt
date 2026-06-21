package com.nexchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexchat.data.local.TokenStorage
import com.nexchat.data.local.entity.ConversationEntity
import com.nexchat.data.repository.ConversationRepository
import com.nexchat.service.NotificationQueue
import com.nexchat.service.SocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val archivedConversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val filter: String = "ALL" // ALL, DIRECT, GROUP
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val tokenStorage: TokenStorage,
    private val socketService: SocketService,
    private val notificationQueue: NotificationQueue
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe conversations from Room
            conversationRepository.observeConversations().collect { conversations ->
                val filter = _uiState.value.filter
                val filtered = filterConversations(conversations, filter, _uiState.value.searchQuery)
                _uiState.value = _uiState.value.copy(
                    conversations = filtered,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            conversationRepository.observeArchived().collect { archived ->
                _uiState.value = _uiState.value.copy(archivedConversations = archived)
            }
        }

        // Fetch from server
        viewModelScope.launch {
            try {
                conversationRepository.fetchConversations()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }

        // Listen for new messages via socket
        viewModelScope.launch {
            socketService.newMessage.collect { json ->
                val conversationId = json.optString("conversationId")
                val senderId = json.optString("senderId")
                val content = json.optString("content")
                val senderName = json.optString("senderName", "Unknown")
                val senderAvatar = json.optString("senderAvatar")
                val type = json.optString("type", "TEXT")

                if (conversationId.isNotEmpty()) {
                    conversationRepository.incrementUnread(conversationId)

                    // Enqueue notification
                    notificationQueue.enqueue(
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        senderAvatar = senderAvatar,
                        content = content,
                        messageType = type
                    )
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter)
        viewModelScope.launch {
            conversationRepository.observeConversations().first().let { conversations ->
                val filtered = filterConversations(conversations, filter, _uiState.value.searchQuery)
                _uiState.value = _uiState.value.copy(conversations = filtered)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            conversationRepository.observeConversations().first().let { conversations ->
                val filtered = filterConversations(conversations, _uiState.value.filter, query)
                _uiState.value = _uiState.value.copy(conversations = filtered)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                conversationRepository.fetchConversations()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setActiveConversation(conversationId: String?) {
        notificationQueue.setActiveConversation(conversationId)
    }

    private fun filterConversations(conversations: List<ConversationEntity>, filter: String, search: String): List<ConversationEntity> {
        return conversations
            .filter { conv ->
                when (filter) {
                    "DIRECT" -> conv.type == "DIRECT"
                    "GROUP" -> conv.type == "GROUP"
                    else -> true
                }
            }
            .filter { conv ->
                search.isBlank() || conv.name?.contains(search, ignoreCase = true) == true ||
                        conv.lastMessageContent?.contains(search, ignoreCase = true) == true
            }
    }

    private fun <T> Flow<T>.first(): T = kotlinx.coroutines.flow.first(this)
}
