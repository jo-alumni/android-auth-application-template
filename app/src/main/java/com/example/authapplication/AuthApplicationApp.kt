package com.example.authapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.navigation.TopLevelDestination
import com.example.authapplication.core.ui.AppTopBar
import com.example.authapplication.navigation.AppBottomBar
import com.example.authapplication.navigation.AppNavHost
import kotlin.math.roundToInt

@Composable
fun AuthApplicationApp(
    appViewModel: AppViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val authState by appViewModel.authState.collectAsStateWithLifecycle()
    when (val state = authState) {
        AuthUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is AuthUiState.Ready -> {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { destination ->
                currentDestination?.hasRoute(destination.route::class) == true
            }
            val showBottomBar = currentTopLevelDestination != null

            var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
            var bottomBarOffsetHeightPx by remember { mutableFloatStateOf(0f) }
            val bottomBarNestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val newOffset = bottomBarOffsetHeightPx + available.y
                        bottomBarOffsetHeightPx = newOffset.coerceIn(-bottomBarHeightPx, 0f)
                        return Offset.Zero
                    }
                }
            }

            // タブ切り替え時は常にボトムバーを表示状態から始める
            LaunchedEffect(currentTopLevelDestination) {
                bottomBarOffsetHeightPx = 0f
            }

            LaunchedEffect(navController) {
                appViewModel.event.collect { event ->
                    when (event) {
                        AppEvent.NavigateLogin -> {
                            navController.navigate(AppRoute.AuthGraph) {
                                popUpTo(AppRoute.MainGraph) { inclusive = true }
                            }
                        }
                    }
                }
            }

            Scaffold(
                modifier = Modifier.nestedScroll(bottomBarNestedScrollConnection),
                topBar = {
                    if (currentTopLevelDestination != null) {
                        AppTopBar(
                            title = currentTopLevelDestination.label,
                            onNotificationClick = { navController.navigate(AppRoute.Notification) },
                            onLogoutClick = { appViewModel.logout() },
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(
                            navController = navController,
                            currentDestination = currentDestination,
                            modifier = Modifier
                                .onSizeChanged { bottomBarHeightPx = it.height.toFloat() }
                                .offset { IntOffset(x = 0, y = -bottomBarOffsetHeightPx.roundToInt()) },
                        )
                    }
                },
            ) { innerPadding ->
                // ボトムバーが隠れている分だけ下端の余白を減らし、表示領域をバーの位置まで広げる
                val density = LocalDensity.current
                val hiddenBottomBarHeight = with(density) { (-bottomBarOffsetHeightPx).toDp() }
                val contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = (innerPadding.calculateBottomPadding() - hiddenBottomBarHeight).coerceAtLeast(0.dp),
                )
                AppNavHost(
                    navController = navController,
                    startDestination = if (state.isAuthenticated) AppRoute.MainGraph else AppRoute.AuthGraph,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}
