package com.example.authappliation.feature.detail

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.authappliation.core.navigation.AppRoute

/** 詳細画面をNavGraphに登録する。NavControllerは公開せずコールバックで通知する。 */
fun NavGraphBuilder.detailScreen(navigateBack: () -> Unit) {
    composable<AppRoute.Detail> { backStackEntry ->
        val detail: AppRoute.Detail = backStackEntry.toRoute()
        DetailScreen(itemId = detail.itemId, onBackClick = navigateBack)
    }
}
