package com.example.authapplication.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.authapplication.feature.favorite.FavoriteRoute
import com.example.authapplication.feature.favorite.R as FavoriteR
import com.example.authapplication.feature.home.HomeRoute
import com.example.authapplication.feature.home.R as HomeR
import com.example.authapplication.feature.search.R as SearchR
import com.example.authapplication.feature.search.SearchRoute

/**
 * ボトムバー / レール / ドロワーに表示するトップレベル画面。
 *
 * 各featureのルートとラベルを束ねる定義なので、全featureを知ってよい `:app` に置く。
 * 共通モジュール（`:app:core`）に置くと、共通モジュールがfeatureを参照することになり
 * 依存の向きが逆流する（配置の方針は docs/navigation-routes.md 参照）。
 *
 * [route] の型が `Any` なのは、ルートを各featureへ分散させた結果、
 * 画面のルートに共通の親型が無くなったため。Navigation Composeの型安全ナビゲーションは
 * `@Serializable` なオブジェクトをそのまま受け取るので、遷移・現在地の判定はこのままで行える。
 *
 * ラベルは文言そのものではなく文字列リソースIDで持つ。
 * enumはComposeの外（`Context` を持たない場所）からも参照されるため、
 * 文言の解決は `stringResource(destination.labelResId)` としてUI側で行う。
 */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    HOME(HomeRoute, HomeR.string.feature_home_title, Icons.Filled.Home),
    SEARCH(SearchRoute, SearchR.string.feature_search_title, Icons.Filled.Search),
    FAVORITE(FavoriteRoute, FavoriteR.string.feature_favorite_title, Icons.Filled.Favorite),
}
