package com.example.authapplication.data.favorite

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.authapplication.domain.debug.FakeErrorInjectionRepository
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FavoriteRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** [PreferenceDataStoreFactory] は拡張子 `.preferences_pb` のファイルしか受け付けない。 */
    private fun prefsFile() = File(temporaryFolder.root, "favorite_prefs.preferences_pb")

    /**
     * DataStoreは同一ファイルに対して同時に複数インスタンスを生成できないため、
     * ブロックを抜けるときにスコープを閉じて次のインスタンスを生成できるようにする。
     */
    private fun <T> withRepository(
        file: File,
        errorInjectionEnabled: Boolean = false,
        block: suspend (FavoriteRepositoryImpl) -> T,
    ): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            val repository = FavoriteRepositoryImpl(
                dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
                errorInjectionRepository = FakeErrorInjectionRepository(errorInjectionEnabled),
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

    /** デバッグメニューのエラー注入が有効なとき、更新だけが失敗し読み出しは成功する。 */
    @Test
    fun `toggleFavorite throws while error injection is enabled`() {
        withRepository(prefsFile(), errorInjectionEnabled = true) { repository ->
            val throwable = runCatching { repository.toggleFavorite("1") }.exceptionOrNull()

            assertTrue(throwable is AppDataException)
            assertEquals(AppError.FAVORITE_TOGGLE, (throwable as AppDataException).error)
            assertEquals(emptySet<String>(), repository.observeFavoriteIds().first())
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
