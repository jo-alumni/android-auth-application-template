package com.example.authapplication.feature.login

import app.cash.turbine.test
import com.example.authapplication.domain.auth.FakeAuthRepository
import com.example.authapplication.domain.auth.SetAuthTokenUseCase
import com.example.authapplication.domain.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login with valid id and password sets dummy auth token and emits NavigateHome event`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(SetAuthTokenUseCase(repository))

        viewModel.event.test {
            viewModel.login(id = "user", password = "password")

            assertEquals(LoginEvent.NavigateHome, awaitItem())
        }
        assertEquals("dummy_token", repository.observeAuthToken().first())
    }

    @Test
    fun `login with blank id sets Error uiState and does not emit NavigateHome event`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(SetAuthTokenUseCase(repository))

        viewModel.event.test {
            viewModel.login(id = "", password = "password")

            expectNoEvents()
        }
        assertEquals(LoginUiState.Error(BLANK_INPUT_MESSAGE), viewModel.uiState.value)
    }

    @Test
    fun `login with blank password sets Error uiState`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(SetAuthTokenUseCase(repository))

        viewModel.login(id = "user", password = "")

        assertEquals(LoginUiState.Error(BLANK_INPUT_MESSAGE), viewModel.uiState.value)
    }

    @Test
    fun `login with blank id and password sets Error uiState`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(SetAuthTokenUseCase(repository))

        viewModel.login(id = "", password = "")

        assertEquals(LoginUiState.Error(BLANK_INPUT_MESSAGE), viewModel.uiState.value)
    }

    private companion object {
        const val BLANK_INPUT_MESSAGE = "IDとパスワードを入力してください"
    }
}
