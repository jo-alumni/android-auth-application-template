package com.example.authapplication.feature.favorite

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppNavTransitions
import com.example.authapplication.core.navigation.AppRoute

/** お気に入り画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.favoriteScreen(
    navigateDetail: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    composable<AppRoute.Favorite>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
        popEnterTransition = AppNavTransitions.fadeEnter,
        popExitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: FavoriteViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        FavoriteScreen(
            uiState = uiState,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            contentPadding = contentPadding,
        )
    }
}
