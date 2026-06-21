package com.nexchat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nexchat.ui.navigation.NexChatNavGraph
import com.nexchat.ui.navigation.DeepLinkHandler
import com.nexchat.ui.theme.NexChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val deepLinkHandler = DeepLinkHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Parse deep link intent
        deepLinkHandler.handleIntent(intent)

        setContent {
            NexChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NexChatNavGraph(
                        initialDeepLink = deepLinkHandler.pendingDeepLink
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkHandler.handleIntent(intent)
    }
}
