package com.example.authapplication.navigation

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
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: AppRoute,
    modifier: Modifier = Modifier,
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
            homeScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
            searchScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
            favoriteScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
            detailScreen(navigateBack = { navController.popBackStack() })
            notificationScreen(navigateBack = { navController.popBackStack() })
        }
    }
}
