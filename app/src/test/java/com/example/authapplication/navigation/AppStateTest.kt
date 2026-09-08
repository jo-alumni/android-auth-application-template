package com.example.authapplication.navigation

import android.app.Application
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.navigation.TopLevelDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [AppState] の単体テスト。
 * 画面を描画せず、実際のNavController（[TestNavHostController]）にアプリと同じ形のNavGraphを
 * 組み立てて、遷移させたあとにState Holderが公開する状態を検証する。
 *
 * NavControllerの生成にContextが必要なためRobolectric上で実行する。
 * [Config] でApplicationを差し替えているのは、マニフェストの `App`（`@HiltAndroidApp`）を
 * 起動するとHiltの初期化が必要になり、ナビゲーションの検証と関係ない準備が増えるため。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppStateTest {

    /**
     * アプリと同じ構成のNavGraphを持つ [AppState] を作る。
     *
     * [AppState] は現在地の購読をコンストラクタで開始するため、購読を動かすには
     * 生成後に [runCurrent] が必要になる。
     */
    private fun TestScope.createAppState(): AppState {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = AppRoute.MainGraph) {
                navigation<AppRoute.AuthGraph>(startDestination = AppRoute.Login) {
                    composable<AppRoute.Login> {}
                }
                navigation<AppRoute.MainGraph>(startDestination = AppRoute.Home) {
                    composable<AppRoute.Home> {}
                    composable<AppRoute.Search> {}
                    composable<AppRoute.Favorite> {}
                    composable<AppRoute.Detail> {}
                    composable<AppRoute.Notification> {}
                }
            }
        }
        return AppState(navController = navController, coroutineScope = backgroundScope)
    }

    @Test
    fun `currentTopLevelDestination is the start destination and the bottom bar is shown`() = runTest {
        val appState = createAppState()
        runCurrent()

        assertEquals(TopLevelDestination.HOME, appState.currentTopLevelDestination)
        assertTrue(appState.shouldShowBottomBar)
    }

    @Test
    fun `navigateToTopLevelDestination switches the selected tab`() = runTest {
        val appState = createAppState()
        runCurrent()

        appState.navigateToTopLevelDestination(TopLevelDestination.SEARCH)
        runCurrent()

        assertEquals(TopLevelDestination.SEARCH, appState.currentTopLevelDestination)
        assertTrue(appState.shouldShowBottomBar)
    }

    @Test
    fun `navigateToTopLevelDestination does not stack tabs on the back stack`() = runTest {
        val appState = createAppState()
        runCurrent()

        appState.navigateToTopLevelDestination(TopLevelDestination.SEARCH)
        appState.navigateToTopLevelDestination(TopLevelDestination.FAVORITE)
        appState.navigateToTopLevelDestination(TopLevelDestination.HOME)
        runCurrent()

        val topLevelEntries = appState.navController.currentBackStack.value.count { entry ->
            TopLevelDestination.entries.any { entry.destination.hasRoute(it.route::class) }
        }
        assertEquals(1, topLevelEntries)
    }

    @Test
    fun `bottom bar is hidden on a screen outside the tabs`() = runTest {
        val appState = createAppState()
        runCurrent()

        appState.navController.navigate(AppRoute.Detail(itemId = "item1"))
        runCurrent()

        assertNull(appState.currentTopLevelDestination)
        assertFalse(appState.shouldShowBottomBar)
    }

    @Test
    fun `navigateNotification moves to the notification screen and hides the bottom bar`() = runTest {
        val appState = createAppState()
        runCurrent()

        appState.navigateNotification()
        runCurrent()

        assertTrue(appState.currentDestination?.hasRoute(AppRoute.Notification::class) == true)
        assertFalse(appState.shouldShowBottomBar)
    }

    @Test
    fun `navigateLogin clears the authenticated screens from the back stack`() = runTest {
        val appState = createAppState()
        runCurrent()
        appState.navigateToTopLevelDestination(TopLevelDestination.SEARCH)
        runCurrent()

        appState.navigateLogin()
        runCurrent()

        assertTrue(appState.currentDestination?.hasRoute(AppRoute.Login::class) == true)
        assertFalse(appState.shouldShowBottomBar)
        assertTrue(
            appState.navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(AppRoute.MainGraph::class)
            },
        )
    }
}
