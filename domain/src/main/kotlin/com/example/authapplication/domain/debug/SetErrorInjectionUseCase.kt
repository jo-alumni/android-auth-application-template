package com.example.authapplication.domain.debug

import javax.inject.Inject

/** エラー注入の有効/無効を切り替えるユースケース。 */
class SetErrorInjectionUseCase @Inject constructor(
    private val repository: ErrorInjectionRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setErrorInjectionEnabled(enabled)
}
