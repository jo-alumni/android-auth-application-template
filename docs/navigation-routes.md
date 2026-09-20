# ルート定義（Route）の配置方針

マルチモジュールでNavigationを組むときは、「画面の宛先を表す型（Route）をどのモジュールに
置くか」を先に決めておく必要がある。決めずに進めると、feature を1つ足すたびに
「このRouteはどこに置くのが正しいのか」を考え直すことになり、モジュールの依存の向きも
その場の都合で変わってしまう。

このドキュメントは本アプリが採った配置（**feature モジュールへの分散**）と、比較検討した
共通モジュールへの集約方式との違い、そして分散方式でしか出てこない論点（feature 間遷移、
複数 feature から遷移される画面、タブ項目の置き場所）への解を残す。規約としての要点は
[.claude/rules/navigation-routes.md](../.claude/rules/navigation-routes.md) にある。

## 結論

**画面のRouteは、その画面を持つ feature モジュールが所有する。**

| 要素 | 置き場所 | 例 |
| --- | --- | --- |
| 画面のRoute | その画面を持つ feature の `XxxRoute.kt` | `HomeRoute`（`:app:feature:home`）/ `DetailRoute(itemId)`（`:app:feature:detail`） |
| 画面をグラフへ登録する関数 | 同じ feature の `XxxNavigation.kt` | `NavGraphBuilder.homeScreen(navigateDetail = ...)` |
| 画面固有のNavigation設定 | 同じ feature | `DETAIL_DEEP_LINK_BASE_PATH`、遷移アニメーションの指定、`dialog<...>` での登録 |
| ネストしたグラフのRoute | `:app` | `AuthGraphRoute` / `MainGraphRoute` |
| タブ項目の定義 | `:app` | `TopLevelDestination` |
| グラフの構造と遷移の実装 | `:app` | `AppNavHost` / `AppState` |

Routeは `XxxRoute.kt` に単独で置き、同じモジュールの `XxxNavigation.kt` がそれを登録する。
ファイルを分けているのは、detektの `MatchingDeclarationName`（トップレベルの型がひとつだけの
ファイルは型名と同じファイル名にする）に合わせるため。

```kotlin
// :app:feature:home/HomeRoute.kt
@Serializable
data object HomeRoute

// :app:feature:home/HomeNavigation.kt
fun NavGraphBuilder.homeScreen(navigateDetail: (String) -> Unit) {
    composable<HomeRoute> { HomeScreen(onItemClick = navigateDetail, /* ... */) }
}

// :app/navigation/AppNavHost.kt — 遷移先を決めるのはここだけ
navigation<MainGraphRoute>(startDestination = HomeRoute) {
    homeScreen(navigateDetail = { itemId -> navController.navigate(DetailRoute(itemId)) })
    // ...
}
```

`:app:core` の `navigation` パッケージに残るのは、画面に依存しない遷移アニメーションの定義
（`AppNavTransitions`）だけになった。副次的に `:app:core` は
kotlinx.serialization を必要としなくなっている。

## 2つの方式の比較

| 観点 | feature 分散（本アプリ） | 共通モジュールへ集約 |
| --- | --- | --- |
| feature 追加時の変更 | feature モジュール内で完結する | 共通モジュール（`:app:core`）の変更が必要 |
| 再コンパイルの範囲 | Routeの変更はその feature と `:app` に閉じる | 共通モジュールに依存する全モジュールが再コンパイル対象 |
| 並行開発 | 画面ごとにファイルが分かれ、衝突しにくい | 画面追加が1ファイルに集中し、コンフリクトしやすい |
| モジュールの独立性 | feature が自分のRouteと引数を所有し、切り出し・再利用しやすい | 共通モジュールが全画面の名前と引数を知る |
| 画面構成の見通し | Route定義は分散するが、グラフ構造は `AppNavHost` で一望できる | `AppRoute.kt` 1ファイルで引数まで含めて読める |
| 型の扱い | 共通の親型が無く、`TopLevelDestination.route` は `Any` になる | `sealed interface AppRoute` として扱える |

### 分散を選んだ理由

1. **共通モジュールが全 feature を知る状態を作らない。** 集約方式では、画面を追加するたびに
   `:app:core` を編集することになり、共通モジュールが全 feature の名前と引数を持つ。
   feature を切り出したくなったとき、Routeが共通モジュールにあると feature 単体で完結しない。
2. **変更の影響範囲が feature に閉じる。** 共通モジュールは全 feature の上流にあるため、
   Routeを1つ足すだけで（ABIの変更として）全 feature とそのテストが再コンパイル対象になる。
   モジュール数が増えるほどビルド時間に効き、複数チームで開発する場合は同じファイルへの
   変更が集中してコンフリクトの常習地点になる。
3. **実プロダクトの標準的な構成に合わせる。** 本リポジトリは学習用テンプレートであり、
   ここで書いた形がそのまま実プロダクトへ持ち込まれる。Now in Android をはじめ、
   マルチモジュールのAndroidアプリでは feature がRouteを所有する形が一般的で、
   テンプレートが実務と違う構成を教える理由が無い。

