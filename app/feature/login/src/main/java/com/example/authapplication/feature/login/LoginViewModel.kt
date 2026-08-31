package com.example.authapplication.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.auth.SetAuthTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val setAuthTokenUseCase: SetAuthTokenUseCase,
) : ViewModel() {

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // このアプリに実際の認証バックエンドは無いため、固定のダミートークンをセットする。
            setAuthTokenUseCase(DUMMY_TOKEN)
            onSuccess()
        }
    }

    private companion object {
        const val DUMMY_TOKEN = "dummy_token"
    }
}
