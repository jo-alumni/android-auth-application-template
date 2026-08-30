---
description: publicなComposable関数(Screen・共通UIパーツなど)には同じファイル内にprivateな@Preview関数を必ず用意する
globs:
  - "app/feature/*/src/main/java/**/*.kt"
alwaysApply: false
---

# public な Composable 関数には必ず Preview を作成する

## 方針
- `public` な `@Composable` 関数(Screen・共通UIパーツなど、他ファイルから呼び出される関数)には、
  同じファイル内に `@Preview` 付きの `private` な Composable 関数を必ず用意する。
- Preview関数名は `対象のComposable名 + Preview` とする(例: `LoginScreen` → `LoginScreenPreview`)。
- 引数に必要なコールバックはすべて空ラムダ(`{}`)で渡し、リストや文字列などのデータはダミーの
  サンプル値を用意する。

## 理由
- Preview を用意することで、ViewModelやNavigationに依存せずComposable単体の見た目を
  Android Studio上ですぐ確認でき、UI崩れに早く気付ける。
- 命名・配置を統一することで、どのファイルにもPreviewが揃っているという前提で開発・レビューできる。

## Before / After

```kotlin
// Bad: Previewが無く、実機/エミュレータを起動しないと見た目を確認できない
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("ログイン画面")
        Button(onClick = onLoginClick) { Text("ログイン") }
    }
}
```

```kotlin
// Good: 同じファイルにPreviewを用意する
@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("ログイン画面")
        Button(onClick = onLoginClick) { Text("ログイン") }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onLoginClick = {})
}
```
