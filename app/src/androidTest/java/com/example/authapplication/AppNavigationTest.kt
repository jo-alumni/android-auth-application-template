package com.example.authapplication

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.theme.AuthApplicationTheme
import com.example.authapplication.domain.auth.ClearAuthTokenUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AuthApplicationApp] をHilt統合計装テストとして起動し、
 * 未認証起動 → ログイン成功 → ログアウト の一連のナビゲーションを検証する。
 * 実際の [AppViewModel] / LoginViewModel / DataStore(暗号化含む)をそのまま使い、
 * 遷移ロジックはテスト側で再実装しない。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var clearAuthTokenUseCase: ClearAuthTokenUseCase

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        // DataStoreは実機/エミュレータ上の実ファイルなので前回テストの認証状態が残り得る。
        // 各テストは必ず未認証状態から開始する。
        runBlocking { clearAuthTokenUseCase() }
    }

    private fun launchApp() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            AuthApplicationTheme {
                AuthApplicationApp(navController = navController)
            }
        }
    }

    private fun waitUntilRoute(route: KClass<out AppRoute>) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            navController.currentDestination?.hasRoute(route) == true
        }
    }

    @Test
    fun unauthenticatedLaunchShowsLoginScreen() {
        launchApp()

        waitUntilRoute(AppRoute.Login::class)

        composeTestRule.onNodeWithText("ログイン画面").assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(AppRoute.Login::class) == true)
    }

    @Test
    fun loginSuccessNavigatesToHomeAndRemovesLoginFromBackStack() {
        launchApp()
        waitUntilRoute(AppRoute.Login::class)

        composeTestRule.onNodeWithText("ログイン").performClick()

        // ログインはDataStoreへの書き込み(暗号化I/O)を伴う非同期処理のため、
        // Homeへの到達を明示的に待つ。
        waitUntilRoute(AppRoute.Home::class)

        composeTestRule.onNodeWithText("ホーム画面").assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(AppRoute.Home::class) == true)
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(AppRoute.Login::class) ||
                    entry.destination.hasRoute(AppRoute.AuthGraph::class)
            },
        )
    }

    @Test
    fun logoutNavigatesBackToLoginScreen() {
        launchApp()
        waitUntilRoute(AppRoute.Login::class)
        composeTestRule.onNodeWithText("ログイン").performClick()
        waitUntilRoute(AppRoute.Home::class)

        // AppTopBarの「ログアウト」ボタン。この時点では同テキストのノードは1つだけ。
        composeTestRule.onNodeWithText("ログアウト").performClick()

        // 確認ダイアログが開くと"ログアウト"というexact textのノードが複数
        // (TopAppBarのボタン/ダイアログタイトル/ダイアログ確認ボタン)存在するため、
        // ダイアログ配下かつクリック可能なノードに絞り込む。
        composeTestRule
            .onNode(hasText("ログアウト") and hasAnyAncestor(isDialog()) and hasClickAction())
            .performClick()

        // ログアウトはDataStoreのクリア(暗号化I/O)を伴う非同期処理のため、
        // Loginへの到達を明示的に待つ。
        waitUntilRoute(AppRoute.Login::class)

        composeTestRule.onNodeWithText("ログイン画面").assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(AppRoute.Login::class) == true)
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(AppRoute.MainGraph::class)
            },
        )
    }
}
