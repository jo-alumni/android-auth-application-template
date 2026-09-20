package com.example.authapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.example.authapplication.feature.detail.DetailRoute
import com.example.authapplication.feature.detail.detailScreen
import com.example.authapplication.feature.favorite.FavoriteRoute
import com.example.authapplication.feature.favorite.favoriteScreen
import com.example.authapplication.feature.home.HomeRoute
import com.example.authapplication.feature.home.homeScreen
import com.example.authapplication.feature.login.LoginRoute
import com.example.authapplication.feature.login.loginScreen
import com.example.authapplication.feature.notification.notificationScreen
import com.example.authapplication.feature.search.SearchRoute
import com.example.authapplication.feature.search.searchScreen

/**
 * 認証前後でネストされたNavGraphに分割する。
 * [AuthGraphRoute] はログイン前の画面群、[MainGraphRoute] はログイン後の画面群を束ねる。
 * グラフ間の遷移では `popUpTo` もグラフ単位（[AuthGraphRoute] / [MainGraphRoute]）で指定し、
 * 遷移元グラフの画面をまとめてバックスタックから取り除く。
 *
 * 画面のルート（[HomeRoute] / [DetailRoute] など）は各featureモジュールが持ち、
 * 「どのルートへ遷移するか」を決めるのはこのファイルだけにする。featureは遷移先を知らず、
 * `navigateDetail: (String) -> Unit` のようなコールバックで通知するだけになる
 * （配置の方針は docs/navigation-routes.md 参照）。
 *
 * WindowInsetsはこの層では扱わない。各画面が必要なinsetsを自分で解決する
 * （docs/window-insets.md 参照）。
 *
 * @param startDestination 起動時に最初に表示するグラフ。認証状態から決まるが、
 *   **一度決まった後は変化させない**。[NavHost] は `startDestination` が変わるとグラフを
 *   作り直すため、変化させると認証状態の変化がナビゲーションへ効く経路が2本になってしまう
 *   （docs/auth-navigation.md 参照）。起動後の認証解除は `AuthApplicationApp` が
 *   [AppState.navigateLogin] を呼んで扱う。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        navigation<AuthGraphRoute>(startDestination = LoginRoute) {
            loginScreen(navigateHome = {
                navController.navigate(MainGraphRoute) {
                    popUpTo(AuthGraphRoute) { inclusive = true }
                }
            })
        }
        navigation<MainGraphRoute>(startDestination = HomeRoute) {
            homeScreen(navigateDetail = { itemId -> navController.navigate(DetailRoute(itemId)) })
            searchScreen(navigateDetail = { itemId -> navController.navigate(DetailRoute(itemId)) })
            favoriteScreen(navigateDetail = { itemId -> navController.navigate(DetailRoute(itemId)) })
            detailScreen(navigateBack = { navController.popBackStack() })
            notificationScreen(navigateBack = { navController.popBackStack() })
        }
    }
}
