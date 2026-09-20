package com.example.authapplication.core.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.authapplication.core.theme.AuthApplicationTheme

/**
 * `@Preview` の中身をアプリ本番と同じテーマで描くためのラッパー。
 *
 * テーマでラップしないPreviewは `MaterialTheme` の既定値（紫のベースライン配色）で描かれるため、
 * 実機での見た目とずれる。特に [androidx.compose.ui.tooling.preview.PreviewLightDark] を付けても、
 * テーマが無ければダークテーマの配色は反映されず、背景だけが黒くなって文字が読めなくなる。
 * どのPreviewでもこの関数で包むことで、見ているものが実機の見た目と同じであることを保証する。
 *
 * [Surface] を重ねているのは、`colorScheme.background` を敷いて文字色（`onBackground`）まで
 * テーマどおりに解決させるため。`@Preview(showBackground = true)` の白背景では、
 * ダークテーマの文字色が白背景に白文字で描かれてしまう。
 *
 * Dynamic Color を切ってあるのは、Preview の見た目を端末の壁紙に左右させず、
 * このアプリが宣言した配色（`LightColorScheme` / `DarkColorScheme`）を確認するため。
 *
 * NOTE: この関数自体は見た目を持たないPreview用の下地なので、
 * `.claude/rules/compose-preview.md` の「publicなComposableにはPreviewを用意する」の対象外。
 * 描画結果は各画面のPreviewが常に通る経路として確認される。
 */
@Composable
fun AppPreview(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AuthApplicationTheme(dynamicColor = false) {
        Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
