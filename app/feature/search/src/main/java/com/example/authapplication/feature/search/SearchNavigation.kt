package com.example.authapplication.feature.search

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

/** 検索画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.searchScreen(
    navigateDetail: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    composable<AppRoute.Search>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
        popEnterTransition = AppNavTransitions.fadeEnter,
        popExitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: SearchViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(viewModel) {
            viewModel.event.collect { event ->
                when (event) {
                    is SearchEvent.ShowErrorSnackbar -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }

        SearchScreen(
            uiState = uiState,
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            onRetryClick = viewModel::retry,
            contentPadding = contentPadding,
            snackbarHostState = snackbarHostState,
        )
    }
}
