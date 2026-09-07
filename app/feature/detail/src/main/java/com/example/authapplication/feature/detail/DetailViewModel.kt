package com.example.authapplication.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.domain.error.toUserMessage
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** 詳細画面の表示状態。 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val item: Item) : DetailUiState

    /** [AppRoute.Detail.itemId] に対応するアイテムが存在しなかった場合の状態。 */
    data object NotFound : DetailUiState

    /**
     * アイテムの取得自体に失敗した状態。[message] を表示し、再読み込みを促す。
     * 「取得できたが存在しない」[NotFound] とはユーザーへの説明が変わるため、別の状態として区別する。
     */
    data class Error(val message: String) : DetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
) : ViewModel() {

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DetailUiState> = retryTrigger
        .flatMapLatest {
            flow {
                val itemId = savedStateHandle.toRoute<AppRoute.Detail>().itemId
                val item = itemRepository.getItemById(itemId)
                emit(if (item != null) DetailUiState.Success(item) else DetailUiState.NotFound)
            }
                .onStart { emit(DetailUiState.Loading) }
                .catch { throwable -> emit(DetailUiState.Error(throwable.toUserMessage())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading,
        )

    fun retry() {
        retryTrigger.update { it + 1 }
    }
}
