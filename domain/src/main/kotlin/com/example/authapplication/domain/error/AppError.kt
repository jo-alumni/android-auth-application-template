package com.example.authapplication.domain.error

/**
 * アプリ内で扱う失敗の種別。
 *
 * :domain はAndroidに依存しないため文字列リソースを参照できない。
 * ここでは「何に失敗したか」だけを表し、実際に表示する文言への変換は
 * 文字列リソースを持つUI層（featureモジュールのViewModel）が担当する。
 */
enum class AppError {
    /** アイテムの取得に失敗した。 */
    ITEM_LOAD,

    /** 通知の取得に失敗した。 */
    NOTIFICATION_LOAD,

    /** お気に入りの更新に失敗した。 */
    FAVORITE_TOGGLE,

    /** リポジトリ層が想定していない例外。各画面共通の文言にフォールバックする。 */
    UNEXPECTED,
}
