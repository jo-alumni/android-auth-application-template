# 認証状態とナビゲーション

認証状態（ログイン済みかどうか）は、アプリのどこにいても変わり得る値でありながら、
「どの画面を見せるか」を直接左右する。この2つをどう結ぶかを1本に決めておかないと、
同じ遷移が複数の経路で起きて、どちらが正なのか読み取れないコードになる。

このドキュメントは本アプリが採った結び方（**状態駆動への一本化**）と、その理由を残す。

## 結論

認証状態がナビゲーションへ効く経路は次の1本だけにする。

| 場面 | 担当 |
| --- | --- |
| 起動時（初回の認証状態の解決） | `AppNavHost` の `startDestination` |
| 起動後に**未認証へ変わった**とき | `AuthApplicationApp` の `LaunchedEffect` → `AppState.navigateLogin()` |
| 起動後に**認証済みへ変わった**とき（ログイン成功） | ログイン画面（`LoginEvent.NavigateHome` → `navigateHome`） |

- `startDestination` は**初回の認証状態が解決したときの値で固定**し、以降は変化させない。
- ログアウト操作もトークン失効も、やることは「トークンを破棄する」だけ。
  ログイン画面へ戻す判断は `AppViewModel.authState` の変化を見る側が一元的に行う。
- そのため `AppViewModel` は遷移イベント（かつての `AppEvent.NavigateLogin`）を持たない。

```kotlin
// AuthApplicationApp.kt
val initialIsAuthenticated = remember { state.isAuthenticated }

LaunchedEffect(appState) {
    appViewModel.authState
        .filterIsInstance<AuthUiState.Ready>()
        .map { ready -> ready.isAuthenticated }
        .distinctUntilChanged()
        // 起動時の状態は startDestination が解決済みなので、そこからの「変化」だけを扱う
        .dropWhile { isAuthenticated -> isAuthenticated == initialIsAuthenticated }
        .collect { isAuthenticated -> if (!isAuthenticated) appState.navigateLogin() }
}

AppNavHost(
    startDestination = if (initialIsAuthenticated) MainGraphRoute else AuthGraphRoute,
    // ...
)
```

## 起動時はスプラッシュで認証状態の確定を待つ

`startDestination` は「初回の認証状態が解決したとき」の値で決まるため、それまでの間
`AuthApplicationApp` は `AuthUiState.Loading` にとどまる。ここで画面を出してしまうと、
起動のたびに「システムのスプラッシュ → 白背景＋ローディング → ホーム/ログイン」という
2段階のちらつきになる。

そこで `androidx.core:core-splashscreen` の `setKeepOnScreenCondition` を使い、
認証状態が確定するまでスプラッシュを維持する。

```kotlin
// MainActivity.kt
private val appViewModel: AppViewModel by viewModels()

override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen() // setContent より前に呼ぶ
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val splashScreenTimeoutAt = SystemClock.uptimeMillis() + SPLASH_SCREEN_TIMEOUT_MILLIS
    splashScreen.setKeepOnScreenCondition {
        appViewModel.authState.value is AuthUiState.Loading &&
            SystemClock.uptimeMillis() < splashScreenTimeoutAt
    }

    setContent { AuthApplicationTheme { AuthApplicationApp(appViewModel = appViewModel) } }
}
```

### テーマ

`MainActivity` には `Theme.SplashScreen` を継承した `Theme.AuthApplication.Splash` を指定し、
その `postSplashScreenTheme` に元の `Theme.AuthApplication` を指定する。
`installSplashScreen()` がスプラッシュを畳むときに `setTheme()` でこちらへ戻すので、
Activityの最終的なテーマは変わらない。
`windowSplashScreenBackground` は `Theme.AuthApplication` のウィンドウ背景（白）に合わせ、
スプラッシュから画面本体へ切り替わるときに背景色が動かないようにしている。

### `setContent` はスプラッシュの裏で先に済ませる

`setKeepOnScreenCondition` は `android.R.id.content` に `OnPreDrawListener` を足して
**描画だけ**を止める。測定・レイアウト・コンポジションは進むので、`setContent` は先に呼んでおく。

