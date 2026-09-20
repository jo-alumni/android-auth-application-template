# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

認証機能付きアプリケーションのナビゲーション構築を学ぶための学習用アプリ（詳細は [docs/app.md](docs/app.md) 参照）。

- 認証状態は DataStore に永続化され、すでに認証済みなら認証画面をスキップしてホーム画面から起動する。認証状態が確定するまではスプラッシュを維持するため、起動時に中間のローディング画面は見えない。
- ログアウトは認証状態のみ解除し、アプリデータ（DataStore 等）は削除しない。
- ログアウト操作以外（トークンの失効）で認証が解除された場合も、自動的にログイン画面へ戻る。
- 認証後はボトムバーで ホーム / 検索 / お気に入り の3画面を行き来でき、各画面から詳細画面へ遷移できる。
- ナビゲーションUIは画面幅で切り替わる（Compact=ボトムバー / Medium=NavigationRail / Expanded=常設ドロワー）。
- お気に入りはホーム/検索/お気に入りの各リストからトグルでき、DataStoreに永続化される（ログアウトしても消えない）。
- 実装初期のUIは画面遷移が成立する最低限のもの（遷移先が分かるボタンがあれば良い）で構わない。

## よく使うコマンド

```bash
# デバッグAPKのビルド
./gradlew assembleDebug

# 全モジュールのユニットテスト実行
./gradlew test

# 特定モジュールのみユニットテスト実行（例: :app）
./gradlew :app:testDebugUnitTest

# 単一テストクラス/メソッドの実行
./gradlew :app:testDebugUnitTest --tests "com.example.authapplication.ExampleUnitTest"
./gradlew :app:testDebugUnitTest --tests "com.example.authapplication.ExampleUnitTest.methodName"

# 実機/エミュレータが必要な計装テスト
./gradlew connectedAndroidTest

# Android Lint
./gradlew lint
```

## モジュール構成とアーキテクチャ

Gradleモジュールは以下の依存方向を持つ多層構成（`:app` が全feature/domain/dataに依存し、feature間の直接依存はない）。

- `:app` — `MainActivity` / `App`（`@HiltAndroidApp`）/ `AppViewModel`（認証状態の集約）/ `AppNavHost`（画面統合のNavGraph）・`AppNavigationScaffold`（画面幅に応じて `AppBottomBar` / `AppNavigationRail` / `AppNavigationDrawerSheet` を組み替える骨組み）/ `AppState`・`BottomBarScrollBehavior`（画面の骨組みが使うState Holder）を持つエントリーポイント。
- `:app:core` — 全feature共通の汎用機能。`AppRoute`（`@Serializable` sealedなNavigation経路定義）、`TopLevelDestination`（ボトムバー/レール/ドロワーの項目）、共通Composable（`AppTopBar`）、テーマを置く。
- `:app:feature:*`（auth, home, search, favorite, detail） — 画面単位の機能モジュール。各モジュールは `NavGraphBuilder` の拡張関数（例: `homeScreen(navigateDetail = ...)`）を公開し、`:app` の `AppNavHost` から呼び出される。
- `:domain` — UseCase・Repositoryインターフェース・モデル（Android非依存のKotlinモジュール）。ViewModelからのデータアクセスは必ずUseCaseを経由し、1行の委譲になるUseCaseも省略しない（[.claude/rules/usecase.md](.claude/rules/usecase.md) 参照）。リポジトリインターフェースを直接呼んでよいのは `:domain` のUseCaseだけ。
- `:data` — Repository実装（DataStoreベースの `AuthRepositoryImpl` など）とHiltの `DataStoreModule` / `RepositoryModule`。

テストは `./gradlew test`（JVM）で完結することを基本にする。ViewModelのユニットテストに加え、Screen Composable単体のUIテストもRobolectric上で `src/test` に置いており、実機/エミュレータが必要な計装テスト（`:app` の `androidTest`）は画面をまたぐナビゲーションの確認だけに絞っている。テスト用のFake（`FakeAuthRepository` など）は `:domain` の `testFixtures` に集約し、計装テストでは `@TestInstallIn` で `RepositoryModule` をFakeへ差し替えるため、実DataStoreの状態には依存しない([.claude/rules/testing.md](.claude/rules/testing.md) 参照)。

