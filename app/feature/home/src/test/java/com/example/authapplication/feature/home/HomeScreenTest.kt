package com.example.authapplication.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.authapplication.domain.item.Item
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [HomeScreen] 単体のUIテスト。ViewModelやNavigationを介さず `uiState` を直接渡し、
 * 状態ごとの表示と、操作がどのコールバックに繋がるかだけを検証する。
 * Robolectric上で動くため実機/エミュレータは不要で、`./gradlew test` だけで完結する。
 */
@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsItemTitles_whenSuccess() {
        composeTestRule.setContent {
            HomeScreen(
                uiState = HomeUiState.Success(ITEMS),
                onItemClick = {},
                onFavoriteClick = {},
                onRetryClick = {},
            )
        }

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
        composeTestRule.onNodeWithText("アイテム2").assertIsDisplayed()
    }

    @Test
    fun showsEmptyMessage_whenEmpty() {
        composeTestRule.setContent {
            HomeScreen(
                uiState = HomeUiState.Empty,
                onItemClick = {},
                onFavoriteClick = {},
                onRetryClick = {},
            )
        }

        composeTestRule.onNodeWithText("アイテムがありません").assertIsDisplayed()
    }

    @Test
    fun showsErrorMessageAndRetryButton_whenError() {
        var retryCount = 0
        composeTestRule.setContent {
            HomeScreen(
                uiState = HomeUiState.Error(LOAD_ERROR_MESSAGE),
                onItemClick = {},
                onFavoriteClick = {},
                onRetryClick = { retryCount++ },
            )
        }

        composeTestRule.onNodeWithText(LOAD_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("再読み込み").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun notifiesClickedItemId_whenItemClicked() {
        var clickedItemId: String? = null
        composeTestRule.setContent {
            HomeScreen(
                uiState = HomeUiState.Success(ITEMS),
                onItemClick = { clickedItemId = it },
                onFavoriteClick = {},
                onRetryClick = {},
            )
        }

        composeTestRule.onNodeWithText("アイテム2").performClick()

        assertEquals("2", clickedItemId)
    }

    /** お気に入りアイコンは状態でcontentDescriptionが変わるため、未登録のアイテムだけを対象にできる。 */
    @Test
    fun notifiesToggledItemId_whenFavoriteIconClicked() {
        var toggledItemId: String? = null
        composeTestRule.setContent {
            HomeScreen(
                uiState = HomeUiState.Success(listOf(ITEMS[0].copy(isFavorite = true), ITEMS[1])),
                onItemClick = {},
                onFavoriteClick = { toggledItemId = it },
                onRetryClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("お気に入りに追加").performClick()

        assertEquals("2", toggledItemId)
    }

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
        )
        const val LOAD_ERROR_MESSAGE = "アイテムの取得に失敗しました"
    }
}
