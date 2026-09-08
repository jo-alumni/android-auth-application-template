package com.example.authapplication.feature.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.authapplication.domain.item.Item
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [DetailScreen] 単体のUIテスト。 */
@RunWith(RobolectricTestRunner::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsItemTitle_whenSuccess() {
        setContent(uiState = DetailUiState.Success(Item(id = "1", title = "アイテム1")))

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
    }

    /** 「取得できたが対象が無い」ことを伝える表示。取得失敗の表示とは文言が変わる。 */
    @Test
    fun showsNotFoundMessage_whenNotFound() {
        setContent(uiState = DetailUiState.NotFound)

        composeTestRule.onNodeWithText("アイテムが見つかりませんでした").assertIsDisplayed()
    }

    /** エラー表示でも「戻る」ボタンが押し出されずに残ることを確認する。 */
    @Test
    fun showsErrorMessageAndKeepsBackButton_whenError() {
        setContent(uiState = DetailUiState.Error(LOAD_ERROR_MESSAGE))

        composeTestRule.onNodeWithText(LOAD_ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("再読み込み").assertIsDisplayed()
        composeTestRule.onNodeWithText("戻る").assertIsDisplayed()
    }

    @Test
    fun notifiesBackClick_whenBackButtonClicked() {
        var backClickCount = 0
        setContent(
            uiState = DetailUiState.Success(Item(id = "1", title = "アイテム1")),
            onBackClick = { backClickCount++ },
        )

        composeTestRule.onNodeWithText("戻る").performClick()

        assertEquals(1, backClickCount)
    }

    private fun setContent(
        uiState: DetailUiState,
        onBackClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DetailScreen(uiState = uiState, onBackClick = onBackClick, onRetryClick = {})
        }
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE = "アイテムの取得に失敗しました"
    }
}
