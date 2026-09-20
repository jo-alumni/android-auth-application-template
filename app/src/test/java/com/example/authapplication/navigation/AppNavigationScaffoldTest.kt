package com.example.authapplication.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.example.authapplication.feature.favorite.R as FavoriteR
import com.example.authapplication.feature.home.R as HomeR
import com.example.authapplication.feature.search.R as SearchR

/**
 * [AppNavigationScaffold] 単体のUIテスト。
 *
 * 表示するナビゲーションUIの種類は [AppNavigationType] として受け取るだけなので、
 * `AppState` やウィンドウサイズを用意せずに種類ごとの表示と操作を検証できる。
 * どの種類を選ぶかの判定は [AppStateTest] が受け持つ。
 */
@RunWith(RobolectricTestRunner::class)
class AppNavigationScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 表示文字列は文字列リソース化されているため、テストからもリソース経由で参照する。 */
    private fun string(resId: Int): String = RuntimeEnvironment.getApplication().getString(resId)

    private fun setContent(
        navigationType: AppNavigationType,
        onDestinationClick: (TopLevelDestination) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppNavigationScaffold(
                navigationType = navigationType,
                currentDestination = TopLevelDestination.HOME,
                onDestinationClick = onDestinationClick,
            ) {
                Text(text = CONTENT)
            }
        }
    }

    @Test
    fun showsContentAndDestinations_whenBottomBar() {
        setContent(navigationType = AppNavigationType.BOTTOM_BAR)

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(HomeR.string.feature_home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(FavoriteR.string.feature_favorite_title)).assertIsDisplayed()
    }

    @Test
    fun showsContentAndDestinations_whenNavigationRail() {
        setContent(navigationType = AppNavigationType.NAVIGATION_RAIL)

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(HomeR.string.feature_home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(FavoriteR.string.feature_favorite_title)).assertIsDisplayed()
    }

    // 常設ドロワーは横に広いウィンドウでのみ使うUIなので、テストもタブレット相当の幅で動かす。
    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun showsContentAndDestinations_whenPermanentDrawer() {
        setContent(navigationType = AppNavigationType.PERMANENT_DRAWER)

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(HomeR.string.feature_home_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(FavoriteR.string.feature_favorite_title)).assertIsDisplayed()
    }

    @Test
    fun showsOnlyContent_whenNone() {
        setContent(navigationType = AppNavigationType.NONE)

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).assertDoesNotExist()
    }

    @Test
    fun notifiesSelectedDestination_whenBottomBarItemClicked() {
        var selected: TopLevelDestination? = null
        setContent(
            navigationType = AppNavigationType.BOTTOM_BAR,
            onDestinationClick = { selected = it },
        )

        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).performClick()

        assertEquals(TopLevelDestination.SEARCH, selected)
    }

    @Test
    fun notifiesSelectedDestination_whenNavigationRailItemClicked() {
        var selected: TopLevelDestination? = null
        setContent(
            navigationType = AppNavigationType.NAVIGATION_RAIL,
            onDestinationClick = { selected = it },
        )

        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).performClick()

        assertEquals(TopLevelDestination.SEARCH, selected)
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun notifiesSelectedDestination_whenDrawerItemClicked() {
        var selected: TopLevelDestination? = null
        setContent(
            navigationType = AppNavigationType.PERMANENT_DRAWER,
            onDestinationClick = { selected = it },
        )

        composeTestRule.onNodeWithText(string(SearchR.string.feature_search_title)).performClick()

        assertEquals(TopLevelDestination.SEARCH, selected)
    }

    private companion object {
        const val CONTENT = "画面の中身"
    }
}
