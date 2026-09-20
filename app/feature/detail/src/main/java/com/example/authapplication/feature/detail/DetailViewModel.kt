package com.example.authapplication.feature.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.authapplication.core.navigation.AppRoute
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.error.toAppError
import com.example.authapplication.domain.item.GetItemUseCase
import com.example.authapplication.domain.item.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.example.authapplication.core.R as CoreR

/** 詳細画面の表示状態。 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val item: Item) : DetailUiState

    /** [AppRoute.Detail.itemId] に対応するアイテムが存在しなかった場合の状態。 */
    data object NotFound : DetailUiState

    /**
     * アイテムの取得自体に失敗した状態。[messageResId] の文言を表示し、再読み込みを促す。
     * 「取得できたが存在しない」[NotFound] とはユーザーへの説明が変わるため、別の状態として区別する。
     * 文言そのものではなく文字列リソースIDを持ち、解決はComposable側の `stringResource` に任せる。
     */
    data class Error(@param:StringRes val messageResId: Int) : DetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getItemUseCase: GetItemUseCase,
) : ViewModel() {

    /** 再読み込みのトリガー。詳しくは `HomeViewModel.retryTrigger` のコメントを参照。 */
    private val retryTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<DetailUiState> = retryTrigger
        // 購読開始時にも一度流し、初回の読み込みとリトライを同じ経路に乗せる。
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                val itemId = savedStateHandle.toRoute<AppRoute.Detail>().itemId
                val item = getItemUseCase(itemId)
                emit(if (item != null) DetailUiState.Success(item) else DetailUiState.NotFound)
            }
                .onStart { emit(DetailUiState.Loading) }
                .catch { throwable -> emit(DetailUiState.Error(throwable.toMessageResId())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading,
        )

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }
}

/** 失敗の種別([AppError])をこの画面で表示する文言のリソースIDへ変換する。詳しくは `HomeViewModel` を参照。 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.ITEM_LOAD -> R.string.feature_detail_error_item_load
    AppError.NOTIFICATION_LOAD, AppError.FAVORITE_TOGGLE, AppError.UNEXPECTED ->
        CoreR.string.core_error_unexpected
}
