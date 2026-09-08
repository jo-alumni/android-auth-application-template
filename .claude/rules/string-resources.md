---
description: 表示文字列はKotlinにハードコードせずモジュールごとのstrings.xmlへ置き、リソース名はresourcePrefixで衝突を防ぐ。ViewModelは文言ではなく文字列リソースIDを公開する
globs:
  - "app/**/src/main/java/**/*.kt"
  - "app/**/src/main/res/values/strings.xml"
  - "domain/src/main/kotlin/**/error/*.kt"
alwaysApply: false
---

# 文字列リソースとモジュールごとのリソース戦略

## 方針

### 1. 表示文字列はすべて文字列リソースにする
- Composable に表示する文言・`contentDescription` は Kotlin にハードコードせず、
  `stringResource(R.string.xxx)` で参照する。
- 例外は **Preview のダミーデータ**（`Item(title = "アイテム1")` など）と、
  リポジトリが返す**データそのもの**（`:data` のモックデータ）。
  これらは「アプリの文言」ではなく「表示する値」なのでリソース化しない。

### 2. 文言はそれを使うモジュールが持つ
- 画面固有の文言は、その画面を持つ **featureモジュール** の
  `src/main/res/values/strings.xml` に置く。
- 「戻る」「閉じる」「キャンセル」「再読み込み」のように画面に依存しない共通の文言だけを
  **`:app:core`** に置く。featureモジュールからは `com.example.authapplication.core.R`
  （`import com.example.authapplication.core.R as CoreR`）で参照する。
- 同じ文言でも「どの画面の文言か」で意味が変わるもの（例: 一覧の取得失敗メッセージ）は、
  共通化せず各featureに置く。将来1画面だけ文言を変えたくなったときに、
  他画面へ影響させずに変更できる。

### 3. リソース名の衝突は resourcePrefix で機械的に防ぐ
- マルチモジュールではリソースがビルド時にマージされるため、同名リソースは上書きされる。
  `AndroidLibraryConventionPlugin` がGradleパスから接頭辞を決めて
  `android.resourcePrefix` に設定しているので、各モジュールのリソース名は必ず接頭辞で始める。
  - `:app:core` → `core_`
  - `:app:feature:home` → `feature_home_`
- 接頭辞に従っていないリソースは Lint の `ResourceName` がエラーにする
  （`./gradlew lint` で検出できる）。モジュールを追加しても接頭辞の設定は不要。

### 4. ViewModel は「文言」ではなく「文字列リソースID」を公開する
- `XxxUiState.Error` / `XxxEvent.ShowErrorSnackbar` が持つのは
  `message: String` ではなく `@param:StringRes messageResId: Int`。
  文言の解決は `stringResource(uiState.messageResId)` のようにUI側で行う。
- Composable に渡す前に文言へ変換しないことで、Configuration変更（言語切り替え）に追従できる。
  Navigationファイルでイベントを受けてSnackbarに出す場合も `LocalContext` ではなく
  `LocalResources.current` から解決する（Lintの `LocalContextGetResourceValueCall` 参照）。
- `TopLevelDestination` のようにComposeの外にある定義も、文言ではなく `labelResId` を持つ。

### 5. `:domain` は文言を持たない
- `:domain` はAndroidに依存しないKotlinモジュールなので文字列リソースを参照できない。
  リポジトリ層の失敗は「文言」ではなく `AppError`（失敗の種別）で表し、
  `AppDataException(AppError.ITEM_LOAD)` のように投げる。
- 種別からリソースIDへの変換は、文言を持つfeatureモジュール側の
  `Throwable.toMessageResId()` で行う。その画面で起こり得ない種別は
  `CoreR.string.core_error_unexpected` にフォールバックさせる。

## 理由
- 文言がKotlinに散らばっていると、多言語対応・文言の一括見直し・アクセシビリティ対応の
  いずれもコードの書き換えになる。リソース化しておけば `values-en/strings.xml` を足すだけで済む。
- 「どのモジュールに置くか」を決めておかないと、共通化したい誘惑で `:app:core` に
  画面固有の文言が集まり、featureモジュールを分けた意味が薄れる。
- ViewModelが文言を持つと、テストが日本語のリテラル比較になり、言語切り替えにも追従できない。
  リソースIDなら「どの文言を出すか」という判断だけをユニットテストで検証できる。

## Before / After

```kotlin
// Bad: 文言がKotlinに埋まっていて、ViewModelも文言そのものを公開している
sealed interface HomeUiState {
    data class Error(val message: String) : HomeUiState
}

.catch { emit(HomeUiState.Error("アイテムの取得に失敗しました")) }

@Composable
fun HomeScreen(uiState: HomeUiState) {
    Text(text = "アイテムがありません")
}
```

```kotlin
// Good: ViewModelはリソースIDを公開し、文言の解決はUI側で行う
sealed interface HomeUiState {
    data class Error(@param:StringRes val messageResId: Int) : HomeUiState
}

.catch { throwable -> emit(HomeUiState.Error(throwable.toMessageResId())) }

/** 失敗の種別をこの画面で表示する文言のリソースIDへ変換する。 */
@StringRes
private fun Throwable.toMessageResId(): Int = when (toAppError()) {
    AppError.ITEM_LOAD -> R.string.feature_home_error_item_load
    AppError.FAVORITE_TOGGLE -> R.string.feature_home_error_favorite_toggle
    AppError.NOTIFICATION_LOAD, AppError.UNEXPECTED -> CoreR.string.core_error_unexpected
}

@Composable
fun HomeScreen(uiState: HomeUiState) {
    Text(text = stringResource(R.string.feature_home_empty))
}
```

```xml
<!-- app/feature/home/src/main/res/values/strings.xml -->
<resources>
    <string name="feature_home_empty">アイテムがありません</string>
    <string name="feature_home_error_item_load">アイテムの取得に失敗しました</string>
</resources>
```
