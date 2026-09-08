package com.example.authapplication.domain.error

/**
 * データの取得・更新に失敗したことを表す例外。
 *
 * :data 層の実装がこの例外を投げ、ViewModelが `Flow.catch` や `runCatching` で捕捉して
 * `XxxUiState.Error` やSnackbarイベントへ変換する。
 * 表示する文言そのものではなく [AppError]（失敗の種別）を持つのがポイントで、
 * 文言の解決は文字列リソースを持つUI層に寄せている。
 */
class AppDataException(val error: AppError) : Exception(error.name)

/**
 * 例外を [AppError] へ変換する。
 *
 * 想定外の例外もここで必ず [AppError.UNEXPECTED] にフォールバックさせることで、
 * ViewModel側は「例外の種類」ではなく「失敗の種別」だけを扱えばよくなる。
 */
fun Throwable.toAppError(): AppError = (this as? AppDataException)?.error ?: AppError.UNEXPECTED
