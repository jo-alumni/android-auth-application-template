package com.example.authappliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authappliation.domain.auth.IsAuthenticatedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Ready(val isAuthenticated: Boolean) : AuthUiState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    isAuthenticatedUseCase: IsAuthenticatedUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = isAuthenticatedUseCase()
        .map<Boolean, AuthUiState> { AuthUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthUiState.Loading,
        )
}
