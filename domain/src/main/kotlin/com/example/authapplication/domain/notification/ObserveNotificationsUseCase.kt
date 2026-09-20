package com.example.authapplication.domain.notification

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 通知一覧を監視するユースケース。 */
class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<List<Notification>> = repository.observeNotifications()
}
