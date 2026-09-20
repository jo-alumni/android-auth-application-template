# ルート定義（Route）の配置方針

マルチモジュールでNavigationを組むときは、「画面の宛先を表す型（Route）をどのモジュールに
置くか」を先に決めておく必要がある。決めずに進めると、feature を1つ足すたびに
「このRouteはどこに置くのが正しいのか」を考え直すことになり、モジュールの依存の向きも
その場の都合で変わってしまう。

このドキュメントは本アプリが採った配置（**`:app:core` への集約**）と、採らなかった
feature 分散方式（Now in Android 方式）との比較、そして将来分散へ切り替えるときの
判断基準と手順を残す。規約としての要点は
[.claude/rules/navigation-routes.md](../.claude/rules/navigation-routes.md) にある。

## 結論

全画面のRouteを `:app:core` の `AppRoute`（`sealed interface`）に集約する。

| 要素 | 置き場所 | 例 |
| --- | --- | --- |
| Route の型 | `:app:core` | `AppRoute.Home` / `AppRoute.Detail(itemId)` |
| ネストしたグラフのRoute | `:app:core` | `AppRoute.AuthGraph` / `AppRoute.MainGraph` |
| タブ項目の定義 | `:app:core` | `TopLevelDestination` |
| 画面をグラフへ登録する関数 | 各 feature | `NavGraphBuilder.homeScreen(navigateDetail = ...)` |
| グラフの構造と遷移の実装 | `:app` | `AppNavHost` / `AppState` |
| 画面固有のNavigation設定 | 各 feature | `DETAIL_DEEP_LINK_BASE_PATH`（`:app:feature:detail`） |

ポイントは「Routeの**型**は共通モジュールにあるが、**どのRouteへ遷移するかの判断**は
feature には無い」こと。feature の Navigation 拡張関数が受け取るのは
`navigateDetail: (String) -> Unit` のようなコールバックだけで、遷移先の `AppRoute.Detail` を
組み立てるのは `:app` の `AppNavHost` に閉じている
（命名規則は [.claude/rules/compose-navigation.md](../.claude/rules/compose-navigation.md)）。

```kotlin
// :app:feature:home — 遷移先を知らない。itemId を返すだけ
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<AppRoute.Home> { HomeScreen(onItemClick = navigateDetail, /* ... */) }
}

// :app — 遷移先の組み立てはここだけ
homeScreen(navigateDetail = { itemId -> navController.navigate(AppRoute.Detail(itemId)) })
```

feature が参照する `AppRoute` は、原則として**自分の画面のRouteだけ**になる
（`composable<AppRoute.Home>`、`savedStateHandle.toRoute<AppRoute.Detail>()`）。

## 2つの方式の比較

| 観点 | 集約（本アプリ） | feature 分散（Now in Android 方式） |
| --- | --- | --- |
| 画面構成の見通し | `AppRoute.kt` 1ファイルで全画面とグラフ構造が読める | 全体像は `AppNavHost` を読み、各 feature のファイルへ飛ぶ必要がある |
| feature 追加時の変更 | `:app:core` の `AppRoute` に1エントリ追加が必要 | feature モジュール内で完結する |
| モジュールの独立性 | `:app:core` が全画面の名前を持つ（型として全 feature の存在を知る） | feature が自分のRouteを所有し、切り出し・再利用がしやすい |
| 再コンパイルの範囲 | `AppRoute.kt` の変更で `:app:core` に依存する全モジュールが再コンパイルされる | Routeの変更はその feature と `:app` に閉じる |
| 迷いの少なさ | 新しいRouteの置き場所を考える必要がない | 「複数 feature から遷移される画面」のRouteの置き場所を都度判断する |
| `TopLevelDestination` | `:app:core` に置ける | `:app` へ移す必要がある（後述） |

### 集約を選んだ理由

1. **学習用テンプレートとして、画面構成が1ファイルで読めることの価値が大きい。**
   `AppRoute.kt` を開けば「認証前（`AuthGraph` → `Login`）」「認証後（`MainGraph` → `Home` /
   `Search` / `Favorite` / `Detail` / `Notification`）」という構造がそのまま読み取れる。
   本アプリの主題はナビゲーション構築そのものなので、全体像が一望できる方を優先した。
2. **現状の規模では分散の利点が効かない。** feature は6つ、画面は7つで、`AppRoute.kt` を
   変更するのは画面を追加するときだけ。再コンパイルの範囲が広がることの実害が、
   全体像を失うコストに見合わない。
3. **新しい画面を追加する手順が一定になる。** 「`AppRoute` にエントリを足す → feature に
   `xxxScreen()` を書く → `AppNavHost` から呼ぶ」の3手順で、判断の余地が無い。
   本プロジェクトは「どの画面を見ても同じ書き方になっている」ことを価値としており
   （[.claude/rules/usecase.md](../.claude/rules/usecase.md) と同じ考え方）、
   画面ごとに置き場所が変わり得る方式は採らない。

### 引き受けたデメリット

- **`:app:core` が全 feature の存在を知る。** ただし知っているのは「画面の名前と引数」だけで、
  依存の向き（feature → `:app:core`）は変わらない。`:app:core` が feature モジュールに
  依存するわけではないので、循環依存にはならない。
