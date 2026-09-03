package com.example.authapplication.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** 詳細画面の表示状態。 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val item: Item) : DetailUiState

    /** [AppRoute.Detail.itemId] に対応するアイテムが存在しなかった場合の状態。 */
    data object NotFound : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = flow {
        val itemId = savedStateHandle.toRoute<AppRoute.Detail>().itemId
        val item = itemRepository.getItemById(itemId)
        emit(if (item != null) DetailUiState.Success(item) else DetailUiState.NotFound)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )
}
