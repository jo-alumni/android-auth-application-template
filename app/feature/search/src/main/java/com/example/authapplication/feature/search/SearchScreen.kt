package com.example.authapplication.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
fun SearchScreen(
    uiState: SearchUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 検索欄はどの状態でも操作できるよう when の外に置く。
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.feature_search_query_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // 絞り込みは入力のたびに走るので、検索キーではキーボードを畳んで結果を見せるだけにする。
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
            when (uiState) {
                SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                SearchUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.feature_search_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                is SearchUiState.NoResults -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.feature_search_no_results, uiState.query),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                is SearchUiState.Error -> {
                    ErrorContent(
                        message = stringResource(uiState.messageResId),
                        onRetryClick = onRetryClick,
                    )
                }

                is SearchUiState.Success -> {
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
 * [SearchScreen] の全状態を1つのPreview関数で描くための供給元。
 * 状態を追加したらここへ足す。
 */
internal class SearchUiStatePreviewParameterProvider : PreviewParameterProvider<SearchUiState> {
    override val values = sequenceOf(
        SearchUiState.Loading,
        SearchUiState.Empty,
        SearchUiState.Success(items = previewItems),
        SearchUiState.NoResults(query = "該当しない長めの検索キーワード"),
        SearchUiState.Error(messageResId = R.string.feature_search_error_item_load),
    )
}

@PreviewLightDark
@Composable
private fun SearchScreenPreview(
    @PreviewParameter(SearchUiStatePreviewParameterProvider::class) uiState: SearchUiState,
) {
    AppPreview {
        SearchScreen(
            uiState = uiState,
            // 絞り込み結果が0件の状態では、検索欄にも同じキーワードが入っているのが実際の見え方。
            query = (uiState as? SearchUiState.NoResults)?.query.orEmpty(),
            onQueryChange = {},
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}

/** 崩れが出やすいのは検索欄と一覧が縦に並ぶ表示なので、フォントスケールは [SearchUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun SearchScreenFontScalePreview() {
    AppPreview {
        SearchScreen(
            uiState = SearchUiState.Success(items = previewItems),
            query = "アイテム",
            onQueryChange = {},
            onItemClick = {},
            onFavoriteClick = {},
            onRetryClick = {},
        )
    }
}
