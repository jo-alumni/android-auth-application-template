package com.example.authapplication.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** ボトムバー配下の画面(ホーム/検索/お気に入り)で共通利用するTopAppBar。ログアウト操作を提供する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
        actions = {
            TextButton(onClick = onLogoutClick) {
                Text("ログアウト")
            }
        },
        modifier = modifier,
    )
}
