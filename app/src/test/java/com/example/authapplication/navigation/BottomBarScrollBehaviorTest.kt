package com.example.authapplication.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [BottomBarScrollBehavior] の単体テスト。
 * Composeのコンポジションを起動せず、State Holderの状態遷移だけを検証する。
 */
class BottomBarScrollBehaviorTest {

    private val behavior = BottomBarScrollBehavior().apply { onBarHeightChanged(BAR_HEIGHT) }

    private fun scroll(y: Float) {
        behavior.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = y),
            source = NestedScrollSource.UserInput,
        )
    }

    @Test
    fun `hiddenHeightPx is zero before scrolling`() {
        assertEquals(0f, behavior.hiddenHeightPx, 0f)
    }

    @Test
    fun `scrolling up hides the bottom bar by the scrolled amount`() {
        scroll(-30f)

        assertEquals(30f, behavior.hiddenHeightPx, 0f)
    }

    @Test
    fun `scrolling up never hides more than the bar height`() {
        scroll(-(BAR_HEIGHT * 2))

        assertEquals(BAR_HEIGHT, behavior.hiddenHeightPx, 0f)
    }

    @Test
    fun `scrolling down shows the bottom bar again and never overshoots`() {
        scroll(-BAR_HEIGHT)

        scroll(40f)
        assertEquals(BAR_HEIGHT - 40f, behavior.hiddenHeightPx, 0f)

        scroll(BAR_HEIGHT * 2)
        assertEquals(0f, behavior.hiddenHeightPx, 0f)
    }

    @Test
    fun `scroll does not consume the scroll amount`() {
        val consumed = behavior.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = -30f),
            source = NestedScrollSource.UserInput,
        )

        assertEquals(Offset.Zero, consumed)
    }

    @Test
    fun `bottom bar does not hide until its height is measured`() {
        val notMeasured = BottomBarScrollBehavior()

        notMeasured.nestedScrollConnection.onPreScroll(
            available = Offset(x = 0f, y = -30f),
            source = NestedScrollSource.UserInput,
        )

        assertEquals(0f, notMeasured.hiddenHeightPx, 0f)
    }

    @Test
    fun `hidden amount follows a shrunk bar height`() {
        scroll(-BAR_HEIGHT)

        behavior.onBarHeightChanged(BAR_HEIGHT / 2)

        assertEquals(BAR_HEIGHT / 2, behavior.hiddenHeightPx, 0f)
    }

    @Test
    fun `reset shows the bottom bar again`() {
        scroll(-BAR_HEIGHT)

        behavior.reset()

        assertEquals(0f, behavior.hiddenHeightPx, 0f)
    }

    private companion object {
        const val BAR_HEIGHT = 160f
    }
}
