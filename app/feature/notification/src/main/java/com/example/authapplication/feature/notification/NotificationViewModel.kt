package com.example.authapplication.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 通知画面の表示状態。 */
sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data object Empty : NotificationUiState
    data class Success(val notifications: List<Notification>) : NotificationUiState
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = notificationRepository.observeNotifications()
        .map { notifications ->
            if (notifications.isEmpty()) NotificationUiState.Empty else NotificationUiState.Success(notifications)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationUiState.Loading,
        )
}
