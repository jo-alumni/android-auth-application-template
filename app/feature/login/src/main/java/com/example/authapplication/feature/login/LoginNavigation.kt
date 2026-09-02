package com.example.authapplication.feature.login

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppRoute

/** ログイン画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.loginScreen(navigateHome: () -> Unit) {
    composable<AppRoute.Login> {
        val viewModel: LoginViewModel = hiltViewModel()

        LaunchedEffect(viewModel) {
            viewModel.event.collect { event ->
                when (event) {
                    LoginEvent.NavigateHome -> navigateHome()
                }
            }
        }

        LoginScreen(onLoginClick = viewModel::login)
    }
}
