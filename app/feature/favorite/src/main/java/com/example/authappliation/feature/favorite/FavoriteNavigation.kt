package com.example.authappliation.feature.favorite

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authappliation.core.navigation.AppRoute

/** お気に入り画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.favoriteScreen(onItemClick: (String) -> Unit) {
    composable<AppRoute.Favorite> {
        val viewModel: FavoriteViewModel = hiltViewModel()
        val items by viewModel.items.collectAsStateWithLifecycle()
        FavoriteScreen(items = items, onItemClick = onItemClick)
    }
}
