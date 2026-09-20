package com.example.authapplication.feature.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppNavTransitions
import com.example.authapplication.core.ui.LocalSnackBarHostState

/** ホーム画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<HomeRoute>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
        popEnterTransition = AppNavTransitions.fadeEnter,
        popExitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = LocalSnackBarHostState.current
        // Configuration変更時に読み直されるよう、Context経由ではなくLocalResourcesから解決する。
        val resources = LocalResources.current

        // ViewModelは「何が起きたか」を発行するだけで、Snackbarを出す判断はUI側が担当する。
        LaunchedEffect(viewModel, resources) {
            viewModel.event.collect { event ->
                when (event) {
                    is HomeEvent.ShowErrorSnackbar -> {
                        // イベントは文字列リソースIDで届くため、文言の解決はここ（UI側）で行う。
                        snackbarHostState.showSnackbar(resources.getString(event.messageResId))
                    }
                }
            }
        }

        HomeScreen(
            uiState = uiState,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            onRetryClick = viewModel::retry,
        )
    }
}
