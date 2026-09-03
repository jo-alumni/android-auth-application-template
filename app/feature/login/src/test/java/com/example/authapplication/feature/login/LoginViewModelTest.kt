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
    fun `login sets dummy auth token and emits NavigateHome event`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(SetAuthTokenUseCase(repository))

        viewModel.event.test {
            viewModel.login()

            assertEquals(LoginEvent.NavigateHome, awaitItem())
        }
        assertEquals("dummy_token", repository.observeAuthToken().first())
    }
}
