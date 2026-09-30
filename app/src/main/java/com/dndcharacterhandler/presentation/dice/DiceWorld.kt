package com.dndcharacterhandler.presentation.dice

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * One die on the table. Dice are treated as rigid convex bodies of unit mass with an isotropic
 * inertia (exact for the platonic dice, close enough for the d10).
 */
internal class DieBody(
    val shape: DieShape,
    var position: Vec3,
    var orientation: Quat
) {
    var velocity: Vec3 = Vec3.ZERO
    var angularVelocity: Vec3 = Vec3.ZERO
    val inverseInertia: Double = 1.0 / (0.3 * shape.circumradius * shape.circumradius)

    /** Following the player's finger instead of falling freely. */
    var held = false
    var handOffset: Vec3 = Vec3.ZERO
    var settled = false
    internal var restTime = 0.0
    internal var leanTime = 0.0
    internal var touchingFloor = false
    /** Direction away from a wall or another die this die is leaning on this step, or null. */
    internal var leanAway: Vec3? = null
    internal var lastLeanAway: Vec3? = null

    fun worldVertex(index: Int): Vec3 = position + orientation.rotate(shape.vertices[index])

    /** The number shown on top — for a d4 the corner pointing up. */
    fun topValue(): Int {
        val vertexValues = shape.vertexValues
        return if (vertexValues != null) {
            vertexValues[shape.vertices.indices.maxBy { worldVertex(it).y }]
        } else {
            shape.faces.maxBy { orientation.rotate(it.normal) dot Vec3.UP }.value
        }
    }

    /** How squarely the die rests: 1 when a face lies flat on the table. */
    fun flatness(): Double =
        shape.faces.maxOf { -(orientation.rotate(it.normal) dot Vec3.UP) }
}

/**
 * The dice table: floor at y = 0, a top-down camera at height [cameraHeight], and invisible walls
 * along the camera's view pyramid — so the screen edges act as walls at every height and a die can
 * never leave the screen. Everything is in "die units" (a d6 is about 1.4 wide).
 */
internal class DiceWorld(private val random: Random = Random.Default) {
    val bodies = mutableListOf<DieBody>()

    var halfWidth = 4.2
        private set
    var halfDepth = 7.5
        private set
    val cameraHeight = 24.0
    val handHeight = 3.2
    private val ceilingHeight = 10.0

    private var hand: Vec3? = null
    private var timeSinceRelease = 0.0
    private var accumulator = 0.0

    /** How many times a cocked die had to be nudged (diagnostics for tests). */
    internal var nudges = 0
        private set

    val isHolding: Boolean get() = hand != null
    val allSettled: Boolean get() = hand == null && bodies.isNotEmpty() && bodies.all { it.settled }

    private class Plane(val normal: Vec3, val offset: Double, val restitution: Double)

    private var planes: List<Plane> = buildPlanes()

    /** Sizes the table to the screen: [halfWidth] is fixed, the depth follows the aspect ratio. */
    fun setAspectRatio(heightOverWidth: Double) {
        halfDepth = halfWidth * heightOverWidth
        planes = buildPlanes()
    }

    private fun buildPlanes(): List<Plane> {
        val h = cameraHeight
        fun wall(nx: Double, ny: Double, nz: Double, halfSize: Double): Plane {
            val normal = Vec3(nx, ny, nz)
            val length = normal.length
            return Plane(normal / length, -h * halfSize / length, restitution = 0.45)
        }
        return listOf(
            Plane(Vec3.UP, 0.0, restitution = 0.3),
            Plane(-Vec3.UP, -ceilingHeight, restitution = 0.2),
            wall(-h, -halfWidth, 0.0, halfWidth),
            wall(h, -halfWidth, 0.0, halfWidth),
            wall(0.0, -halfDepth, -h, halfDepth),
            wall(0.0, -halfDepth, h, halfDepth)
        )
    }

    /** Puts fresh dice of [kinds] into the player's hand at [handPosition]. */
    fun spawnInHand(kinds: List<DieShapeKind>, handPosition: Vec3) {
        bodies.clear()
        kinds.forEach { kind ->
            bodies += DieBody(DieShapes.of(kind), handPosition, Quat.random(random))
        }
        grab(handPosition)
        bodies.forEach { it.position = handPosition + it.handOffset }
    }

