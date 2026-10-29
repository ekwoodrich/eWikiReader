package org.nsh07.wikireader.ui.homeScreen.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.crossfade
import androidx.compose.ui.platform.testTag
import coil3.request.ImageRequest
import org.nsh07.wikireader.R
import org.nsh07.wikireader.ui.homeScreen.viewModel.HomeSubscreen
import org.nsh07.wikireader.ui.image.fitDisplay
import org.nsh07.wikireader.ui.settingsScreen.viewModel.PreferencesState

@Composable
fun PagedArticleContent(
    article: HomeSubscreen.Article,
    preferences: PreferencesState,
    insets: PaddingValues,
    load: (String) -> Unit,
    reference: (String) -> Unit,
    openImage: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    keysEnabled: Boolean = true
) {
    val state = article.readerState
    val density = LocalDensity.current
    val view = LocalView.current
    BackHandler(enabled = state.fullScreen && keysEnabled) { state.fullScreen = false }
    DisposableEffect(view, state.fullScreen) {
        val wasFullScreen = state.fullScreen
        val activity = view.context as? Activity
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        val previousBarsBehavior = controller?.systemBarsBehavior
        if (state.fullScreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (wasFullScreen) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                previousBarsBehavior?.let { controller?.systemBarsBehavior = it }
            }
        }
    }
    val textToolbar = LocalTextToolbar.current
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val direction = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val focus = remember { FocusRequester() }
    val body = typography.bodyLarge.copy(
        fontSize = preferences.fontSize.sp,
        lineHeight = (24 * (preferences.fontSize / 16.0)).toInt().sp,
        fontFamily = if (preferences.fontStyle == "sans") FontFamily.SansSerif else FontFamily.Serif,
        hyphens = Hyphens.None
    )
    val heading = body.copy(fontSize = (preferences.fontSize + 6).sp, lineHeight = (preferences.fontSize * 1.8).sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    var blocks by remember(state) { mutableStateOf<List<ReaderBlock>>(emptyList()) }
    var pages by remember(state) { mutableStateOf<List<ReaderPage>>(emptyList()) }
    var ready by remember(state) { mutableStateOf(false) }
    var failure by remember(state) { mutableStateOf<String?>(null) }
    var pageIndex by remember(state) { mutableIntStateOf(0) }
    val row = stringResource(R.string.reader_row)
    val column = stringResource(R.string.reader_column)
    LaunchedEffect(article.extract, preferences.dataSaver, preferences.renderMath, preferences.fontSize, colors) {
        ready = false
        failure = null
        try {
            blocks = articleBlocks(article, preferences.dataSaver, preferences.renderMath, colors, typography,
                preferences.fontSize, load, reference, { row.format(it) }, { column.format(it) })
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { failure = e.message ?: "Unable to prepare article" }
    }
    DisposableEffect(state) {
        state.pagedActive = true
        onDispose { state.pagedActive = false }
    }
    fun turn(delta: Int) {
        if (!ready) return
        pageIndex = (pageIndex + delta).coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        pages.getOrNull(pageIndex)?.fragments?.firstOrNull()?.let {
            state.block = it.block; state.offset = it.start; state.section = blocks[it.block].section
            state.scrollTargetIndex = state.section
            state.scrollTargetOffset = pages.flatMap { p -> p.fragments }.takeWhile { f -> f != it }
                .filter { f -> blocks[f.block].section == state.section }.sumOf { f -> f.height + with(density) { 8.dp.roundToPx() } }
        }
    }
    Column(modifier.fillMaxSize().padding(insets).padding(bottom = if (state.fullScreen) 0.dp else 88.dp)
        .focusRequester(focus).onPreviewKeyEvent {
            val delta = when (it.key) {
                Key.PageDown -> 1
                Key.PageUp -> -1
                Key.VolumeDown -> if (preferences.volumePageTurns) 1 else 0
                Key.VolumeUp -> if (preferences.volumePageTurns) -1 else 0
                else -> 0
            }
            if (delta != 0 && ready && keysEnabled && textToolbar.status != TextToolbarStatus.Shown) {
                if (it.type == KeyEventType.KeyUp) turn(delta)
                true
            } else false
        }.focusable()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clipToBounds()) {
            val width = with(density) { maxWidth.roundToPx() }
            val height = with(density) { maxHeight.roundToPx() }
            val gap = with(density) { 8.dp.roundToPx() }
            LaunchedEffect(blocks, width, height, body, heading, direction, density) {
                if (blocks.isEmpty() || width <= 0 || height <= 0) return@LaunchedEffect
                ready = false
                val result = paginateArticle(blocks, width, height, gap, measurer, body, heading)
                pages = result
                val entering = state.enteringScrollIndex
                if (entering != null) {
                    var accumulated = 0
                    val fragment = result.flatMap { it.fragments }.firstOrNull {
                        if (blocks[it.block].section < entering) false
                        else if (blocks[it.block].section > entering) true
                        else { accumulated += it.height + gap; accumulated > state.enteringScrollOffset }
                    }
                    if (fragment != null) { state.block = fragment.block; state.offset = fragment.start; state.section = blocks[fragment.block].section }
                    state.enteringScrollIndex = null
                }
                pageIndex = result.indexOfLast { page ->
                    page.fragments.firstOrNull()?.let { it.block < state.block || (it.block == state.block && it.start <= state.offset) } ?: false
                }.coerceAtLeast(0)
                ready = true
            }
            LaunchedEffect(state.requestedSection, ready) {
                val target = state.requestedSection
                if (ready && target != null) {
                    pageIndex = if (target == 0) 0 else pages.indexOfFirst { page -> page.fragments.any { blocks[it.block].section == target } }.coerceAtLeast(0)
                    pages[pageIndex].fragments.firstOrNull { target == 0 || blocks[it.block].section == target }?.let {
                        state.block = it.block; state.offset = it.start; state.section = blocks[it.block].section
                    }
                    state.scrollTargetIndex = target
                    state.scrollTargetOffset = 0
                    state.requestedSection = null
                }
            }
            if (ready) {
                SelectionContainer {
                    Column(Modifier.fillMaxSize().testTag("reader-page")) {
                        for (fragment in pages[pageIndex].fragments) {
                            val block = blocks[fragment.block]
                            if (block.image != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(block.image).fitDisplay(context).crossfade(false).build(),
                                    contentDescription = block.imageDescription.ifEmpty { article.title },
                                    contentScale = ContentScale.Fit,
                                    error = androidx.compose.ui.res.painterResource(R.drawable.error),
                                    modifier = Modifier.fillMaxWidth().height(with(density) { fragment.height.toDp() })
                                        .background(if (preferences.imageBackground) androidx.compose.ui.graphics.Color.White else colors.surface)
                                        .clickable { openImage(block.image, block.imageDescription) }
                                )
                            } else Layout(
                                content = {
                                    Text(displayText(block.text, fragment.start, fragment.end),
                                        style = if (block.heading) heading else body,
                                        modifier = Modifier.fillMaxWidth().graphicsLayer {
                                            scaleX = fragment.textScale; scaleY = fragment.textScale
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        })
                                },
                                modifier = Modifier.fillMaxWidth().height(with(density) { fragment.height.toDp() })
                            ) { children, constraints ->
                                val text = children.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                                layout(constraints.maxWidth, constraints.maxHeight) { text.placeRelative(0, 0) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            } else Text(failure ?: stringResource(R.string.reader_preparing))
        }
        Box(Modifier.fillMaxWidth().height(48.dp).testTag("reader-controls")) {
            Text(
                if (ready) stringResource(R.string.reader_progress, pageIndex + 1, pages.size) else "…",
                modifier = Modifier.align(Alignment.Center).testTag("page-progress")
            )
            Row(Modifier.align(Alignment.CenterStart)) {
                IconButton(onClick = { turn(-1) }, enabled = ready && pageIndex > 0,
                    modifier = Modifier.size(48.dp).testTag("previous-page")) {
                    Icon(androidx.compose.ui.res.painterResource(if (direction == androidx.compose.ui.unit.LayoutDirection.Rtl) R.drawable.keyboard_arrow_right else R.drawable.keyboard_arrow_left),
                        contentDescription = stringResource(R.string.reader_previous))
                }
                IconButton(onClick = { state.fullScreen = !state.fullScreen },
                    modifier = Modifier.size(48.dp).testTag("toggle-reader-fullscreen")) {
                    Icon(androidx.compose.ui.res.painterResource(
                        if (state.fullScreen) R.drawable.close_fullscreen else R.drawable.open_in_full),
                        contentDescription = stringResource(
                            if (state.fullScreen) R.string.reader_exit_fullscreen else R.string.reader_enter_fullscreen))
                }
            }
            IconButton(onClick = { turn(1) }, enabled = ready && pageIndex < pages.lastIndex,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).testTag("next-page")) {
                Icon(androidx.compose.ui.res.painterResource(if (direction == androidx.compose.ui.unit.LayoutDirection.Rtl) R.drawable.keyboard_arrow_left else R.drawable.keyboard_arrow_right),
                    contentDescription = stringResource(R.string.reader_next))
            }
        }
    }
    LaunchedEffect(ready) { if (ready) focus.requestFocus() }
}
