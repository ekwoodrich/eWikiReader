package org.nsh07.wikireader.ui.homeScreen.reader

import androidx.compose.runtime.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.yield

class ArticleReaderState {
    val navigationId: String = java.util.UUID.randomUUID().toString()
    var block by mutableIntStateOf(0)
    var offset by mutableIntStateOf(0)
    var section by mutableIntStateOf(0)
    var pagedActive by mutableStateOf(false)
    var fullScreen by mutableStateOf(false)
    var lastModePaged = false
    var enteringScrollIndex: Int? = null
    var enteringScrollOffset = 0
    var scrollTargetIndex = 0
    var scrollTargetOffset = 0
    var restoreExpandedSection by mutableIntStateOf(-1)
    var requestedSection by mutableStateOf<Int?>(null)
    fun goToSection(index: Int) { requestedSection = index }
}

data class ReaderBlock(
    val text: AnnotatedString,
    val section: Int,
    val heading: Boolean = false,
    val image: String? = null,
    val imageDescription: String = "",
    val hidden: Boolean = false
)
data class PageFragment(val block: Int, val start: Int, val end: Int, val height: Int, val textScale: Float = 1f)
data class ReaderPage(val fragments: List<PageFragment>)

/** Source ranges are retained, so repagination and rich links never depend on page numbers. */
suspend fun paginateArticle(
    blocks: List<ReaderBlock>, width: Int, height: Int, gap: Int,
    measurer: TextMeasurer, body: TextStyle, heading: TextStyle
): List<ReaderPage> {
    require(width > 0 && height > 0)
    val pages = mutableListOf<ReaderPage>()
    var fragments = mutableListOf<PageFragment>()
    var used = 0
    fun flush() {
        if (fragments.isNotEmpty()) pages.add(ReaderPage(fragments.toList()))
        fragments = mutableListOf(); used = 0
    }
    for ((index, block) in blocks.withIndex()) {
        if (index % 8 == 0) yield()
        if (block.hidden) continue
        if (block.image != null) {
            val imageHeight = (height * 0.38f).toInt().coerceAtLeast(1)
            if (used + imageHeight > height) flush()
            fragments.add(PageFragment(index, 0, 0, imageHeight))
            used += imageHeight + gap
            continue
        }
        val style = if (block.heading) heading else body
        var start = 0
        while (start < block.text.length) {
            yield()
            val available = height - used
            // Limit shaping work for pathological paragraphs; source offsets remain contiguous.
            var limit = minOf(start + 4000, block.text.length)
            if (limit < block.text.length) {
                val whitespace = block.text.text.lastIndexOf(' ', limit - 1)
                if (whitespace > start + 3000) limit = whitespace + 1
                val boundaries = java.text.BreakIterator.getCharacterInstance()
                boundaries.setText(block.text.text)
                if (!boundaries.isBoundary(limit)) limit = boundaries.preceding(limit)
            }
            val remaining = block.text.subSequence(start, limit)
            val layout = measurer.measure(remaining, style, constraints = Constraints(maxWidth = width))
            val fitting = (0 until layout.lineCount).lastOrNull { layout.getLineBottom(it) <= available }
            if (fitting == null && fragments.isNotEmpty()) { flush(); continue }
            // A single exceptionally tall line gets its own bounded page rather than looping.
            var line = fitting ?: 0
            var end = start + layout.getLineEnd(line, visibleEnd = false)
            if (end <= start) end = minOf(start + Character.charCount(Character.codePointAt(block.text.text, start)), block.text.length)
            var measured = measurer.measure(displayText(block.text, start, end), style, constraints = Constraints(maxWidth = width))
            while (measured.size.height > available && line > 0) {
                line--
                end = start + layout.getLineEnd(line, visibleEnd = false)
                measured = measurer.measure(displayText(block.text, start, end), style, constraints = Constraints(maxWidth = width))
            }
            if (measured.size.height > available && fragments.isNotEmpty()) { flush(); continue }
            val h = measured.size.height.coerceAtMost(height)
            fragments.add(PageFragment(index, start, end, h, minOf(1f, height.toFloat() / measured.size.height.coerceAtLeast(1))))
            used += h + gap
            start = end
            if (start < block.text.length) flush()
        }
    }
    flush()
    return pages.ifEmpty { listOf(ReaderPage(emptyList())) }
}

// A consumed newline belongs to this source range, but must not create an extra empty
// visual line at the foot of a page. Its paragraph break is represented by page/gap.
fun displayText(text: AnnotatedString, start: Int, end: Int): AnnotatedString {
    val fragment = text.subSequence(start, end)
    return fragment.subSequence(0, fragment.text.trimEnd('\n', '\r').length)
}
