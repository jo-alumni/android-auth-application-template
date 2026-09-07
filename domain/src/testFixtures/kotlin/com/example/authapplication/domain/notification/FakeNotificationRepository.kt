package com.example.authapplication.domain.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

/**
 * テスト用の [NotificationRepository] フェイク実装。
 * [FakeItemRepository][com.example.authapplication.domain.item.FakeItemRepository] と同様、
 * [emitNotifications] が呼ばれるまで何も発行せず、[emitError] で失敗も再現できる。
 */
class FakeNotificationRepository : NotificationRepository {

    private val resultFlow = MutableSharedFlow<Result<List<Notification>>>(replay = 1)

    suspend fun emitNotifications(notifications: List<Notification>) {
        resultFlow.emit(Result.success(notifications))
    }

    /** 以降の [observeNotifications] が [throwable] を投げるようにする。 */
    suspend fun emitError(throwable: Throwable) {
        resultFlow.emit(Result.failure(throwable))
    }

    override fun observeNotifications(): Flow<List<Notification>> = resultFlow.map { it.getOrThrow() }
}
