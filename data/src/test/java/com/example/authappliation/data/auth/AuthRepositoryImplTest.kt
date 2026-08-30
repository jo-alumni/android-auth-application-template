package com.example.authappliation.data.auth

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.example.authappliation.data.auth.proto.AuthPrefs
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun createAead(): Aead {
        AeadConfig.register()
        return KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM")).getPrimitive(Aead::class.java)
    }

    private fun createRepository(aead: Aead, file: File) = AuthRepositoryImpl(
        dataStore = DataStoreFactory.create(
            serializer = AuthPrefsSerializer(aead),
            corruptionHandler = ReplaceFileCorruptionHandler { AuthPrefs.getDefaultInstance() },
            produceFile = { file },
        ),
    )

    @Test
    fun `setAuthToken persists token and observeAuthToken emits it`() = runBlocking {
        val file = temporaryFolder.newFile("auth_prefs_test.pb")
        val repository = createRepository(createAead(), file)

        repository.setAuthToken("dummy_token")

        assertEquals("dummy_token", repository.observeAuthToken().first())
    }

    @Test
    fun `clearAuthToken resets to unauthenticated`() = runBlocking {
        val file = temporaryFolder.newFile("auth_prefs_test.pb")
        val repository = createRepository(createAead(), file)

        repository.setAuthToken("dummy_token")
        repository.clearAuthToken()

        assertNull(repository.observeAuthToken().first())
    }

    @Test
    fun `persisted file is encrypted, not plain protobuf`() = runBlocking {
        val file = temporaryFolder.newFile("auth_prefs_test.pb")
        val repository = createRepository(createAead(), file)

        repository.setAuthToken("dummy_token")

        val plainBytes = AuthPrefs.newBuilder().setAuthToken("dummy_token").build().toByteArray()
        val diskBytes = file.readBytes()
        assertTrue(!diskBytes.contentEquals(plainBytes))
    }

    @Test
    fun `corrupted file falls back to unauthenticated instead of crashing`() = runBlocking {
        val file = temporaryFolder.newFile("auth_prefs_test.pb")
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val repository = createRepository(createAead(), file)

        assertNull(repository.observeAuthToken().first())
    }
}
