package com.example.authappliation.domain.auth

import javax.inject.Inject

/** 認証状態を更新するユースケース。 */
class SetAuthenticatedUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(value: Boolean) = repository.setAuthenticated(value)
}
