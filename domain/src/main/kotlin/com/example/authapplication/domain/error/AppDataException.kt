package com.example.authapplication.domain.error

/**
 * データの取得・更新に失敗したことを表す例外。
 *
 * :data 層の実装がこの例外を投げ、ViewModelが `Flow.catch` や `runCatching` で捕捉して
 * `XxxUiState.Error` やSnackbarイベントへ変換する。
 * [userMessage] はそのまま画面に表示できる日本語のメッセージとする。
 */
class AppDataException(val userMessage: String) : Exception(userMessage)

/**
 * 例外をユーザー向けのメッセージへ変換する。
 *
 * 想定外の例外もここで必ず何らかの文言にフォールバックさせることで、
 * UI側は「例外の種類」ではなく「表示する文字列」だけを扱えばよくなる。
 */
fun Throwable.toUserMessage(): String =
    (this as? AppDataException)?.userMessage ?: UNEXPECTED_ERROR_MESSAGE

private const val UNEXPECTED_ERROR_MESSAGE = "予期しないエラーが発生しました"
