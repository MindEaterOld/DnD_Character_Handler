package com.dndcharacterhandler.presentation.dice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * Materials the workshop makes by itself, as ARGB pixels of a square [PATTERN_SIZE] wide: the same
 * seed always gives the same picture, so a skin keeps only its seed.
 */
internal object DicePatterns {
    const val PATTERN_SIZE = 384

    /** Marble: [vein]-coloured veins winding through [base], finer ones beside them, a faint cloudiness. */
    fun marble(base: Int, vein: Int, seed: Int, size: Int = PATTERN_SIZE): IntArray {
        val noise = ValueNoise(seed)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val v = y.toDouble() / size
            for (x in 0 until size) {
                val u = x.toDouble() / size
                // One smooth warp bends every vein, so they run side by side like real marble.
                val warp = noise.fbm(u * 2.2, v * 2.2)
                val mottle = noise.fbm(u * 8.0 + 3.0, v * 8.0 + 5.0)
                val main = smoothstep(0.9, 0.995, 1 - abs(sin((u * 2.6 + v * 1.4 + warp * 3.2) * PI)))
                val minor = smoothstep(0.955, 0.998, 1 - abs(sin((u * 1.3 - v * 3.1 + warp * 2.4 + 0.7) * PI * 1.6))) * 0.45
                val cloud = (mottle - 0.5) * 0.12 + (warp - 0.5) * 0.1
                pixels[y * size + x] = mix(scale(base, 1 + cloud), vein, maxOf(main, minor))
            }
        }
        return pixels
    }

    /** A nebula: clouds and wisps of [color] with [glow] at their hearts over [base], and stars. */
    fun nebula(base: Int, color: Int, glow: Int, seed: Int, size: Int = PATTERN_SIZE): IntArray {
        val noise = ValueNoise(seed)
        val stars = ValueNoise(seed * 31 + 7)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val v = y.toDouble() / size
            for (x in 0 until size) {
                val u = x.toDouble() / size
                val cloud = smoothstep(0.3, 0.78, noise.fbm(u * 2.5, v * 2.5))
                val wisps = smoothstep(0.58, 0.9, noise.fbm(u * 6.0 + 4.0, v * 6.0 + 1.0)) * 0.3
                val heart = smoothstep(0.5, 0.85, noise.fbm(u * 5.0 + 11.0, v * 5.0 + 3.0)) * cloud
                var pixel = mix(base, color, cloud * 0.9)
                pixel = mix(pixel, color, wisps)
                pixel = mix(pixel, glow, heart * 0.85)
                val sparkle = stars.hash(x, y)
                if (sparkle > 0.995) pixel = mix(pixel, 0xFFFFFFFF.toInt(), (sparkle - 0.995) / 0.005)
                pixels[y * size + x] = pixel
            }
        }
        return pixels
    }

    private fun smoothstep(from: Double, to: Double, value: Double): Double {
        val t = ((value - from) / (to - from)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    private fun channel(color: Int, shift: Int) = (color shr shift) and 0xFF

    private fun mix(from: Int, to: Int, amount: Double): Int {
        val t = amount.coerceIn(0.0, 1.0)
        fun lerp(shift: Int) = (channel(from, shift) + (channel(to, shift) - channel(from, shift)) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (lerp(16) shl 16) or (lerp(8) shl 8) or lerp(0)
    }

    private fun scale(color: Int, factor: Double): Int {
        fun part(shift: Int) = (channel(color, shift) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (part(16) shl 16) or (part(8) shl 8) or part(0)
    }
}

/** Smooth value noise on a lattice hashed from a seed; [fbm] sums five octaves of it, 0 to 1. */
private class ValueNoise(private val seed: Int) {
    fun hash(x: Int, y: Int): Double {
        var h = x * 374761393 + y * 668265263 + seed * 1442695041
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0
    }

    fun value(x: Double, y: Double): Double {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val sx = fx * fx * (3 - 2 * fx)
        val sy = fy * fy * (3 - 2 * fy)
        val top = hash(x0, y0) + (hash(x0 + 1, y0) - hash(x0, y0)) * sx
        val bottom = hash(x0, y0 + 1) + (hash(x0 + 1, y0 + 1) - hash(x0, y0 + 1)) * sx
        return top + (bottom - top) * sy
    }

    fun fbm(x: Double, y: Double): Double {
        var sum = 0.0
        var amplitude = 0.5
        var frequency = 1.0
        var total = 0.0
        repeat(5) {
            sum += value(x * frequency, y * frequency) * amplitude
            total += amplitude
            amplitude *= 0.5
            frequency *= 2.0
        }
        return sum / total
    }
}
