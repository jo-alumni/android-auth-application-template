package com.example.authapplication.feature.notification

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.error.toAppError
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.example.authapplication.core.R as CoreR

/** 通知画面の表示状態。 */
sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data object Empty : NotificationUiState
    data class Success(val notifications: List<Notification>) : NotificationUiState

    /**
     * 通知の取得に失敗した状態。[messageResId] の文言を表示し、再読み込みを促す。
     * 文言そのものではなく文字列リソースIDを持ち、解決はComposable側の `stringResource` に任せる。
     */
    data class Error(@param:StringRes val messageResId: Int) : NotificationUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val observeNotificationsUseCase: ObserveNotificationsUseCase,
) : ViewModel() {

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<NotificationUiState> = retryTrigger
        // 購読開始時にも一度流し、初回の読み込みとリトライを同じ経路に乗せる。
        .onStart { emit(Unit) }
        .flatMapLatest {
            observeNotificationsUseCase()
                .map { notifications ->
                    if (notifications.isEmpty()) {
                        NotificationUiState.Empty
                    } else {
                        NotificationUiState.Success(notifications)
                    }
                }
                .onStart { emit(NotificationUiState.Loading) }
                .catch { throwable -> emit(NotificationUiState.Error(throwable.toMessageResId())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationUiState.Loading,
        )

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }
}

/** 失敗の種別([AppError])をこの画面で表示する文言のリソースIDへ変換する。詳しくは `HomeViewModel` を参照。 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.NOTIFICATION_LOAD -> R.string.feature_notification_error_load
    AppError.ITEM_LOAD, AppError.FAVORITE_TOGGLE, AppError.UNEXPECTED ->
        CoreR.string.core_error_unexpected
}
