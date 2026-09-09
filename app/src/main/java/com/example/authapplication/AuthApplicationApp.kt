package com.example.authapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.ui.AppTopBar
import com.example.authapplication.navigation.AppNavHost
import com.example.authapplication.navigation.AppNavigationScaffold
import com.example.authapplication.navigation.AppNavigationType
import com.example.authapplication.navigation.AppState
import com.example.authapplication.navigation.BottomBarScrollBehavior
import com.example.authapplication.navigation.rememberAppState
import com.example.authapplication.navigation.rememberBottomBarScrollBehavior
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * アプリ全体の骨組み（TopAppBar / ナビゲーションUI / NavHost）を組み立てるComposable。
 *
 * 「今どの画面にいるか」「どのナビゲーションUI（ボトムバー / レール / 常設ドロワー）を出すか」
 * といったナビゲーションの判定は [AppState] が、スクロールに追従したボトムバーの隠蔽は
 * [BottomBarScrollBehavior] が持つ。このComposableはそれらの状態を読んでUIを組み立てるだけにする。
 * 画面幅に応じたUIの組み替えそのものは [AppNavigationScaffold] が担う。
 *
 * 認証状態とナビゲーションの関係は「状態駆動」に一本化してある。
 * 起動時の入り口は `startDestination` が、起動後に未認証へ変わったときの遷移は
 * [AppViewModel.authState] を購読する `LaunchedEffect` が担い、両者の担当は重ならない
 * （選んだ理由と `startDestination` を変化させない理由は docs/auth-navigation.md 参照）。
 */
@Composable
fun AuthApplicationApp(
    appViewModel: AppViewModel = hiltViewModel(),
    appState: AppState = rememberAppState(),
    bottomBarScrollBehavior: BottomBarScrollBehavior = rememberBottomBarScrollBehavior(),
) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    val isErrorInjectionEnabled by appViewModel.isErrorInjectionEnabled.collectAsStateWithLifecycle()
    when (val state = authState) {
        AuthUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is AuthUiState.Ready -> {
            val currentTopLevelDestination = appState.currentTopLevelDestination

            // タブ切り替え時は常にボトムバーを表示状態から始める
            LaunchedEffect(currentTopLevelDestination) {
                bottomBarScrollBehavior.reset()
            }

            // 起動時（初回の認証状態解決）の値。startDestination はこの値だけで決め、以降変化させない。
            // startDestination を変化させるとNavHostがグラフを作り直してしまい、
            // 「状態の変化」と「NavHostの作り直し」の2経路が同時にナビゲーションへ効いてしまう。
            val initialIsAuthenticated = remember { state.isAuthenticated }

            // 認証状態がナビゲーションへ影響する経路はこの1本だけにする（docs/auth-navigation.md 参照）。
            // ログアウト操作でもトークン失効（外部要因）でも、authStateが未認証に変わればここを通る。
            LaunchedEffect(appState) {
                appViewModel.authState
                    .filterIsInstance<AuthUiState.Ready>()
                    .map { ready -> ready.isAuthenticated }
                    .distinctUntilChanged()
                    // 起動時の状態はstartDestinationが解決済みなので、そこからの「変化」だけを扱う。
                    .dropWhile { isAuthenticated -> isAuthenticated == initialIsAuthenticated }
                    .collect { isAuthenticated ->
                        // 認証済みへの変化（ログイン成功）はログイン画面自身が遷移するため扱わない。
                        if (!isAuthenticated) appState.navigateLogin()
                    }
            }

            val navigationType = appState.navigationType
            AppNavigationScaffold(
                navigationType = navigationType,
                currentDestination = currentTopLevelDestination,
                onDestinationSelected = appState::navigateToTopLevelDestination,
                // スクロールに追従して隠すのはボトムバーだけなので、レール/ドロワーのときは繋がない。
                modifier = if (navigationType == AppNavigationType.BOTTOM_BAR) {
                    Modifier.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                },
                bottomBarModifier = Modifier
                    .onSizeChanged { bottomBarScrollBehavior.onBarHeightChanged(it.height.toFloat()) }
                    .offset { IntOffset(x = 0, y = bottomBarScrollBehavior.hiddenHeightPx.roundToInt()) },
                topBar = {
                    if (currentTopLevelDestination != null) {
                        AppTopBar(
                            title = stringResource(currentTopLevelDestination.labelResId),
                            isErrorInjectionEnabled = isErrorInjectionEnabled,
                            onErrorInjectionChange = appViewModel::setErrorInjectionEnabled,
                            onExpireTokenClick = appViewModel::expireAuthToken,
                            onNotificationClick = appState::navigateNotification,
                            onLogoutClick = appViewModel::logout,
                        )
                    }
                },
            ) {
                AppNavHost(
                    navController = appState.navController,
                    startDestination = if (initialIsAuthenticated) AppRoute.MainGraph else AppRoute.AuthGraph,
                )
            }
        }
    }
}
