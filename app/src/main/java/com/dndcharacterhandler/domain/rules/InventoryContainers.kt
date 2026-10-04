package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryItem

/**
 * The inventory as containers hold it: items point at the container they lie in through
 * [InventoryItem.containerId]; an item whose container is gone counts as carried as is.
 */
class InventoryTree(val items: List<InventoryItem>) {
    private val byId = items.filter { it.id != 0L }.associateBy { it.id }

    /**
     * The container [item] really lies in: one that exists, is a container, and doesn't lie inside
     * [item] itself (a loop a broken archive could bring). Anything else leaves the item carried.
     */
    private val parents: Map<Long, InventoryItem> = buildMap {
        items.forEach { item ->
            val parent = item.containerId?.takeIf { it != item.id }?.let(byId::get) ?: return@forEach
            if (parent.category != InventoryCategory.CONTAINER) return@forEach
            var above: InventoryItem? = parent
            val seen = mutableSetOf(item.id)
            while (above != null) {
                if (!seen.add(above.id)) return@forEach
                above = above.containerId?.takeIf { it != above!!.id }?.let(byId::get)
            }
            put(item.id, parent)
        }
    }
    private val children = items.groupBy { item -> parents[item.id]?.id }

    /** The container [item] lies in, if it still exists. */
    fun containerOf(item: InventoryItem): InventoryItem? = parents[item.id]

    /** What the character carries as is: not inside any container. */
    val topLevel: List<InventoryItem> get() = children[null].orEmpty()

    /** What lies right inside [container]. */
    fun contentsOf(container: InventoryItem): List<InventoryItem> =
        if (container.id == 0L) emptyList() else children[container.id].orEmpty()

    /** Everything inside [container], at any depth. */
    fun allContentsOf(container: InventoryItem): List<InventoryItem> {
        val result = mutableListOf<InventoryItem>()
        val seen = mutableSetOf(container.id)
        fun walk(parent: InventoryItem) {
            contentsOf(parent).forEach { item ->
                if (seen.add(item.id)) {
                    result += item
                    walk(item)
                }
            }
        }
        walk(container)
        return result
    }

    /**
     * What fills [container]: its contents' weight, nested containers with theirs — but a nested
     * container whose contents weigh nothing (a Bag of Holding in a backpack) only with its own.
     */
    fun contentsWeight(container: InventoryItem): Double {
        val seen = mutableSetOf(container.id)
        fun weightIn(parent: InventoryItem): Double = contentsOf(parent).sumOf { item ->
            if (!seen.add(item.id)) return@sumOf 0.0
            val own = item.weight * item.quantity
            if (item.containerDetails?.weightlessContents == true) own else own + weightIn(item)
        }
        return weightIn(container)
    }

    /**
     * The weight the character carries: every item, except what lies in a container whose contents
     * weigh nothing (a Bag of Holding), however deep.
     */
    val carriedWeight: Double get() = items.filterNot(::weighsNothing).sumOf { it.weight * it.quantity }

    private fun weighsNothing(item: InventoryItem): Boolean {
        var container = containerOf(item)
        val seen = mutableSetOf(item.id)
        while (container != null && seen.add(container.id)) {
            if (container.containerDetails?.weightlessContents == true) return true
            container = containerOf(container)
        }
        return false
    }

    /** Containers [item] can be put into: any but itself and the ones inside it. */
    fun containersFor(item: InventoryItem): List<InventoryItem> {
        val inside = allContentsOf(item).map { it.id }.toSet()
        return items.filter { it.category == InventoryCategory.CONTAINER && it.id != 0L && it.id != item.id && it.id !in inside }
    }
}
