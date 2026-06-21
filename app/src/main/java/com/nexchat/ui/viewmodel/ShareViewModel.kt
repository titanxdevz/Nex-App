package com.nexchat.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexchat.data.local.entity.ConversationEntity
import com.nexchat.data.remote.dto.CreateMessageRequest
import com.nexchat.data.repository.ConversationRepository
import com.nexchat.data.repository.MediaRepository
import com.nexchat.data.repository.MessageRepository
import com.nexchat.util.InputSanitizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedContent(
    val text: String? = null,
    val uri: Uri? = null,
    val mimeType: String? = null,
    val title: String? = null,
    val subject: String? = null
)

data class ShareUiState(
    val sharedContent: SharedContent? = null,
    val conversations: List<ConversationEntity> = emptyList(),
    val filteredConversations: List<ConversationEntity> = emptyList(),
    val selectedConversationIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSharing: Boolean = false,
    val shareComplete: Boolean = false,
    val error: String? = null,
    val shareMode: ShareMode = ShareMode.SINGLE
)

enum class ShareMode { SINGLE, MULTIPLE }

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun parseIntent(intent: Intent) {
        val sharedContent = when (intent.action) {
            Intent.ACTION_SEND -> parseSingleShare(intent)
            Intent.ACTION_SEND_MULTIPLE -> parseMultipleShare(intent)
            else -> null
        }
        _uiState.value = _uiState.value.copy(sharedContent = sharedContent)
    }

    private fun parseSingleShare(intent: Intent): SharedContent {
        val type = intent.type ?: return SharedContent()

        return when {
            type.startsWith("text/") -> SharedContent(
                text = intent.getStringExtra(Intent.EXTRA_TEXT),
                title = intent.getStringExtra(Intent.EXTRA_TITLE),
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                mimeType = type
            )
            type.startsWith("image/") || type.startsWith("video/") || type.startsWith("audio/") -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                SharedContent(
                    uri = uri,
                    mimeType = type,
                    title = intent.getStringExtra(Intent.EXTRA_TITLE)
                )
            }
            else -> SharedContent(
                text = intent.getStringExtra(Intent.EXTRA_TEXT),
                mimeType = type
            )
        }
    }

    private fun parseMultipleShare(intent: Intent): SharedContent {
        val type = intent.type ?: return SharedContent()
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)

        return SharedContent(
            uri = uris?.firstOrNull(), // For simplicity, take first
            text = intent.getStringExtra(Intent.EXTRA_TEXT),
            mimeType = type,
            title = intent.getStringExtra(Intent.EXTRA_TITLE)
        )
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.observeConversations().collect { conversations ->
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    filteredConversations = filterConversations(conversations, _uiState.value.searchQuery)
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredConversations = filterConversations(_uiState.value.conversations, query)
        )
    }

    fun toggleConversationSelection(conversationId: String) {
        val current = _uiState.value.selectedConversationIds
        val newSelection = if (_uiState.value.shareMode == ShareMode.SINGLE) {
            setOf(conversationId)
        } else {
            if (conversationId in current) current - conversationId else current + conversationId
        }
        _uiState.value = _uiState.value.copy(selectedConversationIds = newSelection)
    }

    fun setShareMode(mode: ShareMode) {
        _uiState.value = _uiState.value.copy(
            shareMode = mode,
            selectedConversationIds = if (mode == ShareMode.SINGLE) emptySet()
            else _uiState.value.selectedConversationIds
        )
    }

    fun share() {
        val content = _uiState.value.sharedContent ?: return
        val selectedIds = _uiState.value.selectedConversationIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true, error = null)

            try {
                var mediaUrl: String? = null

                // Upload media if present
                if (content.uri != null) {
                    val fileName = getFileName(content.uri) ?: "shared_file"
                    val mimeType = content.mimeType ?: "application/octet-stream"
                    val presigned = mediaRepository.getPresignedUrl(fileName, mimeType, 0)
                    presigned.body()?.data?.let { data ->
                        mediaUrl = data.publicUrl
                        // TODO: upload file to data.uploadUrl
                    }
                }

                // Determine message type
                val messageType = when {
                    content.mimeType?.startsWith("image/") == true -> "IMAGE"
                    content.mimeType?.startsWith("video/") == true -> "VIDEO"
                    content.mimeType?.startsWith("audio/") == true -> "AUDIO"
                    else -> "TEXT"
                }

                // Send to each selected conversation
                val messageText = buildShareText(content)

                for (conversationId in selectedIds) {
                    val request = CreateMessageRequest(
                        conversationId = conversationId,
                        content = messageText,
                        type = messageType,
                        mediaUrl = mediaUrl
                    )
                    messageRepository.sendMessage(request)
                }

                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    shareComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    error = e.message ?: "Failed to share"
                )
            }
        }
    }

    private fun buildShareText(content: SharedContent): String {
        val parts = mutableListOf<String>()
        content.title?.let { parts.add(it) }
        content.text?.let { parts.add(InputSanitizer.sanitizeMessage(it)) }
        return parts.joinToString("\n").ifBlank { "Shared content" }
    }

    private fun getFileName(uri: Uri): String? {
        // Simplified - in production use ContentResolver
        return uri.lastPathSegment
    }

    private fun filterConversations(
        conversations: List<ConversationEntity>,
        query: String
    ): List<ConversationEntity> {
        if (query.isBlank()) return conversations
        return conversations.filter { conv ->
            conv.name?.contains(query, ignoreCase = true) == true ||
                    conv.lastMessageContent?.contains(query, ignoreCase = true) == true
        }
    }
}
