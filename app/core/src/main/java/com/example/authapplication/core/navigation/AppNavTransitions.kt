package com.example.authapplication.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * アプリ全体で使い回す `composable<T>()` の画面遷移アニメーション定義。
 *
 * ボトムバー内のタブ切り替え（Home/Search/Favorite）のような同階層の遷移にはフェードを、
 * 詳細画面のように階層を掘り下げる遷移にはスライドを用いる、という使い分けを想定している。
 */
object AppNavTransitions {
    private const val DURATION_MILLIS = 300

    /** 同階層の画面切り替え（ボトムバーのタブ切り替えなど）で新しい画面をフェードインさせる。 */
    val fadeEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { fadeIn(animationSpec = tween(DURATION_MILLIS)) }

    /** 同階層の画面切り替え（ボトムバーのタブ切り替えなど）で元の画面をフェードアウトさせる。 */
    val fadeExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { fadeOut(animationSpec = tween(DURATION_MILLIS)) }

    /** 詳細画面など、階層を掘り下げる遷移先の画面を右からスライドインさせる。 */
    val slideInFromRight: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        {
            slideInHorizontally(
                animationSpec = tween(DURATION_MILLIS),
                initialOffsetX = { fullWidth -> fullWidth },
            ) + fadeIn(animationSpec = tween(DURATION_MILLIS))
        }

    /** 詳細画面など、階層を掘り下げる遷移から戻る際に画面を右へスライドアウトさせる。 */
    val slideOutToRight: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        {
            slideOutHorizontally(
                animationSpec = tween(DURATION_MILLIS),
                targetOffsetX = { fullWidth -> fullWidth },
            ) + fadeOut(animationSpec = tween(DURATION_MILLIS))
        }
}
