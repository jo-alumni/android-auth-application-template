package com.example.authapplication.domain.item

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * テスト用の [ItemRepository] フェイク実装。メモリ上でアイテム一覧を保持する。
 * [emitItems] が呼ばれるまで [observeItems] は何も発行しないため、
 * ViewModelの `Loading` 状態をテストから観測できる。
 */
class FakeItemRepository : ItemRepository {

    private val itemsFlow = MutableSharedFlow<List<Item>>(replay = 1)

    suspend fun emitItems(items: List<Item>) {
        itemsFlow.emit(items)
    }

    override fun observeItems(): Flow<List<Item>> = itemsFlow

    override suspend fun getItemById(id: String): Item? =
        itemsFlow.replayCache.lastOrNull()?.find { it.id == id }
}
