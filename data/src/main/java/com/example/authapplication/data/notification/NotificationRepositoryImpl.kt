package com.example.authapplication.data.notification

import com.example.authapplication.data.debug.throwIfErrorInjected
import com.example.authapplication.domain.debug.ErrorInjectionRepository
import com.example.authapplication.domain.error.AppError
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** メモリ上のモックデータを返す実装（学習用の最小構成）。 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val errorInjectionRepository: ErrorInjectionRepository,
) : NotificationRepository {

    private val mockNotifications = (1..3).map { index ->
        Notification(
            id = index.toString(),
            title = "お知らせ $index",
            message = "サンプル通知メッセージです。",
        )
    }

    /** [ItemRepositoryImpl][com.example.authapplication.data.item.ItemRepositoryImpl] と同じく購読開始時にだけ失敗判定を行う。 */
    override fun observeNotifications(): Flow<List<Notification>> = flow {
        errorInjectionRepository.throwIfErrorInjected(AppError.NOTIFICATION_LOAD)
        emit(mockNotifications)
    }
}
