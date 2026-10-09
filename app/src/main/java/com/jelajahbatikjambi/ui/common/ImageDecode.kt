package com.jelajahbatikjambi.ui.common

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream

/**
 * Decodes a user-picked image (via GetContent / PickVisualMedia) with its
 * EXIF rotation applied.
 *
 * [BitmapFactory.decodeStream]/[decodeByteArray] ignore the JPEG EXIF
 * orientation tag, while [BitmapFactory.decodeFile] applies it. A typical
 * phone photo relies on that tag — the raw sensor image is sideways, and the
 * tag says how to rotate it for display. Saving the naive decode as an AR
 * reference would therefore store a *rotated* photo that the camera can never
 * match against the upright object in the real world. This helper reads the
 * bytes once, decodes them, reads the orientation tag from the same bytes,
 * and rotates when needed, so add/edit flows keep the photo the way the user
 * sees it in the preview (and the way the AR camera sees the real cloth).
 */
fun decodePickedImage(resolver: ContentResolver, uri: Uri): Bitmap? = runCatching {
    resolver.openInputStream(uri)?.use { input ->
        val bytes = input.readBytes()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
        val orientation = ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees == 0f) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) bitmap.recycle()
            rotated
        }
    }
}.getOrNull()