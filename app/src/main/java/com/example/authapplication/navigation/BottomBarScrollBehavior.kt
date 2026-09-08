package com.example.authapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * スクロールに追従してボトムバーを隠す/表示するためのState Holder。
 *
 * [nestedScrollConnection] をScaffoldに繋ぐとスクロール量を受け取り、
 * その分だけボトムバーを下方向へずらす量（[hiddenHeightPx]）を更新する。
 * 画面側は [hiddenHeightPx] をそのまま `Modifier.offset` のy方向に渡し、
 * ボトムバーの実測値を [onBarHeightChanged] で伝える。
 */
@Stable
class BottomBarScrollBehavior {

    /** ボトムバーの高さ(px)。実測するまでは0で、その間は隠れない。 */
    var barHeightPx by mutableFloatStateOf(0f)
        private set

    /** ボトムバーを下方向へずらす量(px)。0なら全表示、[barHeightPx] と等しければ完全に隠れている。 */
    var hiddenHeightPx by mutableFloatStateOf(0f)
        private set

    /**
     * 子のスクロールより先にスクロール量を受け取り、隠す量へ反映する。
     * スクロール自体は消費しない（[Offset.Zero] を返す）ため、リストのスクロールは通常どおり動く。
     */
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // 上方向へのスクロール(available.y < 0)で隠し、下方向へのスクロールで戻す。
            hiddenHeightPx = (hiddenHeightPx - available.y).coerceIn(0f, barHeightPx)
            return Offset.Zero
        }
    }

    /** ボトムバーの実測高さを伝える。隠れている途中で高さが縮んだ場合はずれ量も追従させる。 */
    fun onBarHeightChanged(heightPx: Float) {
        barHeightPx = heightPx
        hiddenHeightPx = hiddenHeightPx.coerceIn(0f, heightPx)
    }

    /** ボトムバーを全表示の状態へ戻す。タブ切り替え時に呼ぶ。 */
    fun reset() {
        hiddenHeightPx = 0f
    }
}

/** [BottomBarScrollBehavior] をコンポジションのライフサイクルに紐付けて生成する。 */
@Composable
fun rememberBottomBarScrollBehavior(): BottomBarScrollBehavior = remember { BottomBarScrollBehavior() }
