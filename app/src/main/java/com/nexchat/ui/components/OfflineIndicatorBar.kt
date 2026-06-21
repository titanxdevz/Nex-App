package com.nexchat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexchat.R
import com.nexchat.service.SocketState
import com.nexchat.ui.theme.NexChatColors

/**
 * Persistent connection status bar shown at the top of the screen.
 * Animates in/out based on socket connection state.
 */
@Composable
fun OfflineIndicatorBar(
    connectionState: SocketState,
    modifier: Modifier = Modifier
) {
    val isVisible = connectionState !is SocketState.Connected

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val (backgroundColor, icon, text) = when (connectionState) {
            is SocketState.Connecting -> Triple(
                NexChatColors.Warning.copy(alpha = 0.9f),
                Icons.Default.Sync,
                stringResource(R.string.status_connecting)
            )
            is SocketState.Error -> Triple(
                NexChatColors.Error.copy(alpha = 0.9f),
                Icons.Default.CloudOff,
                stringResource(R.string.status_offline)
            )
            is SocketState.Disconnected -> Triple(
                NexChatColors.Warning.copy(alpha = 0.9f),
                Icons.Default.CloudQueue,
                stringResource(R.string.status_reconnecting)
            )
            is SocketState.Connected -> Triple(
                NexChatColors.Success,
                Icons.Default.CloudDone,
                stringResource(R.string.status_connected)
            )
        }

        // Pulsing animation for connecting/reconnecting states
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .statusBarsPadding()
                .semantics { contentDescription = text }
                .alpha(if (connectionState is SocketState.Connecting) pulseAlpha else 1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Spinning indicator for connecting state
                if (connectionState is SocketState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(14.dp)
                            .semantics { contentDescription = "Loading" },
                        color = Color.White,
                        strokeWidth = 1.5.dp
                    )
                }
            }
        }
    }
}

private val Color.Companion.White: androidx.compose.ui.graphics.Color
    get() = androidx.compose.ui.graphics.Color.White
