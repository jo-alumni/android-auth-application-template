package com.example.authapplication.domain.notification

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 通知一覧を監視するユースケース。 */
class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<List<Notification>> = repository.observeNotifications()
}
