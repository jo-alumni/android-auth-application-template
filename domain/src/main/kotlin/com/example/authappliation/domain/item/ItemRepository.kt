package com.example.authappliation.domain.item

import kotlinx.coroutines.flow.Flow

/**
 * アイテム一覧の取得を行うリポジトリインターフェース。
 * 実装は :data 層が提供する。
 */
interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun getItemById(id: String): Item?
}
