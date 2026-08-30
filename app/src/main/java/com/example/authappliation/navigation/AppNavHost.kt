package com.example.authappliation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.authappliation.core.navigation.AppRoute
import com.example.authappliation.feature.login.loginScreen
import com.example.authappliation.feature.detail.detailScreen
import com.example.authappliation.feature.favorite.favoriteScreen
import com.example.authappliation.feature.home.homeScreen
import com.example.authappliation.feature.search.searchScreen

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
        loginScreen(navigateHome = {
            navController.navigate(AppRoute.Home) {
                popUpTo(AppRoute.Login) { inclusive = true }
            }
        })
        homeScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        searchScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        favoriteScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        detailScreen(navigateBack = { navController.popBackStack() })
    }
}
