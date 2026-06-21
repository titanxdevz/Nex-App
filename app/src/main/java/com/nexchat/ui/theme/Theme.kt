package com.nexchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NexChatColors.Accent,
    onPrimary = NexChatColors.Primary,
    secondary = NexChatColors.Secondary,
    onSecondary = NexChatColors.Primary,
    background = NexChatColors.ChatBg,
    onBackground = NexChatColors.Primary,
    surface = NexChatColors.Surface,
    onSurface = NexChatColors.Primary,
    surfaceVariant = NexChatColors.Surface2,
    onSurfaceVariant = NexChatColors.Secondary,
    error = NexChatColors.Error,
    onError = NexChatColors.Primary,
    border = NexChatColors.Border,
)

@Composable
fun NexChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = NexChatTypographyDef,
        content = content
    )
}
