package com.example.authapplication.data.favorite

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FavoriteRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** [PreferenceDataStoreFactory] は拡張子 `.preferences_pb` のファイルしか受け付けない。 */
    private fun prefsFile() = File(temporaryFolder.root, "favorite_prefs.preferences_pb")

    /**
     * DataStoreは同一ファイルに対して同時に複数インスタンスを生成できないため、
     * ブロックを抜けるときにスコープを閉じて次のインスタンスを生成できるようにする。
     */
    private fun <T> withRepository(file: File, block: suspend (FavoriteRepositoryImpl) -> T): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            val repository = FavoriteRepositoryImpl(
                dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
            )
            runBlocking { block(repository) }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `observeFavoriteIds is empty by default`() {
        withRepository(prefsFile()) { repository ->
            assertEquals(emptySet<String>(), repository.observeFavoriteIds().first())
        }
    }

    @Test
    fun `toggleFavorite adds and removes an id`() {
        withRepository(prefsFile()) { repository ->
            repository.toggleFavorite("1")
            assertEquals(setOf("1"), repository.observeFavoriteIds().first())

            repository.toggleFavorite("2")
            assertEquals(setOf("1", "2"), repository.observeFavoriteIds().first())

            repository.toggleFavorite("1")
            assertEquals(setOf("2"), repository.observeFavoriteIds().first())
        }
    }

    /** アプリ再起動後もお気に入りが保持されることを、同じファイルを読み直して確認する。 */
    @Test
    fun `favorite ids are persisted across DataStore instances`() {
        val file = prefsFile()
        withRepository(file) { repository -> repository.toggleFavorite("1") }

        withRepository(file) { repository ->
            assertEquals(setOf("1"), repository.observeFavoriteIds().first())
        }
    }
}
