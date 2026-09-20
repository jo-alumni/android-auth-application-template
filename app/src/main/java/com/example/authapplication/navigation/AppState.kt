package com.example.authapplication.navigation

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.window.core.layout.WindowSizeClass
import com.example.authapplication.feature.notification.NotificationRoute
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
 * テストからは通常のプロパティとして読める。ウィンドウサイズ（[windowSizeClass]）も同じく
 * Snapshot Stateで保持するため、画面の回転やウィンドウのリサイズにも再コンポーズで追従する。
 */
@Stable
class AppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope,
    windowSizeClass: WindowSizeClass,
) {

    /**
     * 現在のウィンドウサイズ。回転・折りたたみ・分割画面で変わるため、[rememberAppState] が
     * コンポジションから最新値を書き込む（State Holder自身はComposeのAPIを呼ばない）。
     */
    var windowSizeClass: WindowSizeClass by mutableStateOf(windowSizeClass)
        internal set

    /** 現在表示している画面。まだ遷移が始まっていなければ null。 */
    var currentDestination: NavDestination? by mutableStateOf(navController.currentDestination)
        private set

    /** 現在地がボトムバーのタブ（ホーム/検索/お気に入り）ならその項目。それ以外の画面では null。 */
    val currentTopLevelDestination: TopLevelDestination?
        get() = TopLevelDestination.entries.firstOrNull { destination ->
            currentDestination?.hasRoute(destination.route::class) == true
        }

    /**
     * 表示するナビゲーションUIの種類。タブに属する画面かどうかと、現在のウィンドウ幅で決まる。
     * 判定はここ（State Holder）に閉じ、Composable側は `when` で分岐するだけにする。
     */
    val navigationType: AppNavigationType
        get() = AppNavigationType.of(
            windowSizeClass = windowSizeClass,
            isTopLevelDestination = currentTopLevelDestination != null,
        )

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
        navController.navigate(NotificationRoute)
    }

    /**
     * 認証が解除されたときの遷移。認証後の画面群（[MainGraphRoute]）をバックスタックごと
     * 取り除き、戻るキーでログイン済みの画面に戻れないようにする。
     *
     * 呼び出すのは `AuthApplicationApp` が認証状態の変化を検知したときだけで、
     * ログアウト操作・トークン失効のどちらもこの1本の経路を通る（docs/auth-navigation.md 参照）。
     */
    fun navigateLogin() {
        // popUpTo が消すのは「今積まれているバックスタック」だけ。タブ切り替え
        // （[navigateToTopLevelDestination] の `saveState = true`）で保存された各タブの
        // バックスタックはそれとは別に保持され続けるため、再ログイン後に `restoreState` で
        // 前のセッションの画面が復元されてしまう。認証が解除されたら明示的に破棄する。
        // `clearBackStack` は現在地から辿れるルートしか解決できないため、
        // 認証前のグラフへ移る前（まだ [MainGraphRoute] にいるうち）に呼ぶ。
        TopLevelDestination.entries.forEach { destination ->
            navController.clearBackStack(destination.route)
        }
        navController.navigate(AuthGraphRoute) {
            popUpTo(MainGraphRoute) { inclusive = true }
        }
    }
}

/**
 * [AppState] をコンポジションのライフサイクルに紐付けて生成する。
 *
 * [windowSizeClass] は生成時だけでなく毎回のコンポジションで書き戻す。[AppState] を作り直すと
 * 現在地の購読（`init` で開始するコルーチン）が二重に走ってしまうため、
 * サイズの変化はインスタンスの再生成ではなくプロパティの更新として扱う。
 */
@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
): AppState {
    val appState = remember(navController, coroutineScope) {
        AppState(
            navController = navController,
            coroutineScope = coroutineScope,
            windowSizeClass = windowSizeClass,
        )
    }
    // コンポジション中にSnapshot Stateへ書き込まないよう、確定後に反映する。
    SideEffect { appState.windowSizeClass = windowSizeClass }
    return appState
}
