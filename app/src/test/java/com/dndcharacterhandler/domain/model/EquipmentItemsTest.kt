package com.dndcharacterhandler.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentItemsTest {
    private val staff = InventoryCatalogItem(
        id = "equipment:staff",
        name = "Staff",
        category = InventoryCategory.OTHER,
        weight = 4.0,
        description = "",
        source = InventoryCatalogSource.EQUIPMENT,
        ruName = "Посох"
    )
    private val catalog = mapOf(equipmentNameKey(staff.name) to staff)

    @Test
    fun aDruidsWoodenStaffIsTheCatalogsStaff() {
        val item = matchedEquipmentItem(catalog, CatalogEquipmentRef("phbdfcWoodenstaf", CatalogText("Wooden staff", "Деревянный посох"), "weapon"), 1, russian = true)
        assertEquals("Посох", item.name)
        assertEquals(4.0, item.weight, 0.0)
    }

    @Test
    fun anItemTheCatalogLacksKeepsItsName() {
        val item = matchedEquipmentItem(catalog, CatalogEquipmentRef("x", CatalogText("Water (Pint)", "Вода (пинта)"), "consumable", 1.0), 2, russian = true)
        assertEquals("Вода (пинта)", item.name)
        assertEquals(2, item.quantity)
        assertEquals(InventoryCategory.CONSUMABLE, item.category)
    }
}
