package com.nexchat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nexchat.R
import com.nexchat.ui.theme.NexChatColors

@Composable
fun Avatar(
    url: String?,
    name: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val contentDesc = name ?: stringResource(R.string.a11y_avatar)

    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDesc,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .semantics { this.contentDescription = contentDesc }
        )
    } else {
        val initials = name?.take(2)?.uppercase() ?: "?"
        val colors = listOf(
            Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFFEC4899),
            Color(0xFF14B8A6), Color(0xFFF59E0B), Color(0xFFEF4444)
        )
        val colorIndex = (name?.hashCode() ?: 0).mod(colors.size)
            .let { if (it < 0) it + colors.size else it }

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(colors[colorIndex])
                .semantics { this.contentDescription = contentDesc },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PresenceDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color = if (isOnline) NexChatColors.OnlineGlow else NexChatColors.Offline
    val statusDesc = stringResource(
        if (isOnline) R.string.a11y_online else R.string.a11y_offline
    )

    val infiniteTransition = rememberInfiniteTransition(label = "presence")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOnline) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isOnline) 0f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = statusDesc },
        contentAlignment = Alignment.Center
    ) {
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun Badge(
    count: Int,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "badge_scale"
    )
    val badgeDesc = stringResource(R.string.a11y_unread_messages, count)

    Box(
        modifier = modifier
            .scale(scale)
            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(NexChatColors.Accent)
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .semantics { contentDescription = badgeDesc },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NexChatColors.GlassBg,
                        NexChatColors.GlassBg.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun BubbleGlass(
    isSent: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = if (isSent) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val gradient = if (isSent) {
        Brush.linearGradient(
            listOf(NexChatColors.SentGradientStart, NexChatColors.SentGradientEnd)
        )
    } else {
        Brush.linearGradient(
            listOf(NexChatColors.ReceivedGradientStart, NexChatColors.ReceivedGradientEnd)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
    ) {
        content()
    }
}

@Composable
fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val desc = stringResource(R.string.a11y_loading)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = desc }
    ) {
        repeat(3) { i ->
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = i * 120, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offset.dp)
                    .clip(CircleShape)
                    .background(NexChatColors.Secondary)
            )
        }
    }
}
