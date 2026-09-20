package com.example.authapplication.feature.search

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.authapplication.core.R as CoreR

/** 検索画面の表示状態。 */
sealed interface SearchUiState {
    data object Loading : SearchUiState

    /** 検索対象のデータ自体が0件の状態。 */
    data object Empty : SearchUiState

    /** データはあるが、[query] に一致するアイテムが無かった状態。 */
    data class NoResults(val query: String) : SearchUiState

    data class Success(val items: List<Item>) : SearchUiState

    /**
     * アイテムの取得に失敗した状態。[messageResId] の文言を表示し、再読み込みを促す。
     * 文言そのものではなく文字列リソースIDを持ち、解決はComposable側の `stringResource` に任せる。
     */
    data class Error(@param:StringRes val messageResId: Int) : SearchUiState
}

/** 検索画面へ一度きり通知するイベント。 */
sealed interface SearchEvent {
    /** お気に入りの更新に失敗したことをSnackbarで知らせる。 */
    data class ShowErrorSnackbar(@param:StringRes val messageResId: Int) : SearchEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * 検索キーワード。プロセスdeath後も復元できるよう [SavedStateHandle] で保持する
     * (Composable側の `rememberSaveable` ではなくViewModelを状態の所在とする)。
     */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _event = MutableSharedFlow<SearchEvent>()
    val event: SharedFlow<SearchEvent> = _event.asSharedFlow()

    val uiState: StateFlow<SearchUiState> = retryTrigger
        // 購読開始時にも一度流し、初回の読み込みとリトライを同じ経路に乗せる。
        .onStart { emit(Unit) }
        .flatMapLatest {
            combine(observeItemsUseCase(), query) { items, query -> toUiState(items, query) }
                .onStart { emit(SearchUiState.Loading) }
                .catch { throwable -> emit(SearchUiState.Error(throwable.toMessageResId())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState.Loading,
        )

    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
    }

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    /** 一覧の表示は保ったまま失敗だけを伝えるため、一度きりのイベントとして通知する。 */
    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            runCatching { toggleFavoriteUseCase(itemId) }
                .onFailure { throwable ->
                    _event.emit(SearchEvent.ShowErrorSnackbar(throwable.toMessageResId()))
                }
        }
    }

    /**
     * 「データ自体が0件([SearchUiState.Empty])」と「絞り込み結果が0件([SearchUiState.NoResults])」を
     * 区別できるよう、データの有無を先に判定する。
     */
    private fun toUiState(items: List<Item>, query: String): SearchUiState {
        if (items.isEmpty()) return SearchUiState.Empty
        val filteredItems = if (query.isBlank()) {
            items
        } else {
            items.filter { it.title.contains(query, ignoreCase = true) }
        }
        return if (filteredItems.isEmpty()) {
            SearchUiState.NoResults(query)
        } else {
            SearchUiState.Success(filteredItems)
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}

/** 失敗の種別([AppError])をこの画面で表示する文言のリソースIDへ変換する。詳しくは `HomeViewModel` を参照。 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.ITEM_LOAD -> R.string.feature_search_error_item_load
    AppError.FAVORITE_TOGGLE -> R.string.feature_search_error_favorite_toggle
    AppError.NOTIFICATION_LOAD, AppError.UNEXPECTED -> CoreR.string.core_error_unexpected
}
