package com.example.authapplication.feature.favorite

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.error.toAppError
import com.example.authapplication.domain.favorite.ObserveFavoriteItemsUseCase
import com.example.authapplication.domain.favorite.ToggleFavoriteUseCase
import com.example.authapplication.domain.item.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.authapplication.core.R as CoreR

/** お気に入り画面の表示状態。 */
sealed interface FavoriteUiState {
    data object Loading : FavoriteUiState
    data object Empty : FavoriteUiState
    data class Success(val items: List<Item>) : FavoriteUiState

    /**
     * アイテムの取得に失敗した状態。[messageResId] の文言を表示し、再読み込みを促す。
     * 文言そのものではなく文字列リソースIDを持ち、解決はComposable側の `stringResource` に任せる。
     */
    data class Error(@param:StringRes val messageResId: Int) : FavoriteUiState
}

/** お気に入り画面へ一度きり通知するイベント。 */
sealed interface FavoriteEvent {
    /** お気に入りの更新に失敗したことをSnackbarで知らせる。 */
    data class ShowErrorSnackbar(@param:StringRes val messageResId: Int) : FavoriteEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    observeFavoriteItemsUseCase: ObserveFavoriteItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _event = MutableSharedFlow<FavoriteEvent>()
    val event: SharedFlow<FavoriteEvent> = _event.asSharedFlow()

    val uiState: StateFlow<FavoriteUiState> = retryTrigger
        // 購読開始時にも一度流し、初回の読み込みとリトライを同じ経路に乗せる。
        .onStart { emit(Unit) }
        .flatMapLatest {
            observeFavoriteItemsUseCase()
                .map { items ->
                    if (items.isEmpty()) FavoriteUiState.Empty else FavoriteUiState.Success(items)
                }
                .onStart { emit(FavoriteUiState.Loading) }
                .catch { throwable -> emit(FavoriteUiState.Error(throwable.toMessageResId())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoriteUiState.Loading,
        )

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    /** 一覧の表示は保ったまま失敗だけを伝えるため、一度きりのイベントとして通知する。 */
    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            runCatching { toggleFavoriteUseCase(itemId) }
                .onFailure { throwable ->
                    _event.emit(FavoriteEvent.ShowErrorSnackbar(throwable.toMessageResId()))
                }
        }
    }
}

/** 失敗の種別([AppError])をこの画面で表示する文言のリソースIDへ変換する。詳しくは `HomeViewModel` を参照。 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.ITEM_LOAD -> R.string.feature_favorite_error_item_load
    AppError.FAVORITE_TOGGLE -> R.string.feature_favorite_error_favorite_toggle
    AppError.NOTIFICATION_LOAD, AppError.UNEXPECTED -> CoreR.string.core_error_unexpected
}
