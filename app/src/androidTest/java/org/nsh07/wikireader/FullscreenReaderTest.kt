package org.nsh07.wikireader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeAction
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction

class FullscreenReaderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun hideChromeUsesMoreSpaceAndKeepsSymmetricControls() {
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        compose.runOnUiThread {
            compose.activity.settingsViewModel.onAction(SettingsAction.SaveDisableAnimations(true))
            compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true))
            compose.activity.viewModel.onAction(HomeAction.LoadPage("E Ink"))
        }
        compose.waitUntil(120000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        val normalBounds = compose.onNodeWithTag("reader-page").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("next-page").performClick()
        val article = compose.activity.viewModel.backStack.last() as HomeSubscreen.Article
        val anchor = Pair(article.readerState.block, article.readerState.offset)
        compose.onNodeWithTag("toggle-reader-fullscreen").performClick()
        compose.waitUntil(30000) { article.readerState.fullScreen && compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        val expandedBounds = compose.onNodeWithTag("reader-page").getUnclippedBoundsInRoot()
        assertTrue("Reading area did not grow", (expandedBounds.bottom - expandedBounds.top) > (normalBounds.bottom - normalBounds.top))
        compose.onAllNodesWithContentDescription("Search").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Share page").assertCountEquals(0)
        assertEquals(anchor, Pair(article.readerState.block, article.readerState.offset))
        val controls = compose.onNodeWithTag("reader-controls").getUnclippedBoundsInRoot()
        val position = compose.onNodeWithTag("page-progress").getUnclippedBoundsInRoot()
        assertEquals((controls.left.value + controls.right.value) / 2, (position.left.value + position.right.value) / 2, 0.6f)
        val prev = compose.onNodeWithTag("previous-page").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithTag("next-page").getUnclippedBoundsInRoot()
        assertEquals((prev.left.value + prev.right.value) / 2 - controls.left.value, controls.right.value - (next.left.value + next.right.value) / 2, 0.6f)
        val progress = compose.onNodeWithTag("page-progress").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }
        assertTrue(progress.matches("\\d+ / \\d+".toRegex()))
        compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.UpdateRef("Fullscreen reference")) }
        compose.waitUntil(10000) { compose.activity.viewModel.homeScreenState.value.showRef }
        compose.waitForIdle()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(10000) { !compose.activity.viewModel.homeScreenState.value.showRef }
        assertTrue("Back from a reference must retain fullscreen reading", article.readerState.fullScreen)
        compose.onNodeWithTag("next-page").assertIsEnabled().performClick()
        compose.onNodeWithTag("previous-page").assertIsEnabled().performClick()
        compose.onNodeWithTag("toggle-reader-fullscreen").performClick()
        compose.waitUntil(30000) { !article.readerState.fullScreen && compose.onAllNodesWithContentDescription("Share page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("toggle-reader-fullscreen").performClick()
        compose.waitUntil(30000) { article.readerState.fullScreen && compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(30000) { !article.readerState.fullScreen }
        assertEquals(article.title, (compose.activity.viewModel.backStack.last() as HomeSubscreen.Article).title)
    }
}
