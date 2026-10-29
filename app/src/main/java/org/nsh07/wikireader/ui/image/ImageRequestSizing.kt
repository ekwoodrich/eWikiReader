package org.nsh07.wikireader.ui.image

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale

/** Bound decoded images even when a painter is measured inside an unbounded scrolling layout. */
fun ImageRequest.Builder.fitDisplay(context: Context): ImageRequest.Builder {
    val metrics = context.resources.displayMetrics
    return size(metrics.widthPixels.coerceIn(1, 2048), metrics.heightPixels.coerceIn(1, 2048))
        .scale(Scale.FIT)
        .precision(Precision.EXACT)
}
