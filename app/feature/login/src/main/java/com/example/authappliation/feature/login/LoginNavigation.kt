package com.example.authappliation.feature.login

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authappliation.core.navigation.AppRoute

/** ログイン画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.loginScreen(navigateHome: () -> Unit) {
    composable<AppRoute.Login> {
        val viewModel: LoginViewModel = hiltViewModel()
        LoginScreen(onLoginClick = { viewModel.login(onSuccess = navigateHome) })
    }
}
