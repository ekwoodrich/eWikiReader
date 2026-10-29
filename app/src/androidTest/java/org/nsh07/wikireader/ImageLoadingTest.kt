package org.nsh07.wikireader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.nsh07.wikireader.ui.image.fitDisplay

@RunWith(AndroidJUnit4::class)
class ImageLoadingTest {
    @Test
    fun oversizedImageIsDecodedWithinDisplayBounds() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // A 6000 x 6000 source would allocate 144 MB at original ARGB size.
        val bytes = instrumentation.context.assets.open("oversized.png").use { it.readBytes() }
        val request = ImageRequest.Builder(context)
            .data(bytes)
            .fitDisplay(context)
            .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        assertTrue("Decode failed: $result", result is SuccessResult)
        val image = (result as SuccessResult).image
        assertTrue("Unexpected width ${image.width}", image.width in 1..2048)
        assertTrue("Unexpected height ${image.height}", image.height in 1..2048)
        assertTrue(image.width <= context.resources.displayMetrics.widthPixels)
        assertTrue(image.height <= context.resources.displayMetrics.heightPixels)
    }
}
