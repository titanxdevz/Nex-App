package com.nexchat.ui.screens.share

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexchat.R
import com.nexchat.data.local.entity.ConversationEntity
import com.nexchat.ui.components.Avatar
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.ui.viewmodel.ShareMode
import com.nexchat.ui.viewmodel.ShareUiState
import com.nexchat.ui.viewmodel.ShareViewModel

@Composable
fun ShareScreen(
    uiState: ShareUiState,
    onToggleConversation: (String) -> Unit,
    onSetShareMode: (ShareMode) -> Unit,
    onSearch: (String) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexChatColors.ChatBg)
    ) {
        // ─── Header ────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        stringResource(R.string.share_title),
                        color = NexChatColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (uiState.selectedConversationIds.isNotEmpty()) {
                        Text(
                            "${uiState.selectedConversationIds.size} selected",
                            color = NexChatColors.Accent,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.a11y_close), tint = NexChatColors.Primary)
                }
            },
            actions = {
                // Single / Multiple toggle
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexChatColors.Surface)
                        .padding(2.dp)
                ) {
                    ShareMode.entries.forEach { mode ->
                        val isSelected = uiState.shareMode == mode
                        val label = when (mode) {
                            ShareMode.SINGLE -> "One"
                            ShareMode.MULTIPLE -> "Many"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) NexChatColors.Accent.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { onSetShareMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .semantics { contentDescription = "Share to $label users" }
                        ) {
                            Text(
                                label,
                                color = if (isSelected) NexChatColors.Accent else NexChatColors.Secondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NexChatColors.SidebarBg)
        )

        // ─── Shared Content Preview ────────────────────────────────────────
        uiState.sharedContent?.let { content ->
            SharedContentPreview(content)
        }

        // ─── Search ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.share_search_conversations), color = NexChatColors.Secondary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = NexChatColors.Secondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = NexChatColors.Accent,
                focusedContainerColor = NexChatColors.Surface,
                unfocusedContainerColor = NexChatColors.Surface
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // ─── Conversation List ─────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = uiState.filteredConversations,
                key = { it.id }
            ) { conversation ->
                ShareConversationItem(
                    conversation = conversation,
                    isSelected = conversation.id in uiState.selectedConversationIds,
                    shareMode = uiState.shareMode,
                    onClick = { onToggleConversation(conversation.id) }
                )
            }
        }

        // ─── Send Button ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.selectedConversationIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexChatColors.SidebarBg)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isSharing,
                    colors = ButtonDefaults.buttonColors(containerColor = NexChatColors.Accent),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        val count = uiState.selectedConversationIds.size
                        Text(
                            if (count == 1) stringResource(R.string.share_send)
                            else stringResource(R.string.share_send_to, count),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedContentPreview(content: com.nexchat.ui.viewmodel.SharedContent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NexChatColors.Surface2),
                contentAlignment = Alignment.Center
            ) {
                when {
                    content.mimeType?.startsWith("image/") == true -> {
                        AsyncImage(
                            model = content.uri,
                            contentDescription = "Shared image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    content.mimeType?.startsWith("video/") == true -> {
                        Icon(Icons.Default.VideoFile, null, tint = NexChatColors.Secondary)
                    }
                    content.mimeType?.startsWith("audio/") == true -> {
                        Icon(Icons.Default.AudioFile, null, tint = NexChatColors.Secondary)
                    }
                    content.text?.contains("youtube.com") == true ||
                    content.text?.contains("youtu.be") == true -> {
                        Icon(Icons.Default.PlayCircle, null, tint = Color.Red)
                    }
                    else -> {
                        Icon(Icons.Default.Share, null, tint = NexChatColors.Secondary)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                content.title?.let {
                    Text(
                        it,
                        color = NexChatColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                content.text?.let {
                    Text(
                        it.take(100),
                        color = NexChatColors.Secondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareConversationItem(
    conversation: ConversationEntity,
    isSelected: Boolean,
    shareMode: ShareMode,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "item_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(
                if (isSelected) NexChatColors.Accent.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "${conversation.name ?: "Chat"}. ${
                    if (isSelected) "Selected" else "Not selected"
                }"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(if (shareMode == ShareMode.SINGLE) CircleShape else RoundedCornerShape(6.dp))
                .border(
                    width = 2.dp,
                    color = if (isSelected) NexChatColors.Accent else NexChatColors.Secondary.copy(alpha = 0.5f),
                    shape = if (shareMode == ShareMode.SINGLE) CircleShape else RoundedCornerShape(6.dp)
                )
                .background(
                    if (isSelected) NexChatColors.Accent else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Avatar(
            url = conversation.avatar,
            name = conversation.name ?: "Chat",
            size = 44.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name ?: "Chat",
                color = NexChatColors.Primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.type == "GROUP") {
                Text(
                    text = "Group",
                    color = NexChatColors.Secondary,
                    fontSize = 12.sp
                )
            }
        }

        if (conversation.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NexChatColors.Accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (conversation.unreadCount > 99) "99+" else "${conversation.unreadCount}",
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}
