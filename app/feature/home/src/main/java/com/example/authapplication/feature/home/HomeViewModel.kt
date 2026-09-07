package com.example.authapplication.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.error.toUserMessage
import com.example.authapplication.domain.favorite.ToggleFavoriteUseCase
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ObserveItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ホーム画面の表示状態。 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(val items: List<Item>) : HomeUiState

    /** アイテムの取得に失敗した状態。[message] を表示し、再読み込みを促す。 */
    data class Error(val message: String) : HomeUiState
}

/** ホーム画面へ一度きり通知するイベント。 */
sealed interface HomeEvent {
    /** お気に入りの更新に失敗したことをSnackbarで知らせる。 */
    data class ShowErrorSnackbar(val message: String) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    /**
     * 再読み込みのトリガー。値が変わるたびに [flatMapLatest] が下流のFlowを購読し直す。
     * 例外で異常終了したFlowはそのままでは再開しないため、購読ごと作り直すことでリトライを実現する。
     */
    private val retryTrigger = MutableStateFlow(0)

    private val _event = MutableSharedFlow<HomeEvent>()
    val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = retryTrigger
        .flatMapLatest {
            observeItemsUseCase()
                .map { items -> if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Success(items) }
                // リトライ直後は前回の結果ではなくローディングから始める。
                .onStart { emit(HomeUiState.Loading) }
                // リポジトリ層の例外はここでUiStateへ変換し、UIまで例外を伝播させない。
                .catch { throwable -> emit(HomeUiState.Error(throwable.toUserMessage())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    /**
     * 一覧の表示は保ったまま失敗だけを伝えたいので、[HomeUiState.Error] ではなく
     * 一度きりのイベント([HomeEvent.ShowErrorSnackbar])として通知する。
     */
    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            runCatching { toggleFavoriteUseCase(itemId) }
                .onFailure { throwable ->
                    _event.emit(HomeEvent.ShowErrorSnackbar(throwable.toUserMessage()))
                }
        }
    }
}
