package com.example.authapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.auth.ClearAuthTokenUseCase
import com.example.authapplication.domain.auth.IsAuthenticatedUseCase
import com.example.authapplication.domain.debug.ObserveErrorInjectionUseCase
import com.example.authapplication.domain.debug.SetErrorInjectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Ready(val isAuthenticated: Boolean) : AuthUiState
}

sealed interface AppEvent {
    data object NavigateLogin : AppEvent
}

@HiltViewModel
class AppViewModel @Inject constructor(
    isAuthenticatedUseCase: IsAuthenticatedUseCase,
    observeErrorInjectionUseCase: ObserveErrorInjectionUseCase,
    private val clearAuthTokenUseCase: ClearAuthTokenUseCase,
    private val setErrorInjectionUseCase: SetErrorInjectionUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = isAuthenticatedUseCase()
        .map<Boolean, AuthUiState> { AuthUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthUiState.Loading,
        )

    /**
     * デバッグメニューのエラー注入スイッチの状態。
     * 画面をまたいで使う設定なので、画面ごとのViewModelではなく [AppViewModel] が保持する。
     */
    val isErrorInjectionEnabled: StateFlow<Boolean> = observeErrorInjectionUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _event = MutableSharedFlow<AppEvent>()
    val event: SharedFlow<AppEvent> = _event.asSharedFlow()

    /** ログアウトする。認証状態のみ解除し、アプリデータは削除しない。 */
    fun logout() {
        viewModelScope.launch {
            clearAuthTokenUseCase()
            _event.emit(AppEvent.NavigateLogin)
        }
    }

    fun setErrorInjectionEnabled(enabled: Boolean) {
        viewModelScope.launch { setErrorInjectionUseCase(enabled) }
    }
}
