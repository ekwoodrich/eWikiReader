package org.nsh07.wikireader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeAction
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction

/** Live integration: real network/parser -> settings -> paged article and back stack. */
class ArticleJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun liveArticlesLoadAndPagesNavigate() {
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        compose.runOnUiThread {
            compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true))
            compose.activity.viewModel.onAction(HomeAction.LoadPage("E Ink"))
        }
        compose.waitUntil(120000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle {
            val article = compose.activity.viewModel.backStack.last() as HomeSubscreen.Article
            assertNotEquals("Error", article.title)
            assertTrue("No article content", article.extract.isNotEmpty())
        }
        compose.onNodeWithTag("next-page").assertIsEnabled().performClick()
        compose.onNodeWithTag("previous-page").assertIsEnabled().performClick()
        compose.onNodeWithTag("previous-page").assertIsNotEnabled()
        val original = compose.activity.viewModel.backStack.last() as HomeSubscreen.Article
        compose.onNodeWithTag("next-page").performClick()
        val savedAnchor = Pair(original.readerState.block, original.readerState.offset)
        if (original.savedStatus != org.nsh07.wikireader.data.SavedStatus.SAVED) {
            compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.SaveArticle("en", "Save failed", "Delete failed")) }
        }
        compose.waitUntil(60000) {
            (compose.activity.viewModel.backStack.last() as? HomeSubscreen.Article)?.savedStatus == org.nsh07.wikireader.data.SavedStatus.SAVED
        }
        compose.onNodeWithTag("reader-page").assertExists()
        assertEquals(savedAnchor, Pair(original.readerState.block, original.readerState.offset))
        compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.LoadPage("Electronic paper")) }
        compose.waitUntil(120000) {
            val current = compose.activity.viewModel.backStack.last() as? HomeSubscreen.Article
            current != null && current.title == "Electronic paper" && current.extract.isNotEmpty() && current.readerState.pagedActive &&
                compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty()
        }
        // Complete traversal of a real long, media-rich article.
        var turns = 0
        while (!compose.onNodeWithTag("next-page").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)) {
            compose.onNodeWithTag("next-page").performClick()
            turns++
            assertTrue("Unbounded live traversal", turns < 500)
        }
        android.util.Log.i("eWikiVerification", "Electronic paper traversed ${turns + 1} pages")
        assertTrue(turns > 3)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(30000) { compose.activity.viewModel.backStack.last() is HomeSubscreen.Article &&
            (compose.activity.viewModel.backStack.last() as HomeSubscreen.Article).title == original.title }
        compose.onNodeWithTag("reader-page").assertExists()
        assertEquals(savedAnchor, Pair(original.readerState.block, original.readerState.offset))
        // Font reflow must retain the source anchor instead of jumping to the start.
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SaveFontSize(22)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(savedAnchor, Pair(original.readerState.block, original.readerState.offset))
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(false)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isEmpty() }
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true)) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        // Saved content uses the same renderer, without a network fetch for article text.
        val beforeSavedLoad = (compose.activity.viewModel.backStack.last() as HomeSubscreen.Article).readerState
        compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.LoadSavedArticle(original.pageId!!, original.currentLang ?: "en")) }
        compose.waitUntil(30000) {
            compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() &&
                (compose.activity.viewModel.backStack.last() as? HomeSubscreen.Article)?.let {
                    it.title == original.title && it.readerState !== beforeSavedLoad && it.savedStatus == org.nsh07.wikireader.data.SavedStatus.SAVED
                } == true
        }
        compose.onNodeWithTag("next-page").assertIsEnabled().performClick()
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SaveFontSize(16)) }
        compose.waitUntil(10000) { compose.activity.settingsViewModel.preferencesState.value.fontSize == 16 }
    }
}
