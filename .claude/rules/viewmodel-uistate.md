---
description: ViewModelがUIに公開する状態はLoading/Empty/Successを基本形とするsealed interface XxxUiStateで表現する
globs:
  - "**/*ViewModel.kt"
  - "app/feature/*/src/main/java/**/*Screen.kt"
alwaysApply: false
---

# UiState は sealed interface で表現する

## 方針
- ViewModel が UI へ公開する「画面の表示状態」は、必ず `sealed interface XxxUiState` で表現する。
  `StateFlow<List<Item>>` や `StateFlow<String>` のような生のコレクション/プリミティブを
  画面状態として公開しない。
- 定義位置は **対象 ViewModel と同じファイルの先頭**(`@HiltViewModel` クラスの上)とし、
  KDoc で「◯◯画面の表示状態。」と一行説明を付ける。
- 基本形は `Loading` / `Empty` / `Success(data)` の3状態。値を持たない状態は `data object`、
  値を持つ状態は `data class` で表す。
- 画面固有の状態は基本形に**追加**してよい(例: `DetailUiState.NotFound`、
  `SearchUiState.NoResults(query)`)。「データが0件」と「絞り込み結果が0件」のように
  ユーザーへの説明が変わるものは、別の状態として区別する。
- 「空かどうか」「絞り込み結果がどうか」の判定は **ViewModel 側**で行い、Composable では判定しない。
  Composable は `uiState` を受け取り `when` で網羅する(`else` を書かず、状態の追加漏れを
  コンパイルエラーで検出できる状態を保つ)。
- `stateIn` の指定は全画面で揃える。`initialValue` は `Loading`、
  `started` は `SharingStarted.WhileSubscribed(5_000)`。

## 理由
- 学習用テンプレートの価値は「どの画面を見ても同じ型・同じ書き方になっている」こと。
  画面ごとに公開する型が違うと、読み手はどれが本プロジェクトの流儀なのか判断できない。
- 生の `List` を流すと `emptyList()` が「読み込み中」なのか「本当に0件」なのか区別できず、
  ローディング表示と空状態表示を作り分けられない。
- 状態の判定を ViewModel に寄せることで、状態遷移をユニットテストで検証できる。
  Composable 側は「渡された状態を描くだけ」になり、Preview も状態ごとに用意できる。

## Before / After

```kotlin
// Bad: 生のListを公開しているため、読み込み中と0件を区別できない
@HiltViewModel
class FavoriteViewModel @Inject constructor(
    itemRepository: ItemRepository,
) : ViewModel() {

    val items: StateFlow<List<Item>> = itemRepository.observeItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}

@Composable
fun FavoriteScreen(items: List<Item>, onItemClick: (String) -> Unit) {
    // 空判定がUI側に散らばる。読み込み中は表現できない
    if (items.isEmpty()) Text("お気に入りがありません") else LazyColumn { /* ... */ }
}
```

```kotlin
// Good: sealed interface で状態を表現し、判定はViewModelに寄せる
/** お気に入り画面の表示状態。 */
sealed interface FavoriteUiState {
    data object Loading : FavoriteUiState
    data object Empty : FavoriteUiState
    data class Success(val items: List<Item>) : FavoriteUiState
}

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    itemRepository: ItemRepository,
) : ViewModel() {

    val uiState: StateFlow<FavoriteUiState> = itemRepository.observeItems()
        .map { items ->
            if (items.isEmpty()) FavoriteUiState.Empty else FavoriteUiState.Success(items)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoriteUiState.Loading,
        )
}

@Composable
fun FavoriteScreen(uiState: FavoriteUiState, onItemClick: (String) -> Unit) {
    when (uiState) { // elseを書かない(状態追加時にコンパイルエラーで気付ける)
        FavoriteUiState.Loading -> CircularProgressIndicator()
        FavoriteUiState.Empty -> Text("お気に入りがありません")
        is FavoriteUiState.Success -> LazyColumn { /* uiState.items を描画 */ }
    }
}
```
