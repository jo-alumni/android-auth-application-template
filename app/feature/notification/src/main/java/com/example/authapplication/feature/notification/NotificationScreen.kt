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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.ErrorContent
import com.example.authapplication.core.ui.preview.AppPreview
import com.example.authapplication.domain.notification.Notification
import com.example.authapplication.core.R as CoreR

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
                    title = { Text(stringResource(R.string.feature_notification_title)) },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(CoreR.string.core_close),
                            )
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
                                text = stringResource(R.string.feature_notification_empty),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    is NotificationUiState.Error -> {
                        ErrorContent(
                            message = stringResource(uiState.messageResId),
                            onRetryClick = onRetryClick,
                        )
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

/**
 * Previewで使うサンプル。2件目のメッセージを長文にして、
 * カード内の折り返しと大フォント時の崩れを確認できるようにする。
 */
private val previewNotifications = listOf(
    Notification(id = "1", title = "お知らせ1", message = "サンプル通知メッセージです。"),
    Notification(
        id = "2",
        title = "長いタイトルのお知らせで、1行に収まらない場合の見え方を確認する",
        message = "通知のメッセージは本文が長くなりやすく、カードの高さがどこまで伸びるかを" +
            "確認しておきたい。ここでは複数行に折り返す長さのサンプルを入れている。",
    ),
)

/**
 * [NotificationScreen] の全状態を1つのPreview関数で描くための供給元。
 * 状態を追加したらここへ足す。
 */
internal class NotificationUiStatePreviewParameterProvider : PreviewParameterProvider<NotificationUiState> {
    override val values = sequenceOf(
        NotificationUiState.Loading,
        NotificationUiState.Empty,
        NotificationUiState.Success(notifications = previewNotifications),
        NotificationUiState.Error(messageResId = R.string.feature_notification_error_load),
    )
}

@PreviewLightDark
@Composable
private fun NotificationScreenPreview(
    @PreviewParameter(NotificationUiStatePreviewParameterProvider::class) uiState: NotificationUiState,
) {
    AppPreview {
        NotificationScreen(uiState = uiState, onCloseClick = {}, onRetryClick = {})
    }
}

/** 崩れが出やすいのは本文が長い一覧表示なので、フォントスケールは [NotificationUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun NotificationScreenFontScalePreview() {
    AppPreview {
        NotificationScreen(
            uiState = NotificationUiState.Success(notifications = previewNotifications),
            onCloseClick = {},
            onRetryClick = {},
        )
    }
}
