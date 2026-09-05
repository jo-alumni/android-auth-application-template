package com.example.authapplication.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.auth.SetAuthTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val setAuthTokenUseCase: SetAuthTokenUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> = _event.asSharedFlow()

    fun login(id: String, password: String) {
        // ダミーバリデーション: ID/パスワードが空の場合は失敗扱いにする。
        if (id.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(BLANK_INPUT_MESSAGE)
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            // このアプリに実際の認証バックエンドは無いため、固定のダミートークンをセットする。
            setAuthTokenUseCase(DUMMY_TOKEN)
            _event.emit(LoginEvent.NavigateHome)
        }
    }

    private companion object {
        const val DUMMY_TOKEN = "dummy_token"
        const val BLANK_INPUT_MESSAGE = "IDとパスワードを入力してください"
    }
}
