package com.example.authapplication.domain.debug

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** エラー注入が有効かどうかを監視するユースケース。 */
class ObserveErrorInjectionUseCase @Inject constructor(
    private val repository: ErrorInjectionRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeErrorInjectionEnabled()
}
