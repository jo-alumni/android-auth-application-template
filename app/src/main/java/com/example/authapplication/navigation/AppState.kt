package com.example.authapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.navigation.TopLevelDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * アプリ全体のナビゲーション状態を保持するState Holder。
 *
 * 「今どの画面にいるか」の判定とその画面への遷移操作をこのクラスに集約し、
 * Composable側（`AuthApplicationApp`）は状態を読んでUIを組み立てるだけにする。
 * Composableに判定ロジックを置くとプレビューもテストもできないため、
 * Composeに依存しない形（`@Composable` ではないプロパティ/関数）で公開する。
 *
 * 現在地は [NavHostController.currentBackStackEntryFlow] を購読してSnapshot Stateへ写す。
 * Snapshot Stateなので、Composableから読めば遷移のたびに再コンポーズされ、
 * テストからは通常のプロパティとして読める。
 */
@Stable
class AppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope,
) {

    /** 現在表示している画面。まだ遷移が始まっていなければ null。 */
    var currentDestination: NavDestination? by mutableStateOf(navController.currentDestination)
        private set

    /** 現在地がボトムバーのタブ（ホーム/検索/お気に入り）ならその項目。それ以外の画面では null。 */
    val currentTopLevelDestination: TopLevelDestination?
        get() = TopLevelDestination.entries.firstOrNull { destination ->
            currentDestination?.hasRoute(destination.route::class) == true
        }

    /** ボトムバーを表示するかどうか。タブに属する画面でのみ表示する。 */
    val shouldShowBottomBar: Boolean
        get() = currentTopLevelDestination != null

    init {
        coroutineScope.launch {
            navController.currentBackStackEntryFlow.collect { entry ->
                currentDestination = entry.destination
            }
        }
    }

    /**
     * ボトムバーのタブを切り替える。
     * タブのバックスタックを積み上げないよう、スタート地点まで戻したうえで
     * 各タブの状態（スクロール位置など）は保存・復元する。
     */
    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** 通知画面へ遷移する。 */
    fun navigateNotification() {
        navController.navigate(AppRoute.Notification)
    }

    /**
     * ログアウト後の遷移。認証後の画面群（[AppRoute.MainGraph]）をバックスタックごと取り除き、
     * 戻るキーでログイン済みの画面に戻れないようにする。
     */
    fun navigateLogin() {
        navController.navigate(AppRoute.AuthGraph) {
            popUpTo(AppRoute.MainGraph) { inclusive = true }
        }
    }
}

/** [AppState] をコンポジションのライフサイクルに紐付けて生成する。 */
@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): AppState = remember(navController, coroutineScope) {
    AppState(navController = navController, coroutineScope = coroutineScope)
}