    /** Picks all dice up again: they fly to the finger and follow it. */
    fun grab(handPosition: Vec3) {
        hand = clampToHandArea(handPosition)
        bodies.forEachIndexed { index, body ->
            // A loose sunflower cluster, spaced so neighbouring dice sit side by side in the "hand"
            // instead of inside each other.
            val radius = HAND_SPACING * sqrt(index.toDouble())
            val angle = index * 2.39996
            body.handOffset = Vec3(radius * cos(angle), (index % 2) * 0.35, radius * sin(angle))
            body.held = true
            body.settled = false
            body.restTime = 0.0
        }
    }

    fun moveHand(handPosition: Vec3) {
        if (hand != null) hand = clampToHandArea(handPosition)
    }

    /**
     * Lets go: the dice keep the finger's velocity plus a random spin, so no two throws are alike and
     * even a gentle drop tumbles — the face that ends up on top is the honest result.
     */
    fun release(handVelocity: Vec3) {
        if (hand == null) return
        hand = null
        timeSinceRelease = 0.0
        val throwVelocity = handVelocity.let { if (it.length > MAX_THROW_SPEED) it.normalized() * MAX_THROW_SPEED else it }
        bodies.forEach { body ->
            body.held = false
            val spread = Vec3(random.nextDouble(-1.0, 1.0), 0.0, random.nextDouble(-1.0, 1.0)) * 0.8
            body.velocity = throwVelocity * random.nextDouble(0.85, 1.15) + spread +
                Vec3(0.0, random.nextDouble(0.5, 2.5), 0.0)
            body.angularVelocity = Vec3.randomUnit(random) * random.nextDouble(10.0, 18.0) +
                (Vec3.UP cross body.velocity) * (0.6 / body.shape.circumradius)
        }
    }

    /** Advances the simulation by [seconds] of real time using fixed sub-steps. */
    fun advance(seconds: Double) {
        accumulator += min(seconds, 0.05)
        while (accumulator >= STEP) {
            step(STEP)
            accumulator -= STEP
        }
    }

    private fun clampToHandArea(point: Vec3): Vec3 {
        // Inside the view pyramid at hand height, minus a die radius, so held dice stay on screen.
        val scale = (cameraHeight - handHeight) / cameraHeight
        val maxX = max(0.0, halfWidth * scale - 1.2)
        val maxZ = max(0.0, halfDepth * scale - 1.2)
        return Vec3(point.x.coerceIn(-maxX, maxX), handHeight, point.z.coerceIn(-maxZ, maxZ))
    }

    fun step(dt: Double) {
        val currentHand = hand
        if (currentHand == null) timeSinceRelease += dt

        bodies.forEach { body ->
            if (body.settled) return@forEach
            if (body.held && currentHand != null) {
                // Pulled toward the finger by a soft spring: the dice lag, jostle and swing along.
                val target = currentHand + body.handOffset
                val pull = (target - body.position) * HAND_STIFFNESS - body.velocity * HAND_DAMPING
                body.velocity = body.velocity + pull * dt
                val rolling = (Vec3.UP cross body.velocity) * (0.9 / body.shape.circumradius)
                body.angularVelocity = body.angularVelocity + (rolling - body.angularVelocity) * min(1.0, 6 * dt) +
                    Vec3.randomUnit(random) * (pull.length * 0.004)
            } else {
                body.velocity = body.velocity + GRAVITY * dt
            }
            body.velocity = body.velocity * (1 - 0.05 * dt)
            body.angularVelocity = body.angularVelocity * (1 - 0.1 * dt)
        }

        resolvePlaneContacts()
        repeat(DIE_SOLVER_ITERATIONS) { resolveDieContacts() }

        bodies.forEach { body ->
            if (body.settled) return@forEach
            body.position = body.position + body.velocity * dt
            body.orientation = body.orientation.integrated(body.angularVelocity, dt)
        }

        updateRest(dt)
    }

    private class Contact(val body: DieBody, val r: Vec3, val plane: Plane, val bias: Double) {
        var normalImpulse = 0.0
    }

