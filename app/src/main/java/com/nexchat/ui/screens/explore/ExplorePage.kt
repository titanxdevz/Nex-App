package com.nexchat.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexchat.ui.theme.NexChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorePage(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore", color = NexChatColors.Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NexChatColors.Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexChatColors.SidebarBg)
            )
        },
        containerColor = NexChatColors.ChatBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Explore, null, tint = NexChatColors.Secondary.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("Discover communities", color = NexChatColors.Secondary, fontSize = 16.sp)
            }
        }
    }
}
