package com.example.authapplication.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * 認証トークンの取得・更新を行うリポジトリインターフェース。
 * トークンが存在しない(null)場合は未ログイン状態を表す。
 * 実装は :data 層が提供する。
 */
interface AuthRepository {
    fun observeAuthToken(): Flow<String?>
    suspend fun setAuthToken(token: String)
    suspend fun clearAuthToken()
}
