package org.nsh07.wikireader.ui.image

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader

/** App image loader selected for the current animation preference. */
val LocalAppImageLoader = staticCompositionLocalOf<ImageLoader?> { null }
