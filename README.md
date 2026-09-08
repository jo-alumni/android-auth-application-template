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
:app                    アプリのエントリーポイント（MainActivity, App, AppViewModel, AppNavHost, AppState など）
:app:core               feature 共通の汎用機能（AppRoute, TopLevelDestination, 共通Composable, テーマ）
:app:feature:login      認証（ログイン）画面
:app:feature:home       ホーム画面
:app:feature:search     検索画面
:app:feature:favorite   お気に入り画面
:app:feature:detail     詳細画面
:domain                 UseCase・Repositoryインターフェース・モデル（Android非依存）
:data                   Repository実装（DataStoreベースの AuthRepositoryImpl など）・Hiltモジュール
```

各 feature モジュールは `NavGraphBuilder` の拡張関数（例: `homeScreen(navigateDetail = ...)`）を公開し、`:app` の `AppNavHost` から呼び出されます。

認証状態は `DataStore → AuthRepository → IsAuthenticatedUseCase/SetAuthenticatedUseCase → AppViewModel.authState(StateFlow<AuthUiState>)` という流れで伝播し、`AppNavHost` の startDestination 決定やログアウト時の遷移に使われます。ログアウトなど単発の画面遷移イベントは `AppViewModel.event`（`SharedFlow<AppEvent>`）で通知されます。

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
- [.claude/rules/compose-preview.md](.claude/rules/compose-preview.md) — publicなComposable関数には同名+`Preview`の `@Preview` 関数を必ず用意する

Claude Code 向けの詳細な開発ガイドは [CLAUDE.md](CLAUDE.md) を参照してください。
