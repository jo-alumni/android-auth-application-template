package com.example.authappliation.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * 認証状態の取得・更新を行うリポジトリインターフェース。
 * 実装は :data 層が提供する。
 */
interface AuthRepository {
    fun observeIsAuthenticated(): Flow<Boolean>
    suspend fun setAuthenticated(value: Boolean)
}
