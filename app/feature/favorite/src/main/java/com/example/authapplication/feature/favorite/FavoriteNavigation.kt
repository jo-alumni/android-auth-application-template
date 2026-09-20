package com.example.authapplication.feature.favorite

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppNavTransitions
import com.example.authapplication.core.ui.LocalSnackBarHostState
import kotlinx.serialization.Serializable

/**
 * お気に入り画面のルート。画面を所有するこのモジュールが定義する
 * （配置の方針は docs/navigation-routes.md 参照）。
 */
@Serializable
data object FavoriteRoute

/** お気に入り画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.favoriteScreen(navigateDetail: (String) -> Unit) {
    composable<FavoriteRoute>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
        popEnterTransition = AppNavTransitions.fadeEnter,
        popExitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: FavoriteViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = LocalSnackBarHostState.current
        // Configuration変更時に読み直されるよう、Context経由ではなくLocalResourcesから解決する。
        val resources = LocalResources.current

        LaunchedEffect(viewModel, resources) {
            viewModel.event.collect { event ->
                when (event) {
                    is FavoriteEvent.ShowErrorSnackbar -> {
                        // イベントは文字列リソースIDで届くため、文言の解決はここ（UI側）で行う。
                        snackbarHostState.showSnackbar(resources.getString(event.messageResId))
                    }
                }
            }
        }

        FavoriteScreen(
            uiState = uiState,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            onRetryClick = viewModel::retry,
        )
    }
}
