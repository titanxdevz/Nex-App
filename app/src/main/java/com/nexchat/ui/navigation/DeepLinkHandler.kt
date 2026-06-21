package com.nexchat.ui.navigation

import android.content.Intent
import android.net.Uri

/**
 * Handles deep link intents and extracts navigation targets.
 *
 * Supported deep links:
 * - https://chat.92lrcorps.xyz/invite/{token} → Join group
 * - https://chat.92lrcorps.xyz/u/{username} → User profile / DM
 */
class DeepLinkHandler {

    var pendingDeepLink: DeepLink? = null
        private set

    fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        pendingDeepLink = parseUri(uri)
    }

    fun consumeDeepLink(): DeepLink? {
        val link = pendingDeepLink
        pendingDeepLink = null
        return link
    }

    private fun parseUri(uri: Uri): DeepLink? {
        val host = uri.host ?: return null
        val pathSegments = uri.pathSegments

        if (host != "chat.92lrcorps.xyz") return null

        return when {
            // https://chat.92lrcorps.xyz/invite/{token}
            pathSegments.size >= 2 && pathSegments[0] == "invite" -> {
                DeepLink.JoinGroup(token = pathSegments[1])
            }
            // https://chat.92lrcorps.xyz/u/{username}
            pathSegments.size >= 2 && pathSegments[0] == "u" -> {
                DeepLink.UserProfile(username = pathSegments[1])
            }
            else -> null
        }
    }
}

sealed class DeepLink {
    data class JoinGroup(val token: String) : DeepLink()
    data class UserProfile(val username: String) : DeepLink()
}
