package com.example.authapplication.data.item

import com.example.authapplication.data.debug.throwIfErrorInjected
import com.example.authapplication.domain.debug.ErrorInjectionRepository
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ItemRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** メモリ上のモックデータを返す実装（学習用の最小構成）。 */
@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val errorInjectionRepository: ErrorInjectionRepository,
) : ItemRepository {

    private val mockItems = (1..30).map { index -> Item(id = index.toString(), title = "Item $index") }

    /**
     * エラー注入が有効なら例外を投げてFlowを異常終了させる。
     *
     * 判定を `flow { }` の中（＝購読を開始した時点）で行うのがポイントで、
     * 一覧を表示したままスイッチを切り替えても画面は勝手にエラーへ変わらない。
     * 購読をやり直したとき（＝リトライしたとき）に初めて結果が変わるため、
     * 「エラー表示 → スイッチを戻す → 再読み込み → 成功」という流れを手元で再現できる。
     */
    override fun observeItems(): Flow<List<Item>> = flow {
        errorInjectionRepository.throwIfErrorInjected(AppError.ITEM_LOAD)
        emit(mockItems)
    }

    override suspend fun getItemById(id: String): Item? {
        errorInjectionRepository.throwIfErrorInjected(AppError.ITEM_LOAD)
        return mockItems.find { it.id == id }
    }
}
