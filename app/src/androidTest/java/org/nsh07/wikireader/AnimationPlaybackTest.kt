package org.nsh07.wikireader

import android.graphics.drawable.Animatable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.DrawableImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.nsh07.wikireader.ui.image.PlaybackImage
import org.nsh07.wikireader.ui.theme.LocalAppAnimationsEnabled
import org.nsh07.wikireader.ui.theme.WikiReaderTheme

class AnimationPlaybackTest {
    @get:Rule val compose = createComposeRule()
    @Test fun gifStartsPausedAndCanPlayAndPause() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bytes = instrumentation.context.assets.open("animated.gif").use { it.readBytes() }
        var animation: Animatable? = null
        val request = ImageRequest.Builder(context).data(bytes).listener(onSuccess = { _, result ->
            animation = (result.image as? DrawableImage)?.drawable as? Animatable
        }).build()
        compose.setContent {
            WikiReaderTheme(disableAnimations = true) {
                CompositionLocalProvider(LocalAppAnimationsEnabled provides false) {
                    PlaybackImage(rememberAsyncImagePainter(request), "Test GIF", Modifier.size(100.dp))
                }
            }
        }
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Play animation").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertNotNull(animation); assertFalse(animation!!.isRunning) }
        compose.onNodeWithContentDescription("Play animation").performClick()
        compose.waitUntil(5000) { animation?.isRunning == true }
        compose.onNodeWithContentDescription("Pause animation").performClick()
        compose.waitUntil(5000) { animation?.isRunning == false }
    }
}
