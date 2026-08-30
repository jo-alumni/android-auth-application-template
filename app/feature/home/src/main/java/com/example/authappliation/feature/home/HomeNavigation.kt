package com.example.authappliation.feature.home

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.authappliation.core.navigation.AppRoute

/** ホーム画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.homeScreen(onItemClick: (String) -> Unit) {
    composable<AppRoute.Home> {
        val viewModel: HomeViewModel = hiltViewModel()
        val items by viewModel.items.collectAsStateWithLifecycle()
        HomeScreen(items = items, onItemClick = onItemClick)
    }
}
