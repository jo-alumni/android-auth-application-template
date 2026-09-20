package com.example.authapplication.navigation

import kotlinx.serialization.Serializable

/**
 * 認証前の画面群をまとめるネストされたNavGraphのルート。
 *
 * 画面のルートは各featureモジュールが所有するが、「画面をどう束ねるか」はアプリの構成の判断なので
 * グラフのルートだけは `:app` が持つ（配置の方針は docs/navigation-routes.md 参照）。
 */
@Serializable
data object AuthGraphRoute

/** 認証後の画面群をまとめるネストされたNavGraphのルート。 */
@Serializable
data object MainGraphRoute
