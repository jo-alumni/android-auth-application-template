package com.example.authapplication.feature.search

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authapplication.core.navigation.AppNavTransitions
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.ui.LocalSnackBarHostState

/** 検索画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.searchScreen(navigateDetail: (String) -> Unit) {
    composable<AppRoute.Search>(
        enterTransition = AppNavTransitions.fadeEnter,
        exitTransition = AppNavTransitions.fadeExit,
        popEnterTransition = AppNavTransitions.fadeEnter,
        popExitTransition = AppNavTransitions.fadeExit,
    ) {
        val viewModel: SearchViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val snackbarHostState = LocalSnackBarHostState.current
        // Configuration変更時に読み直されるよう、Context経由ではなくLocalResourcesから解決する。
        val resources = LocalResources.current

        LaunchedEffect(viewModel, resources) {
            viewModel.event.collect { event ->
                when (event) {
                    is SearchEvent.ShowErrorSnackbar -> {
                        // イベントは文字列リソースIDで届くため、文言の解決はここ（UI側）で行う。
                        snackbarHostState.showSnackbar(resources.getString(event.messageResId))
                    }
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
        )
    }
}
