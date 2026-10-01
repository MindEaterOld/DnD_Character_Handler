package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryContainerDetails
import com.dndcharacterhandler.domain.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryContainersTest {
    private fun item(id: Long, weight: Double, containerId: Long? = null, quantity: Int = 1) =
        InventoryItem(id = id, name = "item $id", category = InventoryCategory.OTHER, weight = weight, quantity = quantity,
            isEquipped = false, icon = "", containerId = containerId)

    private fun container(id: Long, weight: Double, containerId: Long? = null, weightless: Boolean = false) =
        item(id, weight, containerId).copy(category = InventoryCategory.CONTAINER, containerDetails = InventoryContainerDetails(30.0, weightless))

    @Test
    fun contentsLieInTheirContainerAndWeighWithIt() {
        val tree = InventoryTree(
            listOf(
                container(1, 5.0),
                item(2, 1.0, containerId = 1, quantity = 10),
                container(3, 1.0, containerId = 1),
                item(4, 1.0, containerId = 3, quantity = 4),
                item(5, 3.0)
            )
        )
        assertEquals(listOf(1L, 5L), tree.topLevel.map { it.id })
        assertEquals(listOf(2L, 3L), tree.contentsOf(tree.items[0]).map { it.id })
        // The torches and the waterskin with its water.
        assertEquals(15.0, tree.contentsWeight(tree.items[0]), 0.0)
        assertEquals(23.0, tree.carriedWeight, 0.0)
    }

    @Test
    fun aBagOfHoldingsContentsWeighNothing() {
        val tree = InventoryTree(
            listOf(
                container(1, 5.0, weightless = true),
                container(2, 1.0, containerId = 1),
                item(3, 2.0, containerId = 2),
                item(4, 3.0)
            )
        )
        // Only the bag itself and what lies outside it, however deep the rest is.
        assertEquals(8.0, tree.carriedWeight, 0.0)
    }

    @Test
    fun anItemGoesIntoAnyContainerButItselfAndTheOnesInsideIt() {
        val items = listOf(container(1, 5.0), container(2, 1.0, containerId = 1), container(3, 1.0), item(4, 1.0))
        val tree = InventoryTree(items)
        assertEquals(listOf(3L), tree.containersFor(items[0]).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), tree.containersFor(items[3]).map { it.id })
    }

    @Test
    fun anItemWhoseContainerIsGoneIsCarriedAsIs() {
        val tree = InventoryTree(listOf(item(1, 1.0, containerId = 42), item(2, 1.0, containerId = 2)))
        assertEquals(listOf(1L, 2L), tree.topLevel.map { it.id })
        assertEquals(null, tree.containerOf(tree.items[0]))
    }
}
