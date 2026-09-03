package com.example.authapplication

import com.example.authapplication.domain.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * テスト用の [AuthRepository] フェイク実装。DataStoreを使わずメモリ上でトークンを保持する。
 * 実際のDataStoreが初回読み込み完了まで値を発行しないのと同様、
 * [setAuthToken]/[clearAuthToken] が呼ばれるまで [observeAuthToken] は何も発行しない。
 */
class FakeAuthRepository : AuthRepository {

    private val tokenFlow = MutableSharedFlow<String?>(replay = 1)

    override fun observeAuthToken(): Flow<String?> = tokenFlow

    override suspend fun setAuthToken(token: String) {
        tokenFlow.emit(token)
    }

    override suspend fun clearAuthToken() {
        tokenFlow.emit(null)
    }
}
