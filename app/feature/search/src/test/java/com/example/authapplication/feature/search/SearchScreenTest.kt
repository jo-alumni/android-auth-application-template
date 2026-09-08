package com.example.authapplication.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.authapplication.domain.item.Item
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [SearchScreen] 単体のUIテスト。
 * 「データが0件([SearchUiState.Empty])」と「絞り込み結果が0件([SearchUiState.NoResults])」で
 * 文言が変わることを、ViewModelを介さず確認する。
 */
@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsEmptyMessage_whenEmpty() {
        setContent(uiState = SearchUiState.Empty)

        composeTestRule.onNodeWithText("アイテムがありません").assertIsDisplayed()
    }

    @Test
    fun showsNoResultsMessageWithQuery_whenNoResults() {
        setContent(uiState = SearchUiState.NoResults(query = "アイテム9"), query = "アイテム9")

        composeTestRule.onNodeWithText("「アイテム9」に一致するアイテムがありません").assertIsDisplayed()
    }

    /** 検索欄はどの状態でも操作でき、入力はコールバックでViewModelへ渡される。 */
    @Test
    fun notifiesTypedQuery_whenSearchFieldEdited() {
        var changedQuery: String? = null
        setContent(uiState = SearchUiState.Empty, onQueryChange = { changedQuery = it })

        composeTestRule.onNodeWithText("検索キーワード").performTextInput("アイテム1")

        assertEquals("アイテム1", changedQuery)
    }

    @Test
    fun showsFilteredItemTitles_whenSuccess() {
        setContent(uiState = SearchUiState.Success(listOf(Item(id = "1", title = "アイテム1"))))

        composeTestRule.onNodeWithText("アイテム1").assertIsDisplayed()
    }

    private fun setContent(
        uiState: SearchUiState,
        query: String = "",
        onQueryChange: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SearchScreen(
                uiState = uiState,
                query = query,
                onQueryChange = onQueryChange,
                onItemClick = {},
                onFavoriteClick = {},
                onRetryClick = {},
            )
        }
    }
}