Android Library設定・Compose有効化・Hilt設定・リソース名の接頭辞(`resourcePrefix`)など、モジュール間で重複しがちなGradle設定は `build-logic`（Convention Plugin。`settings.gradle.kts` の `pluginManagement.includeBuild("build-logic")` で取り込まれるcomposite build）に集約している。各モジュールは `id("authapplication.android.library")` のようなConvention Plugin IDを適用し、`namespace` やモジュール固有の依存関係のみを自身の `build.gradle.kts` に残す。

お気に入り状態は `Preferences DataStore → FavoriteRepository(お気に入りID集合) → ObserveItemsUseCase → 各画面のViewModel` という流れで伝播する。`ObserveItemsUseCase` が `ItemRepository.observeItems()` とお気に入りID集合を `combine` して `Item.isFavorite` を埋めるため、どの画面でトグルしても同じFlowを購読している他画面に即座に反映される。お気に入り画面は `ObserveFavoriteItemsUseCase` で絞り込んだ結果を表示する。

表示文字列は各モジュールの `src/main/res/values/strings.xml` に置き、リソース名は `resourcePrefix`（`:app:core` なら `core_`、`:app:feature:home` なら `feature_home_`）で始める。画面固有の文言はfeatureモジュールが、「戻る」「閉じる」など画面に依存しない文言は `:app:core` が持つ。ViewModelは文言ではなく `@StringRes` のリソースIDを公開し、`stringResource` での解決はUI側で行う([.claude/rules/string-resources.md](.claude/rules/string-resources.md) 参照)。

認証状態は `DataStore → AuthRepository → IsAuthenticatedUseCase/SetAuthTokenUseCase/ClearAuthTokenUseCase → AppViewModel.authState(StateFlow<AuthUiState>)` という流れで伝播する。認証状態とナビゲーションの結び方は**状態駆動に一本化**しており、起動時の入り口だけを `AppNavHost` の `startDestination`（初回の認証状態解決時の値で固定し、以降変化させない）が決め、起動後に未認証へ変わったときの遷移は `AuthApplicationApp` が `authState` を購読して `AppState.navigateLogin()` を呼ぶ。ログアウト操作もトークン失効（外部要因）も「トークンを破棄する」だけで、遷移用の `SharedFlow<AppEvent>` は持たない。起動時は認証状態が確定するまで Core SplashScreen API（`installSplashScreen()` + `setKeepOnScreenCondition`）でスプラッシュを維持し、「スプラッシュ → ローディング → 目的の画面」の2段階のちらつきを出さない。選んだ理由、`startDestination` を変化させたときの `NavHost` の挙動、スプラッシュ維持の条件と打ち切り、ログアウト時にタブの `saveState` も破棄する必要がある理由は [docs/auth-navigation.md](docs/auth-navigation.md) にまとめてある([.claude/rules/viewmodel-event-handling.md](.claude/rules/viewmodel-event-handling.md) 参照)。

Edge to Edge（`enableEdgeToEdge()`）で描画するため、WindowInsetsの解決場所は「そのinsetsを隠すUIを描いた側」に固定している。`:app` は自分が描く `AppTopBar` 分の上端insetsだけを `Modifier.padding` + `consumeWindowInsets` で解決し、下端（ナビゲーションバー）とIMEは各Screenが `WindowInsets` から自分で解決する。そのためNavigation層に `PaddingValues` を通さない。ボトムバーがスクロールで隠れる本アプリで下端を `:app` 側に寄せない理由、および `LazyColumn` の `contentPadding` を動的に変えるとremeasureでカクつくという知見は [docs/window-insets.md](docs/window-insets.md) にまとめてある([.claude/rules/window-insets.md](.claude/rules/window-insets.md) 参照)。

アプリ全体の骨組みを組む `AuthApplicationApp` は「状態を読んでUIを組む」だけにし、ナビゲーションの判定は State Holder の `AppState`（`rememberAppState()` で生成）へ切り出している。`AppState` は `NavHostController.currentBackStackEntryFlow` を購読して現在地をSnapshot Stateとして保持し、`currentTopLevelDestination` / `navigationType` の判定と、タブ切り替え（`navigateToTopLevelDestination()`）・通知画面・ログアウト後の遷移を担う。そのため `AppBottomBar` は `NavHostController` を受け取らず、選択中のタブと `onDestinationSelected` だけを受け取る。スクロールに追従してボトムバーを隠す処理は `BottomBarScrollBehavior`（`rememberBottomBarScrollBehavior()`）に分けている。どちらもコンポジション無しで状態を確認できるため、`AppStateTest` / `BottomBarScrollBehaviorTest` でユニットテストする（`AppStateTest` は NavController が Context を必要とするため Robolectric 上で実行する）。

