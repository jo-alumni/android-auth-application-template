package com.example.authapplication.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** お気に入り画面の表示状態。 */
sealed interface FavoriteUiState {
    data object Loading : FavoriteUiState
    data object Empty : FavoriteUiState
    data class Success(val items: List<Item>) : FavoriteUiState
}

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoriteUiState> = itemRepository.observeItems()
        .map { items ->
            if (items.isEmpty()) FavoriteUiState.Empty else FavoriteUiState.Success(items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoriteUiState.Loading,
        )
}
