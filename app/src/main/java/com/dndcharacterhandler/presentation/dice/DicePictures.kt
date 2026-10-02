package com.dndcharacterhandler.presentation.dice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/*
 * The dice's only platform part in drawing: turning pixels and picture files into Compose pictures.
 * Everything else the dice draw goes through Compose; another platform replaces just these two.
 */

/** A [size]×[size] picture of ARGB [pixels] (a generated material). */
internal fun imageOfPixels(pixels: IntArray, size: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()

/** The picture in [file] (a material, a filled-in face template); null when it can't be read. */
internal fun readPicture(file: File): ImageBitmap? = BitmapFactory.decodeFile(file.path)?.asImageBitmap()
