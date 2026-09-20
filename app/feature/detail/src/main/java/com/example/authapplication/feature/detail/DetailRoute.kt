package com.example.authapplication.feature.detail

import kotlinx.serialization.Serializable

/**
 * 詳細画面のルート。画面を所有するこのモジュールが定義する
 * （配置の方針は docs/navigation-routes.md 参照）。
 *
 * ホーム/検索/お気に入りの各画面からも遷移するが、遷移元はこの型を知らない。
 * 一覧の画面は `navigateDetail: (String) -> Unit` に [itemId] を渡すだけで、
 * このルートを組み立てるのは `:app` の `AppNavHost`。
 */
@Serializable
data class DetailRoute(val itemId: String)
