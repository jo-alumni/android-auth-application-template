package com.example.authapplication.domain.item

import javax.inject.Inject

/** IDを指定して1件のアイテムを取得するユースケース。該当するアイテムが無ければ null を返す。 */
class GetItemUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(id: String): Item? = repository.getItemById(id)
}
