package com.example.authapplication.feature.login

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppNavTransitions
import kotlinx.serialization.Serializable

/**
 * ログイン画面のルート。画面を所有するこのモジュールが定義する
 * （配置の方針は docs/navigation-routes.md 参照）。
 */
@Serializable
data object LoginRoute

/** ログイン画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.loginScreen(navigateHome: () -> Unit) {
    composable<LoginRoute>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: LoginViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.event.collect { event ->
                when (event) {
                    LoginEvent.NavigateHome -> navigateHome()
                }
            }
        }

        LoginScreen(uiState = uiState, onLoginClick = viewModel::login)
    }
}
