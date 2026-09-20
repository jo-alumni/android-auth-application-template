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
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.authapplication.core.theme.AuthApplicationTheme
import com.example.authapplication.domain.auth.FakeAuthRepository
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.Item
import com.example.authapplication.feature.detail.DetailRoute
import com.example.authapplication.feature.home.HomeRoute
import com.example.authapplication.feature.login.LoginRoute
import com.example.authapplication.feature.search.SearchRoute
import com.example.authapplication.navigation.AuthGraphRoute
import com.example.authapplication.navigation.MainGraphRoute
import com.example.authapplication.navigation.TopLevelDestination
import com.example.authapplication.navigation.rememberAppState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.reflect.KClass
import com.example.authapplication.core.R as CoreR
import com.example.authapplication.feature.login.R as LoginR
import com.example.authapplication.feature.search.R as SearchR

/**
 * [AuthApplicationApp] をHilt統合計装テストとして起動し、
 * 未認証起動 / 認証済み起動 / ログイン成功 / ログアウト / トークン失効 のナビゲーションを検証する。
 * 認証状態とナビゲーションの結び方（状態駆動への一本化）は docs/auth-navigation.md 参照。
 * 実際の [AppViewModel] / LoginViewModel をそのまま使い、遷移ロジックはテスト側で再実装しない。
 *
 * リポジトリは [com.example.authapplication.di.TestRepositoryModule] の `@TestInstallIn` で
 * Fakeに差し替わっているため、実DataStore（端末上の実ファイル）には触れない。
 * 起動時の認証状態やアイテム一覧は各テストがFakeへ直接仕込む。
 */
