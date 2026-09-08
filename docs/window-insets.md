# WindowInsets（Edge to Edge）の扱い方

Android 15 以降ではアプリが常に edge-to-edge で描画される。本アプリも `MainActivity` で
`enableEdgeToEdge()` を呼んでおり、ステータスバー / ナビゲーションバー（＝ system bars）の
裏側までコンテンツが広がる。そのため「どの UI が、どの insets 分の余白を確保するのか」を
決めておかないと、画面ごとに二重に余白が入ったり、逆にボタンがジェスチャーバーに
隠れたりする。

このドキュメントは、その方針とそこに至った経緯（学習用の記録）をまとめたもの。
規約としての要点は [.claude/rules/window-insets.md](../.claude/rules/window-insets.md) に
ある。

## 方針: insets は「それを隠す UI を描いた側」が解決する

| 対象 | 解決する場所 | 方法 |
| --- | --- | --- |
| 上端（ステータスバー / `AppTopBar`） | `:app`（`AuthApplicationApp` の `Scaffold`） | `Modifier.padding(top)` + `Modifier.consumeWindowInsets(top)` |
| 下端（ナビゲーションバー） | 各 feature の Screen | スクロールする画面は `LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues())`、しない画面は `Modifier.navigationBarsPadding()` |
| IME（ソフトキーボード） | 入力欄を持つ Screen | `Modifier.imePadding()` |
| 全画面ダイアログ（通知画面） | その画面自身 | 別ウィンドウなので自前の `Scaffold` で上下とも解決する |

`:app` は「自分が描いた `AppTopBar` の分」だけを引き受け、それ以外の insets には触らない。
`Modifier.consumeWindowInsets()` を併用しているため、適用済みの余白は下流の `WindowInsets`
から差し引かれる。つまり画面側で `WindowInsets.statusBars` を読んでも 0 になり、二重適用が
起きない。

```kotlin
// AuthApplicationApp.kt
Scaffold(topBar = { ... }, bottomBar = { ... }) { innerPadding ->
    val topPadding = PaddingValues(top = innerPadding.calculateTopPadding())
    AppNavHost(
        modifier = Modifier
            .padding(topPadding)
            .consumeWindowInsets(topPadding),
        // ...
    )
}
```

## なぜ下端を `:app` で解決しないのか

`Scaffold` の `innerPadding` をそのまま（下端も含めて）コンテンツに適用するのが Compose の
一番素直な形で、実際 [Now in Android] などもそうしている。それを採らなかったのは、本アプリの
ボトムバーが **スクロールに追従して隠れる** ためである。

- `innerPadding.calculateBottomPadding()` は「ボトムバーの高さ（ナビゲーションバー込み）」になる
- これをコンテンツに適用すると、リストはボトムバーの手前で終わる
- ボトムバーがスクロールで下に隠れると、そこに何も描かれない空白が残ってしまう

そこで「レイアウト領域はナビゲーションバーの裏まで広げたまま、スクロール余白
（`LazyColumn` の `contentPadding`）だけをナビゲーションバー分だけ確保する」形にしている。
これはリストを edge-to-edge で見せるときの定石で、スクロールしきればリストの末尾は
ナビゲーションバーの手前まで完全に見える。

`Scaffold(contentWindowInsets = WindowInsets(0))` にする案も検討したが、ログイン画面には
`AppTopBar` が無く、その場合 `innerPadding` の上端はステータスバー分になる（既定の
`contentWindowInsets` が `safeDrawing` のため）。既定のままにしておくと「トップバーがある画面は
トップバー分、無い画面はステータスバー分」が同じ経路で得られるので、既定値を採用している。

## 記録: `contentPadding` をボトムバーの表示状態に追従させると重い

ボトムバーが隠れている間だけ `contentPadding` を減らせば「ボトムバーの裏にリストの末尾が
隠れる」瞬間も無くせるはずだが、これは**やらない**。

`LazyColumn` の `contentPadding` はレイアウト計算に直接使われる**測定入力**なので、値が
変わるたびに remeasure が走る。ボトムバーのスクラップ量（`bottomBarOffsetHeightPx`）に
リアルタイムに追従させると毎フレーム remeasure され、スクロールが目に見えてカクついた。
値を離散化して `animateDpAsState` で繋ぐ緩和も試したが、アニメーション中は結局毎フレーム
値が変わり続けるため解消しなかった。

一方、ボトムバー自体の出し入れは `Modifier.offset { }` で行っている。`offset` のラムダ版は
配置（placement）フェーズだけで解決されるため、毎フレーム値が変わっても measure は
走らない。「測定に効く値は固定し、動かすものは placement で動かす」というのがここでの
学びになる。

そのため `contentPadding` はナビゲーションバー分の**固定値**にしている。ボトムバー表示中は
リスト末尾がボトムバーの背後に隠れることがあるが、スクロールしてボトムバーが隠れれば
ナビゲーションバー手前まで完全に表示される。

## 経緯

かつては `:app` が算出した `PaddingValues` を
`AppNavHost(listContentPadding = ...)` → `homeScreen(contentPadding = ...)` →
`HomeScreen(contentPadding = ...)` とバケツリレーしていた。これは `:app` の `Scaffold` 事情
（ボトムバーがある・スクロールで隠れる）が feature モジュールの公開 API に漏れている状態で、
feature 単体で見たときに「なぜこの引数が必要なのか」が分からなかった。

各画面が `WindowInsets` から自分で解決する形にしたことで、この引数は不要になった。

[Now in Android]: https://github.com/android/nowinandroid
