package com.example.authapplication.feature.home

import app.cash.turbine.test
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

    private companion object {
        val ITEMS = listOf(
            Item(id = "1", title = "アイテム1"),
            Item(id = "2", title = "アイテム2"),
            Item(id = "3", title = "アイテム3"),
        )
    }
}
