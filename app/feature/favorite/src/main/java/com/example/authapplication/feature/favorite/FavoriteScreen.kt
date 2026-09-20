package com.example.authapplication.feature.favorite

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
import androidx.compose.material3.Text
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
import com.example.authapplication.core.ui.ItemCard
import com.example.authapplication.core.ui.preview.AppPreview
import com.example.authapplication.domain.item.Item

@Composable
fun FavoriteScreen(
    uiState: FavoriteUiState,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                            text = stringResource(R.string.feature_favorite_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                is FavoriteUiState.Error -> {
                    ErrorContent(
                        message = stringResource(uiState.messageResId),
                        onRetryClick = onRetryClick,
                    )
                }

                is FavoriteUiState.Success -> {
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
    }
}

/**
 * Previewで使うサンプル。3件目だけ極端に長いタイトルにして、
 * 折り返し・お気に入りボタンの押し出され方・大フォント時の崩れを確認できるようにする。
 */
private val previewItems = listOf(
    Item(id = "1", title = "アイテム1", isFavorite = true),
    Item(id = "2", title = "アイテム2", isFavorite = true),
    Item(id = "3", title = "とても長いタイトルのアイテムで、1行に収まらず折り返したときの見え方を確認する", isFavorite = true),
)

/**
 * [FavoriteScreen] の全状態を1つのPreview関数で描くための供給元。
 * 状態を追加したらここへ足す。
 */
internal class FavoriteUiStatePreviewParameterProvider : PreviewParameterProvider<FavoriteUiState> {
    override val values = sequenceOf(
        FavoriteUiState.Loading,
        FavoriteUiState.Empty,
        FavoriteUiState.Success(items = previewItems),
        FavoriteUiState.Error(messageResId = R.string.feature_favorite_error_item_load),
    )
}

@PreviewLightDark
@Composable
private fun FavoriteScreenPreview(
    @PreviewParameter(FavoriteUiStatePreviewParameterProvider::class) uiState: FavoriteUiState,
) {
    AppPreview {
        FavoriteScreen(
            uiState = uiState,
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}

/** 崩れが出やすいのは文字が増える一覧表示なので、フォントスケールは [FavoriteUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun FavoriteScreenFontScalePreview() {
    AppPreview {
        FavoriteScreen(
            uiState = FavoriteUiState.Success(items = previewItems),
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}
