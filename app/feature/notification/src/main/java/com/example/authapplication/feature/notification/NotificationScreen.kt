package com.example.authapplication.feature.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.ErrorContent
import com.example.authapplication.domain.notification.Notification

/**
 * 通知画面。全画面ダイアログ（= :app のScaffoldとは別ウィンドウ）として表示されるため、
 * 上端・下端いずれのinsetsもこの画面が持つ [Scaffold] で解決する（docs/window-insets.md 参照）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    uiState: NotificationUiState,
    onCloseClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("通知") },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "閉じる")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (uiState) {
                    NotificationUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    NotificationUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "通知はありません",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    is NotificationUiState.Error -> {
                        ErrorContent(message = uiState.message, onRetryClick = onRetryClick)
                    }

                    is NotificationUiState.Success -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items = uiState.notifications, key = { it.id }) { notification ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = notification.title, style = MaterialTheme.typography.titleMedium)
                                        Text(text = notification.message, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationScreenPreview() {
    NotificationScreen(
        uiState = NotificationUiState.Success(
            notifications = listOf(
                Notification(id = "1", title = "お知らせ1", message = "サンプル通知メッセージです。"),
                Notification(id = "2", title = "お知らせ2", message = "サンプル通知メッセージです。"),
            ),
        ),
        onCloseClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationScreenErrorPreview() {
    NotificationScreen(
        uiState = NotificationUiState.Error(message = "通知の取得に失敗しました"),
        onCloseClick = {},
        onRetryClick = {},
    )
}
