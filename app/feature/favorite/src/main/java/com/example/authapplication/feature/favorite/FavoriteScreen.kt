package com.example.authapplication.feature.favorite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.domain.item.Item

@Composable
fun FavoriteScreen(
    uiState: FavoriteUiState,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (uiState) {
            FavoriteUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            FavoriteUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "お気に入りがありません",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            is FavoriteUiState.Success -> {
                LazyColumn(
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = uiState.items, key = { it.id }) { item ->
                        Card(
                            onClick = { onItemClick(item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = item.title, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoriteScreenPreview() {
    FavoriteScreen(
        uiState = FavoriteUiState.Success(
            items = listOf(
                Item(id = "1", title = "アイテム1"),
                Item(id = "2", title = "アイテム2"),
                Item(id = "3", title = "アイテム3"),
            ),
        ),
        onItemClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun FavoriteScreenLoadingPreview() {
    FavoriteScreen(uiState = FavoriteUiState.Loading, onItemClick = {})
}

@Preview(showBackground = true)
@Composable
private fun FavoriteScreenEmptyPreview() {
    FavoriteScreen(uiState = FavoriteUiState.Empty, onItemClick = {})
}
