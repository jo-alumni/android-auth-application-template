package com.example.authapplication.navigation

import androidx.window.core.layout.WindowSizeClass

/**
 * 画面に表示するナビゲーションUIの種類。
 *
 * Material Design のガイドライン（Navigation bar / Navigation rail / Navigation drawer）に沿って、
 * ウィンドウの幅（[WindowSizeClass]）で使い分ける。
 * タブ（ホーム/検索/お気に入り）に属さない画面ではナビゲーションUI自体を出さないため、
 * 「出さない」も種類のひとつ（[NONE]）として表す。
 *
 * 判定は [of] に閉じてあり、Composable側は `when` で分岐するだけにする。
 * `else` を書かずに網羅させることで、種類を追加したときにコンパイルエラーで気付ける。
 */
enum class AppNavigationType {
    /** ナビゲーションUIを表示しない（詳細画面・ログイン画面など）。 */
    NONE,

    /** Compact（幅600dp未満・一般的な縦持ちのスマートフォン）: 画面下部のボトムバー。 */
    BOTTOM_BAR,

    /** Medium（幅600dp以上840dp未満・小型タブレットや横持ちのスマートフォン）: 画面左端のレール。 */
    NAVIGATION_RAIL,

    /** Expanded（幅840dp以上・タブレットや折りたたみ端末の展開時）: 常設のナビゲーションドロワー。 */
    PERMANENT_DRAWER,
    ;

    companion object {

        /**
         * 現在地とウィンドウ幅から、表示するナビゲーションUIの種類を決める。
         *
         * @param windowSizeClass 現在のウィンドウサイズ。`currentWindowAdaptiveInfoV2()` から得る。
         * @param isTopLevelDestination 現在地がタブ（[com.example.authapplication.core.navigation.TopLevelDestination]）の画面かどうか。
         */
        fun of(windowSizeClass: WindowSizeClass, isTopLevelDestination: Boolean): AppNavigationType = when {
            !isTopLevelDestination -> NONE
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> PERMANENT_DRAWER
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> NAVIGATION_RAIL
            else -> BOTTOM_BAR
        }
    }
}
