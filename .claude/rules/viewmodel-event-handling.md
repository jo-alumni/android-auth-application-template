---
description: ViewModelからUIへの単発イベント(画面遷移など)はコールバック引数ではなくSharedFlowによるイベント配信で実現する。ただし認証状態のようにStateFlowの状態がすでに決めている遷移は状態駆動に任せ、イベントを重ねない
globs:
  - "**/*ViewModel.kt"
alwaysApply: false
---

# ViewModel → UI の単発イベントは SharedFlow で実現する

## 方針
- 画面遷移・Snackbar表示・トースト等、ViewModelからUI側へ通知する「一度きりのイベント」は、
  関数の引数にコールバック(`onSuccess: () -> Unit` 等)を渡す方式では実装しない。
- 代わりに ViewModel が `sealed interface XxxEvent` を定義し、`MutableSharedFlow<XxxEvent>` で
  emit、UI(Composable)側は `LaunchedEffect` 内で `collect` してハンドリングする。
- 状態(継続的に参照される値)は引き続き `StateFlow` を使う。「状態」ではなく「出来事」を表すものは
  `SharedFlow` によるイベントとして扱う。
- **すでに `StateFlow` の状態が同じ遷移を決めている場合は、重ねてイベントにしない。**
  認証状態(`AppViewModel.authState`)のように「継続的に参照される値」が遷移先を決めるものは、
  状態の変化を購読して遷移する(状態駆動)。同じ遷移をイベントでも起こすと経路が2本になり、
  どちらが正なのか読み取れなくなる(判断の経緯は [docs/auth-navigation.md](../../docs/auth-navigation.md))。

## 理由
- コールバックを引数で渡す方式は、ViewModelの関数シグネチャにUI側の関心事(遷移先や後続処理)が
  漏れ出し、ViewModelとUIの責務が混ざる。
- `SharedFlow` によるイベント配信にすることで、ViewModelは「何が起きたか」を発行するだけになり、
  UI側がその解釈(どう遷移するか)を一元的に担当できる。
- 一方で、状態が決める遷移までイベントにすると同じ遷移が複数経路で起きる。
  「状態が決めるものは状態駆動」「その場限りの出来事だけをイベント」と線を引くことで、
  どちらの仕組みを使うかを迷わず選べる。

## Before / After

```kotlin
// Bad: コールバックを引数として受け取り、ViewModel内で直接呼び出す
class LoginViewModel : ViewModel() {
    fun login(id: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            setAuthTokenUseCase(token)
            onSuccess()
        }
    }
}

// 呼び出し側
viewModel.login(id, password, onSuccess = { navigateHome() })
```

```kotlin
// Good: SharedFlowでイベントを発行し、UI側でcollectしてハンドリングする
sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
}

class LoginViewModel : ViewModel() {
    private val _event = MutableSharedFlow<LoginEvent>()
    val event: SharedFlow<LoginEvent> = _event.asSharedFlow()

    fun login(id: String, password: String) {
        viewModelScope.launch {
            setAuthTokenUseCase(token)
            _event.emit(LoginEvent.NavigateHome)
        }
    }
}

// 呼び出し側(LoginNavigation.kt)
LaunchedEffect(viewModel) {
    viewModel.event.collect { event ->
        when (event) {
            LoginEvent.NavigateHome -> navigateHome()
        }
    }
}
```

```kotlin
// Bad: 状態(authState)がすでに決めている遷移を、イベントでも起こしてしまう
class AppViewModel : ViewModel() {
    val authState: StateFlow<AuthUiState> = /* ... */

    fun logout() {
        viewModelScope.launch {
            clearAuthTokenUseCase()
            _event.emit(AppEvent.NavigateLogin) // authStateの変化と経路が二重になる
        }
    }
}
```

```kotlin
// Good: 状態が決める遷移は状態駆動に任せ、ViewModelは状態を変えるだけにする
class AppViewModel : ViewModel() {
    val authState: StateFlow<AuthUiState> = /* ... */

    fun logout() {
        viewModelScope.launch { clearAuthTokenUseCase() }
    }
}

// 呼び出し側(AuthApplicationApp.kt) — ログアウトもトークン失効も同じ経路を通る
LaunchedEffect(appState) {
    appViewModel.authState
        .filterIsInstance<AuthUiState.Ready>()
        .map { ready -> ready.isAuthenticated }
        .distinctUntilChanged()
        .dropWhile { isAuthenticated -> isAuthenticated == initialIsAuthenticated }
        .collect { isAuthenticated -> if (!isAuthenticated) appState.navigateLogin() }
}
```
