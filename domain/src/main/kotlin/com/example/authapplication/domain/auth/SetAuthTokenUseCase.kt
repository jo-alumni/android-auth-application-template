package com.example.authapplication.domain.auth

import javax.inject.Inject

/** 認証トークンを保存し、ログイン済み状態にするユースケース。 */
class SetAuthTokenUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(token: String) = repository.setAuthToken(token)
}
