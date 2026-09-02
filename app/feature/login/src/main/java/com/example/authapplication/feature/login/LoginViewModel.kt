package com.example.authapplication.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.auth.SetAuthTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val setAuthTokenUseCase: SetAuthTokenUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> = _event.asSharedFlow()

    fun login() {
        viewModelScope.launch {
            // このアプリに実際の認証バックエンドは無いため、固定のダミートークンをセットする。
            setAuthTokenUseCase(DUMMY_TOKEN)
            _event.emit(LoginEvent.NavigateHome)
        }
    }

    private companion object {
        const val DUMMY_TOKEN = "dummy_token"
    }
}
