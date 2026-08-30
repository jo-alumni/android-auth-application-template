package com.example.authappliation.core.navigation

/** ボトムバーに表示するトップレベル画面。 */
enum class TopLevelDestination(val route: AppRoute, val label: String) {
    HOME(AppRoute.Home, "ホーム"),
    SEARCH(AppRoute.Search, "検索"),
    FAVORITE(AppRoute.Favorite, "お気に入り"),
}
