package com.example.authapplication.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.error.toUserMessage
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** 通知画面の表示状態。 */
sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data object Empty : NotificationUiState
    data class Success(val notifications: List<Notification>) : NotificationUiState

    /** 通知の取得に失敗した状態。[message] を表示し、再読み込みを促す。 */
    data class Error(val message: String) : NotificationUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<NotificationUiState> = retryTrigger
        .flatMapLatest {
            notificationRepository.observeNotifications()
                .map { notifications ->
                    if (notifications.isEmpty()) {
                        NotificationUiState.Empty
                    } else {
                        NotificationUiState.Success(notifications)
                    }
                }
                .onStart { emit(NotificationUiState.Loading) }
                .catch { throwable -> emit(NotificationUiState.Error(throwable.toUserMessage())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationUiState.Loading,
        )

    fun retry() {
        retryTrigger.update { it + 1 }
    }
}
