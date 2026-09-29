package com.dndcharacterhandler.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.dndcharacterhandler.domain.model.AssetReferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

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
    fallback: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    // Cheap, synchronous: classify the reference without reading/decoding any bytes.
    val sourceKind = remember(imageRef) {
        classifyImageSource(imageRef) { name ->
            context.resources.getIdentifier(name, "drawable", context.packageName)
        }
    }

    when (sourceKind) {
        is ImageSourceKind.Drawable -> {
            Image(
                painter = painterResource(id = sourceKind.resId),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }

        is ImageSourceKind.BitmapRef -> {
            // Decode off the main thread so disk I/O + bitmap decode doesn't jank composition
            // (e.g. drawer portraits). Falls back until the bitmap is ready or if decoding fails.
            val painter by produceState<BitmapPainter?>(initialValue = null, sourceKind.reference) {
                value = withContext(Dispatchers.IO) {
                    runCatching { decodeBitmap(context, sourceKind.reference) }
                        .getOrNull()
                        ?.let { BitmapPainter(it.asImageBitmap()) }
                }
            }
            val resolvedPainter = painter
            if (resolvedPainter != null) {
                Image(
                    painter = resolvedPainter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            } else if (fallback != null) {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    fallback()
                }
            }
        }

        null -> {
            if (fallback != null) {
                Box(
                    modifier = modifier,
                    contentAlignment = Alignment.Center
                ) {
                    fallback()
                }
            }
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

private fun decodeBitmap(context: Context, reference: String): android.graphics.Bitmap? =
    when {
        reference.startsWith("content://") || reference.startsWith("file://") -> {
            context.contentResolver.openInputStream(Uri.parse(reference)).use(BitmapFactory::decodeStream)
        }

        reference.startsWith("${AssetReferences.iconsRoot}/") ||
            reference.startsWith("${AssetReferences.portraitsRoot}/") -> {
            context.assets.open(reference).use(BitmapFactory::decodeStream)
        }

        File(reference).exists() -> {
            FileInputStream(reference).use(BitmapFactory::decodeStream)
        }

        else -> null
    }
