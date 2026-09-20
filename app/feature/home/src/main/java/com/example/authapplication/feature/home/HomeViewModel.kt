package com.example.authapplication.feature.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.error.toAppError
import com.example.authapplication.domain.favorite.ToggleFavoriteUseCase
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ObserveItemsUseCase
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

/** ホーム画面の表示状態。 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(val items: List<Item>) : HomeUiState

    /**
     * アイテムの取得に失敗した状態。[messageResId] の文言を表示し、再読み込みを促す。
     * 文言そのものではなく文字列リソースIDを持ち、解決はComposable側の `stringResource` に任せる。
     */
    data class Error(@param:StringRes val messageResId: Int) : HomeUiState
}

/** ホーム画面へ一度きり通知するイベント。 */
sealed interface HomeEvent {
    /** お気に入りの更新に失敗したことをSnackbarで知らせる。 */
    data class ShowErrorSnackbar(@param:StringRes val messageResId: Int) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    /**
     * 再読み込みのトリガー。リトライは「状態」ではなく「出来事」なので [MutableSharedFlow] で表す。
     * 流れるたびに [flatMapLatest] が下流のFlowを購読し直し、例外で異常終了して
     * そのままでは再開しないFlowを作り直すことでリトライを実現する。
     *
     * バッファを1つ持たせて [MutableSharedFlow.tryEmit] で流すため、[retry] はsuspendにならない。
     */
    private val retryTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _event = MutableSharedFlow<HomeEvent>()
    val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = retryTrigger
        // 購読開始時にも一度流し、初回の読み込みとリトライを同じ経路に乗せる。
        .onStart { emit(Unit) }
        .flatMapLatest {
            observeItemsUseCase()
                .map { items -> if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Success(items) }
                // リトライ直後は前回の結果ではなくローディングから始める。
                .onStart { emit(HomeUiState.Loading) }
                // リポジトリ層の例外はここでUiStateへ変換し、UIまで例外を伝播させない。
                .catch { throwable -> emit(HomeUiState.Error(throwable.toMessageResId())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    /**
     * 一覧の表示は保ったまま失敗だけを伝えたいので、[HomeUiState.Error] ではなく
     * 一度きりのイベント([HomeEvent.ShowErrorSnackbar])として通知する。
     */
    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            runCatching { toggleFavoriteUseCase(itemId) }
                .onFailure { throwable ->
                    _event.emit(HomeEvent.ShowErrorSnackbar(throwable.toMessageResId()))
                }
        }
    }
}

/**
 * 失敗の種別([AppError])をこの画面で表示する文言のリソースIDへ変換する。
 *
 * 文言はfeatureモジュールが自分のstrings.xmlで持ち、:domain は種別しか知らない。
 * この画面で起こり得ない種別は共通の文言（:app:core）にフォールバックさせる。
 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.ITEM_LOAD -> R.string.feature_home_error_item_load
    AppError.FAVORITE_TOGGLE -> R.string.feature_home_error_favorite_toggle
    AppError.NOTIFICATION_LOAD, AppError.UNEXPECTED -> CoreR.string.core_error_unexpected
}
