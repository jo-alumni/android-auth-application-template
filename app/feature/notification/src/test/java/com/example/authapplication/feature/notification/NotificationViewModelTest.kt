package com.example.authapplication.feature.notification

import app.cash.turbine.test
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.notification.FakeNotificationRepository
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.ObserveNotificationsUseCase
import com.example.authapplication.domain.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notificationRepository = FakeNotificationRepository()

    private fun createViewModel() =
        NotificationViewModel(ObserveNotificationsUseCase(notificationRepository))

    @Test
    fun `uiState is Loading then Success when repository emits notifications`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(NotificationUiState.Loading, awaitItem())

            notificationRepository.emitNotifications(NOTIFICATIONS)

            assertEquals(NotificationUiState.Success(NOTIFICATIONS), awaitItem())
        }
    }

    @Test
    fun `uiState is Empty when repository emits no notifications`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(NotificationUiState.Loading, awaitItem())

            notificationRepository.emitNotifications(emptyList())

            assertEquals(NotificationUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(NotificationUiState.Loading, awaitItem())

            notificationRepository.emitError(AppDataException(AppError.NOTIFICATION_LOAD))

            assertEquals(NotificationUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), awaitItem())
        }
    }

    @Test
    fun `retry re-subscribes the flow and recovers from Error to Success`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(NotificationUiState.Loading, awaitItem())

            notificationRepository.emitError(AppDataException(AppError.NOTIFICATION_LOAD))
            assertEquals(NotificationUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), awaitItem())

            // リポジトリ側が回復しても、異常終了したFlowは購読し直すまで新しい値を流さない。
            notificationRepository.emitNotifications(NOTIFICATIONS)
            expectNoEvents()

            viewModel.retry()

            // リトライで購読し直すと一度Loadingへ戻るが、結果がすぐ得られる場合は
            // StateFlowが値を畳み込むため、最終的な状態だけを確認する。
            assertEquals(NotificationUiState.Success(NOTIFICATIONS), expectMostRecentItem())
        }
    }

    private companion object {
        val NOTIFICATIONS = listOf(
            Notification(id = "1", title = "お知らせ1", message = "サンプル通知メッセージです。"),
            Notification(id = "2", title = "お知らせ2", message = "サンプル通知メッセージです。"),
        )
        val LOAD_ERROR_MESSAGE_RES_ID = R.string.feature_notification_error_load
    }
}