### 引き受けたデメリットと、その扱い

- **全画面のRouteを1ファイルで見渡せなくなる。** ただしグラフのネスト構造と全画面の登録は
  `AppNavHost` が20行ほどで示しており、「どんな画面があるか」はそこで読める。
  1ファイルに集めることで増える見通しは、画面ごとの引数の一覧に限られる。
- **Routeに共通の親型が無くなる。** `TopLevelDestination.route` は `AppRoute` ではなく `Any`
  になった。Navigation Composeの型安全ナビゲーションは `@Serializable` なオブジェクトを
  そのまま受け取るため、遷移も現在地の判定（`hasRoute(route::class)`）もこのままで行える。
  `sealed interface` による網羅的な `when` は書けなくなるが、元々どこでも使っていない。
- **タブ項目とアイコンの依存が `:app` に移る。** 後述のとおり `TopLevelDestination` は
  `:app` へ移り、Material Icons への依存も `:app` に追加した。
  なお `:app:core` の `AppTopBar` が別のアイコンを使っているため、
  `:app:core` から Material Icons の依存が消えるわけではない。

## 分散方式でだけ出てくる論点

### feature 間遷移（Home → Detail）の型解決

**feature 同士が互いのRoute型を知る必要はない。** 本アプリでは feature の Navigation 拡張関数が
遷移先を知らず、`navigateDetail: (String) -> Unit` で `:app` に通知するだけになっている。
`DetailRoute` を組み立てるのは `:app` の `AppNavHost` だけなので、
`:app:feature:home` は `:app:feature:detail` に依存しない。

この「遷移先の決定を `:app` へ持ち上げる」構造こそが feature 間の直接依存を防いでいる部分で、
Routeをどこに置くかとは独立している。逆にこの持ち上げをやめると、遷移元が遷移先のRoute型を
参照することになり、feature 間に依存が生まれる。

ただしこの方式は万能ではない。画面の階層が深くなるとコールバックを下まで配ることになり、
feature の奥から別の feature へ直接飛びたい要求も出てくる。そのときの選択肢は次の2つで、
本アプリの規模ではまだ必要としていない。

- feature が `fun NavController.navigateToDetail(itemId: String)` のような遷移用の拡張関数を
  公開し、呼び出し側のモジュールがその feature に依存する（Now in Android の方式）
- Routeと遷移関数だけを持つ薄い `:api` モジュールを feature から切り出し、
  実装（`:impl`）への依存なしに遷移できるようにする

### 複数 feature から遷移される画面（Detail）の扱い

上記のとおり Home / Search / Favorite は `itemId: String` を渡すだけで `DetailRoute` を
参照しないため、**`DetailRoute` は `:app:feature:detail` に置ける**。
「複数の画面から遷移されるから共通モジュールへ」という判断は、遷移先の組み立てを `:app` に
閉じている限り発生しない。詳細画面のディープリンク（`DETAIL_DEEP_LINK_BASE_PATH`）も
同じモジュールにあり、詳細画面へ入る経路の定義が1か所にまとまっている。

### `TopLevelDestination` の置き場所

`TopLevelDestination` は3つのタブのRouteとラベルを束ねる定義なので、
Routeを分散させると `:app:core` には置けない。共通モジュールが feature を参照することになり、
依存が逆流するため。そこで `:app` の `navigation` パッケージへ移した。
ラベルの文字列リソースも、画面を持つ feature が持つ方針（
[.claude/rules/string-resources.md](../.claude/rules/string-resources.md)）に合わせて
`:app:core` の `core_destination_*` から各 feature の `feature_xxx_title` へ移してある。

### グラフのRoute（`AuthGraphRoute` / `MainGraphRoute`）の置き場所

`:app` に置く。ネストの構造は「アプリが画面をどう束ねるか」の判断であって、
個々の feature の持ち物ではない。参照するのも `AppNavHost` と `AppState` だけで、
feature からは見えない。

## 集約方式が向く場合

次のような状況なら、`sealed interface AppRoute` に集約する方が読みやすいこともある。

- 画面数が一桁で、今後 feature モジュールを増やす予定が無い
- 1人で開発していて、並行変更によるコンフリクトが起きない
- 全画面の引数仕様を1ファイルで見渡せることを、モジュールの独立性より優先したい

その場合でも、**遷移先の決定を `:app` に閉じる**部分は変えない方がよい。
feature 間の直接依存を防いでいるのはRouteの配置ではなくこちらで、
あとから分散へ移す際のコストもこの構造があるかどうかで決まる。

## 関連ドキュメント

- [.claude/rules/navigation-routes.md](../.claude/rules/navigation-routes.md) — 規約としての要点
- [.claude/rules/compose-navigation.md](../.claude/rules/compose-navigation.md) — Navigation
  ファイルのコールバックの命名規則
- [docs/auth-navigation.md](auth-navigation.md) — 認証状態とナビゲーションの結び方
