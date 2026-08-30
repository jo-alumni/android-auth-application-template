package com.example.authappliation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.authappliation.core.navigation.AppRoute
import com.example.authappliation.feature.auth.authScreen
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
        authScreen(onLoginSuccess = {
            navController.navigate(AppRoute.Home) {
                popUpTo(AppRoute.Login) { inclusive = true }
            }
        })
        homeScreen(onItemClick = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        searchScreen(onItemClick = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        favoriteScreen(onItemClick = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
        detailScreen(onBackClick = { navController.popBackStack() })
    }
}
