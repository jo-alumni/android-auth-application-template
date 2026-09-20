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
import org.robolectric.RuntimeEnvironment
import com.example.authapplication.core.R as CoreR

/** [DetailScreen] 単体のUIテスト。 */
@RunWith(RobolectricTestRunner::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 表示文字列は文字列リソース化されているため、テストからもリソース経由で参照する。 */
    private fun string(resId: Int, vararg formatArgs: Any): String =
        RuntimeEnvironment.getApplication().getString(resId, *formatArgs)

    @Test
    fun showsItemTitle_whenSuccess() {
        setContent(uiState = DetailUiState.Success(Item(id = "1", title = "アイテム1")))

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
    }

    /** 「取得できたが対象が無い」ことを伝える表示。取得失敗の表示とは文言が変わる。 */
    @Test
    fun showsNotFoundMessage_whenNotFound() {
        setContent(uiState = DetailUiState.NotFound)

        composeTestRule.onNodeWithText(string(R.string.feature_detail_not_found)).assertIsDisplayed()
    }

    /** エラー表示でも「戻る」ボタンが押し出されずに残ることを確認する。 */
    @Test
    fun showsErrorMessageAndKeepsBackButton_whenError() {
        setContent(uiState = DetailUiState.Error(R.string.feature_detail_error_item_load))

        composeTestRule.onNodeWithText(string(R.string.feature_detail_error_item_load))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(CoreR.string.core_retry)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(CoreR.string.core_back)).assertIsDisplayed()
    }

    @Test
    fun notifiesBackClick_whenBackButtonClicked() {
        var backClickCount = 0
        setContent(
            uiState = DetailUiState.Success(Item(id = "1", title = "アイテム1")),
            onBackClick = { backClickCount++ },
        )

        composeTestRule.onNodeWithText(string(CoreR.string.core_back)).performClick()

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
}
