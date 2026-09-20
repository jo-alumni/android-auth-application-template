package com.example.authapplication.feature.detail

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.authapplication.core.navigation.AppNavTransitions
import kotlinx.serialization.Serializable

/**
 * 詳細画面のルート。画面を所有するこのモジュールが定義する
 * （配置の方針は docs/navigation-routes.md 参照）。
 *
 * ホーム/検索/お気に入りの各画面からも遷移するが、遷移元はこの型を知らない。
 * 一覧の画面は `navigateDetail: (String) -> Unit` に [itemId] を渡すだけで、
 * このルートを組み立てるのは `:app` の `AppNavHost`。
 */
@Serializable
data class DetailRoute(val itemId: String)

/** 詳細画面のディープリンクのベースURL。`{itemId}` は [DetailRoute.itemId] にマッピングされる。 */
const val DETAIL_DEEP_LINK_BASE_PATH = "authapplication://detail"

/**
 * 詳細画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。
 *
 * [DetailRoute] に対応する `navDeepLink` を設定しているため、
 * `authapplication://detail/{itemId}` 形式のURI（通知やIntent経由）からも本画面へ直接遷移できる。
 */
fun NavGraphBuilder.detailScreen(navigateBack: () -> Unit) {
    composable<DetailRoute>(
        deepLinks = listOf(navDeepLink<DetailRoute>(basePath = DETAIL_DEEP_LINK_BASE_PATH)),
        enterTransition = AppNavTransitions.slideInFromRight,
        popExitTransition = AppNavTransitions.slideOutToRight,
    ) {
        val viewModel: DetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DetailScreen(
            uiState = uiState,
            onBackClick = navigateBack,
            onRetryClick = viewModel::retry,
        )
    }
}
