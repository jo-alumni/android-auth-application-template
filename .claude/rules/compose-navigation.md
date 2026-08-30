---
description: NavGraphBuilder拡張関数(xxxScreen)の画面遷移コールバック引数は「何をしたとき(on〜)」ではなく「何をするか(navigate〜)」で命名する
globs:
  - "app/feature/*/src/main/java/**/*Navigation.kt"
alwaysApply: false
---

# Compose Navigation: 画面コールバックの命名規則

## 命名パターン
- `navigate` + 遷移操作・遷移先を表す語で命名する
  - 例: `navigateDetail`(詳細画面へ遷移する)、`navigateHome`(ホーム画面へ遷移する)、`navigateBack`(前の画面へ戻る)
- `onItemClick` / `onXxxClick` / `onXxxSuccess` のような「画面内で発生したイベント」を表す命名は、
  Navigationファイルのコールバックとしては使わない。
  - そのイベント名は呼び出し先のScreen Composable(例: `HomeScreen(onItemClick = ...)`)側の引数名としては引き続き妥当。
    Navigationファイル側で受け取った時点で「遷移」という意味に変換して命名し直す。

## Before / After

```kotlin
// Bad: 「アイテムをクリックしたとき」という発生イベント視点の命名
fun NavGraphBuilder.homeScreen(onItemClick: (String) -> Unit) {
    composable<AppRoute.Home> {
        HomeScreen(items = items, onItemClick = onItemClick)
    }
}

// Good: 「詳細画面へ遷移する」という操作視点の命名
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<AppRoute.Home> {
        HomeScreen(items = items, onItemClick = navigateDetail)
    }
}
```

```kotlin
// Bad
fun NavGraphBuilder.detailScreen(onBackClick: () -> Unit) { ... }
fun NavGraphBuilder.authScreen(onLoginSuccess: () -> Unit) { ... }

// Good
fun NavGraphBuilder.detailScreen(navigateBack: () -> Unit) { ... }
fun NavGraphBuilder.authScreen(navigateHome: () -> Unit) { ... } // 実際の遷移先(Home)を反映
```