    private fun resolvePlaneContacts() {
        val contacts = mutableListOf<Contact>()
        bodies.forEach { body ->
            body.touchingFloor = false
            body.leanAway = null
            if (body.settled) return@forEach
            for (plane in planes) {
                var deepest = 0.0
                for (index in body.shape.vertices.indices) {
                    val point = body.worldVertex(index)
                    val separation = (plane.normal dot point) - plane.offset
                    if (separation >= CONTACT_SLOP) continue
                    val r = point - body.position
                    val normalSpeed = (body.velocity + (body.angularVelocity cross r)) dot plane.normal
                    // Bounce only off real impacts; resting contacts get no restitution so dice settle.
                    val bias = if (normalSpeed < -BOUNCE_THRESHOLD) -plane.restitution * normalSpeed else 0.0
                    contacts += Contact(body, r, plane, bias)
                    deepest = max(deepest, -separation)
                    if (plane.normal.y > 0.99) {
                        body.touchingFloor = true
                    } else if (plane.normal.y > -0.99) {
                        body.leanAway = Vec3(plane.normal.x, 0.0, plane.normal.z).normalized()
                    }
                }
                if (deepest > 0) {
                    body.position = body.position + plane.normal * (deepest * 0.4)
                }
            }
        }
        if (contacts.isEmpty()) return

        repeat(SOLVER_ITERATIONS) {
            for (contact in contacts) {
                val body = contact.body
                val normal = contact.plane.normal
                val r = contact.r
                val relative = body.velocity + (body.angularVelocity cross r)
                val normalSpeed = relative dot normal
                val rCrossN = r cross normal
                val normalMass = 1.0 / (1.0 + body.inverseInertia * (rCrossN dot rCrossN))
                val previous = contact.normalImpulse
                contact.normalImpulse = max(previous + (contact.bias - normalSpeed) * normalMass, 0.0)
                val deltaNormal = contact.normalImpulse - previous
                applyImpulse(body, r, normal * deltaNormal)

                val afterNormal = body.velocity + (body.angularVelocity cross r)
                val tangentVelocity = afterNormal - normal * (afterNormal dot normal)
                val tangentSpeed = tangentVelocity.length
                if (tangentSpeed > 1e-7) {
                    val tangent = tangentVelocity / tangentSpeed
                    val rCrossT = r cross tangent
                    val tangentMass = 1.0 / (1.0 + body.inverseInertia * (rCrossT dot rCrossT))
                    val friction = min(tangentSpeed * tangentMass, FRICTION * contact.normalImpulse)
                    applyImpulse(body, r, tangent * -friction)
                }
            }
        }
    }

    private fun applyImpulse(body: DieBody, r: Vec3, impulse: Vec3) {
        body.velocity = body.velocity + impulse
        body.angularVelocity = body.angularVelocity + (r cross impulse) * body.inverseInertia
    }

    /** A point where two dice touch; [normal] points out of the second die toward the first. */
    private class PairContact(val point: Vec3, val normal: Vec3, val depth: Double)

    /**
     * Die-to-die collisions between the actual polyhedra: every corner of one die that has entered
     * the other is pushed out through the nearest face. Edge-on-edge hits, which have no corner
     * inside, are caught by each die's inscribed sphere, so dice can't sink into each other.
     */
    private fun resolveDieContacts() {
        for (i in bodies.indices) for (j in i + 1 until bodies.size) {
            val a = bodies[i]
            val b = bodies[j]
            if (a.settled && b.settled) continue
            val delta = a.position - b.position
            val distance = delta.length
            if (distance >= a.shape.circumradius + b.shape.circumradius || distance < 1e-9) continue

            val contacts = cornerContacts(corner = a, solid = b) +
                cornerContacts(corner = b, solid = a).map { PairContact(it.point, -it.normal, it.depth) }
            // Deep overlap (or an edge-on-edge hit with no corner inside): push the inscribed spheres apart.
            val sphereOverlap = a.shape.inradius + b.shape.inradius - distance
            val allContacts = if (sphereOverlap > 0) {
                val normal = delta / distance
                contacts + PairContact(b.position + normal * b.shape.inradius, normal, sphereOverlap)
            } else {
                contacts
            }
            allContacts.forEach { resolvePairContact(a, b, it) }
        }
    }

