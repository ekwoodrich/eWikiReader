package org.nsh07.wikireader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.homeScreen.reader.*
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.settingsScreen.viewModel.PreferencesState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalTestApi::class)
class PaginationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun measuredPagesCoverEverySourceCharacterAndFit() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        compose.waitForIdle()
        compose.runOnIdle {
            runBlocking {
                val rich = buildAnnotatedString {
                    append(("A long paragraph with emoji 🌍 and words. ").repeat(240))
                    append("\nעברית العربية 日本語\nLast line.")
                    addStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), 10, 90)
                }
                for (width in listOf(250, 600)) for (size in listOf(16, 28)) {
                    val style = TextStyle(fontSize = size.sp, lineHeight = (size * 1.5).sp)
                    val pages = paginateArticle(listOf(ReaderBlock(rich, 1)), width, 650, 12, measurer, style, style)
                    assertTrue(pages.size > 1)
                    val ranges = pages.flatMap { it.fragments }
                    assertEquals(0, ranges.first().start)
                    assertEquals(rich.length, ranges.last().end)
                    ranges.zipWithNext().forEach { (a,b) -> assertEquals(a.end, b.start) }
                    assertEquals(rich.text, ranges.joinToString("") { rich.subSequence(it.start,it.end).text })
                    for (page in pages) {
                        assertTrue(page.fragments.sumOf { it.height } + (page.fragments.size - 1) * 12 <= 650)
                        page.fragments.forEach {
                            assertEquals(it.height, measurer.measure(displayText(rich, it.start,it.end), style,
                                constraints = androidx.compose.ui.unit.Constraints(maxWidth = width)).size.height)
                        }
                    }
                }
            }
        }
    }

    @Test fun controlsTraverseEntireArticleAndReturnToSamePage() {
        val article = HomeSubscreen.Article(title = "Pagination fixture", extract = listOf(listOf(
            AnnotatedString(("Reading text for deterministic complete traversal. ").repeat(500))
        )))
        compose.setContent {
            MaterialTheme { PagedArticleContent(article, PreferencesState(pagedReading = true), PaddingValues(0.dp), {}, {}, { _,_ -> }) }
        }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("previous-page").assertIsNotEnabled()
        val firstBlock = article.readerState.block
        val firstOffset = article.readerState.offset
        var turns = 0
        val timings = mutableListOf<Long>()
        while (!compose.onNodeWithTag("next-page").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)) {
            val started = android.os.SystemClock.elapsedRealtime()
            compose.onNodeWithTag("next-page").performClick()
            compose.waitForIdle()
            timings.add(android.os.SystemClock.elapsedRealtime() - started)
            turns++
            assertTrue("Unbounded page traversal", turns < 250)
        }
        timings.sort()
        android.util.Log.i("eWikiVerification", "Fixture turns=$turns median=${timings[timings.size/2]}ms p95=${timings[(timings.size*0.95).toInt().coerceAtMost(timings.lastIndex)]}ms (includes test synchronization)")
        assertTrue(turns > 3)
        repeat(turns) { compose.onNodeWithTag("previous-page").performClick() }
        compose.onNodeWithTag("previous-page").assertIsNotEnabled()
        assertEquals(firstBlock, article.readerState.block)
        assertEquals(firstOffset, article.readerState.offset)
    }
    @Test fun sectionsReflowAndHardwareKeysPreserveReadingPosition() {
        val article = HomeSubscreen.Article(title = "State fixture", extract = listOf(
            listOf(AnnotatedString("Introduction. ".repeat(150))),
            listOf(AnnotatedString("Target section")),
            listOf(AnnotatedString("Section text. ".repeat(200)))
        ))
        var font by mutableIntStateOf(16)
        compose.setContent {
            MaterialTheme { PagedArticleContent(article, PreferencesState(pagedReading = true, fontSize = font), PaddingValues(0.dp), {}, {}, { _,_ -> }) }
        }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("reader-page").performKeyInput {
            keyDown(androidx.compose.ui.input.key.Key.PageDown)
            keyUp(androidx.compose.ui.input.key.Key.PageDown)
        }
        compose.onNodeWithTag("previous-page").assertIsEnabled()
        compose.runOnIdle { article.readerState.goToSection(3) }
        compose.waitForIdle()
        compose.onNodeWithText("Target section").assertExists()
        val anchor = Pair(article.readerState.block, article.readerState.offset)
        compose.runOnIdle { font = 24 }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("reader-page").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(anchor, Pair(article.readerState.block, article.readerState.offset))
    }

}
