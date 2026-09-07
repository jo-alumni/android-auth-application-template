package com.example.authapplication.domain.favorite

import kotlinx.coroutines.flow.Flow

/**
 * お気に入り登録したアイテムIDの集合を保持するリポジトリインターフェース。
 * 実装は :data 層が提供し、値は永続化される。
 */
interface FavoriteRepository {
    fun observeFavoriteIds(): Flow<Set<String>>

    /**
     * [itemId] のお気に入り状態を反転する。
     * 「読み出して書き戻す」処理は競合すると更新が失われるため、
     * 実装側でアトミックに行う前提でトグルそのものをリポジトリの責務とする。
     */
    suspend fun toggleFavorite(itemId: String)
}
