package com.nexchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexchat.data.local.TokenStorage
import com.nexchat.data.repository.AuthRepository
import com.nexchat.service.SocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val step: AuthStep = AuthStep.LOGIN
)

enum class AuthStep { LOGIN, REGISTER, OTP_VERIFY, FORGOT_PASSWORD }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val socketService: SocketService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()
            _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
            if (loggedIn) {
                socketService.connect()
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = authRepository.login(email = email, password = password)
                if (result.isSuccessful && result.body()?.data != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    socketService.connect()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message() ?: "Login failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }

    fun register(displayName: String, email: String, password: String, username: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = authRepository.register(displayName, email = email, password = password, username = username)
                if (result.isSuccessful && result.body()?.data != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    socketService.connect()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message() ?: "Registration failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }

    fun loginWithOtp(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = authRepository.login(email = email, code = code)
                if (result.isSuccessful && result.body()?.data != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                    socketService.connect()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message() ?: "OTP verification failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }

    fun sendOtp(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = authRepository.sendOtp(email = email)
                if (result.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, step = AuthStep.OTP_VERIFY)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message() ?: "Failed to send OTP")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.forgotPassword(email)
                _uiState.value = _uiState.value.copy(isLoading = false, step = AuthStep.LOGIN)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            socketService.disconnect()
            authRepository.logout()
            _uiState.value = AuthUiState(isLoggedIn = false)
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun setStep(step: AuthStep) { _uiState.value = _uiState.value.copy(step = step) }
}
