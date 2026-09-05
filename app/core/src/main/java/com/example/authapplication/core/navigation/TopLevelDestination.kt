package com.example.authapplication.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** ボトムバーに表示するトップレベル画面。 */
enum class TopLevelDestination(val route: AppRoute, val label: String, val icon: ImageVector) {
    HOME(AppRoute.Home, "ホーム", Icons.Filled.Home),
    SEARCH(AppRoute.Search, "検索", Icons.Filled.Search),
    FAVORITE(AppRoute.Favorite, "お気に入り", Icons.Filled.Favorite),
}