    /** Corners of [corner] that are inside [solid], each with the nearest face's normal (out of [solid]). */
    private fun cornerContacts(corner: DieBody, solid: DieBody): List<PairContact> {
        val toSolid = solid.orientation.conjugate()
        val contacts = mutableListOf<PairContact>()
        for (index in corner.shape.vertices.indices) {
            val world = corner.worldVertex(index)
            val local = toSolid.rotate(world - solid.position)
            if (local.length > solid.shape.circumradius) continue
            var nearest: DieFace? = null
            var nearestSeparation = -Double.MAX_VALUE
            var inside = true
            for (face in solid.shape.faces) {
                val separation = (face.normal dot local) - (face.normal dot face.center)
                if (separation > 0) {
                    inside = false
                    break
                }
                if (separation > nearestSeparation) {
                    nearestSeparation = separation
                    nearest = face
                }
            }
            if (inside && nearest != null) {
                contacts += PairContact(world, solid.orientation.rotate(nearest.normal), -nearestSeparation)
            }
        }
        return contacts
    }

    private fun resolvePairContact(a: DieBody, b: DieBody, contact: PairContact) {
        val normal = contact.normal
        val movableA = if (a.settled) 0.0 else 1.0
        val movableB = if (b.settled) 0.0 else 1.0
        if (movableA + movableB == 0.0) return
        val horizontal = Vec3(normal.x, 0.0, normal.z)
        if (horizontal.length > 1e-6) {
            a.leanAway = horizontal.normalized()
            b.leanAway = -horizontal.normalized()
        }

        val ra = contact.point - a.position
        val rb = contact.point - b.position
        val relative = (a.velocity + (a.angularVelocity cross ra)) - (b.velocity + (b.angularVelocity cross rb))
        val normalSpeed = relative dot normal
        if (normalSpeed < 0) {
            val raCrossN = ra cross normal
            val rbCrossN = rb cross normal
            val effectiveMass = 1.0 / (
                movableA * (1 + a.inverseInertia * (raCrossN dot raCrossN)) +
                    movableB * (1 + b.inverseInertia * (rbCrossN dot rbCrossN))
                )
            val restitution = if (normalSpeed < -BOUNCE_THRESHOLD) 0.25 else 0.0
            val impulse = -(1 + restitution) * normalSpeed * effectiveMass
            if (a.settled && impulse > WAKE_IMPULSE) wake(a)
            if (b.settled && impulse > WAKE_IMPULSE) wake(b)
            if (!a.settled) applyImpulse(a, ra, normal * impulse)
            if (!b.settled) applyImpulse(b, rb, normal * -impulse)

            val after = (a.velocity + (a.angularVelocity cross ra)) - (b.velocity + (b.angularVelocity cross rb))
            val tangentVelocity = after - normal * (after dot normal)
            val tangentSpeed = tangentVelocity.length
            if (tangentSpeed > 1e-7) {
                val tangent = tangentVelocity / tangentSpeed
                val raCrossT = ra cross tangent
                val rbCrossT = rb cross tangent
                val tangentMass = 1.0 / (
                    movableA * (1 + a.inverseInertia * (raCrossT dot raCrossT)) +
                        movableB * (1 + b.inverseInertia * (rbCrossT dot rbCrossT))
                    )
                val friction = min(tangentSpeed * tangentMass, DIE_FRICTION * impulse)
                if (!a.settled) applyImpulse(a, ra, tangent * -friction)
                if (!b.settled) applyImpulse(b, rb, tangent * friction)
            }
        }

        val share = 0.4 * contact.depth / (movableA + movableB)
        a.position = a.position + normal * (share * movableA)
        b.position = b.position - normal * (share * movableB)
        // Two slow dice pressed together would otherwise keep nudging each other and creep.
        if (a.velocity.length < 0.5 && b.velocity.length < 0.5) {
            val damping = max(0.0, 1 - 3 * STEP)
            if (!a.held) a.velocity = a.velocity * damping
            if (!b.held) b.velocity = b.velocity * damping
        }
    }

    private fun wake(body: DieBody) {
        body.settled = false
        body.restTime = 0.0
    }

