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
fun HomeScreen(
    uiState: HomeUiState,
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
    }
}

/**
 * Previewで使うサンプル。3件目だけ極端に長いタイトルにして、
 * 折り返し・お気に入りボタンの押し出され方・大フォント時の崩れを確認できるようにする。
 */
private val previewItems = listOf(
    Item(id = "1", title = "アイテム1", isFavorite = true),
    Item(id = "2", title = "アイテム2"),
    Item(id = "3", title = "とても長いタイトルのアイテムで、1行に収まらず折り返したときの見え方を確認する", isFavorite = true),
)

/**
 * [HomeScreen] の全状態を1つのPreview関数で描くための供給元。
 *
 * 状態を追加したらここへ足す。Preview関数を状態ごとに増やす方式と違い、
 * 足し忘れても「その状態のPreviewだけ無い」ではなく一覧から欠けるので気付きやすい。
 */
internal class HomeUiStatePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState.Loading,
        HomeUiState.Empty,
        HomeUiState.Success(items = previewItems),
        HomeUiState.Error(messageResId = R.string.feature_home_error_item_load),
    )
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) uiState: HomeUiState,
) {
    AppPreview {
        HomeScreen(
            uiState = uiState,
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}

/** 崩れが出やすいのは文字が増える一覧表示なので、フォントスケールは [HomeUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun HomeScreenFontScalePreview() {
    AppPreview {
        HomeScreen(
            uiState = HomeUiState.Success(items = previewItems),
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}
