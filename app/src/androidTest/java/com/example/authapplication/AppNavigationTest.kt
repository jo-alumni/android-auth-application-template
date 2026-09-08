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
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.core.theme.AuthApplicationTheme
import com.example.authapplication.domain.auth.FakeAuthRepository
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.Item
import com.example.authapplication.navigation.rememberAppState
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
 * 未認証起動 / 認証済み起動 / ログイン成功 / ログアウト のナビゲーションを検証する。
 * 実際の [AppViewModel] / LoginViewModel をそのまま使い、遷移ロジックはテスト側で再実装しない。
 *
 * リポジトリは [com.example.authapplication.di.TestRepositoryModule] の `@TestInstallIn` で
 * Fakeに差し替わっているため、実DataStore（端末上の実ファイル）には触れない。
 * 起動時の認証状態やアイテム一覧は各テストがFakeへ直接仕込む。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var authRepository: FakeAuthRepository

    @Inject
    lateinit var itemRepository: FakeItemRepository

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        // 実DataStoreと違いFakeはテストごとに作り直されるため、前回テストの状態は残らない。
        // ホーム画面の表示内容だけ、全テスト共通の初期データとして仕込んでおく。
        runBlocking { itemRepository.emitItems(ITEMS) }
    }

    /** 認証状態は「まだ読み込めていない」状態から始まるため、起動前に必ずどちらかを仕込む。 */
    private fun setAuthenticated(isAuthenticated: Boolean) {
        runBlocking {
            if (isAuthenticated) authRepository.setAuthToken(TOKEN) else authRepository.clearAuthToken()
        }
    }

    private fun launchApp() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            AuthApplicationTheme {
                AuthApplicationApp(appState = rememberAppState(navController = navController))
            }
        }
    }

    private fun waitUntilRoute(route: KClass<out AppRoute>) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            navController.currentDestination?.hasRoute(route) == true
        }
    }

    private fun login() {
        composeTestRule.onNodeWithText("ID").performTextInput("user")
        composeTestRule.onNodeWithText("パスワード").performTextInput("password")
        composeTestRule.onNodeWithText("ログイン").performClick()
    }

    @Test
    fun unauthenticatedLaunchShowsLoginScreen() {
        setAuthenticated(false)

        launchApp()

        waitUntilRoute(AppRoute.Login::class)
        composeTestRule.onNodeWithText("ログイン画面").assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(AppRoute.Login::class) == true)
    }

    /** 認証済みなら認証画面をスキップし、ホーム画面から起動する。 */
    @Test
    fun authenticatedLaunchSkipsLoginScreen() {
        setAuthenticated(true)

        launchApp()

        waitUntilRoute(AppRoute.Home::class)
        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(AppRoute.Login::class)
            },
        )
    }

    @Test
    fun loginSuccessNavigatesToHomeAndRemovesLoginFromBackStack() {
        setAuthenticated(false)
        launchApp()
        waitUntilRoute(AppRoute.Login::class)

        login()

        // ログインは認証状態の書き込みを伴う非同期処理のため、Homeへの到達を明示的に待つ。
        waitUntilRoute(AppRoute.Home::class)

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
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
        setAuthenticated(true)
        launchApp()
        waitUntilRoute(AppRoute.Home::class)

        // AppTopBarの「ログアウト」ボタン。この時点では同テキストのノードは1つだけ。
        composeTestRule.onNodeWithText("ログアウト").performClick()

        // 確認ダイアログが開くと"ログアウト"というexact textのノードが複数
        // (TopAppBarのボタン/ダイアログタイトル/ダイアログ確認ボタン)存在するため、
        // ダイアログ配下かつクリック可能なノードに絞り込む。
        composeTestRule
            .onNode(hasText("ログアウト") and hasAnyAncestor(isDialog()) and hasClickAction())
            .performClick()

        // ログアウトは認証状態のクリアを伴う非同期処理のため、Loginへの到達を明示的に待つ。
        waitUntilRoute(AppRoute.Login::class)

        composeTestRule.onNodeWithText("ログイン画面").assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(AppRoute.Login::class) == true)
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(AppRoute.MainGraph::class)
            },
        )
    }

    private companion object {
        const val TOKEN = "dummy_token"
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
        )
    }
}
