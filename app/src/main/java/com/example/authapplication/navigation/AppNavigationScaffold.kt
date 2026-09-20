package com.example.authapplication.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.authapplication.core.navigation.TopLevelDestination

/**
 * ナビゲーションUI（ボトムバー / レール / 常設ドロワー）と画面本体を組み合わせる骨組み。
 *
 * どのUIを使うかは [navigationType] として受け取るだけで、この関数は判定を行わない
 * （判定は [AppState.navigationType]）。そのためプレビューもテストも、`AppState` や
 * ViewModelを用意せずに種類を直接渡して行える。
 *
 * 種類が変わっても [content] の呼び出し位置（`Row` の中の `Scaffold`）は変えない。
 * 呼び出し位置が変わるとComposeはその部分木を作り直すため、画面側の `rememberSaveable` や
 * スクロール位置が、ウィンドウのリサイズや折りたたみ端末の開閉で失われてしまう。
 *
 * insetsの責務分担は docs/window-insets.md を参照。ここで解決するのは
 * 「自分が描いた上端（ステータスバー / [topBar]）」と「レール/ドロワーが覆う左端」だけで、
 * 下端（ナビゲーションバー）とIMEは各画面が自分で解決する。
 *
 * @param bottomBarModifier ボトムバーに適用する `Modifier`。スクロール追従で隠すための
 *   オフセットなど、[BottomBarScrollBehavior] の都合をここから渡す。
 * @param snackbarHostModifier `SnackbarHost` に適用する `Modifier`。Snackbarはボトムバーの
 *   **実測高**の分だけ持ち上げて配置されるが、`Modifier.offset` で隠したバーの高さは縮まないため、
 *   バーと同じオフセットをここにも渡して追従させる。
 * @param snackbarHostState Snackbarの表示に使う状態。アプリ本体からは
 *   `LocalSnackBarHostState` の値を渡す（CompositionLocalをここで直接読まないのは、
 *   このComposableをProvider無しでプレビュー・テストできるようにするため）。
 */
@Composable
fun AppNavigationScaffold(
    navigationType: AppNavigationType,
    currentDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    bottomBarModifier: Modifier = Modifier,
    snackbarHostModifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        // elseを書かない（種類を追加したときにコンパイルエラーで気付ける）。
        when (navigationType) {
            AppNavigationType.NONE, AppNavigationType.BOTTOM_BAR -> Unit

            AppNavigationType.NAVIGATION_RAIL -> AppNavigationRail(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
            )

            AppNavigationType.PERMANENT_DRAWER -> AppNavigationDrawerSheet(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
            )
        }

        // 左端をレール/ドロワーが覆っているときは、その分を下流のWindowInsetsから差し引く。
        val startInsets = when (navigationType) {
            AppNavigationType.NONE, AppNavigationType.BOTTOM_BAR -> WindowInsets(0, 0, 0, 0)
            AppNavigationType.NAVIGATION_RAIL,
            AppNavigationType.PERMANENT_DRAWER,
            -> WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .consumeWindowInsets(startInsets),
            topBar = topBar,
            bottomBar = {
                if (navigationType == AppNavigationType.BOTTOM_BAR) {
                    AppBottomBar(
                        currentDestination = currentDestination,
                        onDestinationClick = onDestinationClick,
                        modifier = bottomBarModifier,
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState, modifier = snackbarHostModifier)
            },
        ) { innerPadding ->
            // 上端のinsetsだけを解決してconsumeし、下端は解決しない。ボトムバーはスクロールに
            // 追従して隠れるため、その分の余白をここで一律に確保すると、隠れたときに空白が残る。
            val topPadding = PaddingValues(top = innerPadding.calculateTopPadding())
            Box(
                modifier = Modifier
                    .padding(topPadding)
                    // 適用済みの余白を下流のWindowInsetsから差し引き、各画面での二重適用を防ぐ。
                    .consumeWindowInsets(topPadding),
            ) {
                content()
            }
        }
    }
}

/**
 * 端末サイズごとの表示を確認するプレビュー。`currentWindowAdaptiveInfoV2()` は
 * プレビュー時にはプレビューのウィンドウサイズを返すため、実機と同じ判定で
 * ボトムバー / レール / 常設ドロワーが切り替わる。
 */
@PreviewScreenSizes
@Composable
private fun AppNavigationScaffoldPreview() {
    AppNavigationScaffold(
        navigationType = AppNavigationType.of(
            windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass,
            isTopLevelDestination = true,
        ),
        currentDestination = TopLevelDestination.HOME,
        onDestinationClick = {},
        topBar = { Text(text = "ホーム") },
    ) {
        Text(text = "画面の中身", modifier = Modifier.fillMaxSize())
    }
}
