package com.nexchat.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexchat.ui.theme.NexChatColors
import com.nexchat.ui.viewmodel.AuthStep
import com.nexchat.ui.viewmodel.AuthViewModel

// ─── Smooth transition specs for auth step switching ────────────────────────

private const val TRANSITION_DURATION = 350

// Determine slide direction based on step ordinal
private val AuthStep.slideDirection: Int
    get() = when (this) {
        AuthStep.LOGIN -> 0
        AuthStep.REGISTER -> 1
        AuthStep.OTP_VERIFY -> 2
        AuthStep.FORGOT_PASSWORD -> 3
    }

private fun slideTransitionSpec(): ContentTransform {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    ) + fadeIn(
        animationSpec = tween(TRANSITION_DURATION / 2, easing = FastOutSlowInEasing)
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    ) + fadeOut(
        animationSpec = tween(TRANSITION_DURATION / 2, easing = FastOutSlowInEasing)
    )
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Animate logo entrance
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexChatColors.ChatBg)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Logo with bounce-in animation
            Text(
                text = "NexChat",
                color = NexChatColors.Accent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .scale(logoScale)
                    .graphicsLayer { alpha = logoAlpha }
            )
            Text(
                text = "Connect with everyone",
                color = NexChatColors.Secondary,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .graphicsLayer { alpha = logoAlpha }
            )

            // AnimatedContent with smooth slide transitions between auth steps
            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = { slideTransitionSpec() },
                label = "auth_step_transition",
                contentKey = { it.ordinal }
            ) { step ->
                Column {
                    when (step) {
                        AuthStep.LOGIN -> LoginForm(
                            onLogin = { email, password -> viewModel.login(email, password) },
                            onLoginWithOtp = { email -> viewModel.sendOtp(email) },
                            onForgotPassword = { viewModel.setStep(AuthStep.FORGOT_PASSWORD) },
                            onSwitchToRegister = { viewModel.setStep(AuthStep.REGISTER) },
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onClearError = { viewModel.clearError() }
                        )
                        AuthStep.REGISTER -> RegisterForm(
                            onRegister = { name, email, password, username ->
                                viewModel.register(name, email, password, username)
                            },
                            onSwitchToLogin = { viewModel.setStep(AuthStep.LOGIN) },
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onClearError = { viewModel.clearError() }
                        )
                        AuthStep.OTP_VERIFY -> OtpForm(
                            onVerify = { code -> viewModel.loginWithOtp("", code) },
                            isLoading = uiState.isLoading,
                            error = uiState.error
                        )
                        AuthStep.FORGOT_PASSWORD -> ForgotPasswordForm(
                            onSubmit = { email -> viewModel.forgotPassword(email) },
                            onBack = { viewModel.setStep(AuthStep.LOGIN) },
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }
        }
    }
}

// ─── Animated button with press scale ──────────────────────────────────────

@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = NexChatColors.Accent),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
private fun AnimatedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "btn_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexChatColors.Accent),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(NexChatColors.Accent.copy(alpha = 0.5f))
        ),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

// ─── Login Form ────────────────────────────────────────────────────────────

@Composable
private fun LoginForm(
    onLogin: (String, String) -> Unit,
    onLoginWithOtp: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchToRegister: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Welcome back", color = NexChatColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Sign in to continue", color = NexChatColors.Secondary, fontSize = 13.sp)

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it; onClearError() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexChatColors.Accent,
                    unfocusedBorderColor = NexChatColors.Border,
                    focusedLabelColor = NexChatColors.Accent,
                    cursorColor = NexChatColors.Accent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it; onClearError() },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            "Toggle password",
                            tint = NexChatColors.Secondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexChatColors.Accent,
                    unfocusedBorderColor = NexChatColors.Border,
                    focusedLabelColor = NexChatColors.Accent,
                    cursorColor = NexChatColors.Accent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (email.isNotBlank() && password.isNotBlank()) onLogin(email, password)
                }),
                singleLine = true
            )

            Text(
                text = "Forgot password?",
                color = NexChatColors.Accent,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .clickable { onForgotPassword() }
            )

            Spacer(Modifier.height(16.dp))

            // Error with animated visibility
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                error?.let {
                    Text(it, color = NexChatColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            AnimatedButton(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedOutlinedButton(
                onClick = { onLoginWithOtp(email) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && email.isNotBlank()
            ) {
                Text("Sign in with OTP")
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Row {
        Text("Don't have an account? ", color = NexChatColors.Secondary, fontSize = 13.sp)
        Text(
            "Sign Up", color = NexChatColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSwitchToRegister() }
        )
    }
}

// ─── Register Form ─────────────────────────────────────────────────────────

@Composable
private fun RegisterForm(
    onRegister: (String, String, String, String?) -> Unit,
    onSwitchToLogin: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Create account", color = NexChatColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = displayName, onValueChange = { displayName = it; onClearError() },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                error?.let {
                    Text(it, color = NexChatColors.Error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            AnimatedButton(
                onClick = { onRegister(displayName, email, password, username.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && displayName.isNotBlank() && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Create Account", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Row {
        Text("Already have an account? ", color = NexChatColors.Secondary, fontSize = 13.sp)
        Text(
            "Sign In", color = NexChatColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSwitchToLogin() }
        )
    }
}

// ─── OTP Form ──────────────────────────────────────────────────────────────

@Composable
private fun OtpForm(onVerify: (String) -> Unit, isLoading: Boolean, error: String?) {
    var code by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verify OTP", color = NexChatColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Enter the code sent to your email", color = NexChatColors.Secondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("Verification Code") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                error?.let { Text(it, color = NexChatColors.Error, fontSize = 12.sp) }
            }

            AnimatedButton(
                onClick = { onVerify(code) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && code.length >= 4
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Verify", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Forgot Password Form ──────────────────────────────────────────────────

@Composable
private fun ForgotPasswordForm(onSubmit: (String) -> Unit, onBack: () -> Unit, isLoading: Boolean) {
    var email by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexChatColors.Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Reset Password", color = NexChatColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Enter your email to receive a reset link", color = NexChatColors.Secondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexChatColors.Accent, unfocusedBorderColor = NexChatColors.Border, cursorColor = NexChatColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            AnimatedButton(
                onClick = { onSubmit(email) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && email.isNotBlank()
            ) { Text("Send Reset Link", color = Color.White, fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))
            Text(
                "Back to Sign In", color = NexChatColors.Accent, fontSize = 13.sp,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
        }
    }
}
