---
description: リポジトリ層の例外はFlow.catchでUiState.Errorへ変換し、リトライは購読をやり直すことで実現する。一覧を保ったまま伝えたい失敗はSharedFlowイベント経由でSnackbarに出す
globs:
  - "**/*ViewModel.kt"
  - "app/feature/*/src/main/java/**/*Screen.kt"
  - "data/src/main/java/**/*RepositoryImpl.kt"
alwaysApply: false
---

# エラー処理とリトライ

## 方針

### 1. 例外はUiStateに変換してからUIへ渡す
- リポジトリ層は失敗を `AppDataException(userMessage)` として投げる。
  `userMessage` はそのまま画面に出せる日本語にする。
- ViewModelは `Flow.catch { }` で例外を捕まえ、`XxxUiState.Error(message)` へ変換する。
  変換には `Throwable.toUserMessage()` を使い、想定外の例外も必ず文言にフォールバックさせる。
- Composableには例外を伝播させない。Composableは `when(uiState)` で `Error` を描くだけにする。

### 2. `Error` は各UiStateの基本形に含める
- 画面状態を取得するFlowが失敗し得るなら、`XxxUiState` に
  `data class Error(val message: String)` を必ず用意する。
- 「取得に失敗した」`Error` と「取得できたが対象が無い」`Empty` / `NotFound` は
  ユーザーへの説明が変わるため、別の状態として区別する。

### 3. リトライは「購読のやり直し」で実装する
- 例外で異常終了したFlowは、そのままでは再開しない。
  `MutableStateFlow<Int>` のトリガーを `flatMapLatest` の上流に置き、
  値を進めることで下流のFlowを購読し直す。
- リトライ直後にローディングへ戻すため、`catch` の手前に
  `onStart { emit(XxxUiState.Loading) }` を置く。
- UIには `ErrorContent(message, onRetryClick)`(:app:core)を使い、
  どの画面でも同じ見た目・同じ操作で再読み込みできるようにする。

### 4. 画面はそのままで伝えたい失敗はSnackbarイベントにする
- お気に入りのトグルなど「一覧の表示は保ったまま、操作だけが失敗した」ケースは
  `XxxUiState.Error` にしない。画面全体がエラー表示に置き換わってしまう。
- `sealed interface XxxEvent` に `ShowErrorSnackbar(message)` を定義し、
  `SharedFlow` で emit する([viewmodel-event-handling.md](viewmodel-event-handling.md) の方式)。
  Navigationファイル側で `LaunchedEffect` で collect し、`SnackbarHostState.showSnackbar` を呼ぶ。

## 理由
- 例外をUiStateへ変換する境界をViewModelに固定すると、「どこで落ちるか分からない」状態が無くなり、
  失敗時の表示をユニットテストで検証できる。
- リトライを「購読のやり直し」として表現すると、成功時のFlowの組み立て方を変えずに済む。
- 「画面全体を差し替える失敗」と「その場限りの失敗」を状態とイベントで書き分けることで、
  UiStateとSharedFlowの使い分けの基準がコード上に現れる。

## Before / After

```kotlin
// Bad: 例外がそのまま伝播してクラッシュし、リトライ手段も無い
val uiState: StateFlow<HomeUiState> = observeItemsUseCase()
    .map { items -> if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Success(items) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

fun toggleFavorite(itemId: String) {
    // 失敗すると viewModelScope ごと落ちる
    viewModelScope.launch { toggleFavoriteUseCase(itemId) }
}
```

```kotlin
// Good: catchでUiStateへ変換し、trigger + flatMapLatest でリトライできるようにする
private val retryTrigger = MutableStateFlow(0)

private val _event = MutableSharedFlow<HomeEvent>()
val event: SharedFlow<HomeEvent> = _event.asSharedFlow()

val uiState: StateFlow<HomeUiState> = retryTrigger
    .flatMapLatest {
        observeItemsUseCase()
            .map { items -> if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Success(items) }
            .onStart { emit(HomeUiState.Loading) }
            .catch { throwable -> emit(HomeUiState.Error(throwable.toUserMessage())) }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

fun retry() {
    retryTrigger.update { it + 1 }
}

// 一覧の表示は保ったまま失敗だけを伝えるので、状態ではなくイベントにする
fun toggleFavorite(itemId: String) {
    viewModelScope.launch {
        runCatching { toggleFavoriteUseCase(itemId) }
            .onFailure { _event.emit(HomeEvent.ShowErrorSnackbar(it.toUserMessage())) }
    }
}
```

## 動作確認の方法
実際に失敗する通信処理が無いため、TopAppBarのデバッグメニュー(工具アイコン)の
「エラーを発生させる」スイッチでリポジトリ層に例外を注入できる。

- スイッチON → タブを切り替える等で一覧を購読し直す → エラー表示になる
- スイッチONのまま「再読み込み」 → エラーのまま
- スイッチOFF → 「再読み込み」 → 一覧が表示される
- 一覧の表示中にスイッチON → お気に入りをトグル → Snackbarでエラーが表示され、一覧はそのまま
