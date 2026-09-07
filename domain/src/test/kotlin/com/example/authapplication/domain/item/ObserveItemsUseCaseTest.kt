package com.example.authapplication.domain.item

import app.cash.turbine.test
import com.example.authapplication.domain.favorite.FakeFavoriteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveItemsUseCaseTest {

    private val itemRepository = FakeItemRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val observeItems = ObserveItemsUseCase(itemRepository, favoriteRepository)

    @Test
    fun `emits items with isFavorite false when nothing is favorited`() = runTest {
        observeItems().test {
            itemRepository.emitItems(ITEMS)

            assertEquals(ITEMS, awaitItem())
        }
    }

    @Test
    fun `emits items with isFavorite true for favorited ids`() = runTest {
        favoriteRepository.toggleFavorite("2")

        observeItems().test {
            itemRepository.emitItems(ITEMS)

            assertEquals(listOf(false, true, false), awaitItem().map { it.isFavorite })
        }
    }

    @Test
    fun `re-emits when favorite state changes`() = runTest {
        observeItems().test {
            itemRepository.emitItems(ITEMS)
            assertEquals(listOf(false, false, false), awaitItem().map { it.isFavorite })

            favoriteRepository.toggleFavorite("1")
            assertEquals(listOf(true, false, false), awaitItem().map { it.isFavorite })

            favoriteRepository.toggleFavorite("1")
            assertEquals(listOf(false, false, false), awaitItem().map { it.isFavorite })
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
