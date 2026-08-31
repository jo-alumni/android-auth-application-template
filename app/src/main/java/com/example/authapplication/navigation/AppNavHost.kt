package com.example.authapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.feature.login.loginScreen
import com.example.authapplication.feature.detail.detailScreen
import com.example.authapplication.feature.favorite.favoriteScreen
import com.example.authapplication.feature.home.homeScreen
import com.example.authapplication.feature.search.searchScreen

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
