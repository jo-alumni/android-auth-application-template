package com.example.authapplication.feature.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** [LoginScreen] 単体のUIテスト。入力値がそのままコールバックへ渡ることを確認する。 */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 表示文字列は文字列リソース化されているため、テストからもリソース経由で参照する。 */
    private fun string(resId: Int, vararg formatArgs: Any): String =
        RuntimeEnvironment.getApplication().getString(resId, *formatArgs)

    @Test
    fun passesTypedIdAndPasswordToCallback_whenLoginButtonClicked() {
        var loggedIn: Pair<String, String>? = null
        composeTestRule.setContent {
            LoginScreen(
                uiState = LoginUiState.Idle,
                onLoginClick = { id, password -> loggedIn = id to password },
            )
        }

        composeTestRule.onNodeWithText(string(R.string.feature_login_id_label))
            .performTextInput("user")
        composeTestRule.onNodeWithText(string(R.string.feature_login_password_label))
            .performTextInput("password")
        composeTestRule.onNodeWithText(string(R.string.feature_login_submit)).performClick()

        assertEquals("user" to "password", loggedIn)
    }

    @Test
    fun showsErrorMessage_whenError() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = LoginUiState.Error(R.string.feature_login_error_blank_input),
                onLoginClick = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText(string(R.string.feature_login_error_blank_input))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.feature_login_submit)).assertIsEnabled()
    }

    /** 二重送信を防ぐため、ログイン処理中はボタンを押せない。 */
    @Test
    fun disablesLoginButton_whenLoading() {
        composeTestRule.setContent {
            LoginScreen(uiState = LoginUiState.Loading, onLoginClick = { _, _ -> })
        }

        composeTestRule.onNodeWithText(string(R.string.feature_login_submitting)).assertIsNotEnabled()
    }
}
