package org.nsh07.wikireader.ui.homeScreen.reader

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.github.tomtung.latex2unicode.LaTeX2Unicode
import org.nsh07.wikireader.parser.*
import org.nsh07.wikireader.parser.ReferenceData.infoboxTemplates
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen

/** Reuse upstream rich-text parsers, including link/citation listeners, in reading order. */
suspend fun articleBlocks(
    article: HomeSubscreen.Article, dataSaver: Boolean, renderMath: Boolean,
    colors: ColorScheme, typography: Typography, fontSize: Int,
    load: (String) -> Unit, reference: (String) -> Unit,
    rowLabel: (Int) -> String, columnLabel: (Int) -> String
): List<ReaderBlock> {
    val out = mutableListOf<ReaderBlock>()
    val lang = article.currentLang ?: "en"
    fun text(value: AnnotatedString, section: Int, heading: Boolean = false) {
        if (value.isNotEmpty()) out.add(ReaderBlock(value, section, heading))
    }
    fun image(source: String, caption: AnnotatedString, section: Int) {
        out.add(ReaderBlock(AnnotatedString(""), section, image = source, imageDescription = caption.text, hidden = dataSaver))
        text(caption, section)
    }
    fun rich(value: String) = value.toWikitextAnnotatedString(colors, typography, load, fontSize, showRef = reference)
    fun file(value: String, section: Int) {
        val name = value.removePrefix("[[").substringAfter(':').substringBefore('|').substringBefore("]]")
        val caption = value.removeSuffix("]]").substringAfter('|', "").split('|').lastOrNull().orEmpty()
        image("https://$lang.wikipedia.org/wiki/Special:FilePath/${java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")}?width=1264", rich(caption), section)
    }
    text(AnnotatedString(article.title), 0, true)
    article.photoDesc?.let { text(AnnotatedString(it), 0) }
    article.photo?.let { image(it.source, AnnotatedString(""), 0) }
    for ((extractIndex, blocks) in article.extract.withIndex()) {
        val section = if (extractIndex == 0) 1 else if (extractIndex % 2 == 1) extractIndex + 2 else extractIndex + 1
        if (extractIndex % 2 == 1) {
            text(AnnotatedString(blocks.joinToString("") { it.text }.replace("<.+?>".toRegex(), "")), section, true)
            continue
        }
        for (value in blocks) {
            val raw = value.text
            when {
                raw.startsWith("[[File:") -> file(raw, section)
                raw.startsWith("<gallery") -> raw.substringAfter('>').substringBefore("</gallery>").lines().map { it.trim() }
                    .filter { it.isNotEmpty() }.forEach { file("[[${if (it.startsWith("File:")) it else "File:$it"}]]", section) }
                raw.startsWith("<math", true) -> {
                    val latex = raw.substringAfter('>').substringBefore("</math>")
                    if (renderMath && !dataSaver) image("https://latex.codecogs.com/png.image?" + java.net.URLEncoder.encode(latex, "UTF-8"), AnnotatedString(""), section)
                    else text(AnnotatedString(runCatching { LaTeX2Unicode.convert(latex) }.getOrDefault(latex)), section)
                }
                raw.startsWith("{|") -> {
                    val (caption, rows) = parseWikitable(raw, colors, typography, load, reference, fontSize)
                    text(caption, section, true)
                    rows.forEachIndexed { row, cells ->
                        text(AnnotatedString(rowLabel(row + 1)), section, true)
                        cells.forEachIndexed { column, cell ->
                            text(buildAnnotatedString { append(columnLabel(column + 1)); append(": "); append(cell) }, section)
                        }
                    }
                }
                infoboxTemplates.any { raw.startsWith(it, true) } -> {
                    parseInfobox(raw, colors, typography, load, reference, fontSize, includeMedia = true).forEach { (label, item) ->
                        text(label, section, true)
                        if (item.text.startsWith("File:", true)) file("[[${item.text}]]", section)
                        else if (item.text.startsWith("[[") && item.text.contains(':') && item.text.endsWith("]]")) file(item.text, section)
                        else if (item.text.startsWith("http") && item.text.substringBefore('?').matches(".*\\.(png|jpg|jpeg|svg|gif)".toRegex(RegexOption.IGNORE_CASE))) image(item.text, AnnotatedString(""), section)
                        else text(item, section)
                    }
                }
                else -> text(value, section)
            }
        }
    }
    return out
}
