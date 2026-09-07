package com.example.authapplication.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 検索画面の表示状態。 */
sealed interface SearchUiState {
    data object Loading : SearchUiState

    /** 検索対象のデータ自体が0件の状態。 */
    data object Empty : SearchUiState

    /** データはあるが、[query] に一致するアイテムが無かった状態。 */
    data class NoResults(val query: String) : SearchUiState

    data class Success(val items: List<Item>) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    itemRepository: ItemRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * 検索キーワード。プロセスdeath後も復元できるよう [SavedStateHandle] で保持する
     * (Composable側の `rememberSaveable` ではなくViewModelを状態の所在とする)。
     */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<SearchUiState> =
        combine(itemRepository.observeItems(), query) { items, query -> toUiState(items, query) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SearchUiState.Loading,
            )

    fun onQueryChange(query: String) {
        savedStateHandle[KEY_QUERY] = query
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
