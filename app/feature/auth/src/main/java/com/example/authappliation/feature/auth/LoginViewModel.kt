package com.example.authappliation.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authappliation.domain.auth.SetAuthenticatedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val setAuthenticatedUseCase: SetAuthenticatedUseCase,
) : ViewModel() {

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            setAuthenticatedUseCase(true)
            onSuccess()
        }
    }
}
