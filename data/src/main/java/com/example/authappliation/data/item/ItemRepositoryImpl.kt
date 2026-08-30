package com.example.authappliation.data.item

import com.example.authappliation.domain.item.Item
import com.example.authappliation.domain.item.ItemRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** メモリ上のモックデータを返す実装（学習用の最小構成）。 */
@Singleton
class ItemRepositoryImpl @Inject constructor() : ItemRepository {

    private val mockItems = (1..5).map { index -> Item(id = index.toString(), title = "Item $index") }

    override fun observeItems(): Flow<List<Item>> = flowOf(mockItems)

    override suspend fun getItemById(id: String): Item? = mockItems.find { it.id == id }
}
