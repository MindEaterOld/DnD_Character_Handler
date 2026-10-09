package com.dndcharacterhandler.domain.model

/**
 * Which part of a portrait shows in its frame: the point of the picture at the frame's centre
 * ([focusX], [focusY], from 0 to 1 across the picture) and how far in it is ([zoom]: 1 = the picture
 * just covers the frame, as a centre crop would). It doesn't depend on the frame's size, so the
 * overview's octagon and the drawer's small circle show the same part.
 */
data class PortraitFraming(
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
    val zoom: Float = 1f
) {
    /** Where the picture lies in its frame: its left and top edges (from the frame's), its size. */
    data class Placement(val left: Float, val top: Float, val width: Float, val height: Float)

    /**
     * The picture ([imageWidth] × [imageHeight]) laid in a frame of [frameWidth] × [frameHeight]:
     * scaled to cover it, then [zoom]ed, with the focus at the frame's centre — but never so far that
     * an edge of the picture comes into the frame.
     */
    fun placement(imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float): Placement {
        val cover = maxOf(frameWidth / imageWidth, frameHeight / imageHeight)
        val width = imageWidth * cover * zoom.coerceIn(1f, MAX_ZOOM)
        val height = imageHeight * cover * zoom.coerceIn(1f, MAX_ZOOM)
        // In floats a picture scaled to cover can come out a hair smaller than the frame: then it has no room to move.
        val left = (frameWidth / 2f - focusX * width).coerceIn(minOf(frameWidth - width, 0f), 0f)
        val top = (frameHeight / 2f - focusY * height).coerceIn(minOf(frameHeight - height, 0f), 0f)
        return Placement(left, top, width, height)
    }

    /** Dragged by [dx], [dy] in the frame: the picture follows the finger, its edges stay outside the frame. */
    fun panned(dx: Float, dy: Float, imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float): PortraitFraming {
        val placed = placement(imageWidth, imageHeight, frameWidth, frameHeight)
        val moved = copy(focusX = focusX - dx / placed.width, focusY = focusY - dy / placed.height)
        return moved.settled(imageWidth, imageHeight, frameWidth, frameHeight)
    }

    /** At [newZoom] (between 1 and [MAX_ZOOM]), the focus kept where it can be. */
    fun zoomedTo(newZoom: Float, imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float): PortraitFraming =
        copy(zoom = newZoom.coerceIn(1f, MAX_ZOOM)).settled(imageWidth, imageHeight, frameWidth, frameHeight)

    /** The focus brought to where the picture still covers the frame, so a later drag starts from what shows. */
    fun settled(imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float): PortraitFraming {
        val placed = placement(imageWidth, imageHeight, frameWidth, frameHeight)
        return copy(
            focusX = (frameWidth / 2f - placed.left) / placed.width,
            focusY = (frameHeight / 2f - placed.top) / placed.height,
            zoom = zoom.coerceIn(1f, MAX_ZOOM)
        )
    }

    companion object {
        /** How far in the picture can go. */
        const val MAX_ZOOM = 4f
    }
}
