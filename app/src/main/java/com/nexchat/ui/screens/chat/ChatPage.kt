package com.nexchat.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexchat.R
import com.nexchat.data.local.entity.ConversationEntity
import com.nexchat.ui.components.Avatar
import com.nexchat.ui.components.Badge
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.ui.viewmodel.ChatViewModel

@Composable
fun ChatPage(
    onNavigateToAuth: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToDm: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToCommunity: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedConversation by remember { mutableStateOf<ConversationEntity?>(null) }

    // Animate sidebar entrance
    val sidebarOffset by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "sidebar_offset"
    )

    Row(modifier = Modifier.fillMaxSize().background(NexChatColors.ChatBg)) {
        // ─── Sidebar ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(340.dp)
                .offset(x = sidebarOffset)
                .background(NexChatColors.SidebarBg)
        ) {
            // Header
            TopAppBar(
                title = {
                    Text("Messages", color = NexChatColors.Primary, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexChatColors.SidebarBg),
                actions = {
                    IconButton(
                        onClick = { /* new chat */ },
                        modifier = Modifier.semantics { contentDescription = stringResource(R.string.a11y_new_chat) }
                    ) {
                        Icon(Icons.Default.Edit, null, tint = NexChatColors.Secondary)
                    }
                }
            )

            // Search bar with animated focus
            var searchQuery by remember { mutableStateOf("") }
            var searchFocused by remember { mutableStateOf(false) }
            val searchBorderColor by animateColorAsState(
                targetValue = if (searchFocused) NexChatColors.Accent.copy(alpha = 0.5f) else Color.Transparent,
                animationSpec = tween(200),
                label = "search_border"
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .semantics { contentDescription = stringResource(R.string.a11y_search) },
                placeholder = { Text(stringResource(R.string.chat_search), color = NexChatColors.Secondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NexChatColors.Secondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = searchBorderColor,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NexChatColors.Accent,
                    focusedContainerColor = NexChatColors.Surface,
                    unfocusedContainerColor = NexChatColors.Surface.copy(alpha = 0.5f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filter tabs with animated selection indicator
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "DIRECT", "GROUP").forEach { filter ->
                    val isActive = uiState.filter == filter
                    val chipScale by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.95f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                        label = "chip_scale"
                    )

                    FilterChip(
                        selected = isActive,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                filter,
                                fontSize = 11.sp,
                                modifier = Modifier.scale(chipScale)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexChatColors.Accent.copy(alpha = 0.2f),
                            selectedLabelColor = NexChatColors.Accent,
                            containerColor = Color.Transparent,
                            labelColor = NexChatColors.Secondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = NexChatColors.Border,
                            selectedBorderColor = NexChatColors.Accent.copy(alpha = 0.5f),
                            enabled = true,
                            selected = isActive
                        )
                    )
                }
            }

            // Conversation list with animated content
            AnimatedContent(
                targetState = uiState.isLoading,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "conversation_loading"
            ) { isLoading ->
                if (isLoading) {
                    // Shimmer loading skeleton
                    Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                        repeat(6) {
                            ShimmerConversationItem()
                            if (it < 5) Spacer(Modifier.height(4.dp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = uiState.conversations,
                            key = { it.id }
                        ) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                isSelected = selectedConversation?.id == conversation.id,
                                onClick = {
                                    selectedConversation = conversation
                                    if (conversation.type == "GROUP") {
                                        onNavigateToGroup(conversation.id)
                                    } else {
                                        onNavigateToDm(conversation.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom navigation bar
            NavigationBar(
                containerColor = NexChatColors.SidebarBg,
                contentColor = NexChatColors.Secondary,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Chat, null) },
                    label = { Text(stringResource(R.string.nav_chats), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NexChatColors.Accent,
                        selectedTextColor = NexChatColors.Accent,
                        indicatorColor = NexChatColors.Accent.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.nav_chats) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToCalls,
                    icon = { Icon(Icons.Default.Phone, null) },
                    label = { Text(stringResource(R.string.nav_calls), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.nav_calls) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToStatus,
                    icon = { Icon(Icons.Default.Circle, null) },
                    label = { Text(stringResource(R.string.nav_status), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.nav_status) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToExplore,
                    icon = { Icon(Icons.Default.Explore, null) },
                    label = { Text(stringResource(R.string.nav_explore), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.nav_explore) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToFriends,
                    icon = { Icon(Icons.Default.PersonAdd, null) },
                    label = { Text(stringResource(R.string.nav_friends), fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                    modifier = Modifier.semantics { contentDescription = stringResource(R.string.nav_friends) }
                )
            }
        }

        // ─── Chat Window ───────────────────────────────────────────────────
        AnimatedContent(
            targetState = selectedConversation,
            transitionSpec = {
                if (targetState != null) {
                    // Slide in from right when selecting a conversation
                    slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(tween(200, delayMillis = 50)) togetherWith
                    fadeOut(tween(150))
                } else {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                }
            },
            label = "chat_content"
        ) { conversation ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(NexChatColors.ChatBg),
                contentAlignment = Alignment.Center
            ) {
                if (conversation == null) {
                    // Empty state with animated entrance
                    EmptyState()
                } else {
                    // Would show ChatWindow here
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = conversation.name ?: "Chat",
                            color = NexChatColors.Primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Conversation with ${conversation.name}",
                            color = NexChatColors.Secondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val iconOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_float"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            null,
            tint = NexChatColors.Secondary.copy(alpha = 0.2f),
            modifier = Modifier
                .size(80.dp)
                .offset(y = iconOffset.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Select a conversation",
            color = NexChatColors.Secondary,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Choose from the sidebar or start a new chat",
            color = NexChatColors.Secondary.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ShimmerConversationItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar shimmer
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NexChatColors.Surface2)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Name shimmer
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .width(120.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NexChatColors.Surface2)
            )
            Spacer(Modifier.height(6.dp))
            // Preview shimmer
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(180.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NexChatColors.Surface2)
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "conv_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> NexChatColors.ActiveBg.copy(alpha = 0.5f)
            isPressed -> NexChatColors.ActiveBg.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(150),
        label = "conv_bg"
    )

    val convDesc = buildString {
        append(conversation.name ?: "Chat")
        if (conversation.unreadCount > 0) {
            append(". ${conversation.unreadCount} unread messages")
        }
        if (conversation.lastMessageContent != null) {
            append(". Last message: ${conversation.lastMessageContent.take(50)}")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = convDesc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Avatar(
                url = conversation.avatar,
                name = conversation.name ?: "Chat",
                size = 48.dp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.name ?: "Chat",
                    color = NexChatColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (conversation.pinnedAt != null) {
                    Icon(
                        Icons.Default.PushPin, null,
                        tint = NexChatColors.Secondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.lastMessageContent ?: "",
                    color = NexChatColors.Secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (conversation.unreadCount > 0) {
                    Badge(count = conversation.unreadCount)
                }
            }
        }
    }
}
