package com.example.authapplication.feature.notification

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import kotlinx.serialization.Serializable

/**
 * 通知画面のルート。画面を所有するこのモジュールが定義する
 * （配置の方針は docs/navigation-routes.md 参照）。
 */
@Serializable
data object NotificationRoute

/**
 * 通知画面を全画面ダイアログとしてNavGraphに登録する。NavControllerは公開せずコールバックで通知する。
 * `DialogProperties(usePlatformDefaultWidth = false)` によりダイアログの幅制限を外し、画面全体を覆う。
 */
fun NavGraphBuilder.notificationScreen(navigateBack: () -> Unit) {
    dialog<NotificationRoute>(
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val viewModel: NotificationViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        NotificationScreen(
            uiState = uiState,
            onCloseClick = navigateBack,
            onRetryClick = viewModel::retry,
        )
    }
}
