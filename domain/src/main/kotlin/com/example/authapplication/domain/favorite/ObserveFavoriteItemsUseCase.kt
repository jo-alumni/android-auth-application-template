package com.example.authapplication.domain.favorite

import com.example.authapplication.domain.item.Item
import com.example.authapplication.domain.item.ObserveItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** お気に入り登録済みのアイテムだけに絞り込んで監視するユースケース。 */
class ObserveFavoriteItemsUseCase @Inject constructor(
    private val observeItemsUseCase: ObserveItemsUseCase,
) {
    operator fun invoke(): Flow<List<Item>> =
        observeItemsUseCase().map { items -> items.filter { it.isFavorite } }
}
