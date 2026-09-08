package com.example.authapplication.feature.favorite

import app.cash.turbine.test
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.favorite.FakeFavoriteRepository
import com.example.authapplication.domain.favorite.ObserveFavoriteItemsUseCase
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
class FavoriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = FakeItemRepository()
    private val favoriteRepository = FakeFavoriteRepository()

    private fun createViewModel() = FavoriteViewModel(
        observeFavoriteItemsUseCase = ObserveFavoriteItemsUseCase(
            ObserveItemsUseCase(itemRepository, favoriteRepository),
        ),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository),
    )

    @Test
    fun `uiState is Loading then Empty when no item is favorited`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)

            assertEquals(FavoriteUiState.Empty, awaitItem())
        }
    }

    /** 他画面(ホーム/検索)でお気に入り登録済みのアイテムだけが並ぶことを確認する。 */
    @Test
    fun `uiState is Success with favorited items only`() = runTest {
        favoriteRepository.toggleFavorite("2")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)

            assertEquals(
                FavoriteUiState.Success(listOf(ITEMS[1].copy(isFavorite = true))),
                awaitItem(),
            )
        }
    }

    @Test
    fun `toggleFavorite removes the item from the list`() = runTest {
        favoriteRepository.toggleFavorite("2")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            itemRepository.emitItems(ITEMS)
            assertEquals(
                FavoriteUiState.Success(listOf(ITEMS[1].copy(isFavorite = true))),
                awaitItem(),
            )

            viewModel.toggleFavorite("2")

            assertEquals(FavoriteUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            itemRepository.emitError(AppDataException(AppError.ITEM_LOAD))

            assertEquals(FavoriteUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), awaitItem())
        }
    }

    @Test
    fun `retry re-subscribes the flow and recovers from Error to Success`() = runTest {
        favoriteRepository.toggleFavorite("2")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            itemRepository.emitError(AppDataException(AppError.ITEM_LOAD))
            assertEquals(FavoriteUiState.Error(LOAD_ERROR_MESSAGE_RES_ID), awaitItem())

            // リポジトリ側が回復しても、異常終了したFlowは購読し直すまで新しい値を流さない。
            itemRepository.emitItems(ITEMS)
            expectNoEvents()

            viewModel.retry()

            // リトライで購読し直すと一度Loadingへ戻るが、結果がすぐ得られる場合は
            // StateFlowが値を畳み込むため、最終的な状態だけを確認する。
            assertEquals(
                FavoriteUiState.Success(listOf(ITEMS[1].copy(isFavorite = true))),
                expectMostRecentItem(),
            )
        }
    }

    @Test
    fun `toggleFavorite emits ShowErrorSnackbar event when it fails`() = runTest {
        val viewModel = createViewModel()
        favoriteRepository.toggleError = AppDataException(AppError.FAVORITE_TOGGLE)

        viewModel.event.test {
            viewModel.toggleFavorite("2")

            assertEquals(FavoriteEvent.ShowErrorSnackbar(TOGGLE_ERROR_MESSAGE_RES_ID), awaitItem())
        }
    }

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
            Item(id = "3", title = "アイテム3"),
        )
        val LOAD_ERROR_MESSAGE_RES_ID = R.string.feature_favorite_error_item_load
        val TOGGLE_ERROR_MESSAGE_RES_ID = R.string.feature_favorite_error_favorite_toggle
    }
}
