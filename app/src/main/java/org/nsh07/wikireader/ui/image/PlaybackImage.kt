package org.nsh07.wikireader.ui.image

import android.graphics.drawable.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.nsh07.wikireader.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.DrawableImage
import coil3.compose.AsyncImagePainter
import org.nsh07.wikireader.ui.theme.LocalAppAnimationsEnabled

/** Keep the decoded first frame visible until this image is explicitly played. */
@Composable
fun PlaybackImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null
) {
    val enabled = LocalAppAnimationsEnabled.current
    val state = (painter as? AsyncImagePainter)?.state?.collectAsState()?.value
    val drawable = ((state as? AsyncImagePainter.State.Success)?.result?.image as? DrawableImage)?.drawable as? Animatable
    var playing by remember(drawable, enabled) { mutableStateOf(false) }
    // AsyncImagePainter starts its drawable when it becomes remembered. Apply our policy afterwards.
    LaunchedEffect(drawable, enabled, playing) {
        if (drawable != null) {
            if (enabled || playing) drawable.start() else drawable.stop()
        }
    }
    DisposableEffect(drawable) { onDispose { drawable?.stop() } }
    Box {
        Image(painter, contentDescription, modifier, contentScale = contentScale, colorFilter = colorFilter)
        if (!enabled && drawable != null) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter), shape = MaterialTheme.shapes.small) {
                IconButton(onClick = { playing = !playing }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        painterResource(if (playing) R.drawable.pause_animation else R.drawable.play_animation),
                        stringResource(if (playing) R.string.pause_animation else R.string.play_animation)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackAsyncImage(
    model: Any?,
    imageLoader: coil3.ImageLoader,
    contentDescription: String?,
    contentScale: ContentScale,
    error: Painter?,
    modifier: Modifier
) {
    val painter = coil3.compose.rememberAsyncImagePainter(
        model = model, imageLoader = imageLoader, error = error, contentScale = contentScale
    )
    PlaybackImage(painter, contentDescription, modifier, contentScale)
}