これは「ちらつかないように」以上の意味を持つ。`AppViewModel.authState` は
`SharingStarted.WhileSubscribed(5_000)` なので、**購読されている間だけ**上流（DataStore）を読む。
購読を始めるのはコンポジション側の `collectAsStateWithLifecycle` なので、
`setContent` を後回しにすると `authState.value` は `Loading` のまま動かず、スプラッシュも解けない。

### タイムアウトが「条件の中の時刻比較」で成立する理由

`OnPreDrawListener` が `false` を返すと `ViewRootImpl` は描画を飛ばして
トラバースをスケジュールし直す。つまりこの条件は**フレームごとに評価され直す**。
そのため `authState` が変化しなくても、時刻の比較だけで条件を外すことができる。

打ち切りを入れているのは、DataStoreのファイルが壊れている等で読み込みが終わらないときに
スプラッシュのまま操作不能になるのを避けるため。1秒を過ぎたら `AuthApplicationApp` の
`Loading`（`CircularProgressIndicator`）へ進む。通常の起動でこの表示が見えることはない。

### `AppViewModel` の取得口が2つあることについて

`MainActivity` の `by viewModels()` と `AuthApplicationApp` の既定引数 `hiltViewModel()` は、
ViewModelStoreOwner（このActivity）もキー（既定のクラス名）も同じなので**同じインスタンス**を返す。
とはいえ読み手にそれを推測させたくないので、`MainActivity` からは明示的に引数で渡している。
既定引数の `hiltViewModel()` は、`MainActivity` を介さず `AuthApplicationApp` を直接起動する
計装テスト（`AppNavigationTest`）のために残してある。

## 以前の実装の何が問題だったか

ログアウト時に2つの経路が同時に働いていた。

1. **イベント駆動**: `AppViewModel.logout()` が `AppEvent.NavigateLogin` を emit し、
   `AuthApplicationApp` がそれを受けて `navigate(AuthGraph) { popUpTo(MainGraph) { inclusive = true } }`
2. **状態駆動**: `authState` が `Ready(false)` に変わることで、`AppNavHost` へ渡す
   `startDestination` が `MainGraph` → `AuthGraph` へ変化する

`NavHost` は `startDestination` が変わるとグラフを作り直すため、2番目も実質的に
ナビゲーションへ影響し得る。どちらが実際に画面を切り替えているのかコードから読み取れず、
片方だけを直しても直った気になれない。学習用テンプレートとして最も避けたい状態だった。

### `startDestination` を変えたときの `NavHost` の挙動

`NavHost` は `startDestination`（と graph の内容）から `NavGraph` を組み立て、
値が変わると **新しいグラフを `navController.graph` に差し替える**。
差し替え時、`NavController` は現在のバックスタックを新しいグラフに対して復元しようとするが、
これは「ログアウトしたのでログイン画面へ戻す」という**意図した遷移ではなく、
グラフ再構築の副作用**でしかない。

- 遷移アニメーションや `popUpTo` の指定が効かない（`navigate` を経由しないため）
- どの画面がバックスタックから消えるかが `NavHost` の再構築ロジック依存になり、
  「MainGraph配下を確実に消す」といった保証をコードで表現できない

そのため `startDestination` は**入り口を決めるためだけ**に使い、
起動後の遷移は必ず `navigate()`（= `AppState.navigateLogin()`）で行う。

## なぜ状態駆動を選んだか（案Aの採用理由）

トークンは**ユーザーのログアウト操作以外でも失効する**。サーバ側での失効、有効期限切れ、
別端末からのパスワード変更などだ。イベント駆動（案B）に寄せると、
「失効を検知した箇所」がそれぞれ遷移イベントを emit する責任を負うことになり、
経路が増えるほど「ログイン画面へ戻し忘れる」箇所が生まれる。

状態駆動なら、**認証が解除された理由を問わず** `authState` が `Ready(false)` になる一点に
集約される。失効を検知した側は「トークンを破棄する」だけでよく、遷移を知らなくていい。

