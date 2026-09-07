package com.example.authapplication.domain.item

/**
 * ホーム/検索/お気に入り/詳細画面で表示するダミーのアイテムモデル。
 *
 * [isFavorite] はアイテム自体が持つ属性ではなく「そのユーザーがお気に入り登録しているか」を表す。
 * そのため [ItemRepository] は常に `false` のまま返し、
 * [ObserveItemsUseCase] がお気に入りID集合と `combine` して埋める。
 */
data class Item(
    val id: String,
    val title: String,
    val isFavorite: Boolean = false,
)
