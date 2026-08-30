package com.example.authappliation.feature.search

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authappliation.core.navigation.AppRoute

/** 検索画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.searchScreen(onItemClick: (String) -> Unit) {
    composable<AppRoute.Search> {
        val viewModel: SearchViewModel = hiltViewModel()
        val items by viewModel.items.collectAsStateWithLifecycle()
        SearchScreen(items = items, onItemClick = onItemClick)
    }
}
