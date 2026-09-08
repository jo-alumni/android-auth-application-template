package com.example.authapplication

import app.cash.turbine.test
import com.example.authapplication.domain.auth.ClearAuthTokenUseCase
import com.example.authapplication.domain.auth.FakeAuthRepository
import com.example.authapplication.domain.auth.IsAuthenticatedUseCase
import com.example.authapplication.domain.debug.FakeErrorInjectionRepository
import com.example.authapplication.domain.debug.ObserveErrorInjectionUseCase
import com.example.authapplication.domain.debug.SetErrorInjectionUseCase
import com.example.authapplication.domain.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val errorInjectionRepository = FakeErrorInjectionRepository()

    private fun createViewModel(repository: FakeAuthRepository) = AppViewModel(
        isAuthenticatedUseCase = IsAuthenticatedUseCase(repository),
        observeErrorInjectionUseCase = ObserveErrorInjectionUseCase(errorInjectionRepository),
        clearAuthTokenUseCase = ClearAuthTokenUseCase(repository),
        setErrorInjectionUseCase = SetErrorInjectionUseCase(errorInjectionRepository),
    )

    @Test
    fun `authState is Loading then Ready(false) when repository reports no token`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = createViewModel(repository)

        viewModel.authState.test {
            assertEquals(AuthUiState.Loading, awaitItem())

            repository.clearAuthToken()

            assertEquals(AuthUiState.Ready(isAuthenticated = false), awaitItem())
        }
    }

    @Test
    fun `authState is Loading then Ready(true) when repository reports a token`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = createViewModel(repository)

        viewModel.authState.test {
            assertEquals(AuthUiState.Loading, awaitItem())

            repository.setAuthToken("dummy_token")

            assertEquals(AuthUiState.Ready(isAuthenticated = true), awaitItem())
        }
    }

    @Test
    fun `logout clears auth token and emits NavigateLogin event`() = runTest {
        val repository = FakeAuthRepository()
        repository.setAuthToken("dummy_token")
        val viewModel = createViewModel(repository)

        viewModel.event.test {
            viewModel.logout()

            assertEquals(AppEvent.NavigateLogin, awaitItem())
        }
        assertEquals(null, repository.observeAuthToken().first())
    }

    @Test
    fun `setErrorInjectionEnabled updates isErrorInjectionEnabled`() = runTest {
        val viewModel = createViewModel(FakeAuthRepository())

        viewModel.isErrorInjectionEnabled.test {
            assertEquals(false, awaitItem())

            viewModel.setErrorInjectionEnabled(true)
            assertEquals(true, awaitItem())

            viewModel.setErrorInjectionEnabled(false)
            assertEquals(false, awaitItem())
        }
    }
}