    private fun updateRest(dt: Double) {
        bodies.forEach { body ->
            if (body.settled || body.held) return@forEach
            val speed = body.velocity.length
            val spin = body.angularVelocity.length
            // Rolling resistance bleeds off the last bit of motion — but only once a face is (almost)
            // flat on the table. Damping a die that is still tipping over an edge would freeze it there.
            if (body.touchingFloor && speed < 0.6 && spin < 1.5 && body.flatness() > 0.97) {
                val damping = max(0.0, 1 - 7 * dt)
                body.velocity = body.velocity * damping
                body.angularVelocity = body.angularVelocity * damping
            }
            // Spinning friction: contact friction only resists sliding, so a die pinned on a corner
            // (often against a wall) could otherwise keep twirling in place like a top.
            if (body.touchingFloor && speed < 0.3) {
                body.angularVelocity = body.angularVelocity * max(0.0, 1 - 4 * dt)
            }
            // The floor contact can flicker for a step on a resting die, so it only has to start the
            // rest timer, not hold it.
            val resting = speed < 0.15 && spin < 0.35
            if (resting && (body.touchingFloor || body.restTime > 0)) {
                body.restTime += dt
            } else {
                body.restTime = 0.0
            }
            // A die leaning on a wall or another die — or lying on top of another die — is stable enough
            // to jitter there forever without ever counting as "at rest", so once it has stopped
            // tumbling it gets pushed away from what it leans on. Contacts with a neighbour flicker
            // from step to step, so the last known direction is kept.
            body.leanAway?.let { body.lastLeanAway = it }
            val supported = body.touchingFloor || body.leanAway != null
            val stuck = body.flatness() < FLAT_ENOUGH || !body.touchingFloor
            // Resting on another die jiggles more than resting on the table, so allow more speed there.
            val speedLimit = if (body.touchingFloor) 1.0 else 2.5
            if (supported && stuck && speed < speedLimit && spin < 3.0) {
                body.leanTime += dt
                if (body.leanTime > LEAN_SECONDS) {
                    val away = body.lastLeanAway
                        ?: Vec3(random.nextDouble(-1.0, 1.0), 0.0, random.nextDouble(-1.0, 1.0)).normalized()
                    body.velocity = body.velocity + away * 2.5 + Vec3(0.0, 2.5, 0.0)
                    body.angularVelocity = body.angularVelocity + Vec3.randomUnit(random) * 5.0
                    body.leanTime = 0.0
                    nudges++
                }
            } else if (!stuck) {
                body.leanTime = 0.0
            } else {
                // Still being jostled: let the timer run down instead of restarting it on every bump.
                body.leanTime = max(0.0, body.leanTime - 2 * dt)
            }
            val timedOut = timeSinceRelease > MAX_ROLL_SECONDS
            if (body.restTime > REST_SECONDS || timedOut) {
                if (body.flatness() > FLAT_ENOUGH || timedOut) {
                    body.settled = true
                    body.velocity = Vec3.ZERO
                    body.angularVelocity = Vec3.ZERO
                } else {
                    // Cocked die (leaning on a wall or another die): nudge it until a face lies flat.
                    body.velocity = body.velocity + Vec3(random.nextDouble(-0.6, 0.6), 2.5, random.nextDouble(-0.6, 0.6))
                    body.angularVelocity = body.angularVelocity + Vec3.randomUnit(random) * 5.0
                    body.restTime = 0.0
                    nudges++
                }
            }
        }
    }

    companion object {
        const val STEP = 1.0 / 240.0
        private val GRAVITY = Vec3(0.0, -38.0, 0.0)
        private const val HAND_STIFFNESS = 170.0
        private const val HAND_DAMPING = 15.0
        private const val MAX_THROW_SPEED = 26.0
        private const val SOLVER_ITERATIONS = 6
        private const val FRICTION = 0.45
        private const val DIE_FRICTION = 0.3
        private const val DIE_SOLVER_ITERATIONS = 2
        private const val HAND_SPACING = 1.35
        private const val BOUNCE_THRESHOLD = 1.5
        private const val CONTACT_SLOP = 0.002
        private const val WAKE_IMPULSE = 0.8
        private const val REST_SECONDS = 0.3
        private const val LEAN_SECONDS = 0.4
        private const val FLAT_ENOUGH = 0.985
        private const val MAX_ROLL_SECONDS = 12.0
    }
}
