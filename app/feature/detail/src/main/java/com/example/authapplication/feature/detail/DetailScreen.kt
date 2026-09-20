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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.authapplication.core.ui.ErrorContent
import com.example.authapplication.core.ui.preview.AppPreview
import com.example.authapplication.domain.item.Item
import com.example.authapplication.core.R as CoreR

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
                Text(
                    text = stringResource(CoreR.string.core_loading),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            is DetailUiState.Success -> {
                Text(text = uiState.item.title, style = MaterialTheme.typography.headlineSmall)
            }

            DetailUiState.NotFound -> {
                Text(
                    text = stringResource(R.string.feature_detail_not_found),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            is DetailUiState.Error -> {
                ErrorContent(
                    message = stringResource(uiState.messageResId),
                    onRetryClick = onRetryClick,
                    // 「戻る」ボタンをエラー表示に押し出されないよう、エラー表示は余った領域だけを使う。
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Button(onClick = onBackClick) {
            Text(stringResource(CoreR.string.core_back))
        }
    }
}

/** Previewで使うサンプル。タイトルを長くして、見出しの折り返しと「戻る」ボタンの位置を確認する。 */
private val previewItem = Item(
    id = "1",
    title = "とても長いタイトルのアイテムで、見出しが複数行になったときの見え方を確認する",
    isFavorite = true,
)

/**
 * [DetailScreen] の全状態を1つのPreview関数で描くための供給元。
 * 状態を追加したらここへ足す。
 */
internal class DetailUiStatePreviewParameterProvider : PreviewParameterProvider<DetailUiState> {
    override val values = sequenceOf(
        DetailUiState.Loading,
        DetailUiState.Success(item = previewItem),
        DetailUiState.NotFound,
        DetailUiState.Error(messageResId = R.string.feature_detail_error_item_load),
    )
}

@PreviewLightDark
@Composable
private fun DetailScreenPreview(
    @PreviewParameter(DetailUiStatePreviewParameterProvider::class) uiState: DetailUiState,
) {
    AppPreview {
        DetailScreen(uiState = uiState, onBackClick = {}, onRetryClick = {})
    }
}

/** 崩れが出やすいのは見出しが伸びる表示なので、フォントスケールは [DetailUiState.Success] で確認する。 */
@PreviewFontScale
@Composable
private fun DetailScreenFontScalePreview() {
    AppPreview {
        DetailScreen(
            uiState = DetailUiState.Success(item = previewItem),
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
