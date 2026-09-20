package com.example.authapplication.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.authapplication.core.ui.preview.AppPreview

/**
 * 画面幅がMedium以上のときに、[AppBottomBar] の代わりに画面左端へ表示するナビゲーションレール。
 *
 * 受け取る引数は [AppBottomBar] と同じで、「今どのタブか」と「タブが選ばれた」だけを扱う。
 * どちらを表示するかの判定は [AppState.navigationType] が持つ。
 *
 * insetsはレール自身が解決する（`NavigationRail` の既定の `windowInsets` が
 * 左端・上下の system bars を含む）。docs/window-insets.md の「それを隠すUIを描いた側が
 * 解決する」という方針どおり、レールが覆う左端の余白はレールが引き受ける。
 */
@Composable
fun AppNavigationRail(
    currentDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            // ラベルはenumが文字列リソースIDで持つため、文言の解決はComposable側で行う。
            val label = stringResource(destination.labelResId)
            NavigationRailItem(
                selected = destination == currentDestination,
                onClick = { onDestinationClick(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AppNavigationRailPreview() {
    AppPreview {
        AppNavigationRail(
            currentDestination = TopLevelDestination.HOME,
            onDestinationClick = {},
        )
    }
}

/** レールは幅が固定なので、大フォントではラベルがアイコンからはみ出しやすい。 */
@PreviewFontScale
@Composable
private fun AppNavigationRailFontScalePreview() {
    AppPreview {
        AppNavigationRail(
            currentDestination = TopLevelDestination.FAVORITE,
            onDestinationClick = {},
        )
    }
}
