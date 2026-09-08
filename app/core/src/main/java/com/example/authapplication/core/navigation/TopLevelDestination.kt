package com.example.authapplication.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.authapplication.core.R

/**
 * ボトムバーに表示するトップレベル画面。
 *
 * ラベルは文言そのものではなく文字列リソースIDで持つ。
 * enumはComposeの外（`Context` を持たない場所）からも参照されるため、
 * 文言の解決は `stringResource(destination.labelResId)` としてUI側で行う。
 */
enum class TopLevelDestination(
    val route: AppRoute,
    @param:StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    HOME(AppRoute.Home, R.string.core_destination_home, Icons.Filled.Home),
    SEARCH(AppRoute.Search, R.string.core_destination_search, Icons.Filled.Search),
    FAVORITE(AppRoute.Favorite, R.string.core_destination_favorite, Icons.Filled.Favorite),
}
