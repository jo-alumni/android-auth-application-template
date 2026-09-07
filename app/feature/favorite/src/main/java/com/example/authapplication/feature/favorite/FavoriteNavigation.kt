package com.example.authapplication.feature.favorite

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(viewModel) {
            viewModel.event.collect { event ->
                when (event) {
                    is FavoriteEvent.ShowErrorSnackbar -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }

        FavoriteScreen(
            uiState = uiState,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            onRetryClick = viewModel::retry,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
        )
    }
}
