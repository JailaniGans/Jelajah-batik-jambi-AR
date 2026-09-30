package com.jelajahbatikjambi.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes and displays a motif image — either a bundled `assets/` image
 * (e.g. `assets/images/`) or, for a user-uploaded custom motif, an absolute
 * file path in internal storage. Distinguished by a simple convention: a
 * path starting with "/" is an absolute file path (asset paths in this app
 * are always relative, like "images/angso_duo.jpeg"), never both at once.
 * All images are local and small, so a plain manual decode off the main
 * thread is simpler and lighter than pulling in an image-loading library
 * built for network images (§56).
 */
@Composable
fun AssetImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (path.startsWith("/")) {
                    BitmapFactory.decodeFile(path)
                } else {
                    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
