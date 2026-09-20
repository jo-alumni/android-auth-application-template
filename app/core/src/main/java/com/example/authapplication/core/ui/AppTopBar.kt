package com.example.authapplication.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.R
import com.example.authapplication.core.ui.preview.AppPreview

/**
 * ボトムバー配下の画面(ホーム/検索/お気に入り)で共通利用するTopAppBar。
 * 通知画面への遷移とログアウト操作、および失敗系・トークン失効の動作確認用のデバッグメニューを提供する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    isErrorInjectionEnabled: Boolean,
    onErrorInjectionChange: (Boolean) -> Unit,
    onExpireTokenClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDebugMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },
        actions = {
            // このアプリには実際に失敗する通信処理が無いため、エラー表示とリトライを手元で
            // 確認できるようにデバッグメニューからエラーを注入できるようにしている。
            IconButton(onClick = { showDebugMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = stringResource(R.string.core_debug_menu),
                )
            }
            DropdownMenu(expanded = showDebugMenu, onDismissRequest = { showDebugMenu = false }) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.core_inject_error))
                            Switch(
                                checked = isErrorInjectionEnabled,
                                onCheckedChange = onErrorInjectionChange,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    },
                    onClick = { onErrorInjectionChange(!isErrorInjectionEnabled) },
                )
                // ユーザーのログアウト操作以外（サーバ側での失効・期限切れ）でも認証は解除され得る。
                // その場合に自動でログイン画面へ戻ることを手元で確認するための項目。
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.core_expire_token)) },
                    onClick = {
                        showDebugMenu = false
                        onExpireTokenClick()
                    },
                )
            }
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.core_notification),
                )
            }
            TextButton(onClick = { showLogoutDialog = true }) {
                Text(stringResource(R.string.core_logout))
            }
        },
        modifier = modifier,
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.core_logout)) },
            text = { Text(stringResource(R.string.core_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                ) {
                    Text(stringResource(R.string.core_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.core_cancel))
                }
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun AppTopBarPreview() {
    AppPreview {
        AppTopBar(
            title = "ホーム",
            isErrorInjectionEnabled = false,
            onErrorInjectionChange = {},
            onExpireTokenClick = {},
            onNotificationClick = {},
            onLogoutClick = {},
        )
    }
}

/** アイコンと「ログアウト」ボタンが横に並ぶため、大フォントではタイトルが潰れやすい。 */
@PreviewFontScale
@Composable
private fun AppTopBarFontScalePreview() {
    AppPreview {
        AppTopBar(
            title = "お気に入り",
            isErrorInjectionEnabled = true,
            onErrorInjectionChange = {},
            onExpireTokenClick = {},
            onNotificationClick = {},
            onLogoutClick = {},
        )
    }
}
