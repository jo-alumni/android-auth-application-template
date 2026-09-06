package com.example.authapplication.data.notification

import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.domain.notification.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** メモリ上のモックデータを返す実装（学習用の最小構成）。 */
@Singleton
class NotificationRepositoryImpl @Inject constructor() : NotificationRepository {

    private val mockNotifications = (1..3).map { index ->
        Notification(
            id = index.toString(),
            title = "お知らせ $index",
            message = "サンプル通知メッセージです。",
        )
    }

    override fun observeNotifications(): Flow<List<Notification>> = flowOf(mockNotifications)
}
