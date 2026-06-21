package com.nexchat.ui.screens.join

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexchat.ui.components.Avatar
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.ui.viewmodel.JoinGroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupPage(
    token: String,
    onJoined: () -> Unit,
    onBack: () -> Unit,
    viewModel: JoinGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(token) {
        viewModel.previewInvite(token)
    }

    LaunchedEffect(uiState.joined) {
        if (uiState.joined) onJoined()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Group", color = NexChatColors.Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NexChatColors.Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexChatColors.SidebarBg)
            )
        },
        containerColor = NexChatColors.ChatBg
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.GroupAdd, null, tint = NexChatColors.Accent, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.groupName?.let { "Join $it" } ?: "You've been invited to join a group",
                        color = NexChatColors.Primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))

                    if (uiState.memberCount != null) {
                        Text(
                            "${uiState.memberCount} members",
                            color = NexChatColors.Secondary,
                            fontSize = 13.sp
                        )
                    } else {
                        Text("Tap below to join the conversation", color = NexChatColors.Secondary, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    AnimatedVisibility(
                        visible = uiState.error != null,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
                    ) {
                        uiState.error?.let {
                            Text(it, color = NexChatColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }

                    Button(
                        onClick = { viewModel.joinGroup(token) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = NexChatColors.Accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Join Group", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
