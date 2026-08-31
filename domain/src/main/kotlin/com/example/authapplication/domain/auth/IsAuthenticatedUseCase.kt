package com.example.authapplication.domain.auth

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 認証トークンの有無から現在の認証状態を監視するユースケース。 */
class IsAuthenticatedUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeAuthToken().map { token -> token != null }
}
