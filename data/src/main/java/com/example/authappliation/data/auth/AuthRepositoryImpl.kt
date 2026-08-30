package com.example.authappliation.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.authappliation.domain.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")

/** DataStore Preferencesで認証状態を永続化する実装。 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AuthRepository {

    override fun observeIsAuthenticated(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_AUTHENTICATED] ?: false }

    override suspend fun setAuthenticated(value: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_IS_AUTHENTICATED] = value }
    }
}
