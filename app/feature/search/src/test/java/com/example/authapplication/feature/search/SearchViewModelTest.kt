package com.example.authapplication.feature.search

import androidx.lifecycle.SavedStateHandle
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
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val favoriteRepository = FakeFavoriteRepository()

    private fun createViewModel(repository: FakeItemRepository) = SearchViewModel(
        observeItemsUseCase = ObserveItemsUseCase(repository, favoriteRepository),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(favoriteRepository),
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun `uiState is Loading then Success when repository emits items`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitItems(ITEMS)

            assertEquals(SearchUiState.Success(ITEMS), awaitItem())
        }
    }

    @Test
    fun `uiState is Empty when repository emits no items`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitItems(emptyList())

            assertEquals(SearchUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `uiState is Success with filtered items when query matches`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitItems(ITEMS)
            assertEquals(SearchUiState.Success(ITEMS), awaitItem())

            viewModel.onQueryChange("アイテム2")

            assertEquals(SearchUiState.Success(listOf(ITEMS[1])), awaitItem())
        }
    }

    @Test
    fun `uiState is NoResults when query matches nothing but items exist`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitItems(ITEMS)
            assertEquals(SearchUiState.Success(ITEMS), awaitItem())

            viewModel.onQueryChange(UNMATCHED_QUERY)

            assertEquals(SearchUiState.NoResults(UNMATCHED_QUERY), awaitItem())
        }
    }

    @Test
    fun `toggleFavorite flips isFavorite of the target item`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitItems(ITEMS)
            assertEquals(SearchUiState.Success(ITEMS), awaitItem())

            viewModel.toggleFavorite("2")

            assertEquals(
                SearchUiState.Success(listOf(ITEMS[0], ITEMS[1].copy(isFavorite = true), ITEMS[2])),
                awaitItem(),
            )
        }
    }

    @Test
    fun `query is exposed and updated by onQueryChange`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.query.test {
            assertEquals("", awaitItem())

            viewModel.onQueryChange(UNMATCHED_QUERY)

            assertEquals(UNMATCHED_QUERY, awaitItem())
        }
    }

    @Test
    fun `uiState is Error when repository throws`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitError(AppDataException(LOAD_ERROR_MESSAGE))

            assertEquals(SearchUiState.Error(LOAD_ERROR_MESSAGE), awaitItem())
        }
    }

    @Test
    fun `retry re-subscribes the flow and recovers from Error to Success`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            assertEquals(SearchUiState.Loading, awaitItem())

            repository.emitError(AppDataException(LOAD_ERROR_MESSAGE))
            assertEquals(SearchUiState.Error(LOAD_ERROR_MESSAGE), awaitItem())

            // リポジトリ側が回復しても、異常終了したFlowは購読し直すまで新しい値を流さない。
            repository.emitItems(ITEMS)
            expectNoEvents()

            viewModel.retry()

            // リトライで購読し直すと一度Loadingへ戻るが、結果がすぐ得られる場合は
            // StateFlowが値を畳み込むため、最終的な状態だけを確認する。
            assertEquals(SearchUiState.Success(ITEMS), expectMostRecentItem())
        }
    }

    @Test
    fun `toggleFavorite emits ShowErrorSnackbar event when it fails`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = createViewModel(repository)
        favoriteRepository.toggleError = AppDataException(TOGGLE_ERROR_MESSAGE)

        viewModel.event.test {
            viewModel.toggleFavorite("2")

            assertEquals(SearchEvent.ShowErrorSnackbar(TOGGLE_ERROR_MESSAGE), awaitItem())
        }
    }

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
            Item(id = "3", title = "アイテム3"),
        )
        const val UNMATCHED_QUERY = "該当なし"
        const val LOAD_ERROR_MESSAGE = "アイテムの取得に失敗しました"
        const val TOGGLE_ERROR_MESSAGE = "お気に入りの更新に失敗しました"
    }
}
