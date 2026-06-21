package com.nexchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexchat.ui.screens.share.ShareScreen
import com.nexchat.ui.theme.NexChatTheme
import com.nexchat.ui.viewmodel.ShareMode
import com.nexchat.ui.viewmodel.ShareViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dedicated activity for handling share intents from other apps.
 * Shows a contact/conversation picker to share content to NexChat conversations.
 *
 * Supports:
 * - Single share: tap a conversation to share immediately
 * - Multiple share: toggle conversations, then tap Send
 * - All content types: text, images, videos, audio, URLs
 */
@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shareIntent = intent

        setContent {
            NexChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShareActivityContent(
                        shareIntent = shareIntent,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun ShareActivityContent(
    shareIntent: Intent,
    onFinish: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(shareIntent) {
        viewModel.parseIntent(shareIntent)
    }

    LaunchedEffect(uiState.shareComplete) {
        if (uiState.shareComplete) {
            onFinish()
        }
    }

    ShareScreen(
        uiState = uiState,
        onToggleConversation = { viewModel.toggleConversationSelection(it) },
        onSetShareMode = { viewModel.setShareMode(it) },
        onSearch = { viewModel.setSearchQuery(it) },
        onShare = { viewModel.share() },
        onDismiss = { onFinish() }
    )
}
