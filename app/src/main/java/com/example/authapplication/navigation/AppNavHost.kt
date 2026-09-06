package com.example.authapplication.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.feature.login.loginScreen
import com.example.authapplication.feature.detail.detailScreen
import com.example.authapplication.feature.favorite.favoriteScreen
import com.example.authapplication.feature.home.homeScreen
import com.example.authapplication.feature.notification.notificationScreen
import com.example.authapplication.feature.search.searchScreen

/**
 * 認証前後でネストされたNavGraphに分割する。
 * [AppRoute.AuthGraph] はログイン前の画面群、[AppRoute.MainGraph] はログイン後の画面群を束ねる。
 * グラフ間の遷移では `popUpTo` もグラフ単位（[AppRoute.AuthGraph] / [AppRoute.MainGraph]）で指定し、
 * 遷移元グラフの画面をまとめてバックスタックから取り除く。
 *
 * [listContentPadding] はホーム/検索/お気に入りのリストにのみ渡すスクロール余白で、これらの画面の
 * レイアウト領域自体（[modifier]）はシステムナビゲーションバー領域まで広げつつ、リストの末尾に
 * 必要な余白だけを持たせる用途に使う。
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    modifier: Modifier = Modifier,
    listContentPadding: PaddingValues = PaddingValues(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        navigation<AppRoute.AuthGraph>(startDestination = AppRoute.Login) {
            loginScreen(navigateHome = {
                navController.navigate(AppRoute.MainGraph) {
                    popUpTo(AppRoute.AuthGraph) { inclusive = true }
                }
            })
        }
        navigation<AppRoute.MainGraph>(startDestination = AppRoute.Home) {
            homeScreen(
                navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) },
                contentPadding = listContentPadding,
            )
            searchScreen(
                navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) },
                contentPadding = listContentPadding,
            )
            favoriteScreen(
                navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) },
                contentPadding = listContentPadding,
            )
            detailScreen(navigateBack = { navController.popBackStack() })
            notificationScreen(navigateBack = { navController.popBackStack() })
        }
    }
}