// バックスタック全体を覗く手段は NavController.currentBackStack しかなく、
// これは @RestrictTo(LIBRARY_GROUP) のAPI。「Loginが残っていないこと」を確かめるには
// 現在地(currentDestination)だけでは足りないため、テスト限定の利用として抑制する。
@Suppress("RestrictedApi")
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

    /** 表示文字列は文字列リソース化されているため、テストからもリソース経由で参照する。 */
    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

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

    private fun waitUntilRoute(route: KClass<*>) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            navController.currentDestination?.hasRoute(route) == true
        }
    }

    private fun login() {
        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_id_label))
            .performTextInput("user")
        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_password_label))
            .performTextInput("password")
        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_submit)).performClick()
    }

    private fun logout() {
        // AppTopBarの「ログアウト」ボタン。この時点では同テキストのノードは1つだけ。
        composeTestRule.onNodeWithText(string(CoreR.string.core_logout)).performClick()

        // 確認ダイアログが開くと"ログアウト"というexact textのノードが複数
        // (TopAppBarのボタン/ダイアログタイトル/ダイアログ確認ボタン)存在するため、
        // ダイアログ配下かつクリック可能なノードに絞り込む。
        composeTestRule
            .onNode(
                hasText(string(CoreR.string.core_logout)) and
                    hasAnyAncestor(isDialog()) and
                    hasClickAction(),
            )
            .performClick()
    }

    /**
     * ボトムバーのタブを選ぶ。TopAppBarのタイトルにも同じ文言が出るため、
     * クリックできるノード（[androidx.compose.material3.NavigationBarItem]）に絞り込む。
     */
    private fun selectTab(destination: TopLevelDestination) {
        composeTestRule
            .onNode(hasText(string(destination.labelResId)) and hasClickAction())
            .performClick()
    }

    @Test
    fun unauthenticatedLaunchShowsLoginScreen() {
        setAuthenticated(false)

        launchApp()

        waitUntilRoute(LoginRoute::class)
        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_title)).assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(LoginRoute::class) == true)
    }

    /** 認証済みなら認証画面をスキップし、ホーム画面から起動する。 */
    @Test
    fun authenticatedLaunchSkipsLoginScreen() {
        setAuthenticated(true)

        launchApp()

        waitUntilRoute(HomeRoute::class)
        composeTestRule.onNodeWithText(ITEMS.first().title).assertIsDisplayed()
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(LoginRoute::class)
            },
        )
    }

    @Test
    fun loginSuccessNavigatesToHomeAndRemovesLoginFromBackStack() {
        setAuthenticated(false)
        launchApp()
        waitUntilRoute(LoginRoute::class)

        login()

        // ログインは認証状態の書き込みを伴う非同期処理のため、Homeへの到達を明示的に待つ。
        waitUntilRoute(HomeRoute::class)

        composeTestRule.onNodeWithText(ITEMS.first().title).assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(HomeRoute::class) == true)
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(LoginRoute::class) ||
                    entry.destination.hasRoute(AuthGraphRoute::class)
            },
        )
    }

    /**
     * ログアウトは `AppEvent` ではなく、認証状態（`AppViewModel.authState`）の変化を通じて
     * ログイン画面へ戻る。認証後の画面がバックスタックに1つも残らないことまで確認する。
     */
    @Test
    fun logoutClearsMainGraphBackStack() {
        setAuthenticated(true)
        launchApp()
        waitUntilRoute(HomeRoute::class)

        logout()

        // ログアウトは認証状態のクリアを伴う非同期処理のため、Loginへの到達を明示的に待つ。
        waitUntilRoute(LoginRoute::class)

        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_title)).assertIsDisplayed()
        assertTrue(navController.currentDestination?.hasRoute(LoginRoute::class) == true)
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(MainGraphRoute::class) ||
                    entry.destination.hasRoute(HomeRoute::class)
            },
        )
        // 戻るキーで認証後の画面へ戻れないこと。
        assertNull(navController.previousBackStackEntry)
    }

    /**
     * ログアウト操作以外の理由（サーバ側での失効・有効期限切れ）でトークンが失われた場合も、
     * 自動的にログイン画面へ戻ること。UIを操作せず、外からトークンを破棄して再現する。
     * ホーム画面ではなく詳細画面から失効させ、`MainGraph` のどの階層にいても戻ることを確認する。
     */
    @Test
    fun tokenExpirationNavigatesBackToLoginScreen() {
        setAuthenticated(true)
        launchApp()
        waitUntilRoute(HomeRoute::class)
        composeTestRule.onNodeWithText(ITEMS.first().title).performClick()
        waitUntilRoute(DetailRoute::class)

        // ユーザー操作を経ない外部要因での失効。
        runBlocking { authRepository.clearAuthToken() }

        waitUntilRoute(LoginRoute::class)
        composeTestRule.onNodeWithText(string(LoginR.string.feature_login_title)).assertIsDisplayed()
        assertTrue(
            navController.currentBackStack.value.none { entry ->
                entry.destination.hasRoute(MainGraphRoute::class) ||
                    entry.destination.hasRoute(DetailRoute::class)
            },
        )
        assertNull(navController.previousBackStackEntry)
    }

    /**
     * ログアウト後に再ログインしても、ログアウト前のタブの状態が復元されないこと。
     *
     * タブ切り替えは `saveState` / `restoreState` を使うため、タブごとの状態は保存される。
     * まずその保存が効くことを確かめたうえで、ログアウトを挟むと破棄されることを確認する。
     * ここでは検索キーワード（検索画面のViewModelが `SavedStateHandle` に持つ）を
     * 保存される状態の代表として使い、絞り込み結果の表示で判定する。
     */
    @Test
    fun reLoginAfterLogoutStartsFromHomeWithoutRestoringSavedState() {
        setAuthenticated(true)
        launchApp()
        waitUntilRoute(HomeRoute::class)

        selectTab(TopLevelDestination.SEARCH)
        waitUntilRoute(SearchRoute::class)
        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_query_label))
            .performTextInput(ITEMS.first().title)
        // ソフトキーボードがボトムバーに重ならないよう閉じてからタブを操作する。
        Espresso.closeSoftKeyboard()
        composeTestRule.onNodeWithText(ITEMS.last().title).assertDoesNotExist()

        // ホームタブへ移ると検索タブの状態が保存され、戻ると復元される。
        selectTab(TopLevelDestination.HOME)
        waitUntilRoute(HomeRoute::class)
        selectTab(TopLevelDestination.SEARCH)
        waitUntilRoute(SearchRoute::class)
        composeTestRule.onNodeWithText(ITEMS.last().title).assertDoesNotExist()

        logout()
        waitUntilRoute(LoginRoute::class)
        login()
        waitUntilRoute(HomeRoute::class)

        // 再ログイン後はホーム画面から始まり、検索タブの状態も残っていない。
        assertTrue(navController.currentDestination?.hasRoute(HomeRoute::class) == true)
        selectTab(TopLevelDestination.SEARCH)
        waitUntilRoute(SearchRoute::class)
        composeTestRule.onNodeWithText(ITEMS.last().title).assertIsDisplayed()
    }

    private companion object {
        const val TOKEN = "dummy_token"
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
        )
    }
}
