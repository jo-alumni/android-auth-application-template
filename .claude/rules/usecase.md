---
description: ViewModelはRepositoryを直接注入せず、必ず:domainのUseCaseを経由する。1行の委譲になるUseCaseも省略しない
globs:
  - "**/*ViewModel.kt"
  - "domain/src/main/kotlin/**/*UseCase.kt"
  - "domain/src/main/kotlin/**/*Repository.kt"
alwaysApply: false
---

# ViewModel からのデータアクセスは必ず UseCase を経由する

## 方針

### 1. ViewModel は Repository を直接注入しない
- `@HiltViewModel` のコンストラクタが受け取るのは **UseCase だけ**にする。
  `ItemRepository` / `NotificationRepository` のようなリポジトリインターフェースを
  ViewModel へ注入しない。
- リポジトリの1メソッドを呼ぶだけで、UseCase が実質1行の委譲になる場合も**省略しない**
  （`GetItemUseCase`、`ObserveNotificationsUseCase`、`ToggleFavoriteUseCase` など）。
  「ロジックがあるときだけ UseCase を作る」という基準は採らない。
- 逆に、リポジトリを直接呼んでよいのは **`:domain` の UseCase だけ**。
  `:app` / feature モジュールからリポジトリインターフェースを参照しない。

### 2. UseCase は `:domain` に置き、機能ごとのパッケージに分ける
- 置き場所は `domain/src/main/kotlin/com/example/authapplication/domain/<機能>/`。
  リポジトリインターフェースとモデルと同じパッケージに並べる
  （例: `domain/item/` に `Item` / `ItemRepository` / `ObserveItemsUseCase` / `GetItemUseCase`）。
- クラス名は「動詞 + 対象 + `UseCase`」。Flow を返すものは `Observe`、
  1回で完了する取得は `Get`、更新は `Set` / `Clear` / `Toggle` を使う。
  - `ObserveItemsUseCase` / `ObserveNotificationsUseCase` / `GetItemUseCase`
  - `SetAuthTokenUseCase` / `ClearAuthTokenUseCase` / `ToggleFavoriteUseCase`
- 呼び出しは `operator fun invoke()` で定義し、`observeItemsUseCase()` のように関数として呼ぶ。
  UseCase は状態を持たず、公開するのは `invoke` 1つだけにする。
- KDoc で「〜するユースケース。」と一行説明を付ける。

### 3. 複数リポジトリの結合・絞り込みは UseCase 側に置く
- 複数のリポジトリを `combine` する、他の UseCase の結果を `map` で絞り込む、といった
  組み立ては UseCase が持ち、ViewModel には持ち込まない。
  ViewModel の責務は「UseCase の Flow を `XxxUiState` へ変換すること」に限る。
  - `ObserveItemsUseCase` — `ItemRepository` と `FavoriteRepository` を `combine` して
    `Item.isFavorite` を埋める
  - `ObserveFavoriteItemsUseCase` — `ObserveItemsUseCase` の結果を `filter` で絞り込む
- UseCase が他の UseCase に依存してよい（`ObserveFavoriteItemsUseCase` → `ObserveItemsUseCase`）。
  同じ組み立てを2か所で書かないための第一手段にする。

### 4. テストは「ロジックを持つ UseCase」だけに書く
- `combine` / `filter` などの組み立てを持つ UseCase は `domain/src/test` にテストを書く
  （`ObserveItemsUseCaseTest` / `ObserveFavoriteItemsUseCaseTest`）。
- 1行の委譲しかしない UseCase に単体テストは書かない。検証する振る舞いが無く、
  リポジトリのFakeを呼び直すだけのテストになるため。
  その UseCase は ViewModel のテストから Fake リポジトリを包んで使う
  （`DetailViewModel(savedStateHandle, GetItemUseCase(itemRepository))`）。

## 理由
- 学習用テンプレートの価値は「どの画面を見ても同じ型・同じ書き方になっている」こと。
  「ロジックがあるときだけ UseCase」という基準にすると、画面を追加するたびに
  “これはロジックか”という判断が要り、読み手はどちらが本プロジェクトの流儀か決められない。
  例外を作らないことで、新しい画面は既存の画面をそのまま模写すれば書ける。
- 依存の向きが「ViewModel → UseCase → Repository」に固定されるため、
  ViewModel のコンストラクタを見れば、その画面が何をするのかが操作名で並ぶ。
- 「1行の委譲だったものにロジックが増える」ときに、ViewModel と feature モジュールを
  触らずに UseCase の中だけで対応できる。実際 `ObserveItemsUseCase` は
  お気に入り機能の追加で `ItemRepository` の単純な委譲から `combine` へ育っている。

## Before / After

```kotlin
// Bad: ViewModel がリポジトリを直接注入している
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = notificationRepository.observeNotifications()
        .map { /* ... */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationUiState.Loading)
}
```

```kotlin
// Good: 1行の委譲でも UseCase を挟み、ViewModel は UseCase だけを受け取る
/** 通知一覧を監視するユースケース。 */
class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<List<Notification>> = repository.observeNotifications()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val observeNotificationsUseCase: ObserveNotificationsUseCase,
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = observeNotificationsUseCase()
        .map { /* ... */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationUiState.Loading)
}
```

```kotlin
// Bad: 複数リポジトリの結合が ViewModel に漏れている
@HiltViewModel
class HomeViewModel @Inject constructor(
    itemRepository: ItemRepository,
    favoriteRepository: FavoriteRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        itemRepository.observeItems(),
        favoriteRepository.observeFavoriteIds(),
    ) { items, favoriteIds ->
        items.map { it.copy(isFavorite = it.id in favoriteIds) }
    }.map { /* ... */ }
}
```

```kotlin
// Good: 結合は UseCase が持ち、ViewModel は UiState への変換だけを行う
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeItemsUseCase: ObserveItemsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeItemsUseCase()
        .map { items -> if (items.isEmpty()) HomeUiState.Empty else HomeUiState.Success(items) }
}
```

## 新しい画面を追加するときの手順
1. 必要なデータの取得・更新を `:domain` の UseCase として定義する（無ければ追加する）。
2. ViewModel のコンストラクタで、その UseCase だけを受け取る。
3. UseCase の Flow を `XxxUiState` へ変換する（[viewmodel-uistate.md](viewmodel-uistate.md)）。
