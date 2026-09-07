package com.example.authapplication.data.favorite

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.authapplication.domain.favorite.FavoriteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Preferences DataStoreにお気に入りIDの集合を永続化する実装。
 * 認証情報と違い秘匿性が無いため、暗号化せずPreferences DataStoreをそのまま使う。
 */
@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FavoriteRepository {

    override fun observeFavoriteIds(): Flow<Set<String>> =
        dataStore.data.map { preferences -> preferences[KEY_FAVORITE_ITEM_IDS].orEmpty() }

    /** [DataStore.edit] は読み出しから書き込みまでをアトミックに行うため、トグルの更新が失われない。 */
    override suspend fun toggleFavorite(itemId: String) {
        dataStore.edit { preferences ->
            val favoriteIds = preferences[KEY_FAVORITE_ITEM_IDS].orEmpty()
            preferences[KEY_FAVORITE_ITEM_IDS] = if (itemId in favoriteIds) {
                favoriteIds - itemId
            } else {
                favoriteIds + itemId
            }
        }
    }

    private companion object {
        val KEY_FAVORITE_ITEM_IDS = stringSetPreferencesKey("favorite_item_ids")
    }
}
