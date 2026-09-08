package com.example.authapplication.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.ErrorContent
import com.example.authapplication.domain.item.Item

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            // 下端のinsetsは各画面が自分で解決する（docs/window-insets.md 参照）。
            // スクロールしない画面なので、レイアウト領域そのものをナビゲーションバー手前で止める。
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (uiState) {
            DetailUiState.Loading -> {
                Text(text = "読み込み中...", style = MaterialTheme.typography.headlineSmall)
            }

            is DetailUiState.Success -> {
                Text(text = uiState.item.title, style = MaterialTheme.typography.headlineSmall)
            }

            DetailUiState.NotFound -> {
                Text(
                    text = "アイテムが見つかりませんでした",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            is DetailUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetryClick = onRetryClick,
                    // 「戻る」ボタンをエラー表示に押し出されないよう、エラー表示は余った領域だけを使う。
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Button(onClick = onBackClick) {
            Text("戻る")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPreview() {
    DetailScreen(
        uiState = DetailUiState.Success(item = Item(id = "1", title = "アイテム1")),
        onBackClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenNotFoundPreview() {
    DetailScreen(uiState = DetailUiState.NotFound, onBackClick = {}, onRetryClick = {})
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenErrorPreview() {
    DetailScreen(
        uiState = DetailUiState.Error(message = "アイテムの取得に失敗しました"),
        onBackClick = {},
        onRetryClick = {},
    )
}
