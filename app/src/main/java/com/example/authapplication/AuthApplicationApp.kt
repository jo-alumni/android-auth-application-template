package com.example.authapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.ui.AppTopBar
import com.example.authapplication.navigation.AppBottomBar
import com.example.authapplication.navigation.AppNavHost
import com.example.authapplication.navigation.AppState
import com.example.authapplication.navigation.BottomBarScrollBehavior
import com.example.authapplication.navigation.rememberAppState
import com.example.authapplication.navigation.rememberBottomBarScrollBehavior
import kotlin.math.roundToInt

/**
 * アプリ全体の骨組み（TopAppBar / ボトムバー / NavHost）を組み立てるComposable。
 *
 * 「今どの画面にいるか」「ボトムバーを出すか」といったナビゲーションの判定は [AppState] が、
 * スクロールに追従したボトムバーの隠蔽は [BottomBarScrollBehavior] が持つ。
 * このComposableはそれらの状態を読んでUIを組み立てるだけにする。
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

            LaunchedEffect(appState) {
                appViewModel.event.collect { event ->
                    when (event) {
                        AppEvent.NavigateLogin -> appState.navigateLogin()
                    }
                }
            }

            Scaffold(
                modifier = Modifier.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection),
                topBar = {
                    if (currentTopLevelDestination != null) {
                        AppTopBar(
                            title = currentTopLevelDestination.label,
                            isErrorInjectionEnabled = isErrorInjectionEnabled,
                            onErrorInjectionChange = appViewModel::setErrorInjectionEnabled,
                            onNotificationClick = appState::navigateNotification,
                            onLogoutClick = { appViewModel.logout() },
                        )
                    }
                },
                bottomBar = {
                    if (appState.shouldShowBottomBar) {
                        AppBottomBar(
                            currentDestination = currentTopLevelDestination,
                            onDestinationSelected = appState::navigateToTopLevelDestination,
                            modifier = Modifier
                                .onSizeChanged { bottomBarScrollBehavior.onBarHeightChanged(it.height.toFloat()) }
                                .offset { IntOffset(x = 0, y = bottomBarScrollBehavior.hiddenHeightPx.roundToInt()) },
                        )
                    }
                },
            ) { innerPadding ->
                // リストの表示領域(レイアウトサイズ)自体はシステムナビゲーションバー領域まで広げる。
                // ボトムバーのスクロール連動オフセット(BottomBarScrollBehavior.hiddenHeightPx)に
                // リアルタイム/準リアルタイムに追従させてLazyColumnのcontentPaddingを動かすと、
                // contentPaddingはレイアウト計算に直接使われる測定入力のため、フレームごとに
                // remeasureが発生してカクつく(離散化+animateDpAsStateで緩和を試みても、
                // アニメーション中は結局毎フレーム値が変わり続けるため解消しなかった)。
                // そこで動的な追従はやめ、contentPaddingはシステムナビゲーションバー分の固定値にする。
                // ボトムバー表示中はリスト末尾がボトムバーの背後に隠れることがあるが、ボトムバーが
                // 隠れればナビゲーションバー手前まで完全に表示される。
                val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                AppNavHost(
                    navController = appState.navController,
                    startDestination = if (state.isAuthenticated) AppRoute.MainGraph else AppRoute.AuthGraph,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                    listContentPadding = PaddingValues(bottom = navigationBarPadding),
                )
            }
        }
    }
}
