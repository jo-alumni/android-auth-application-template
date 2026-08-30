package com.example.authappliation.feature.auth

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authappliation.core.navigation.AppRoute

/** ログイン画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.authScreen(navigateHome: () -> Unit) {
    composable<AppRoute.Login> {
        val viewModel: LoginViewModel = hiltViewModel()
        LoginScreen(onLoginClick = { viewModel.login(onSuccess = navigateHome) })
    }
}
