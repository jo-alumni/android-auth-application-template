package com.example.authapplication.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
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
        SearchScreen(
            uiState = uiState,
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onItemClick = navigateDetail,
            onFavoriteClick = viewModel::toggleFavorite,
            contentPadding = contentPadding,
        )
    }
}
