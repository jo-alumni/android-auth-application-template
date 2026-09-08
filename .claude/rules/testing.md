---
description: テストの置き場所・使う道具・書き方（ViewModelのユニットテスト、RobolectricによるScreenのUIテスト、@TestInstallInでの計装テスト）を定める
globs:
  - "**/src/test/**/*.kt"
  - "**/src/androidTest/**/*.kt"
  - "**/src/testFixtures/**/*.kt"
alwaysApply: false
---

# テストの方針

## 1. テストの種類と置き場所

| 対象 | 置き場所 | 実行 |
| --- | --- | --- |
| UseCase / Repository実装 | 対象モジュールの `src/test` | `./gradlew test` |
| ViewModel | 対象featureモジュールの `src/test` | `./gradlew test` |
| Screen Composable単体 | 対象featureモジュールの `src/test`（Robolectric） | `./gradlew test` |
| 画面をまたぐナビゲーション | `:app` の `src/androidTest` | `./gradlew connectedAndroidTest` |

実機/エミュレータが必要なテストは「画面をまたぐ導線の確認」だけに絞る。
1画面で完結する確認は Robolectric で `src/test` に置き、`./gradlew test` だけで回るようにする。

## 2. Fakeは `:domain` の `testFixtures` に置く

- テスト用のリポジトリ実装（`FakeXxxRepository`）は `domain/src/testFixtures` に置き、
  各モジュールから `testImplementation(testFixtures(projects.domain))` で共有する。
  モジュールごとに同じFakeを書かない。
- Fakeは「実装の都合」ではなく「実装の観測できる振る舞い」を模す。
  例えば `FakeAuthRepository` は、実DataStoreが初回読み込みを終えるまで値を出さないのに合わせて、
  値を仕込むまで何も発行しない。おかげで ViewModel の `Loading` をテストから観測できる。
- 失敗も再現できるようにする（`emitError()` / `toggleError`）。
  「エラー表示 → リポジトリ回復 → リトライ → 成功」を1つのテストで書けることを基準にする。
- モック化ライブラリ（Mockito/MockK）は使わない。Fakeを手で書く。

## 3. ViewModelのユニットテスト

- `MainDispatcherRule`（`:domain` の testFixtures）で `Dispatchers.Main` を差し替える。
- `StateFlow` / `SharedFlow` の検証には Turbine（`flow.test { ... }`）を使う。
- 検証するのは **UiStateの遷移**。基本形は次の4つで、画面固有の状態があれば足す。
  - `Loading → Success` / `Loading → Empty`
  - リポジトリが例外を投げたときの `Error`
  - `retry()` による `Error → Success`（購読のやり直し）と、失敗し続けるときに `Error` のままであること
  - 一覧を保ったまま伝える失敗（お気に入りトグル）は `event` に `ShowErrorSnackbar` が流れること
- 状態が即座に確定して `StateFlow` に畳み込まれる場合は `awaitItem()` を並べず
  `expectMostRecentItem()` で最終状態を確認する（例: `DetailViewModelTest`）。
- テストメソッド名はバッククォート付きの英文で、`uiState is X when Y` のように
  「どの状態か」と「どの条件か」が読み取れる形にする。

## 4. Screen ComposableのUIテスト（Robolectric）

- `@RunWith(RobolectricTestRunner::class)` + `createComposeRule()` で、実機なしにJVM上で動かす。
- ViewModelやNavigationは介さず、`uiState` を直接渡して呼び出す。
  Screenは「渡された状態を描くだけ」なので、テストも状態と表示の対応だけを確認する。
- 検証するのは次の2点に絞る。UIの見た目（色・余白）はPreviewで確認する範囲とし、テストにはしない。
  - 状態ごとに出る文言（`Empty` と `NoResults` のように説明が変わるものは必ず区別して検証する）
  - 操作（クリック・入力）が、どのコールバックにどの値で伝わるか
- ノードの特定にはユーザーが見える文言か `contentDescription` を使う。
  テストのためだけに `testTag` を製品コードへ足さない。
- Robolectricの共通設定は `gradle/robolectric/robolectric.properties` の1か所に置き、
  `authapplication.android.compose` Convention Plugin が全モジュールへ読み込ませる。
  モジュールごとに `robolectric.properties` を複製しない。

## 5. 計装テストは `@TestInstallIn` でFakeに差し替える

- `:app` の `androidTest` では `@TestInstallIn` で `RepositoryModule` を
  `TestRepositoryModule` に置き換え、リポジトリをFakeにする。
  実DataStore（端末上の実ファイル）に触れないため、前回のテストが書き込んだ状態が残らない。
- 差し替えるのは**実装を束ねるモジュール**だけでよい。`DataStoreModule` を要求するのは
  リポジトリ実装なので、実装が注入されなくなればDataStoreも生成されない。
- テストから状態を仕込めるよう、`TestRepositoryModule` はインターフェース型に加えて
  Fakeの具象型でも注入できるようにしておく（`@Inject lateinit var authRepository: FakeAuthRepository`）。
- `@Before` で「前回の実データを消す」処理を書かない。テストごとにHiltコンポーネントが
  作り直されるため、各テストは必要な初期状態を**仕込む**側に書く。

## 理由
- テストを書く場所と道具が画面ごとにばらつくと、学習用テンプレートとしての価値が落ちる。
  どの画面を見ても同じ形のテストが並んでいることを優先する。
- 実機が要るテストが増えるほど手元で回らなくなる。Robolectricを挟むことで
  「UIテスト＝実機が必要」という前提を外し、`./gradlew test` に寄せられる。
- 計装テストが実ファイルの状態に依存すると、単体では通るのに連続実行で落ちる、
  という再現しにくい失敗を生む。差し替えの仕組みで構造的に断ち切る。
