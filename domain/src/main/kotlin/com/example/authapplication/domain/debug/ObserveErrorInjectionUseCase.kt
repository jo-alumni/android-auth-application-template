package com.example.authapplication.domain.debug

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** エラー注入が有効かどうかを監視するユースケース。 */
class ObserveErrorInjectionUseCase @Inject constructor(
    private val repository: ErrorInjectionRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeErrorInjectionEnabled()
}
