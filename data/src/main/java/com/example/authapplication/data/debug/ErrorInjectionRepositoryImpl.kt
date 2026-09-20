package com.example.authapplication.data.debug

import com.example.authapplication.domain.debug.ErrorInjectionRepository
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * エラー注入スイッチをメモリ上に保持する実装。
 * 動作確認用の一時的な設定なので永続化せず、アプリを再起動すれば必ず無効に戻る。
 */
@Singleton
class ErrorInjectionRepositoryImpl @Inject constructor() : ErrorInjectionRepository {

    private val enabled = MutableStateFlow(false)

    override fun observeErrorInjectionEnabled(): Flow<Boolean> = enabled

    override suspend fun setErrorInjectionEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}

/**
 * エラー注入が有効なら [AppDataException] を投げる。
 * 各リポジトリ実装が「失敗し得る処理」の先頭で呼び出し、[error] にその処理固有の失敗の種別を渡す。
 */
internal suspend fun ErrorInjectionRepository.throwIfErrorInjected(error: AppError) {
    if (observeErrorInjectionEnabled().first()) {
        throw AppDataException(error)
    }
}
