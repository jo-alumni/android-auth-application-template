package com.example.authapplication.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.ErrorContent
import com.example.authapplication.core.ui.ItemCard
import com.example.authapplication.domain.item.Item

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Snackbarの表示はイベントを受け取るNavigation側が制御するため、
    // ホストの状態を外から渡せるようにする(Previewでは既定値で足りる)。
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState) {
                HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                HomeUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.feature_home_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                is HomeUiState.Error -> {
                    ErrorContent(
                        message = stringResource(uiState.messageResId),
                        onRetryClick = onRetryClick,
                    )
                }

                is HomeUiState.Success -> {
                    LazyColumn(
                        // レイアウト領域はナビゲーションバーの裏まで広げたまま、スクロール余白
                        // (contentPadding)だけを確保して末尾までスクロールできるようにする。
                        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = uiState.items, key = { it.id }) { item ->
                            ItemCard(
                                title = item.title,
                                isFavorite = item.isFavorite,
                                onClick = { onItemClick(item.id) },
                                onFavoriteClick = { onFavoriteClick(item.id) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState.Success(
            items = listOf(
                Item(id = "1", title = "アイテム1", isFavorite = true),
                Item(id = "2", title = "アイテム2"),
                Item(id = "3", title = "アイテム3"),
            ),
        ),
        onItemClick = {},
        onFavoriteClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    HomeScreen(
        uiState = HomeUiState.Loading,
        onItemClick = {},
        onFavoriteClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    HomeScreen(
        uiState = HomeUiState.Empty,
        onItemClick = {},
        onFavoriteClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    HomeScreen(
        uiState = HomeUiState.Error(messageResId = R.string.feature_home_error_item_load),
        onItemClick = {},
        onFavoriteClick = {},
        onRetryClick = {},
    )
}
