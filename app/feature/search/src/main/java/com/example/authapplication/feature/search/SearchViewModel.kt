package com.example.authapplication.feature.search

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 検索画面の表示状態。 */
sealed interface SearchUiState {
    data object Loading : SearchUiState

    /** 検索対象のデータ自体が0件の状態。 */
    data object Empty : SearchUiState

    /** データはあるが、[query] に一致するアイテムが無かった状態。 */
    data class NoResults(val query: String) : SearchUiState

    data class Success(val items: List<Item>) : SearchUiState

    /** アイテムの取得に失敗した状態。[message] を表示し、再読み込みを促す。 */
    data class Error(val message: String) : SearchUiState
}

/** 検索画面へ一度きり通知するイベント。 */
sealed interface SearchEvent {
    /** お気に入りの更新に失敗したことをSnackbarで知らせる。 */
    data class ShowErrorSnackbar(val message: String) : SearchEvent
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
    private val retryTrigger = MutableStateFlow(0)

    private val _event = MutableSharedFlow<SearchEvent>()
    val event: SharedFlow<SearchEvent> = _event.asSharedFlow()

    val uiState: StateFlow<SearchUiState> = retryTrigger
        .flatMapLatest {
            combine(observeItemsUseCase(), query) { items, query -> toUiState(items, query) }
                .onStart { emit(SearchUiState.Loading) }
                .catch { throwable -> emit(SearchUiState.Error(throwable.toUserMessage())) }
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
        retryTrigger.update { it + 1 }
    }

    /** 一覧の表示は保ったまま失敗だけを伝えるため、一度きりのイベントとして通知する。 */
    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            runCatching { toggleFavoriteUseCase(itemId) }
                .onFailure { throwable ->
                    _event.emit(SearchEvent.ShowErrorSnackbar(throwable.toUserMessage()))
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
