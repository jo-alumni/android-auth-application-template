package com.example.authapplication.domain.debug

import kotlinx.coroutines.flow.Flow

/**
 * 失敗系の動作を確認するために、リポジトリ層の処理をわざと失敗させるかどうかを保持するリポジトリ。
 *
 * 学習用テンプレートには実際に失敗する通信処理が無いため、
 * 「エラー表示 → リトライ → 成功」の流れを手元で再現できるようにデバッグ用のスイッチを用意する。
 * 実行中の動作確認が目的なので、値は永続化せずプロセス内にのみ保持する。
 */
interface ErrorInjectionRepository {
    fun observeErrorInjectionEnabled(): Flow<Boolean>

    suspend fun setErrorInjectionEnabled(enabled: Boolean)
}
