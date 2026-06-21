package com.nexchat.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexchat.ui.components.Avatar
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = NexChatColors.Primary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NexChatColors.Primary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexChatColors.SidebarBg)
            )
        },
        containerColor = NexChatColors.ChatBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile section
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(url = null, name = "User", size = 56.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Your Name", color = NexChatColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("@username", color = NexChatColors.Secondary, fontSize = 13.sp)
                    }
                }
            }

            // Settings items
            val settingsItems = listOf(
                Triple(Icons.Default.Person, "Account", "Email, phone, username"),
                Triple(Icons.Default.Lock, "Privacy", "Blocked users, last seen"),
                Triple(Icons.Default.Notifications, "Notifications", "Message & call alerts"),
                Triple(Icons.Default.Palette, "Appearance", "Theme, chat wallpaper"),
                Triple(Icons.Default.Storage, "Data & Storage", "Network usage, downloads"),
                Triple(Icons.Default.Security, "Security", "Two-factor, app lock"),
                Triple(Icons.Default.Info, "About", "Version, terms, privacy"),
            )

            settingsItems.forEach { (icon, title, subtitle) ->
                ListItem(
                    headlineContent = { Text(title, color = NexChatColors.Primary) },
                    supportingContent = { Text(subtitle, color = NexChatColors.Secondary, fontSize = 12.sp) },
                    leadingContent = { Icon(icon, null, tint = NexChatColors.Secondary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = NexChatColors.Secondary) },
                    modifier = Modifier.clickable { /* navigate */ }
                )
                HorizontalDivider(color = NexChatColors.Border.copy(alpha = 0.3f))
            }

            Spacer(Modifier.height(16.dp))

            // Logout
            ListItem(
                headlineContent = { Text("Sign Out", color = NexChatColors.Error) },
                leadingContent = { Icon(Icons.Default.Logout, null, tint = NexChatColors.Error) },
                modifier = Modifier.clickable {
                    authViewModel.logout()
                    onLogout()
                }
            )
        }
    }
}