- **`AppRoute.kt` の変更が広く再コンパイルを誘発する。** `sealed interface` にエントリを
  足すことはABIの変更なので、`:app:core` に依存する全モジュールが再コンパイル対象になる。
  発生するのは画面追加時だけなので許容する。

## 採らなかった案: feature 分散（Now in Android 方式）

分散方式では、Routeと`NavGraphBuilder`拡張、そして他モジュールから呼ばれる遷移関数を
feature がまとめて所有する。

```kotlin
// :app:feature:detail — Routeを自分で持つ
@Serializable
data class DetailRoute(val itemId: String)

fun NavGraphBuilder.detailScreen(navigateBack: () -> Unit) {
    composable<DetailRoute> { /* ... */ }
}

// 外から遷移させたい場合は NavController の拡張関数として公開する
fun NavController.navigateToDetail(itemId: String) = navigate(DetailRoute(itemId))
```

issue で論点に挙がった項目について、分散を採る場合の解を示しておく。

### feature 間遷移（Home → Detail）の型解決

**feature 同士が互いのRoute型を知る必要はない。** 本アプリはすでに、feature の Navigation
拡張関数が遷移先を知らない形になっている（`navigateDetail: (String) -> Unit` を受け取り、
`AppRoute.Detail` を組み立てるのは `:app`）。分散してもこの形は変わらず、
`DetailRoute` を知るのは `:app:feature:detail` 自身と `:app` だけで済む。

つまり「遷移先の決定を `:app` へ持ち上げる」構造は集約/分散のどちらでも共通で、
Route をどこに置くかとは独立に決められる。逆に、feature が直接 `navController.navigate(...)`
を呼ぶ設計なら、遷移先のRoute型を共有する場所（共通モジュール、または feature 間の直接依存）が
必要になる。本アプリが feature 間の直接依存を持たずに済んでいるのはこの構造のおかげ。

### 複数 feature から遷移される画面（Detail）の扱い

上記のとおり Home / Search / Favorite は `itemId: String` を返すだけで `Detail` のRouteを
参照しないため、**`Detail` のRouteは `:app:feature:detail` に置ける**。「複数の画面から
遷移されるから共通モジュールへ」という判断は、遷移先の組み立てを `:app` に閉じている限り
発生しない。

なお、画面固有のNavigation設定のうち、すでに feature 側にあるものもある。詳細画面の
ディープリンクのベースパス（`DETAIL_DEEP_LINK_BASE_PATH`）は `:app:feature:detail` にあり、
`:app:core` は知らない。Route型だけが共通モジュールにあり、その使い方は feature が持つ、
という切り分けになっている。

### `TopLevelDestination` の扱い

`TopLevelDestination`（`:app:core`）は3つのタブのRoute（`AppRoute.Home` / `Search` /
`Favorite`）と Material Icons に依存している。分散方式ではこの enum を `:app:core` に
置けない。core が各 feature のRoute型を参照することになり、依存が逆流するため。
Now in Android でも `TopLevelDestination` は app モジュール側にある。

したがって分散へ切り替える場合は `TopLevelDestination` を `:app` へ移す。アイコンへの依存も
一緒に移るため、`:app:core` から `material-icons-core` の依存を落とせる。

### グラフのRoute（`AuthGraph` / `MainGraph`）の扱い

どちらの方式でも `:app` 側に置くのが自然。ネストの構造は「アプリが画面をどう束ねるか」の
判断であって、個々の feature の持ち物ではない。集約している現状でも、この2つを参照するのは
`AppNavHost` と `AppState` だけで、feature からは参照されていない。

## 分散へ切り替える判断基準

次のいずれかに当てはまったら再検討する。

- feature モジュールが増え、`AppRoute.kt` を触るたびの再コンパイル時間が体感できるようになった
- feature を別アプリ・別リポジトリへ持ち出したくなった（Routeが core にあると feature 単体で
  完結しない）
- 画面ごとの引数が増え、`AppRoute.kt` が「全画面の引数仕様表」になって読みづらくなった

切り替えるときの手順は次のとおりで、いずれも `:app` と各 feature の内部で完結する。
Screen Composable も ViewModel の公開する `UiState` もRoute型に依存していないため、
UI・状態のコードには影響しない。

1. 各featureに `XxxRoute` を移し、`composable<XxxRoute>` と
   `savedStateHandle.toRoute<XxxRoute>()` を feature 内で解決する
2. `TopLevelDestination` を `:app` へ移す（Material Icons への依存も一緒に移す）
3. `AuthGraph` / `MainGraph` を `:app` の `navigation` パッケージへ移す
4. `:app:core` から `navigation` パッケージと `material-icons-core` の依存を削除する

## 関連ドキュメント

- [.claude/rules/navigation-routes.md](../.claude/rules/navigation-routes.md) — 規約としての要点
- [.claude/rules/compose-navigation.md](../.claude/rules/compose-navigation.md) — Navigation
  ファイルのコールバックの命名規則
- [docs/auth-navigation.md](auth-navigation.md) — 認証状態とナビゲーションの結び方
