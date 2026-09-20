package com.example.authapplication

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.authapplication.core.theme.AuthApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * スプラッシュを維持する上限時間。
 *
 * 永続化層の読み込みが終わらない（DataStoreのファイルが壊れている等）ときに
 * スプラッシュのまま操作不能になるのを避けるための保険。
 * ここを過ぎたらスプラッシュを畳み、[AuthApplicationApp] のローディング表示へ進む。
 */
private const val SPLASH_SCREEN_TIMEOUT_MILLIS = 1_000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * [AuthApplicationApp] が `hiltViewModel()` で取得するものと同じインスタンス。
     * どちらも ViewModelStoreOwner がこのActivityで、キーも既定（クラス名）のため一致する。
     * 二重に生成されないことを明示するため、Composeへは引数で渡す。
     */
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // setContentView / setContent より前に呼ぶ必要がある。
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 認証状態が確定するまでスプラッシュを維持し、
        // 「システムのスプラッシュ → ローディング → 目的の画面」の2段階のちらつきを無くす
        // （docs/auth-navigation.md 参照）。
        // 判定は描画のたびに呼ばれるため、時間経過だけで条件が外れるこの書き方で打ち切りも効く。
        val splashScreenTimeoutAt = SystemClock.uptimeMillis() + SPLASH_SCREEN_TIMEOUT_MILLIS
        splashScreen.setKeepOnScreenCondition {
            appViewModel.authState.value is AuthUiState.Loading &&
                SystemClock.uptimeMillis() < splashScreenTimeoutAt
        }

        // スプラッシュが描画を止めている間もコンポジションは進むので、setContent は先に済ませておく。
        // authState は WhileSubscribed で購読中だけ上流を読むため、
        // ここでコンポジションが購読を始めないと Loading のまま解決しない。
        setContent {
            AuthApplicationTheme {
                AuthApplicationApp(appViewModel = appViewModel)
            }
        }
    }
}
