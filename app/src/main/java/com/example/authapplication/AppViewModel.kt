package com.example.authapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.auth.ClearAuthTokenUseCase
import com.example.authapplication.domain.auth.IsAuthenticatedUseCase
import com.example.authapplication.domain.debug.ObserveErrorInjectionUseCase
import com.example.authapplication.domain.debug.SetErrorInjectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** アプリ全体の認証状態。起動直後は永続化層の読み込みが終わるまで [Loading]。 */
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Ready(val isAuthenticated: Boolean) : AuthUiState
}

/**
 * 認証状態と、画面をまたぐデバッグ設定を保持するアプリ全体のViewModel。
 *
 * このViewModelは「認証状態がどう変わったか」を [authState] として公開するだけで、
 * 「どこへ遷移するか」は持たない。ログアウト・トークン失効のいずれも
 * トークンを破棄するだけで、ログイン画面へ戻す判断は `AuthApplicationApp` が
 * [authState] の変化から行う（理由は docs/auth-navigation.md 参照）。
 * そのため遷移を指示する単発イベント（`SharedFlow`）は持たない。
 */
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

    /** ログアウトする。認証状態のみ解除し、アプリデータは削除しない。 */
    fun logout() {
        viewModelScope.launch { clearAuthTokenUseCase() }
    }

    /**
     * トークンの失効（サーバ側での失効・有効期限切れ）を模してトークンを破棄する。
     *
     * 本アプリには実際に失効し得る通信処理が無いため、デバッグメニューから手動で起こせるようにしている。
     * 処理は [logout] と同じだが、「ユーザーのログアウト操作以外でも認証は解除され得る」ことと、
     * その場合も同じ経路でログイン画面へ戻ることを手元で確認するために別の関数として公開する。
     */
    fun expireAuthToken() {
        viewModelScope.launch { clearAuthTokenUseCase() }
    }

    fun setErrorInjectionEnabled(enabled: Boolean) {
        viewModelScope.launch { setErrorInjectionUseCase(enabled) }
    }
}
