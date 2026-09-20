---
description: 画面のRoute定義は:app:coreのAppRoute(sealed interface)に集約し、featureは自分のRouteを定義せず遷移先の決定も持たない
globs:
  - "app/core/src/main/java/**/navigation/*.kt"
  - "app/src/main/java/**/navigation/*.kt"
  - "app/feature/*/src/main/java/**/*Navigation.kt"
alwaysApply: false
---

# Route定義は `:app:core` の `AppRoute` に集約する

## 方針

### 1. Routeは `:app:core` に置く
- 画面のRoute（Navigation Composeの型安全ナビゲーションで使う宛先の型）は、
  すべて `:app:core` の `AppRoute`（`sealed interface`）のメンバとして定義する。
  feature モジュールに `XxxRoute` を作らない。
- 引数を持たない画面は `@Serializable data object`、引数を持つ画面は
  `@Serializable data class` にする（`AppRoute.Detail(val itemId: String)`）。
- ネストしたグラフのRoute（`AuthGraph` / `MainGraph`）も同じ `AppRoute` に置く。
  参照するのは `:app`（`AppNavHost` / `AppState`）だけにする。
- feature が参照してよい `AppRoute` は、原則として**自分の画面のRouteだけ**
  （`composable<AppRoute.Home>`、`savedStateHandle.toRoute<AppRoute.Detail>()`）。

### 2. 遷移先の決定は `:app` に閉じる
- feature の `NavGraphBuilder` 拡張関数は、他画面のRouteを組み立てない。
  遷移は `navigateDetail: (String) -> Unit` のようなコールバックで `:app` へ委ねる
  （命名は [compose-navigation.md](compose-navigation.md)）。
- そのため feature は `navController` を受け取らない。`navController.navigate(AppRoute.Xxx)`
  を書いてよいのは `:app` の `AppNavHost` と `AppState` だけ。
- Screen Composable にはRoute型を渡さない。渡すのは `itemId: String` のような素の値にする。

### 3. 画面固有のNavigation設定は feature が持つ
- ディープリンクのベースパス（`DETAIL_DEEP_LINK_BASE_PATH`）、遷移アニメーションの指定、
  `dialog<...>` で登録するかどうかといった「その画面をどう登録するか」は、
  Route定義ではなく feature 側の `XxxNavigation.kt` に書く。
- `:app:core` が持つのは「宛先の名前と引数」までにとどめる。

### 4. 画面を追加する手順
1. `:app:core` の `AppRoute` にエントリを追加する（引数があれば `data class`）。
2. feature モジュールに `NavGraphBuilder.xxxScreen(navigateYyy = ...)` を書く。
3. `:app` の `AppNavHost` から呼び、遷移の実装（`navController.navigate(...)`）を書く。
4. タブとして表示する画面なら `:app:core` の `TopLevelDestination` にも追加する。

## 理由
- 全画面のRouteが1ファイルに並ぶため、`AppRoute.kt` を読めばアプリの画面構成と
  グラフのネスト構造がそのまま分かる。ナビゲーション構築を主題にした学習用テンプレートでは
  この見通しを優先する。
- Routeの置き場所に判断の余地が無くなり、画面を追加する手順が常に同じになる。
  特に `Detail` のように複数の画面から遷移される画面で「どこに置くべきか」を考えずに済む。
- 遷移先の決定を `:app` に閉じることで、feature 同士が互いのRoute型を知る必要がなくなり、
  feature 間の直接依存が発生しない。
- feature 分散方式（Now in Android 方式）との比較、この方針を選んだ経緯、
  将来分散へ切り替えるときの判断基準と手順は
  [docs/navigation-routes.md](../../docs/navigation-routes.md) にまとめてある。

## Before / After

```kotlin
// Bad: featureが自分でRouteを定義し、遷移先まで組み立てている
// :app:feature:home
@Serializable
data class DetailRoute(val itemId: String) // 置き場所が画面ごとにばらつく

fun NavGraphBuilder.homeScreen(navController: NavHostController) {
    composable<HomeRoute> {
        HomeScreen(onItemClick = { itemId -> navController.navigate(DetailRoute(itemId)) })
    }
}
```

```kotlin
// Good: Routeは :app:core、遷移の実装は :app、featureはコールバックで通知するだけ
// :app:core/navigation/AppRoute.kt
sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class Detail(val itemId: String) : AppRoute
}

// :app:feature:home/HomeNavigation.kt
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<AppRoute.Home> {
        HomeScreen(onItemClick = navigateDetail, /* ... */)
    }
}

// :app/navigation/AppNavHost.kt
homeScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
```
