package com.example.authapplication.domain.auth

import javax.inject.Inject

/** 認証トークンを破棄し、未ログイン状態にするユースケース。 */
class ClearAuthTokenUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.clearAuthToken()
}
