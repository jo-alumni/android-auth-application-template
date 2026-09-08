package com.example.authapplication.feature.home

import app.cash.turbine.test
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.favorite.FakeFavoriteRepository
import com.example.authapplication.domain.favorite.ToggleFavoriteUseCase
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ObserveItemsUseCase
import com.example.authapplication.domain.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = FakeItemRepository()
    private val favoriteRepository = FakeFavoriteRepository()

    private fun createViewModel() = HomeViewModel(
        observeItemsUseCase = ObserveItemsUseCase(itemRepository, favoriteRepository),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository),
    )

    @Test
    fun `uiState is Loading then Success when repository emits items`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)

            assertEquals(HomeUiState.Success(ITEMS), awaitItem())
        }
    }

    @Test
    fun `uiState is Empty when repository emits no items`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitItems(emptyList())

            assertEquals(HomeUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitError(AppDataException(LOAD_ERROR_MESSAGE))

            assertEquals(HomeUiState.Error(LOAD_ERROR_MESSAGE), awaitItem())
        }
    }

    /** 「エラー表示 → リトライ → 成功」の一連の流れ。 */
    @Test
    fun `retry re-subscribes the flow and recovers from Error to Success`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitError(AppDataException(LOAD_ERROR_MESSAGE))
            assertEquals(HomeUiState.Error(LOAD_ERROR_MESSAGE), awaitItem())

            // リポジトリ側が回復しても、異常終了したFlowは購読し直すまで新しい値を流さない。
            itemRepository.emitItems(ITEMS)
            expectNoEvents()

            viewModel.retry()

            // リトライで購読し直すと一度Loadingへ戻るが、結果がすぐ得られる場合は
            // StateFlowが値を畳み込むため、最終的な状態だけを確認する。
            assertEquals(HomeUiState.Success(ITEMS), expectMostRecentItem())
        }
    }

    @Test
    fun `retry keeps Error state when the repository still fails`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitError(AppDataException(LOAD_ERROR_MESSAGE))
            assertEquals(HomeUiState.Error(LOAD_ERROR_MESSAGE), awaitItem())

            viewModel.retry()

            // 購読をやり直しても失敗し続けるため、最終的な状態はErrorのまま。
            assertEquals(HomeUiState.Error(LOAD_ERROR_MESSAGE), viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleFavorite flips isFavorite of the target item`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)
            assertEquals(HomeUiState.Success(ITEMS), awaitItem())

            viewModel.toggleFavorite("2")
            assertEquals(
                HomeUiState.Success(listOf(ITEMS[0], ITEMS[1].copy(isFavorite = true), ITEMS[2])),
                awaitItem(),
            )

            viewModel.toggleFavorite("2")
            assertEquals(HomeUiState.Success(ITEMS), awaitItem())
        }
    }

    /** 一覧の表示は保ったまま、更新の失敗だけをSnackbarイベントとして通知する。 */
    @Test
    fun `toggleFavorite emits ShowErrorSnackbar event and keeps uiState when it fails`() = runTest {
        val viewModel = createViewModel()
        favoriteRepository.toggleError = AppDataException(TOGGLE_ERROR_MESSAGE)

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)
            assertEquals(HomeUiState.Success(ITEMS), awaitItem())

            viewModel.event.test {
                viewModel.toggleFavorite("2")

                assertEquals(HomeEvent.ShowErrorSnackbar(TOGGLE_ERROR_MESSAGE), awaitItem())
            }

            // 失敗しても一覧の表示状態は変わらない。
            expectNoEvents()
        }
    }

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
            Item(id = "3", title = "アイテム3"),
        )
        const val LOAD_ERROR_MESSAGE = "アイテムの取得に失敗しました"
        const val TOGGLE_ERROR_MESSAGE = "お気に入りの更新に失敗しました"
    }
}
