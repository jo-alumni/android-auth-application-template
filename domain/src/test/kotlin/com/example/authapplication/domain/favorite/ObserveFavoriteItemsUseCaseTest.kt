package com.example.authapplication.domain.favorite

import app.cash.turbine.test
import com.example.authapplication.domain.item.FakeItemRepository
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ObserveItemsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFavoriteItemsUseCaseTest {

    private val itemRepository = FakeItemRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val observeFavoriteItems = ObserveFavoriteItemsUseCase(
        ObserveItemsUseCase(itemRepository, favoriteRepository),
    )

    @Test
    fun `emits empty list when nothing is favorited`() = runTest {
        observeFavoriteItems().test {
            itemRepository.emitItems(ITEMS)

            assertEquals(emptyList<Item>(), awaitItem())
        }
    }

    /** 他画面(ホーム/検索)でのトグルが、そのままお気に入り一覧に伝播することを表すテスト。 */
    @Test
    fun `emits favorited items immediately after toggleFavorite`() = runTest {
        observeFavoriteItems().test {
            itemRepository.emitItems(ITEMS)
            assertEquals(emptyList<Item>(), awaitItem())

            favoriteRepository.toggleFavorite("2")
            assertEquals(listOf(ITEMS[1].copy(isFavorite = true)), awaitItem())

            favoriteRepository.toggleFavorite("2")
            assertEquals(emptyList<Item>(), awaitItem())
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
