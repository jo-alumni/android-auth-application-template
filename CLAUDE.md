# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

認証機能付きアプリケーションのナビゲーション構築を学ぶための学習用アプリ（詳細は [docs/app.md](docs/app.md) 参照）。

- 認証状態は DataStore に永続化され、すでに認証済みなら認証画面をスキップしてホーム画面から起動する。
- ログアウトは認証状態のみ解除し、アプリデータ（DataStore 等）は削除しない。
- 認証後はボトムバーで ホーム / 検索 / お気に入り の3画面を行き来でき、各画面から詳細画面へ遷移できる。
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

- `:app` — `MainActivity` / `App`（`@HiltAndroidApp`）/ `AppViewModel`（認証状態の集約）/ `AppNavHost`・`AppBottomBar`（画面統合のNavGraph）を持つエントリーポイント。
- `:app:core` — 全feature共通の汎用機能。`AppRoute`（`@Serializable` sealedなNavigation経路定義）、`TopLevelDestination`（ボトムバー項目）、共通Composable（`AppTopBar`）、テーマを置く。
- `:app:feature:*`（auth, home, search, favorite, detail） — 画面単位の機能モジュール。各モジュールは `NavGraphBuilder` の拡張関数（例: `homeScreen(navigateDetail = ...)`）を公開し、`:app` の `AppNavHost` から呼び出される。
- `:domain` — UseCase・Repositoryインターフェース・モデル（Android非依存のKotlinモジュール）。
- `:data` — Repository実装（DataStoreベースの `AuthRepositoryImpl` など）とHiltの `DataStoreModule` / `RepositoryModule`。

認証状態は `DataStore → AuthRepository → IsAuthenticatedUseCase/SetAuthenticatedUseCase → AppViewModel.authState(StateFlow<AuthUiState>)` という流れで伝播し、`AppNavHost` の startDestination 決定やログアウト時の遷移に使われる。ログアウトなどの単発の画面遷移イベントは `AppViewModel.event`（`SharedFlow<AppEvent>`）で通知される([.claude/rules/viewmodel-event-handling.md](.claude/rules/viewmodel-event-handling.md) 参照)。

## 規約

- `.claude/rules/` 配下に規約ファイルを追加・編集する際は、必ず先頭にYAML frontmatterをつける。frontmatterには少なくとも以下のキーを含める。
  - `description`: そのルールが何を定めているかの一文サマリ
  - `globs`: そのルールが適用される対象ファイルのglobパターン(配列)
  - `alwaysApply`: 常に適用するかどうか(通常は `false`)
- 現在定義済みのルール:
  - [.claude/rules/viewmodel-event-handling.md](.claude/rules/viewmodel-event-handling.md) — ViewModel→UIの単発イベントはコールバック引数ではなくSharedFlowで配信する
  - [.claude/rules/compose-navigation.md](.claude/rules/compose-navigation.md) — Navigationファイルのコールバックは `navigateXxx` のように遷移視点で命名する
  - [.claude/rules/compose-preview.md](.claude/rules/compose-preview.md) — publicなComposable関数には同名+`Preview`の `@Preview` 関数を必ず用意する
