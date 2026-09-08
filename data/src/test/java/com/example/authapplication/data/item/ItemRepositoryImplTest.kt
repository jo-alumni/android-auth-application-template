package com.example.authapplication.data.item

import com.example.authapplication.domain.debug.FakeErrorInjectionRepository
import com.example.authapplication.domain.error.AppDataException
import com.example.authapplication.domain.error.AppError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemRepositoryImplTest {

    private val errorInjectionRepository = FakeErrorInjectionRepository()
    private val repository = ItemRepositoryImpl(errorInjectionRepository)

    @Test
    fun `observeItems emits mock items while error injection is disabled`() = runTest {
        assertTrue(repository.observeItems().first().isNotEmpty())
    }

    @Test
    fun `observeItems throws while error injection is enabled`() = runTest {
        errorInjectionRepository.setErrorInjectionEnabled(true)

        val throwable = runCatching { repository.observeItems().toList() }.exceptionOrNull()

        assertTrue(throwable is AppDataException)
        assertEquals(AppError.ITEM_LOAD, (throwable as AppDataException).error)
    }

    /**
     * 失敗判定は購読開始時にだけ行うため、スイッチを戻して購読し直せば成功する
     * (＝画面側の「再読み込み」で回復できる)。
     */
    @Test
    fun `observeItems succeeds again after error injection is disabled and resubscribed`() = runTest {
        errorInjectionRepository.setErrorInjectionEnabled(true)
        assertTrue(runCatching { repository.observeItems().toList() }.isFailure)

        errorInjectionRepository.setErrorInjectionEnabled(false)

        assertEquals("1", repository.observeItems().first().first().id)
    }

    @Test
    fun `getItemById throws while error injection is enabled`() = runTest {
        errorInjectionRepository.setErrorInjectionEnabled(true)

        val throwable = runCatching { repository.getItemById("1") }.exceptionOrNull()

        assertTrue(throwable is AppDataException)
        assertEquals(AppError.ITEM_LOAD, (throwable as AppDataException).error)
    }
}
