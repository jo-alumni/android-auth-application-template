---
description: 画面のRouteはそれを持つfeatureモジュールが定義し、共通モジュールには置かない。遷移先の決定は:appに閉じる
globs:
  - "app/feature/*/src/main/java/**/*Navigation.kt"
  - "app/src/main/java/**/navigation/*.kt"
  - "app/core/src/main/java/**/navigation/*.kt"
alwaysApply: false
---

# Routeはfeatureモジュールが所有し、遷移先の決定は `:app` に閉じる

## 方針

### 1. Routeは画面を持つfeatureモジュールに置く
- 画面のRoute（Navigation Composeの型安全ナビゲーションで使う宛先の型）は、
  その画面を持つfeatureモジュールの `XxxRoute.kt` に定義し、同じモジュールの
  `XxxNavigation.kt` が `composable<XxxRoute>` で登録する。
  `:app:core` のような共通モジュールに全画面のRouteを集約しない。
  ファイルを分けるのは、detektの `MatchingDeclarationName` に合わせるため
  （トップレベルの型がひとつだけのファイルは型名と同じファイル名にする）。
- 引数を持たない画面は `@Serializable data object`、引数を持つ画面は
  `@Serializable data class` にする（`DetailRoute(val itemId: String)`）。
- 命名は「画面名 + `Route`」（`HomeRoute` / `DetailRoute`）。
- ネストしたグラフのRoute（`AuthGraphRoute` / `MainGraphRoute`）と、
  タブ項目の定義（`TopLevelDestination`）は `:app` に置く。
  どちらも「アプリが画面をどう束ねるか」の判断で、featureの持ち物ではない。
- `:app:core` の `navigation` パッケージに置いてよいのは、画面に依存しないもの
  （遷移アニメーションの `AppNavTransitions` など）だけにする。

### 2. 遷移先の決定は `:app` に閉じる
- featureの `NavGraphBuilder` 拡張関数は、他画面のRouteを組み立てない。
  遷移は `navigateDetail: (String) -> Unit` のようなコールバックで `:app` へ委ねる
  （命名は [compose-navigation.md](compose-navigation.md)）。
  feature間の直接依存を防いでいるのはこの構造なので、Routeの配置以上に崩さない。
- そのためfeatureは `navController` を受け取らない。`navController.navigate(XxxRoute)`
  を書いてよいのは `:app` の `AppNavHost` と `AppState` だけ。
- Screen ComposableにはRoute型を渡さない。渡すのは `itemId: String` のような素の値にする。
  ViewModelが遷移引数を読む場合だけ、自分の画面のRouteを
  `savedStateHandle.toRoute<DetailRoute>()` で参照してよい。

### 3. 画面固有のNavigation設定もfeatureが持つ
- ディープリンクのベースパス（`DETAIL_DEEP_LINK_BASE_PATH`）、遷移アニメーションの指定、
  `dialog<...>` で登録するかどうかは、Route定義と同じファイルに置く。
  「その画面へ入る経路」の定義が1モジュールにまとまる状態を保つ。
- タブのラベルなど画面に紐づく文言も、その画面を持つfeatureの `strings.xml` に置く
  （[string-resources.md](string-resources.md)）。

### 4. 画面を追加する手順
1. featureモジュールに `@Serializable` なRouteを持つ `XxxRoute.kt` と、
   `NavGraphBuilder.xxxScreen(navigateYyy = ...)` を持つ `XxxNavigation.kt` を書く。
2. `:app` の `AppNavHost` から呼び、遷移の実装（`navController.navigate(...)`）を書く。
3. タブとして表示する画面なら `:app` の `TopLevelDestination` に追加し、
   ラベルの文字列はそのfeatureの `strings.xml` に置く。

## 理由
- 共通モジュールに集約すると、画面を追加するたびに共通モジュールの変更が必要になり、
  そこに依存する全モジュールが再コンパイル対象になる。並行開発では同じファイルに
  変更が集中してコンフリクトしやすい。
- featureが自分のRouteと引数を所有していれば、そのモジュールだけで画面の入口が完結し、
  切り出しや再利用ができる。
- 遷移先の決定を `:app` に閉じることで、Routeを分散させてもfeature同士が
  互いのRoute型を知る必要がなくなり、feature間の直接依存が発生しない。
- 集約方式との比較、この方針を選んだ経緯、集約が向く場合、
  feature間の直接遷移が必要になったときの選択肢（`navigateToXxx` 拡張 / `:api` モジュール）は
  [docs/navigation-routes.md](../../docs/navigation-routes.md) にまとめてある。

## Before / After

```kotlin
// Bad: 共通モジュールが全画面のRouteを持ち、featureが増えるたびに共通モジュールを触る
// :app:core/navigation/AppRoute.kt
sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class Detail(val itemId: String) : AppRoute
}
```

```kotlin
// Bad: featureが遷移先のRouteを組み立てている（feature間に依存が生まれる）
// :app:feature:home/HomeNavigation.kt
fun NavGraphBuilder.homeScreen(navController: NavHostController) {
    composable<HomeRoute> {
        HomeScreen(onItemClick = { itemId -> navController.navigate(DetailRoute(itemId)) })
    }
}
```

```kotlin
// Good: Routeは画面を持つfeatureが定義し、遷移の組み立ては :app が行う
// :app:feature:home/HomeRoute.kt
@Serializable
data object HomeRoute

// :app:feature:home/HomeNavigation.kt
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<HomeRoute> {
        HomeScreen(onItemClick = navigateDetail, /* ... */)
    }
}

// :app:feature:detail/DetailRoute.kt
@Serializable
data class DetailRoute(val itemId: String)

// :app/navigation/AppNavHost.kt
navigation<MainGraphRoute>(startDestination = HomeRoute) {
    homeScreen(navigateDetail = { itemId -> navController.navigate(DetailRoute(itemId)) })
    detailScreen(navigateBack = { navController.popBackStack() })
}
```
