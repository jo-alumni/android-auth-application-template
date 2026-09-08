package com.example.authapplication.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.R

/**
 * データの取得に失敗したときに表示する共通のエラー表示。
 *
 * ホーム/検索/お気に入り/詳細/通知のいずれの画面でも同じ見た目・同じ操作（再読み込み）になるよう、
 * `XxxUiState.Error` の分岐からこのComposableを呼び出す。
 * :app:core は :domain に依存しないため、モデルではなく表示に必要な文字列だけを受け取る。
 */
@Composable
fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetryClick) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Text(text = stringResource(R.string.core_retry), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentPreview() {
    ErrorContent(message = stringResource(R.string.core_error_unexpected), onRetryClick = {})
}
