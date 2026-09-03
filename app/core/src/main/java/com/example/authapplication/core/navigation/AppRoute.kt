package com.example.authapplication.core.navigation

import kotlinx.serialization.Serializable

/**
 * アプリ全体のナビゲーション先を表す型安全なルート定義。
 * route文字列を組み立てる代わりに、Navigation Composeの型安全ナビゲーション
 * （composable<T> / navController.navigate(T)）でこれらの型を直接使用する。
 */
sealed interface AppRoute {
    /** 認証前の画面群（[Login]）をまとめるネストされたNavGraphのルート。 */
    @Serializable
    data object AuthGraph : AppRoute

    @Serializable
    data object Login : AppRoute

    /** 認証後の画面群（[Home] / [Search] / [Favorite] / [Detail]）をまとめるネストされたNavGraphのルート。 */
    @Serializable
    data object MainGraph : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data object Favorite : AppRoute

    @Serializable
    data class Detail(val itemId: String) : AppRoute
}