```
ログアウト操作 ─┐
トークン失効   ─┼→ トークンを破棄 → authState = Ready(false) → navigateLogin()
（将来）401応答 ─┘
```

### ログイン方向を状態駆動にしていない理由

逆方向（未認証 → 認証済み）は、ログイン画面が `LoginEvent.NavigateHome` を受けて
自分で遷移する。認証解除が「アプリのどの画面にいても外から起こり得ること」なのに対し、
ログイン成功は「ログイン画面という特定の画面での操作の結末」だからだ。
遷移を起こす主体を「その出来事を受け止める場所」に置くと、
アプリの骨組み（`:app`）と画面（feature）の責務が混ざらない。

なお、両方向を状態駆動にすると、ログイン成功時に
`LoginScreen` の `navigateHome` と骨組み側の遷移が二重に走る。
一本化の目的からしても、どちらか一方に決める必要がある。

## ログアウト時に「保存されたバックスタック」も破棄する

タブ切り替え（`AppState.navigateToTopLevelDestination()`）は
`popUpTo(startDestination) { saveState = true }` + `restoreState = true` を使うため、
**タブごとのバックスタックが NavController 内部に保存される**。

`popUpTo(MainGraph) { inclusive = true }` が消せるのは「いま積まれているバックスタック」だけで、
この保存済みの状態は残る。そのため popUpTo だけでは、
ログアウト → 再ログインの後にタブを開いた瞬間、前のセッションで開いていた画面
（例: 検索タブで開いていた詳細画面）が `restoreState` で復元されてしまう。

`AppState.navigateLogin()` は保存済みの状態も明示的に破棄する。

```kotlin
fun navigateLogin() {
    // clearBackStack は現在地から辿れるルートしか解決できないため、
    // 認証前のグラフへ移る前（まだ MainGraph にいるうち）に呼ぶ
    TopLevelDestination.entries.forEach { destination ->
        navController.clearBackStack(destination.route)
    }
    navController.navigate(AuthGraphRoute) {
        popUpTo(MainGraphRoute) { inclusive = true }
    }
}
```

`clearBackStack` を `navigate` の後に呼ぶと、現在地が `Login` になっているために
`Home` などのルートを解決できず `IllegalStateException` になる。順序が意味を持つ。

## トークン失効の動作確認

実際に失効し得る通信処理が無いため、TopAppBarのデバッグメニュー（工具アイコン）の
**「トークンを失効させる」** から手動で起こせるようにしている
（`AppViewModel.expireAuthToken()` → `ClearAuthTokenUseCase`）。

- ホーム/検索/お気に入りのどの画面から実行してもログイン画面へ戻る
- 詳細画面など `MainGraph` 配下の深い階層にいても同じ
- 戻るキーでログイン済みの画面へは戻れない（`popUpTo(MainGraph) { inclusive = true }`）

処理内容はログアウトと同じだが、「ユーザー操作以外でも認証は解除され得る」ことと、
その場合も同じ経路を通ることを確認できるよう、あえて別の入り口として用意している。

## テスト

`:app` の `AppNavigationTest`（計装テスト）で画面をまたぐ導線を担保する。

- `tokenExpirationNavigatesBackToLoginScreen`
  — UIを経由せず外部要因としてトークンを破棄しても、ログイン画面へ戻ること
- `logoutClearsMainGraphBackStack`
  — ログアウト後に `MainGraph` 配下のエントリが1つも残らず、戻り先も無いこと
- `reLoginAfterLogoutStartsFromHomeWithoutRestoringSavedState`
  — ログアウト前の検索キーワード（タブの `saveState` で保存される状態）が
    再ログイン後に復元されないこと

バックスタックの操作そのものは、`AppStateTest`（Robolectric・`./gradlew test` で回る）でも
検証している。

- `tab back stack is saved and restored while staying authenticated`
  — ログインしたままなら、タブの状態は保存・復元されること（保存が効いていることの確認）
- `navigateLogin discards the saved back stack of the tabs`
  — `navigateLogin()` を挟むと保存済みの状態が破棄されること
- `navigateLogin clears the authenticated screens from the back stack`
  — 認証後の画面がバックスタックに残らないこと
