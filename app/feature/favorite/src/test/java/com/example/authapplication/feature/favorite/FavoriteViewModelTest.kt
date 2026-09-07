package com.example.authapplication.feature.favorite

import app.cash.turbine.test
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.Item
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

    @Test
    fun `uiState is Loading then Success when repository emits items`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = FavoriteViewModel(itemRepository = repository)

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            repository.emitItems(ITEMS)

            assertEquals(FavoriteUiState.Success(ITEMS), awaitItem())
        }
    }

    @Test
    fun `uiState is Empty when repository emits no items`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = FavoriteViewModel(itemRepository = repository)

        viewModel.uiState.test {
            assertEquals(FavoriteUiState.Loading, awaitItem())

            repository.emitItems(emptyList())

            assertEquals(FavoriteUiState.Empty, awaitItem())
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
