package com.example.authapplication.domain.favorite

import javax.inject.Inject

/** アイテムのお気に入り状態を反転するユースケース。 */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoriteRepository,
) {
    suspend operator fun invoke(itemId: String) = repository.toggleFavorite(itemId)
}
