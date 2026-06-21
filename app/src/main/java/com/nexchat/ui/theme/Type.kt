package com.nexchat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object NexChatTypography {
    val bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = NexChatColors.Primary)
    val bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = NexChatColors.Primary)
    val bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = NexChatColors.Secondary)
    val labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NexChatColors.Primary)
    val labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexChatColors.Secondary)
    val labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NexChatColors.Secondary)
    val titleLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NexChatColors.Primary)
    val titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NexChatColors.Primary)
    val timestamp = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = NexChatColors.Secondary)
}

val NexChatTypographyDef = Typography(
    bodyLarge = NexChatTypography.bodyLarge,
    bodyMedium = NexChatTypography.bodyMedium,
    bodySmall = NexChatTypography.bodySmall,
    labelLarge = NexChatTypography.labelLarge,
    labelMedium = NexChatTypography.labelMedium,
    labelSmall = NexChatTypography.labelSmall,
    titleLarge = NexChatTypography.titleLarge,
    titleMedium = NexChatTypography.titleMedium,
)
