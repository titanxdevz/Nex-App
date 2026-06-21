package com.nexchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexchat.data.repository.InviteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val joined: Boolean = false,
    val groupName: String? = null,
    val memberCount: Int? = null
)

@HiltViewModel
class JoinGroupViewModel @Inject constructor(
    private val inviteRepository: InviteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinGroupUiState())
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    fun previewInvite(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = inviteRepository.preview(token)
                val data = result.body()?.data
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groupName = data?.get("name") as? String,
                    memberCount = data?.get("memberCount") as? Int
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load invite details"
                )
            }
        }
    }

    fun joinGroup(token: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = inviteRepository.join(token)
                if (result.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, joined = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message() ?: "Failed to join group"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Network error"
                )
            }
        }
    }
}
