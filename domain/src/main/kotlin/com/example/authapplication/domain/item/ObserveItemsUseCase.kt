package com.example.authapplication.domain.item

import com.example.authapplication.domain.favorite.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 全アイテムと、お気に入りID集合を `combine` して
 * [Item.isFavorite] を埋めたアイテム一覧を監視するユースケース。
 *
 * どちらか一方のFlowが新しい値を流すたびに結合結果が再計算されるため、
 * ある画面でお気に入りをトグルすると、同じFlowを購読している他の画面にも即座に反映される。
 */
class ObserveItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    operator fun invoke(): Flow<List<Item>> = combine(
        itemRepository.observeItems(),
        favoriteRepository.observeFavoriteIds(),
    ) { items, favoriteIds ->
        items.map { item -> item.copy(isFavorite = item.id in favoriteIds) }
    }
}
