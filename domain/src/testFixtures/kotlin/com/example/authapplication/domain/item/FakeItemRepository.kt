package com.example.authapplication.domain.item

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

/**
 * テスト用の [ItemRepository] フェイク実装。メモリ上でアイテム一覧を保持する。
 * [emitItems] が呼ばれるまで [observeItems] は何も発行しないため、
 * ViewModelの `Loading` 状態をテストから観測できる。
 *
 * 成功/失敗のどちらも流せるよう `Result` を保持しており、[emitError] で失敗させた後に
 * [emitItems] で成功に戻せば「エラー表示 → リトライ → 成功」をテストで再現できる。
 */
class FakeItemRepository : ItemRepository {

    private val resultFlow = MutableSharedFlow<Result<List<Item>>>(replay = 1)

    suspend fun emitItems(items: List<Item>) {
        resultFlow.emit(Result.success(items))
    }

    /** 以降の [observeItems] / [getItemById] が [throwable] を投げるようにする。 */
    suspend fun emitError(throwable: Throwable) {
        resultFlow.emit(Result.failure(throwable))
    }

    override fun observeItems(): Flow<List<Item>> = resultFlow.map { it.getOrThrow() }

    override suspend fun getItemById(id: String): Item? =
        resultFlow.replayCache.lastOrNull()?.getOrThrow()?.find { it.id == id }
}
