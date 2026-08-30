package com.example.authappliation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.authappliation.core.navigation.AppRoute

/**
 * ナビゲーション骨格。現時点ではプレースホルダー画面のみを繋いでおり、
 * 各featureモジュールの実装（Step5以降）で実際の画面Composableに置き換える。
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
        composable<AppRoute.Login> {
            PlaceholderScreen(title = "ログイン画面") {
                Button(onClick = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Login) { inclusive = true }
                    }
                }) {
                    Text("ログイン")
                }
            }
        }
        composable<AppRoute.Home> {
            PlaceholderScreen(title = "ホーム画面") {
                Button(onClick = { navController.navigate(AppRoute.Detail(itemId = "1")) }) {
                    Text("詳細画面へ")
                }
            }
        }
        composable<AppRoute.Search> {
            PlaceholderScreen(title = "検索画面") {
                Button(onClick = { navController.navigate(AppRoute.Detail(itemId = "1")) }) {
                    Text("詳細画面へ")
                }
            }
        }
        composable<AppRoute.Favorite> {
            PlaceholderScreen(title = "お気に入り画面") {
                Button(onClick = { navController.navigate(AppRoute.Detail(itemId = "1")) }) {
                    Text("詳細画面へ")
                }
            }
        }
        composable<AppRoute.Detail> { backStackEntry ->
            val detail: AppRoute.Detail = backStackEntry.toRoute()
            PlaceholderScreen(title = "詳細画面 (id: ${detail.itemId})") {
                Button(onClick = { navController.popBackStack() }) {
                    Text("戻る")
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}
