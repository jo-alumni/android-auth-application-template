package com.example.authapplication.domain.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** テスト用の [ErrorInjectionRepository] フェイク実装。メモリ上でスイッチの状態を保持する。 */
class FakeErrorInjectionRepository(
    initialEnabled: Boolean = false,
) : ErrorInjectionRepository {

    private val enabled = MutableStateFlow(initialEnabled)

    override fun observeErrorInjectionEnabled(): Flow<Boolean> = enabled

    override suspend fun setErrorInjectionEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}
