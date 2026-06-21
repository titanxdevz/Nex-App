package com.nexchat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexchat.R
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.util.BiometricHelper
import com.nexchat.util.NexChatLog

/**
 * Full-screen app lock with PIN pad + biometric support.
 * Mirrors the web AppLockGate component.
 */
@Composable
fun AppLockGate(
    isLocked: Boolean,
    hasPin: Boolean,
    biometricEnabled: Boolean,
    onUnlock: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onSetupPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLocked) return

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isSetupMode by remember { mutableStateOf(false) }
    var setupStep by remember { mutableIntStateOf(0) } // 0=enter, 1=confirm
    var setupFirstPin by remember { mutableStateOf("") }

    val shakeOffset by animateFloatAsState(
        targetValue = if (error) 0f else 0f,
        animationSpec = if (error) {
            keyframes {
                durationMillis = 400
                0f at 0
                (-8f) at 50
                8f at 100
                (-6f) at 150
                6f at 200
                (-4f) at 250
                4f at 300
                0f at 400
            }
        } else {
            spring()
        },
        label = "shake"
    )

    LaunchedEffect(error) {
        if (error) {
            kotlinx.coroutines.delay(600)
            error = false
            pin = ""
        }
    }

    fun submit(value: String) {
        if (isSetupMode) {
            if (setupStep == 0) {
                setupFirstPin = value
                setupStep = 1
                pin = ""
            } else {
                if (value == setupFirstPin) {
                    onSetupPin(value)
                } else {
                    error = true
                    isSetupMode = false
                    setupStep = 0
                }
            }
        } else {
            onUnlock(value)
        }
    }

    fun press(digit: String) {
        if (error) return
        val next = (pin + digit).take(8)
        pin = next
        if (next.length >= 4) submit(next)
    }

    fun backspace() { pin = pin.dropLast(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexChatColors.ChatBg)
            .semantics { contentDescription = "App lock screen" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon with pulse
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NexChatColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = NexChatColors.Accent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (isSetupMode) {
                    if (setupStep == 0) "Set your PIN" else "Confirm your PIN"
                } else "NexChat is locked",
                color = NexChatColors.Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isSetupMode) {
                    if (setupStep == 0) "Enter a 4-8 digit PIN" else "Re-enter your PIN"
                } else "Enter your PIN to continue",
                color = NexChatColors.Secondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(32.dp))

            // PIN dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.offset(x = shakeOffset.dp)
            ) {
                val displayLength = maxOf(4, pin.length)
                repeat(displayLength) { i ->
                    val filled = i < pin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    error -> NexChatColors.Error
                                    filled -> NexChatColors.Accent
                                    else -> NexChatColors.SidebarHover
                                }
                            )
                            .semantics {
                                contentDescription = if (filled) "PIN digit entered" else "PIN digit empty"
                            }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Biometric button
            if (biometricEnabled && !isSetupMode) {
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NexChatColors.Accent.copy(alpha = 0.1f))
                        .semantics { contentDescription = "Unlock with biometric" }
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = NexChatColors.Accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(32.dp))
            }

            // Number pad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "del")
            )

            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Spacer(modifier = Modifier.size(64.dp))
                            "del" -> {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { backspace() }
                                        .semantics { contentDescription = "Delete" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Backspace,
                                        contentDescription = null,
                                        tint = NexChatColors.Secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.92f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    ),
                                    label = "key_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(NexChatColors.SidebarBg)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) { press(key) }
                                        .semantics { contentDescription = "Number $key" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = NexChatColors.Primary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Setup PIN link (only when not in setup mode and no PIN set)
            if (!hasPin && !isSetupMode) {
                Text(
                    text = "Set up PIN lock",
                    color = NexChatColors.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { isSetupMode = true }
                        .semantics { contentDescription = "Set up PIN lock" }
                )
            }
        }
    }
}
