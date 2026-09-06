package com.example.authapplication.domain.notification

import kotlinx.coroutines.flow.Flow

/**
 * 通知一覧の取得を行うリポジトリインターフェース。
 * 実装は :data 層が提供する。
 */
interface NotificationRepository {
    fun observeNotifications(): Flow<List<Notification>>
}
