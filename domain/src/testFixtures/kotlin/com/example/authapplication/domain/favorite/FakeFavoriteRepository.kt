package com.example.authapplication.domain.favorite

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * テスト用の [FavoriteRepository] フェイク実装。メモリ上でお気に入りID集合を保持する。
 * [initialFavoriteIds] で「すでにお気に入り登録済み」の状態から始めることもできる。
 */
class FakeFavoriteRepository(
    initialFavoriteIds: Set<String> = emptySet(),
) : FavoriteRepository {

    private val favoriteIds = MutableStateFlow(initialFavoriteIds)

    /** 設定すると [toggleFavorite] が常にこの例外を投げる。更新失敗時の挙動をテストするために使う。 */
    var toggleError: Throwable? = null

    override fun observeFavoriteIds(): Flow<Set<String>> = favoriteIds

    override suspend fun toggleFavorite(itemId: String) {
        toggleError?.let { throw it }
        favoriteIds.update { ids -> if (itemId in ids) ids - itemId else ids + itemId }
    }
}
