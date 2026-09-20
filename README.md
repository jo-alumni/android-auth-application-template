# AuthApplication

認証機能付きアプリケーションのナビゲーション構築を学ぶための学習用 Android アプリです。
Jetpack Compose / Navigation / Hilt / DataStore を使い、マルチモジュール構成でアプリを組み立てる練習用テンプレートとして作られています。

## 機能

- **認証**
  - すでに認証済みの場合は認証画面をスキップし、ホーム画面からアプリが起動する
  - 認証状態は DataStore（Proto DataStore + Tink による AEAD 暗号化）に永続化される
  - ホーム / 検索 / お気に入り画面共通の TopAppBar から「ログアウト」を実行できる
  - ログアウトすると認証状態のみ解除され、アプリデータ（DataStore 等）は削除されない
- **画面遷移**
  - 認証後はボトムバーで ホーム / 検索 / お気に入り の3画面を行き来できる
  - 各画面から詳細画面へ遷移できる

より詳しいアプリ仕様は [docs/app.md](docs/app.md) を参照してください。

## モジュール構成

多層のマルチモジュール構成になっており、`:app` が全 feature / domain / data に依存します。feature モジュール同士の直接依存はありません。

```
:app                       アプリのエントリーポイント（MainActivity, App, AppViewModel, AppNavHost, AppState など）
:app:core                  feature 共通の汎用機能（AppRoute, TopLevelDestination, 共通Composable, テーマ）
:app:feature:login         認証（ログイン）画面
:app:feature:home          ホーム画面
:app:feature:search        検索画面
:app:feature:favorite      お気に入り画面
:app:feature:detail        詳細画面
:app:feature:notification  通知画面
:domain                    UseCase・Repositoryインターフェース・モデル（Android非依存）
:data                      Repository実装（DataStoreベースの AuthRepositoryImpl など）・Hiltモジュール
```

各 feature モジュールは `NavGraphBuilder` の拡張関数（例: `homeScreen(navigateDetail = ...)`）を公開し、`:app` の `AppNavHost` から呼び出されます。

画面の Route 定義は各 feature ではなく `:app:core` の `AppRoute`（`sealed interface`）に集約し、どの Route へ遷移するかの決定は `:app` の `AppNavHost` に閉じています。feature が受け取るのは `navigateDetail: (String) -> Unit` のようなコールバックだけなので、feature 同士が互いの Route 型を知る必要はありません。この配置を選んだ理由と feature 分散方式（Now in Android 方式）との比較は [docs/navigation-routes.md](docs/navigation-routes.md) を参照してください。

ViewModel からのデータアクセスは必ず `:domain` の UseCase を経由します。リポジトリの1メソッドを呼ぶだけで1行の委譲になる UseCase（`GetItemUseCase` など）も省略せず、リポジトリインターフェースを直接呼んでよいのは `:domain` の UseCase だけ、という基準に統一しています（[.claude/rules/usecase.md](.claude/rules/usecase.md) 参照）。

認証状態は `DataStore → AuthRepository → IsAuthenticatedUseCase/SetAuthTokenUseCase/ClearAuthTokenUseCase → AppViewModel.authState(StateFlow<AuthUiState>)` という流れで伝播します。認証状態とナビゲーションの結び方は状態駆動に一本化しており、起動時の入り口だけを `AppNavHost` の `startDestination` が決め、起動後に未認証へ変わったときの遷移は `AuthApplicationApp` が `authState` を購読して行います。そのためログアウト操作でもトークン失効でも同じ経路でログイン画面へ戻ります（詳しくは [docs/auth-navigation.md](docs/auth-navigation.md)）。

アプリ全体の骨組みを組む `AuthApplicationApp` は状態を読んでUIを組み立てるだけにし、「今どのタブにいるか」「ボトムバーを表示するか」といったナビゲーションの判定は State Holder の `AppState`（`rememberAppState()`）が、スクロールに追従したボトムバーの隠蔽は `BottomBarScrollBehavior`（`rememberBottomBarScrollBehavior()`）が持ちます。

## 技術スタック

- Kotlin / Jetpack Compose
- Navigation Compose（型安全な `@Serializable` route）
- Hilt（DI）
- DataStore（Proto DataStore）+ Tink（認証情報の暗号化）
- Kotlin Coroutines / Flow
- Kotlin Serialization

## セットアップ

### 必要環境

- JDK 11 以上
- Android Studio（最新の Stable 版を推奨）

### ビルド

```bash
# デバッグAPKのビルド
./gradlew assembleDebug
```

### テスト

```bash
# 全モジュールのユニットテスト実行
./gradlew test

# 特定モジュールのみユニットテスト実行（例: :app）
./gradlew :app:testDebugUnitTest

# 単一テストクラス/メソッドの実行
./gradlew :app:testDebugUnitTest --tests "com.example.authapplication.ExampleUnitTest"
./gradlew :app:testDebugUnitTest --tests "com.example.authapplication.ExampleUnitTest.methodName"

# 実機/エミュレータが必要な計装テスト
./gradlew connectedAndroidTest
```

### Lint

```bash
./gradlew lint
```

## 開発時の規約

`.claude/rules/` 配下にプロジェクトの実装規約をまとめています。

- [.claude/rules/viewmodel-event-handling.md](.claude/rules/viewmodel-event-handling.md) — ViewModel→UIの単発イベントはコールバック引数ではなくSharedFlowで配信する
- [.claude/rules/compose-navigation.md](.claude/rules/compose-navigation.md) — Navigationファイルのコールバックは `navigateXxx` のように遷移視点で命名する
- [.claude/rules/navigation-routes.md](.claude/rules/navigation-routes.md) — 画面のRoute定義は `:app:core` の `AppRoute` に集約し、遷移先の決定は `:app` に閉じる
- [.claude/rules/compose-preview.md](.claude/rules/compose-preview.md) — publicなComposable関数には同名+`Preview`の `@Preview` 関数を必ず用意する
- [.claude/rules/viewmodel-uistate.md](.claude/rules/viewmodel-uistate.md) — ViewModelが公開する画面状態は `sealed interface XxxUiState`（Loading/Empty/Success/Error）で表現する
- [.claude/rules/error-handling.md](.claude/rules/error-handling.md) — リポジトリ層の例外は `Flow.catch` で `UiState.Error` に変換し、リトライは購読のやり直しで実現する
- [.claude/rules/window-insets.md](.claude/rules/window-insets.md) — `:app` は上端insetsのみを解決してconsumeし、下端とIMEは各Screenが自分で解決する
- [.claude/rules/testing.md](.claude/rules/testing.md) — Fakeは `:domain` の testFixtures に集約し、ScreenのUIテストはRobolectricで `src/test` に置く
- [.claude/rules/string-resources.md](.claude/rules/string-resources.md) — 表示文字列はモジュールごとの `strings.xml` に置き、ViewModelは文言ではなく文字列リソースIDを公開する
- [.claude/rules/usecase.md](.claude/rules/usecase.md) — ViewModelはRepositoryを直接注入せず、必ず `:domain` のUseCaseを経由する

Claude Code 向けの詳細な開発ガイドは [CLAUDE.md](CLAUDE.md) を参照してください。
