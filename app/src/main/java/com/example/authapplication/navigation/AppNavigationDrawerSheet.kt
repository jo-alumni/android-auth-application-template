package com.example.authapplication.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

/**
 * 画面幅がExpandedのときに、画面左端へ常設するナビゲーションドロワー。
 *
 * 受け取る引数は [AppBottomBar] / [AppNavigationRail] と同じで、「今どのタブか」と
 * 「タブが選ばれた」だけを扱う。どれを表示するかの判定は [AppState.navigationType] が持つ。
 *
 * `PermanentNavigationDrawer` ではなく `PermanentDrawerSheet` を直接使うのは、
 * 画面本体との並びを [AppNavigationScaffold] の `Row` 1か所に集約するため
 * （`PermanentNavigationDrawer` の中身は本文と並べる `Row` でしかない）。
 */
@Composable
fun AppNavigationDrawerSheet(
    currentDestination: TopLevelDestination?,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    PermanentDrawerSheet(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            // ラベルはenumが文字列リソースIDで持つため、文言の解決はComposable側で行う。
            val label = stringResource(destination.labelResId)
            NavigationDrawerItem(
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = label) },
                label = { Text(label) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 600)
@Composable
private fun AppNavigationDrawerSheetPreview() {
    AppNavigationDrawerSheet(
        currentDestination = TopLevelDestination.HOME,
        onDestinationSelected = {},
    )
}
