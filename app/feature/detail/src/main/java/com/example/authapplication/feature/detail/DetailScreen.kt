package com.example.authapplication.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.domain.item.Item

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val message = when (uiState) {
            DetailUiState.Loading -> "読み込み中..."
            is DetailUiState.Success -> uiState.item.title
            DetailUiState.NotFound -> "アイテムが見つかりませんでした"
        }
        Text(text = message, style = MaterialTheme.typography.headlineSmall)
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
    )
}
