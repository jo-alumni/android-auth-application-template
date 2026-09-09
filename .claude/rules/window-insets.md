---
description: WindowInsetsは:appが上端のみを解決してconsumeし、下端とIMEは各Screenが自分で解決する
globs:
  - "app/src/main/java/**/*.kt"
  - "app/feature/**/*.kt"
alwaysApply: false
---

# WindowInsets は「それを隠す UI を描いた側」が解決する

## 方針
- `:app`(`AppNavigationScaffold`)は、自分が描く `AppTopBar` 分の**上端 insets だけ**を解決する。
  `Modifier.padding(topPadding)` に加えて `Modifier.consumeWindowInsets(topPadding)` を必ず併用し、
  適用済みの余白を下流の `WindowInsets` から差し引く。
- 画面幅がMedium以上のときに `:app` が描く `AppNavigationRail` / `AppNavigationDrawerSheet` も同じ扱いで、
  **左端の insets は描いた側**(`AppNavigationScaffold`)が解決する。レール/ドロワー自身の既定の
  `windowInsets` が余白を確保するので、本文側の `Scaffold` には
  `Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))` を付けて
  覆われた分を下流から差し引く。
- **下端(ナビゲーションバー)と IME は各 Screen が自分で解決する**。`:app` からは解決しないし、
  `PaddingValues` を Navigation 層経由で feature に渡さない。
  - スクロールする画面: `LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues())`
    でレイアウト領域はナビゲーションバーの裏まで広げ、スクロール余白だけを確保する。
  - スクロールしない画面: `Modifier.navigationBarsPadding()` でレイアウト領域自体を手前で止める。
  - 入力欄を持つ画面: `Modifier.navigationBarsPadding().imePadding()` の順で重ねる
    (先に消費した分が後段から差し引かれる)。
- 全画面ダイアログ(`dialog<...>` で登録する通知画面)は `:app` の `Scaffold` とは**別ウィンドウ**
  なので、上端・下端とも自前の `Scaffold` で解決する。
- `LazyColumn` の `contentPadding` に、ボトムバーの表示状態のような**毎フレーム変わる値**を
  渡さない。`contentPadding` は測定入力なので remeasure を誘発してスクロールがカクつく
  (詳細は [docs/window-insets.md](../../docs/window-insets.md))。

## 理由
- insets の解決場所が「バーを描いた側」に固定されるので、どの画面を読んでも余白の出どころが
  1箇所に決まり、二重適用や適用漏れが起きない。
- `:app` の `Scaffold` 事情(ボトムバーがある/スクロールで隠れる)を feature モジュールの
  公開 API に漏らさずに済む。feature は自分の画面だけを見て insets を決められる。
- ボトムバーがスクロールで隠れる本アプリでは、`:app` が下端の余白を一律に確保すると
  バーが隠れたときに空白が残る。下端を画面側に委ねることで、リストは edge-to-edge のまま
  末尾までスクロールできる。

## Before / After

```kotlin
// Bad: :app のレイアウト都合(PaddingValues)を Navigation 経由で feature に配る
AppNavHost(
    listContentPadding = PaddingValues(bottom = navigationBarPadding),
    // ...
)

fun NavGraphBuilder.homeScreen(
    navigateDetail: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(), // feature の API が :app の都合に汚染される
) { /* ... */ }

@Composable
fun HomeScreen(uiState: HomeUiState, contentPadding: PaddingValues = PaddingValues()) {
    LazyColumn(contentPadding = contentPadding) { /* ... */ }
}
```

```kotlin
// Good: :app は上端だけを解決して consume し、下端は画面が自分で解決する
// AppNavigationScaffold.kt
val topPadding = PaddingValues(top = innerPadding.calculateTopPadding())
AppNavHost(
    modifier = Modifier
        .padding(topPadding)
        .consumeWindowInsets(topPadding),
    // ...
)

// HomeNavigation.kt — insets を通さない
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) { /* ... */ }

// HomeScreen.kt — 自分で WindowInsets を読む
@Composable
fun HomeScreen(uiState: HomeUiState) {
    LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues()) { /* ... */ }
}
```
