package com.example.authapplication.domain.notification

/** ホーム/検索/お気に入りから遷移する通知画面で表示するダミーの通知モデル。 */
data class Notification(
    val id: String,
    val title: String,
    val message: String,
)
