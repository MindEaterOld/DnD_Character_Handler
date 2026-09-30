package com.dndcharacterhandler.presentation.dice

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class DiceWorldTest {
    /** Picks the dice up, shakes them along a random direction and throws them. */
    private fun throwDice(kinds: List<DieShapeKind>, random: Random): DiceWorld {
        val world = DiceWorld(Random(random.nextLong()))
        world.setAspectRatio(2.1)
        var hand = Vec3(random.nextDouble(-2.0, 2.0), world.handHeight, random.nextDouble(-4.0, 4.0))
        world.spawnInHand(kinds, hand)
        val direction = Vec3(random.nextDouble(-1.0, 1.0), 0.0, random.nextDouble(-1.0, 1.0)).normalized()
        val speed = random.nextDouble(0.0, 20.0)
        repeat(60) {
            hand = hand + direction * (speed * DiceWorld.STEP)
            world.moveHand(hand)
            world.step(DiceWorld.STEP)
        }
        world.release(direction * speed)
        var steps = 0
        while (!world.allSettled && steps < 240 * 20) {
            world.step(DiceWorld.STEP)
            steps++
            world.bodies.forEach { body ->
                val p = body.position
                val scale = (world.cameraHeight - p.y) / world.cameraHeight
                assertTrue("die fell through the table", p.y > -0.3)
                assertTrue("die left the screen", abs(p.x) < world.halfWidth * scale + 0.3 && abs(p.z) < world.halfDepth * scale + 0.3)
            }
        }
        assertTrue("dice never settled", world.allSettled)
        return world
    }

    @Test
    fun thrownDiceStayOnScreenAndLandFlat() {
        val random = Random(1)
        repeat(40) {
            val world = throwDice(listOf(DieShapeKind.D20, DieShapeKind.D6, DieShapeKind.D8, DieShapeKind.D4), random)
            // A die that timed out could stay cocked; with the nudging rules that should be rare.
            val flat = world.bodies.count { it.flatness() > 0.98 }
            assertTrue("only $flat of ${world.bodies.size} dice landed flat", flat >= world.bodies.size - 1)
        }
    }

    @Test
    fun aFullHandfulFitsOnScreenEvenAtTheEdges() {
        for (aspect in listOf(2.1, 1.6)) {
            val world = DiceWorld(Random(12))
            world.setAspectRatio(aspect)
            val kinds = List(MAX_DICE_BODIES) { DieShapeKind.entries[it % DieShapeKind.entries.size] }
            world.spawnInHand(kinds, Vec3(0.0, world.handHeight, 0.0))
            // The finger goes past every edge and corner of the screen.
            for (x in listOf(-20.0, 0.0, 20.0)) for (z in listOf(-40.0, 0.0, 40.0)) {
                world.moveHand(Vec3(x, world.handHeight, z))
                val hand = world.handPosition!!
                world.bodies.forEach { body ->
                    // Where the die is pulled to must leave room for the die itself inside the screen.
                    val target = hand + body.handOffset
                    val scale = (world.cameraHeight - target.y) / world.cameraHeight
                    assertTrue(
                        "held die pulled off screen to $target (aspect $aspect)",
                        abs(target.x) <= world.halfWidth * scale - 1.0 && abs(target.z) <= world.halfDepth * scale - 1.0
                    )
                }
            }
        }
    }

    @Test
    fun regrabbingFromAcrossTheTableStaysControlled() {
        val world = throwDice(listOf(DieShapeKind.D6), Random(7))
        // The die is picked up from where it lies with the finger at the far end of the table.
        val die = world.bodies.single()
        world.grab(Vec3(-die.position.x * 10, world.handHeight, if (die.position.z > 0) -40.0 else 40.0))
        var fastest = 0.0
        var fastestSpin = 0.0
        repeat(240) {
            world.step(DiceWorld.STEP)
            fastest = maxOf(fastest, die.velocity.length)
            fastestSpin = maxOf(fastestSpin, die.angularVelocity.length)
        }
        assertTrue("held die flew at $fastest", fastest < 35.0)
        assertTrue("held die spun at $fastestSpin", fastestSpin < 30.0)
    }

    /** Chi-square goodness of fit against a fair die, compared with the p = 0.001 critical value. */
    private fun assertFair(kind: DieShapeKind, sides: Int, throws: Int, criticalValue: Double, seed: Int) {
        val random = Random(seed)
        val counts = IntArray(sides + 1)
        repeat(throws) { counts[throwDice(listOf(kind), random).bodies.single().topValue()]++ }
        val values = if (kind == DieShapeKind.D10) (0 until sides) else (1..sides)
        val expected = throws.toDouble() / sides
        val chiSquare = values.sumOf { (counts[it] - expected) * (counts[it] - expected) / expected }
        assertTrue("$kind looks biased: chi^2 = $chiSquare (critical $criticalValue)", chiSquare < criticalValue)
    }

    @Test
    fun d6IsFair() = assertFair(DieShapeKind.D6, sides = 6, throws = 900, criticalValue = 20.52, seed = 6)

    @Test
    fun d20IsFair() = assertFair(DieShapeKind.D20, sides = 20, throws = 1200, criticalValue = 43.82, seed = 20)

    @Test
    fun d4IsFair() = assertFair(DieShapeKind.D4, sides = 4, throws = 600, criticalValue = 16.27, seed = 4)
}
