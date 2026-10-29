package org.nsh07.wikireader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeAction
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.settingsScreen.viewModel.SettingsAction

class LanguageAndMediaJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun languagesMathMediaAndRecreation() {
        compose.waitUntil(30000) { compose.activity.viewModel.appStatus.value == org.nsh07.wikireader.data.AppStatus.INITIALIZED }
        compose.runOnUiThread { compose.activity.settingsViewModel.onAction(SettingsAction.SavePagedReading(true)) }
        for ((lang, title) in listOf("en" to "Pythagorean theorem", "ja" to "電子ペーパー", "ar" to "ويكيبيديا", "en" to "E Ink")) {
            compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.LoadPage(title, lang)) }
            compose.waitUntil(120000) {
                val a = compose.activity.viewModel.backStack.last() as? HomeSubscreen.Article
                a != null && a.currentLang == lang && a.title.replace('_',' ') == title && a.extract.isNotEmpty() &&
                    compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty()
            }
            repeat(3) {
                if (!compose.onNodeWithTag("next-page").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled))
                    compose.onNodeWithTag("next-page").performClick()
            }
            android.util.Log.i("eWikiVerification", "Live language/media passed: $lang / $title")
        }
        val article = compose.activity.viewModel.backStack.last() as HomeSubscreen.Article
        val anchor = Pair(article.readerState.block, article.readerState.offset)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(anchor, Pair((compose.activity.viewModel.backStack.last() as HomeSubscreen.Article).readerState.block,
            (compose.activity.viewModel.backStack.last() as HomeSubscreen.Article).readerState.offset))
        compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.ScrollToTop) }
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("E Ink").onFirst().performClick()
        compose.waitUntil(30000) { compose.activity.viewModel.backStack.last() is HomeSubscreen.Image }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        // Exercise a real citation listener retained in the source annotations.
        val citation = article.extract.flatten().flatMap { text ->
            text.getLinkAnnotations(0, text.length).filter { range ->
                text.text.substring(range.start, range.end).trim().let { it.isNotEmpty() && it.all(Char::isDigit) }
            }.map { it.item }
        }.filterIsInstance<androidx.compose.ui.text.LinkAnnotation.Url>().firstOrNull()
        assertNotNull("Expected article citations", citation)
        compose.runOnUiThread { citation!!.linkInteractionListener!!.onClick(citation) }
        compose.waitUntil(10000) { compose.activity.viewModel.homeScreenState.value.showRef }
        compose.waitForIdle()
        val beforeKey = Pair(article.readerState.block, article.readerState.offset)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_PAGE_DOWN)
        assertEquals(beforeKey, Pair(article.readerState.block, article.readerState.offset))
        compose.runOnUiThread { compose.activity.viewModel.onAction(HomeAction.HideRef) }
    }
}
