package com.example.authapplication.feature.favorite

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

/** [FavoriteScreen] 単体のUIテスト。 */
@RunWith(RobolectricTestRunner::class)
class FavoriteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** お気に入り画面の「0件」はホーム/検索とは文言が変わる。 */
    @Test
    fun showsFavoriteSpecificEmptyMessage_whenEmpty() {
        setContent(uiState = FavoriteUiState.Empty)

        composeTestRule.onNodeWithText("お気に入りがありません").assertIsDisplayed()
    }

    @Test
    fun showsFavoriteItems_whenSuccess() {
        setContent(
            uiState = FavoriteUiState.Success(
                listOf(Item(id = "1", title = "アイテム1", isFavorite = true)),
            ),
        )

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
        // お気に入り一覧に並ぶのは登録済みのアイテムだけなので、アイコンは常に「削除」側になる。
        composeTestRule.onNodeWithContentDescription("お気に入りから削除").assertIsDisplayed()
    }

    @Test
    fun notifiesToggledItemId_whenFavoriteIconClicked() {
        var toggledItemId: String? = null
        setContent(
            uiState = FavoriteUiState.Success(
                listOf(Item(id = "1", title = "アイテム1", isFavorite = true)),
            ),
            onFavoriteClick = { toggledItemId = it },
        )

        composeTestRule.onNodeWithContentDescription("お気に入りから削除").performClick()

        assertEquals("1", toggledItemId)
    }

    private fun setContent(
        uiState: FavoriteUiState,
        onFavoriteClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            FavoriteScreen(
                uiState = uiState,
                onItemClick = {},
                onFavoriteClick = onFavoriteClick,
                onRetryClick = {},
            )
        }
    }
}
