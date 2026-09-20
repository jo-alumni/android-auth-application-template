package com.example.authapplication.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.authapplication.core.R
import com.example.authapplication.core.ui.preview.AppPreview

/**
 * ホーム/検索/お気に入りの一覧で共通利用するアイテム行。
 * :app:core は :domain に依存しないため、モデルではなく表示に必要な値だけを受け取る。
 */
@Composable
fun ItemCard(
    title: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            )
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.core_favorite_remove else R.string.core_favorite_add,
                    ),
                )
            }
        }
    }
}

/** Previewで使うサンプル。1行に収まらない長さにして、折り返しと大フォント時の崩れを確認する。 */
private const val LONG_TITLE =
    "とても長いタイトルのアイテムで、1行に収まらず折り返したときにお気に入りボタンが押し出されないかを確認する"

/** [ItemCard] の見た目が変わる組み合わせ（タイトルの長さ × お気に入りのON/OFF）を網羅する供給元。 */
internal class ItemCardPreviewParameterProvider : PreviewParameterProvider<Pair<String, Boolean>> {
    override val values = sequenceOf(
        "アイテム1" to false,
        "アイテム1" to true,
        LONG_TITLE to true,
    )
}

@PreviewLightDark
@Composable
private fun ItemCardPreview(
    @PreviewParameter(ItemCardPreviewParameterProvider::class) titleAndFavorite: Pair<String, Boolean>,
) {
    val (title, isFavorite) = titleAndFavorite
    AppPreview {
        ItemCard(title = title, isFavorite = isFavorite, onClick = {}, onFavoriteClick = {})
    }
}

@PreviewFontScale
@Composable
private fun ItemCardFontScalePreview() {
    AppPreview {
        ItemCard(title = LONG_TITLE, isFavorite = true, onClick = {}, onFavoriteClick = {})
    }
}
