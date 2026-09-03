package com.example.authapplication.feature.detail

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.authapplication.core.navigation.AppRoute

/** 詳細画面のディープリンクのベースURL。`{itemId}` は [AppRoute.Detail.itemId] にマッピングされる。 */
const val DETAIL_DEEP_LINK_BASE_PATH = "authapplication://detail"

/**
 * 詳細画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。
 *
 * [AppRoute.Detail] に対応する `navDeepLink` を設定しているため、
 * `authapplication://detail/{itemId}` 形式のURI（通知やIntent経由）からも本画面へ直接遷移できる。
 */
fun NavGraphBuilder.detailScreen(navigateBack: () -> Unit) {
    composable<AppRoute.Detail>(
        deepLinks = listOf(navDeepLink<AppRoute.Detail>(basePath = DETAIL_DEEP_LINK_BASE_PATH)),
    ) {
        val viewModel: DetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DetailScreen(uiState = uiState, onBackClick = navigateBack)
    }
}
