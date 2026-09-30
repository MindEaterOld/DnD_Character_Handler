package com.dndcharacterhandler.presentation.dice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Small 3D math kit for the dice table. Pure Kotlin (no Android types) so the physics can be
// unit-tested on the JVM.

internal data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vec3(x * scale, y * scale, z * scale)
    operator fun div(scale: Double) = Vec3(x / scale, y / scale, z / scale)
    operator fun unaryMinus() = Vec3(-x, -y, -z)
    infix fun dot(other: Vec3) = x * other.x + y * other.y + z * other.z
    infix fun cross(other: Vec3) = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    val length: Double get() = sqrt(this dot this)

    fun normalized(): Vec3 {
        val length = length
        return if (length < 1e-12) this else this / length
    }

    /** This vector, shortened to [maxLength] if it is longer. */
    fun limitedTo(maxLength: Double): Vec3 {
        val length = length
        return if (length > maxLength) this * (maxLength / length) else this
    }

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
        val UP = Vec3(0.0, 1.0, 0.0)

        /** Uniformly distributed unit vector. */
        fun randomUnit(random: Random): Vec3 {
            val z = random.nextDouble(-1.0, 1.0)
            val angle = random.nextDouble(0.0, 2 * PI)
            val radius = sqrt(1 - z * z)
            return Vec3(radius * cos(angle), radius * sin(angle), z)
        }
    }
}

/** Unit quaternion (w + xi + yj + zk) describing an orientation. */
internal data class Quat(val w: Double, val x: Double, val y: Double, val z: Double) {
    operator fun times(other: Quat) = Quat(
        w * other.w - x * other.x - y * other.y - z * other.z,
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w
    )

    fun normalized(): Quat {
        val length = sqrt(w * w + x * x + y * y + z * z)
        return Quat(w / length, x / length, y / length, z / length)
    }

    /** The inverse rotation (for a unit quaternion). */
    fun conjugate(): Quat = Quat(w, -x, -y, -z)

    /** Rotates [v] by this orientation. */
    fun rotate(v: Vec3): Vec3 {
        val axis = Vec3(x, y, z)
        val t = (axis cross v) * 2.0
        return v + t * w + (axis cross t)
    }

    /** Orientation after spinning with world-space angular velocity [omega] for [dt] seconds. */
    fun integrated(omega: Vec3, dt: Double): Quat {
        val spin = Quat(0.0, omega.x, omega.y, omega.z) * this
        return Quat(
            w + 0.5 * dt * spin.w,
            x + 0.5 * dt * spin.x,
            y + 0.5 * dt * spin.y,
            z + 0.5 * dt * spin.z
        ).normalized()
    }

    companion object {
        val IDENTITY = Quat(1.0, 0.0, 0.0, 0.0)

        /** Rotation by [angle] radians about [axis]. */
        fun axisAngle(axis: Vec3, angle: Double): Quat {
            val unit = axis.normalized()
            val s = sin(angle / 2)
            return Quat(cos(angle / 2), unit.x * s, unit.y * s, unit.z * s)
        }

        /** The shortest rotation that turns unit vector [from] into unit vector [to]. */
        fun rotationBetween(from: Vec3, to: Vec3): Quat {
            val cos = from dot to
            if (cos < -0.999999) {
                // Opposite vectors: half a turn about any axis perpendicular to them.
                val axis = (from cross Vec3(1.0, 0.0, 0.0)).let { if (it.length < 1e-6) from cross Vec3.UP else it }
                return axisAngle(axis, PI)
            }
            val axis = from cross to
            return Quat(1 + cos, axis.x, axis.y, axis.z).normalized()
        }

        /** Uniformly distributed random orientation (Shoemake's method). */
        fun random(random: Random): Quat {
            val u1 = random.nextDouble()
            val u2 = random.nextDouble(0.0, 2 * PI)
            val u3 = random.nextDouble(0.0, 2 * PI)
            val a = sqrt(1 - u1)
            val b = sqrt(u1)
            return Quat(a * sin(u2), a * cos(u2), b * sin(u3), b * cos(u3))
        }
    }
}
