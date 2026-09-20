package com.example.authapplication.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

/**
 * ホーム/検索/お気に入りを切り替えるボトムバー。
 *
 * NavControllerは受け取らず、「今どのタブか」([currentDestination]) と
 * 「タブが選ばれた」([onDestinationClick]) だけを扱う。遷移の実処理は [AppState] が持つ。
 */
@Composable
fun AppBottomBar(
    currentDestination: TopLevelDestination?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            // ラベルはenumが文字列リソースIDで持つため、文言の解決はComposable側で行う。
            val label = stringResource(destination.labelResId)
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onDestinationClick(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    AppBottomBar(
        currentDestination = TopLevelDestination.HOME,
        onDestinationClick = {},
    )
}
