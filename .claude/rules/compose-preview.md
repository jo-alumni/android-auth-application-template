---
description: publicなComposable関数(Screen・共通UIパーツなど)には同じファイル内にprivateな@Preview関数を必ず用意し、全状態・ダークテーマ・フォントスケールを網羅する
globs:
  - "app/**/src/main/java/**/*.kt"
alwaysApply: false
---

# public な Composable 関数には必ず Preview を作成する

## 方針

### 1. 配置と命名
- `public` な `@Composable` 関数(Screen・共通UIパーツなど、他ファイルから呼び出される関数)には、
  同じファイル内に `@Preview` 系アノテーション付きの `private` な Composable 関数を必ず用意する。
- Preview関数名は `対象のComposable名 + Preview` とする(例: `LoginScreen` → `LoginScreenPreview`)。
  フォントスケール確認用に分けたものだけ `対象のComposable名 + FontScalePreview` とする。
- 引数に必要なコールバックはすべて空ラムダ(`{}`)で渡し、リストや文字列などのデータはダミーの
  サンプル値を用意する。

### 2. 中身は必ず `AppPreview` でラップする
- Preview の中身は `AppPreview`(`:app:core` の `core/ui/preview/AppPreview.kt`)で包む。
  `AppPreview` は `AuthApplicationTheme` + `Surface` を被せ、アプリ本番と同じ配色・同じ文字色で描く。
- テーマでラップしない Preview は `MaterialTheme` の既定値(紫のベースライン配色)で描かれるため、
  実機の見た目とずれる。とくに `@PreviewLightDark` はテーマが無いと意味をなさず、
  背景だけが黒くなって文字が読めない絵になる。
- `@Preview(showBackground = true)` は使わない。背景は `AppPreview` の `Surface` が
  `colorScheme.background` で敷く。`showBackground` の白背景では、ダークテーマの白い文字が
  白背景に描かれてしまう。
- `AppPreview` 自身は見た目を持たない Preview 用の下地なので、この規約の「Preview を用意する」
  対象外とする。描画結果は全画面の Preview が必ず通る経路として確認される。

### 3. 状態を持つ Composable は全状態を網羅する
- `XxxUiState` を受け取る Composable は、状態ごとに Preview 関数を並べるのではなく、
  `@PreviewParameter` + `PreviewParameterProvider` で**1つの Preview 関数に全状態を流す**。
- Provider は同じファイルに `internal class XxxUiStatePreviewParameterProvider` として置き、
  `values` に `XxxUiState` の全状態を宣言順で並べる。状態を追加したらここへ足す。
  (`private class` にしないのは、Preview ツールが Provider をリフレクションで生成するため。)
- Preview 関数を状態ごとに増やす方式は、状態を追加したときに「その状態の Preview だけ無い」
  という抜けに気付けない。1か所に並べておけば、一覧から欠けていることがすぐ分かる。

### 4. ダークテーマとフォントスケールを確認できるようにする
- 状態を網羅する Preview には `@PreviewLightDark` を付け、明るい/暗い両方で描く。
- 大フォント時の崩れは `@PreviewFontScale` で確認する。**全状態 × 全フォントスケール**は
  描画数が爆発して実用にならないので、`@PreviewFontScale` は**最も崩れやすい代表1状態**
  (文字が増える `Success` や、エラー文言が加わる `Error`)だけに付けた別関数にする。
- 画面サイズで**構造そのものが変わる** Composable(`AppNavigationScaffold`)にだけ
  `@PreviewScreenSizes` を付ける。1画面の中身は画面サイズで構造を変えないので付けない。
- マルチプレビューのアノテーションを増やしたら、detekt の
  `style.UnusedPrivateFunction.ignoreAnnotated`(`gradle/detekt/detekt.yml`)にも追加する。
  Preview 関数は誰からも呼ばれないため、登録し忘れると未使用関数として落ちる。

### 5. サンプル値は「崩れやすい値」を入れる
- 一覧のサンプルには、必ず1件は**折り返しが起きる長いタイトル**を混ぜる。
  通知のように本文を持つものは**複数行になる長文**を入れる。
- サンプル値はファイル先頭の `private val previewItems` のように1か所へまとめ、
  状態網羅の Preview とフォントスケールの Preview で同じ値を使う。
- 短いハッピーパスの値しか置かないと、Preview を用意しても「UI崩れに早く気付ける」という
  目的を果たせない。崩れるのは長文・大フォント・ダークテーマ・エラー表示の側。

## 理由
- Preview を用意することで、ViewModelやNavigationに依存せずComposable単体の見た目を
  Android Studio上ですぐ確認でき、UI崩れに早く気付ける。
- 命名・配置を統一することで、どのファイルにもPreviewが揃っているという前提で開発・レビューできる。
- 「全状態 × 明暗」と「代表1状態 × フォントスケール」に分けることで、確認したい軸を
  落とさずに、Preview の描画数を実用的な範囲に収められる。

## Before / After

```kotlin
// Bad: 成功時の1パターンだけ。テーマも被せていないので、実機の配色でもダークテーマでも確認できない
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState.Success(items = listOf(Item(id = "1", title = "アイテム1"))),
        onItemClick = {},
        onFavoriteClick = {},
        onRetryClick = {},
    )
}
```

```kotlin
// Good: 全状態を1関数に流し、AppPreviewで本番と同じテーマにし、明暗とフォントスケールを分けて見る
/** Previewで使うサンプル。3件目だけ極端に長いタイトルにして、折り返しの見え方を確認する。 */
private val previewItems = listOf(
    Item(id = "1", title = "アイテム1", isFavorite = true),
    Item(id = "2", title = "アイテム2"),
    Item(id = "3", title = "とても長いタイトルのアイテムで、1行に収まらず折り返したときの見え方を確認する"),
)

/** [HomeScreen] の全状態を1つのPreview関数で描くための供給元。状態を追加したらここへ足す。 */
internal class HomeUiStatePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState.Loading,
        HomeUiState.Empty,
        HomeUiState.Success(items = previewItems),
        HomeUiState.Error(messageResId = R.string.feature_home_error_item_load),
    )
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) uiState: HomeUiState,
) {
    AppPreview {
        HomeScreen(uiState = uiState, onItemClick = {}, onFavoriteClick = {}, onRetryClick = {})
    }
}

/** 崩れが出やすいのは文字が増える一覧表示なので、フォントスケールは [HomeUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun HomeScreenFontScalePreview() {
    AppPreview {
        HomeScreen(
            uiState = HomeUiState.Success(items = previewItems),
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}
```

## スクリーンショットテスト(Paparazzi / Roborazzi)について
現時点では**導入しない**。Preview は「人が見て気付く」ための仕組みで、ここまでの整備で
状態・明暗・フォントスケールは一通り目視できる。スクリーンショットテストはそれを
自動化する次の一手だが、参照画像(PNG)をリポジトリに抱えることになり、
Android Studio のプレビュー描画とレンダラが違うため差分の追跡コストが先に立つ。

導入を検討するのは、次のどちらかが起きたときにする。
- 目視では追い切れない数まで Preview が増えたとき
- 配色やタイポグラフィを変更する作業で、意図しない画面まで変わっていないかを機械的に確認したいとき

導入する場合は Roborazzi を選ぶ。本プロジェクトは Screen の UI テストを Robolectric で
`src/test` に置く方針([testing.md](testing.md))なので、同じ Robolectric 上で動く
Roborazzi なら `./gradlew test` に載せられ、テストの置き場所を増やさずに済む。
