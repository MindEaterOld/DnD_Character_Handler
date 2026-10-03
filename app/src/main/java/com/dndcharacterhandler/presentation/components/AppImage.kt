package com.dndcharacterhandler.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.model.PortraitFraming
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface ImageSourceKind {
    data class Drawable(@DrawableRes val resId: Int) : ImageSourceKind
    data class BitmapRef(val reference: String) : ImageSourceKind
}

@Composable
fun AppImage(
    imageRef: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** Which part of the picture shows (a portrait's own framing); null draws it with [contentScale]. */
    framing: PortraitFraming? = null,
    fallback: @Composable (() -> Unit)? = null
) {
    val painter = rememberAppImagePainter(imageRef)
    when {
        painter != null && framing != null -> FramedImage(painter, framing, contentDescription, modifier)
        painter != null -> Image(painter = painter, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
        fallback != null -> Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
    }
}

/**
 * The picture behind [imageRef] (a drawable, an asset, a file or a content URI), or null while it
 * loads or when it can't be read. A file is decoded off the main thread so disk I/O doesn't jank
 * composition (e.g. drawer portraits); a preview, one static frame that doesn't wait, decodes at once.
 */
@Composable
fun rememberAppImagePainter(imageRef: String?): Painter? {
    val context = LocalContext.current
    // Cheap, synchronous: classify the reference without reading/decoding any bytes.
    val sourceKind = remember(imageRef) {
        classifyImageSource(imageRef) { name ->
            context.resources.getIdentifier(name, "drawable", context.packageName)
        }
    }
    return when (sourceKind) {
        is ImageSourceKind.Drawable -> painterResource(id = sourceKind.resId)
        is ImageSourceKind.BitmapRef -> {
            val inPreview = LocalInspectionMode.current
            val previewPainter = remember(sourceKind.reference, inPreview) {
                if (!inPreview) null
                else runCatching { decodeBitmap(context, sourceKind.reference) }.getOrNull()?.let { BitmapPainter(it.asImageBitmap()) }
            }
            val painter by produceState(initialValue = previewPainter, sourceKind.reference) {
                if (previewPainter != null) return@produceState
                value = withContext(Dispatchers.IO) {
                    runCatching { decodeBitmap(context, sourceKind.reference) }
                        .getOrNull()
                        ?.let { BitmapPainter(it.asImageBitmap()) }
                }
            }
            painter
        }
        null -> null
    }
}

/** [painter] laid in the box by [framing]: covering it, the framing's focus at its centre. */
@Composable
private fun FramedImage(painter: Painter, framing: PortraitFraming, contentDescription: String?, modifier: Modifier) {
    Canvas(
        modifier = modifier
            .clipToBounds()
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        val image = painter.intrinsicSize
        if (image.isUnspecified || image.width <= 0f || image.height <= 0f) {
            with(painter) { draw(size) }
            return@Canvas
        }
        val placed = framing.placement(image.width, image.height, size.width, size.height)
        translate(placed.left, placed.top) {
            with(painter) { draw(Size(placed.width, placed.height)) }
        }
    }
}

private fun classifyImageSource(
    rawValue: String?,
    drawableResolver: (String) -> Int
): ImageSourceKind? {
    val reference = rawValue?.trim().orEmpty()
    if (reference.isEmpty()) return null

    val drawableName = when {
        reference.startsWith(AssetReferences.drawableReferencePrefix) -> {
            reference.removePrefix(AssetReferences.drawableReferencePrefix)
        }

        reference.startsWith("res:drawable/") -> {
            reference.removePrefix("res:drawable/")
        }

        !reference.contains('/') && !reference.contains(':') && !reference.contains('\\') -> {
            reference
        }

        else -> null
    }

    if (!drawableName.isNullOrBlank()) {
        val resId = drawableResolver(drawableName)
        if (resId != 0) {
            return ImageSourceKind.Drawable(resId)
        }
    }

    return ImageSourceKind.BitmapRef(reference)
}

/**
 * Longest side, in pixels, that images are decoded at. Portraits are often full-size camera photos;
 * decoding those as-is costs ~200 MB per copy and can crash with "trying to draw too large bitmap".
 * 2048 px still looks sharp in the full-screen portrait viewer.
 */
private const val MaxDecodedImageSide = 2048

private fun decodeBitmap(context: Context, reference: String): android.graphics.Bitmap? {
    val open: () -> InputStream? = when {
        reference.startsWith("content://") || reference.startsWith("file://") -> {
            { context.contentResolver.openInputStream(Uri.parse(reference)) }
        }

        reference.startsWith("${AssetReferences.iconsRoot}/") ||
            reference.startsWith("${AssetReferences.portraitsRoot}/") -> {
            { context.assets.open(reference) }
        }

        File(reference).exists() -> {
            { FileInputStream(reference) }
        }

        else -> return null
    }

    // First pass reads only the dimensions, second pass decodes at a power-of-two scale.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    (open() ?: return null).use { BitmapFactory.decodeStream(it, null, bounds) }
    val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
    var sampleSize = 1
    while (longestSide / sampleSize > MaxDecodedImageSide) sampleSize *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return (open() ?: return null).use { BitmapFactory.decodeStream(it, null, options) }
}
