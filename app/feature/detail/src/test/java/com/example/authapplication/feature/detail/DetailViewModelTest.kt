package com.example.authapplication.feature.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.GetItemUseCase
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [DetailViewModel] は `SavedStateHandle.toRoute<AppRoute.Detail>()` で遷移引数を取り出す。
 * この復元処理はAndroidの `Bundle` に依存するため、素のJVMではなくRobolectric上で実行する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = FakeItemRepository()

    /** `AppRoute.Detail(itemId)` で遷移してきた状態を、遷移引数を詰めた [SavedStateHandle] で再現する。 */
    private fun createViewModel(itemId: String) = DetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId)),
        getItemUseCase = GetItemUseCase(itemRepository),
    )

    @Test
    fun `uiState is Loading before subscribed and Success once the item is loaded`() = runTest {
        itemRepository.emitItems(ITEMS)
        val viewModel = createViewModel(itemId = "2")

        // 購読が始まるまでは stateIn の initialValue のまま。
        assertEquals(DetailUiState.Loading, viewModel.uiState.value)

        viewModel.uiState.test {
            // アイテムの取得は即座に完了するため、Loadingは畳み込まれる。最終的な状態だけを確認する。
            assertEquals(DetailUiState.Success(ITEMS[1]), expectMostRecentItem())
        }
    }

    /** 「取得できたが対象が無い」ことを示す状態。取得自体の失敗([DetailUiState.Error])とは区別する。 */
    @Test
    fun `uiState is NotFound when no item matches the route argument`() = runTest {
        itemRepository.emitItems(ITEMS)
        val viewModel = createViewModel(itemId = "999")

        viewModel.uiState.test {
            assertEquals(DetailUiState.NotFound, expectMostRecentItem())
        }
    }

    @Test
    fun `uiState is Error when the repository throws`() = runTest {
        itemRepository.emitError(AppDataException(AppError.ITEM_LOAD))
        val viewModel = createViewModel(itemId = "2")

        viewModel.uiState.test {
            assertEquals(DetailUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), expectMostRecentItem())
        }
    }

    /** 「エラー表示 → リトライ → 成功」の一連の流れ。 */
    @Test
    fun `retry re-subscribes the flow and recovers from Error to Success`() = runTest {
        itemRepository.emitError(AppDataException(AppError.ITEM_LOAD))
        val viewModel = createViewModel(itemId = "2")

        viewModel.uiState.test {
            assertEquals(DetailUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), expectMostRecentItem())

            // リポジトリ側が回復しても、異常終了したFlowは購読し直すまで新しい値を流さない。
            itemRepository.emitItems(ITEMS)
            expectNoEvents()

            viewModel.retry()

            assertEquals(DetailUiState.Success(ITEMS[1]), expectMostRecentItem())
        }
    }

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
            Item(id = "3", title = "アイテム3"),
        )
        val LOAD_ERROR_MESSAGE_RES_ID = R.string.feature_detail_error_item_load
    }
}
