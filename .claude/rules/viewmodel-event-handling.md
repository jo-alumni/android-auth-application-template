---
description: ViewModelからUIへの単発イベント(画面遷移など)はコールバック引数ではなくSharedFlowによるイベント配信で実現する
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

## 理由
- コールバックを引数で渡す方式は、ViewModelの関数シグネチャにUI側の関心事(遷移先や後続処理)が
  漏れ出し、ViewModelとUIの責務が混ざる。
- `SharedFlow` によるイベント配信にすることで、ViewModelは「何が起きたか」を発行するだけになり、
  UI側がその解釈(どう遷移するか)を一元的に担当できる。

## Before / After

```kotlin
// Bad: コールバックを引数として受け取り、ViewModel内で直接呼び出す
class AppViewModel : ViewModel() {
    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            setAuthenticatedUseCase(false)
            onSuccess()
        }
    }
}

// 呼び出し側
appViewModel.logout(onSuccess = {
    navController.navigate(AppRoute.Login) { popUpTo(0) { inclusive = true } }
})
```

```kotlin
// Good: SharedFlowでイベントを発行し、UI側でcollectしてハンドリングする
sealed interface AppEvent {
    data object NavigateLogin : AppEvent
}

class AppViewModel : ViewModel() {
    private val _event = MutableSharedFlow<AppEvent>()
    val event: SharedFlow<AppEvent> = _event.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            setAuthenticatedUseCase(false)
            _event.emit(AppEvent.NavigateLogin)
        }
    }
}

// 呼び出し側
LaunchedEffect(navController) {
    appViewModel.event.collect { event ->
        when (event) {
            AppEvent.NavigateLogin -> {
                navController.navigate(AppRoute.Login) { popUpTo(0) { inclusive = true } }
            }
        }
    }
}

appViewModel.logout()
```
