package com.example.authapplication.data.auth

import androidx.datastore.core.DataStore
import com.example.authapplication.data.auth.proto.AuthPrefs
import com.example.authapplication.data.auth.proto.copy
import com.example.authapplication.domain.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tink AEAD暗号化されたProto DataStoreで認証トークンを永続化する実装。 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<AuthPrefs>,
) : AuthRepository {

    override fun observeAuthToken(): Flow<String?> =
        dataStore.data.map { prefs -> prefs.authToken.ifEmpty { null } }

    override suspend fun setAuthToken(token: String) {
        require(token.isNotBlank()) { "token must not be blank" }
        dataStore.updateData { prefs -> prefs.copy { authToken = token } }
    }

    override suspend fun clearAuthToken() {
        dataStore.updateData { prefs -> prefs.copy { authToken = "" } }
    }
}
