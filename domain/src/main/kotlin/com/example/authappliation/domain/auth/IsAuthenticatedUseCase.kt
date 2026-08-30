package com.example.authappliation.domain.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** 現在の認証状態を監視するユースケース。 */
class IsAuthenticatedUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeIsAuthenticated()
}