ナビゲーションUIの出し分けは `AppState.windowSizeClass`（`currentWindowAdaptiveInfoV2()` から受け取り、回転・リサイズのたびに `rememberAppState()` が書き戻す）と現在地から `AppNavigationType`（`NONE` / `BOTTOM_BAR` / `NAVIGATION_RAIL` / `PERMANENT_DRAWER`）を決める形にしてある。判定は `AppNavigationType.of()` に閉じ、`AppNavigationScaffold` は受け取った種類を `when` で分岐するだけにする（`else` を書かず、種類の追加漏れをコンパイルエラーで検出できる状態を保つ）。`AppNavigationScaffold` は種類が変わっても画面本体（`content`）の呼び出し位置を変えず、`Row` の中の `Scaffold` に固定する。呼び出し位置が変わるとComposeが部分木を作り直し、画面側の `rememberSaveable` やスクロール位置がリサイズのたびに失われるため。レール/ドロワーが覆う左端のinsetsは、それを描いた `AppNavigationScaffold` が `consumeWindowInsets` で差し引く。

失敗系は `:data` のリポジトリ実装が `AppDataException`（`:domain` の `domain/error`）を投げ、各ViewModelが `Flow.catch` で `XxxUiState.Error` に変換する。`:domain` はAndroidに依存せず文言を持てないため、例外が運ぶのはメッセージではなく `AppError`（失敗の種別）で、文言（文字列リソースID）への変換は各featureモジュールが行う。リトライは `MutableSharedFlow<Unit>` のトリガーを `flatMapLatest` の上流に置き、購読をやり直すことで実現している。一覧の表示を保ったまま伝えたい失敗（お気に入りトグルの失敗）は状態ではなく `SharedFlow<XxxEvent>` のSnackbarイベントとして通知する。実際に失敗する通信処理が無いため、TopAppBarのデバッグメニューの「エラーを発生させる」スイッチ（`ErrorInjectionRepository`）でリポジトリ層に例外を注入して動作確認できる([.claude/rules/error-handling.md](.claude/rules/error-handling.md) 参照)。

## 規約

- `.claude/rules/` 配下に規約ファイルを追加・編集する際は、必ず先頭にYAML frontmatterをつける。frontmatterには少なくとも以下のキーを含める。
  - `description`: そのルールが何を定めているかの一文サマリ
  - `globs`: そのルールが適用される対象ファイルのglobパターン(配列)
  - `alwaysApply`: 常に適用するかどうか(通常は `false`)
- 現在定義済みのルール:
  - [.claude/rules/viewmodel-event-handling.md](.claude/rules/viewmodel-event-handling.md) — ViewModel→UIの単発イベントはコールバック引数ではなくSharedFlowで配信する。ただし状態（`authState`）が決める遷移は状態駆動に任せ、イベントを重ねない
  - [.claude/rules/compose-navigation.md](.claude/rules/compose-navigation.md) — Navigationファイルのコールバックは `navigateXxx` のように遷移視点で命名する
  - [.claude/rules/compose-preview.md](.claude/rules/compose-preview.md) — publicなComposable関数には同名+`Preview`の `@Preview` 関数を必ず用意する
  - [.claude/rules/viewmodel-uistate.md](.claude/rules/viewmodel-uistate.md) — ViewModelが公開する画面状態は `sealed interface XxxUiState`(Loading/Empty/Success/Error)で表現する
  - [.claude/rules/error-handling.md](.claude/rules/error-handling.md) — リポジトリ層の例外は `Flow.catch` で `UiState.Error` に変換し、リトライは購読のやり直しで実現する
  - [.claude/rules/window-insets.md](.claude/rules/window-insets.md) — `:app` は上端insetsのみを解決してconsumeし、下端とIMEは各Screenが自分で解決する
  - [.claude/rules/testing.md](.claude/rules/testing.md) — Fakeは `:domain` の testFixtures に集約し、ScreenのUIテストはRobolectricで `src/test` に置く。計装テストは `@TestInstallIn` でリポジトリを差し替える
  - [.claude/rules/string-resources.md](.claude/rules/string-resources.md) — 表示文字列はモジュールごとの `strings.xml` に置き、ViewModelは文言ではなく文字列リソースIDを公開する
  - [.claude/rules/usecase.md](.claude/rules/usecase.md) — ViewModelはRepositoryを直接注入せず、必ず `:domain` のUseCaseを経由する
