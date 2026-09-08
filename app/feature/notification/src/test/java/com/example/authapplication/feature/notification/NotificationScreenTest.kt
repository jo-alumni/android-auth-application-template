package com.example.authapplication.feature.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.authapplication.domain.notification.Notification
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [NotificationScreen] 単体のUIテスト。 */
@RunWith(RobolectricTestRunner::class)
class NotificationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsTitleAndMessage_whenSuccess() {
        setContent(
            uiState = NotificationUiState.Success(
                listOf(Notification(id = "1", title = "お知らせ1", message = "サンプル通知メッセージです。")),
            ),
        )

        composeTestRule.onNodeWithText("お知らせ1").assertIsDisplayed()
        composeTestRule.onNodeWithText("サンプル通知メッセージです。").assertIsDisplayed()
    }

    @Test
    fun showsEmptyMessage_whenEmpty() {
        setContent(uiState = NotificationUiState.Empty)

        composeTestRule.onNodeWithText("通知はありません").assertIsDisplayed()
    }

    @Test
    fun notifiesRetryClick_whenRetryButtonClicked() {
        var retryCount = 0
        setContent(
            uiState = NotificationUiState.Error("通知の取得に失敗しました"),
            onRetryClick = { retryCount++ },
        )

        composeTestRule.onNodeWithText("再読み込み").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun notifiesCloseClick_whenCloseIconClicked() {
        var closeCount = 0
        setContent(uiState = NotificationUiState.Empty, onCloseClick = { closeCount++ })

        composeTestRule.onNodeWithContentDescription("閉じる").performClick()

        assertEquals(1, closeCount)
    }

    private fun setContent(
        uiState: NotificationUiState,
        onCloseClick: () -> Unit = {},
        onRetryClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            NotificationScreen(
                uiState = uiState,
                onCloseClick = onCloseClick,
                onRetryClick = onRetryClick,
            )
        }
    }
}
