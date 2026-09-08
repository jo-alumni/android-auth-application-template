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

/** [LoginScreen] 単体のUIテスト。入力値がそのままコールバックへ渡ることを確認する。 */
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun passesTypedIdAndPasswordToCallback_whenLoginButtonClicked() {
        var loggedIn: Pair<String, String>? = null
        composeTestRule.setContent {
            LoginScreen(
                uiState = LoginUiState.Idle,
                onLoginClick = { id, password -> loggedIn = id to password },
            )
        }

        composeTestRule.onNodeWithText("ID").performTextInput("user")
        composeTestRule.onNodeWithText("パスワード").performTextInput("password")
        composeTestRule.onNodeWithText("ログイン").performClick()

        assertEquals("user" to "password", loggedIn)
    }

    @Test
    fun showsErrorMessage_whenError() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = LoginUiState.Error("IDとパスワードを入力してください"),
                onLoginClick = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("IDとパスワードを入力してください").assertIsDisplayed()
        composeTestRule.onNodeWithText("ログイン").assertIsEnabled()
    }

    /** 二重送信を防ぐため、ログイン処理中はボタンを押せない。 */
    @Test
    fun disablesLoginButton_whenLoading() {
        composeTestRule.setContent {
            LoginScreen(uiState = LoginUiState.Loading, onLoginClick = { _, _ -> })
        }

        composeTestRule.onNodeWithText("ログイン中...").assertIsNotEnabled()
    }
}
