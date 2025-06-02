package cz.jeme.mrthvolium.corpse

import cz.jeme.mrthvolium.Mrthvolium
import cz.jeme.mrthvolium.config.Config
import cz.jeme.mrthvolium.persistence.PersistentData
import cz.jeme.mrthvolium.util.CorpseTagResolvers
import cz.jeme.mrthvolium.util.isMenuItem
import cz.jeme.mrthvolium.util.isNullOrEmpty
import cz.jeme.mrthvolium.util.nms
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import net.minecraft.world.entity.EquipmentSlot as NMSEquipmentSlot

class CorpseInventoryHolder internal constructor(val corpse: Corpse) : InventoryHolder {
    companion object {
        // must not be mutated!
        private val emptyItem = ItemStack.empty()

        @Suppress("UnstableApiUsage")
        private val lootButton by lazy {
            ItemStack.of(Material.POPPED_CHORUS_FRUIT).apply {
                setData(
                    DataComponentTypes.ITEM_MODEL,
                    Config.Corpse.Inventory.LootButton.itemModel
                )
                setData(
                    DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,
                    Config.Corpse.Inventory.LootButton.itemEnchantmentGlint
                )
                setData(
                    DataComponentTypes.ITEM_NAME,
                    Config.Corpse.Inventory.LootButton.itemName
                )
                setData(
                    DataComponentTypes.LORE,
                    ItemLore.lore()
                        .lines(Config.Corpse.Inventory.LootButton.itemLore)
                )
                PersistentData.IS_MENU_ITEM.write(this, true)
            }
        }

        @Suppress("UnstableApiUsage")
        private val placeholder = ItemStack.of(Material.POPPED_CHORUS_FRUIT).apply {
            setData(
                DataComponentTypes.ITEM_MODEL,
                Material.GRAY_STAINED_GLASS_PANE.key
            )
            setData(
                DataComponentTypes.ITEM_NAME,
                Component.empty()
            )
            PersistentData.IS_MENU_ITEM.write(this, true)
        }

        const val INVENTORY_SIZE = 5 * 9 // 4 rows, 4 armor, 1 offhand, 4 reserved
        const val FEET_SLOT = INVENTORY_SIZE - 9 // first slot in last row
        const val LEGS_SLOT = INVENTORY_SIZE - 8 // second slot in last row
        const val CHEST_SLOT = INVENTORY_SIZE - 7 // third slot in last row
        const val HEAD_SLOT = INVENTORY_SIZE - 6 // fourth slot in last row
        val ARMOR_SLOTS = FEET_SLOT..HEAD_SLOT
        const val OFF_HAND_SLOT = INVENTORY_SIZE - 5 // fifth slot in last row
        val RESERVED_SLOTS = INVENTORY_SIZE - 4..INVENTORY_SIZE - 1
        const val LOOT_BUTTON_SLOT = INVENTORY_SIZE - 1 // last slot
    }

    private val inventory = Bukkit.createInventory(
        this,
        INVENTORY_SIZE,
        MiniMessage.miniMessage().deserialize(
            Config.Corpse.Inventory.name,
            CorpseTagResolvers.cachedOrNewNameForMainPart(corpse)
        )
    )

    init {
        val contents = PersistentData.CORPSE_INVENTORY.require(corpse.mainPart)

        inventory.contents = contents.toTypedArray()

        if (Config.Corpse.Inventory.LootButton.enabled)
            inventory.setItem(LOOT_BUTTON_SLOT, lootButton)

        for (slot in RESERVED_SLOTS) {
            if (inventory.getItem(slot).isNullOrEmpty)
                inventory.setItem(slot, placeholder)
        }
    }

    override fun getInventory() = inventory

    fun updateStoredInventory() {
        PersistentData.CORPSE_INVENTORY.write(
            corpse.mainPart,
            inventory.contents.map {
                if (it == null || it.isMenuItem) emptyItem else it
            }
        )
        corpse.updateEquipment()
    }

    fun remove(dropItems: Boolean) {
        if (dropItems) {
            val dropLocation = corpse.mainPart.location
            inventory
                .filter { !it.isNullOrEmpty && !it.isMenuItem }
                .forEach {
                    dropLocation.world.dropItem(
                        dropLocation,
                        it
                    )
                }
        }
        inventory.close()
        inventory.clear()
    }

    internal fun onClick(event: InventoryClickEvent) {
        fun updateNextTick() = Bukkit.getScheduler().runTask(Mrthvolium) { ->
            updateStoredInventory()
        }

        val slot = event.slot
        if (slot in RESERVED_SLOTS) {
            event.isCancelled = true
            if (slot == LOOT_BUTTON_SLOT && Config.Corpse.Inventory.LootButton.enabled) {
                val playerInventory = event.whoClicked.inventory
                inventory.forEachIndexed { index, stack ->
                    if (stack.isNullOrEmpty || stack.isMenuItem) return@forEachIndexed
                    val spaceAvailable = playerInventory.getItem(index).isNullOrEmpty
                    val currentEquipment = slotToEquipment(index)


                    @Suppress("UnstableApiUsage")
                    if (
                        spaceAvailable &&
                        (currentEquipment == null ||
                                currentEquipment.isHand ||
                                currentEquipment == stack.getData(DataComponentTypes.EQUIPPABLE)?.slot())
                    ) {
                        playerInventory.setItem(index, stack)
                        inventory.setItem(index, null)
                    } else {
                        val overflow = playerInventory.addItem(stack)
                        inventory.setItem(index, overflow[0])
                    }
                }
                updateNextTick()
            }
        } else updateNextTick()
    }

    fun getEquipment(slot: NMSEquipmentSlot): ItemStack? = when (slot) {
        NMSEquipmentSlot.HEAD -> inventory.getItem(HEAD_SLOT)
        NMSEquipmentSlot.CHEST -> inventory.getItem(CHEST_SLOT)
        NMSEquipmentSlot.LEGS -> inventory.getItem(LEGS_SLOT)
        NMSEquipmentSlot.FEET -> inventory.getItem(FEET_SLOT)
        NMSEquipmentSlot.OFFHAND -> inventory.getItem(OFF_HAND_SLOT)
        NMSEquipmentSlot.MAINHAND -> inventory.getItem(corpse.selectedSlot)

        else -> null
    }

    fun getEquipment(slot: EquipmentSlot) = getEquipment(slot.nms)

    fun slotToEquipment(slot: Int): EquipmentSlot? = when (slot) {
        HEAD_SLOT -> EquipmentSlot.HEAD
        CHEST_SLOT -> EquipmentSlot.CHEST
        LEGS_SLOT -> EquipmentSlot.LEGS
        FEET_SLOT -> EquipmentSlot.FEET
        OFF_HAND_SLOT -> EquipmentSlot.OFF_HAND
        corpse.selectedSlot -> EquipmentSlot.HAND

        else -> null
    }

    internal fun onClose(event: InventoryCloseEvent) {
        if (corpse.removed) {
            // this can happen if a second InventoryCloseEvent is fired
            // if the player clicks the corpse again during the 1-tick window
            return
        }
        if (inventory.all { it.isNullOrEmpty || it.isMenuItem }) {
            corpse.poof()
            Bukkit.getScheduler().runTask(Mrthvolium) { ->
                // Delay removal of the main part by 1 tick to avoid triggering a duplicate InventoryCloseEvent.
                // - Removing the mainPart triggers an EntityRemoveEvent, which is handled by CorpseManager.
                // - That handler removes the corpse and calls corpse.remove(), which internally calls
                //   the remove method of this inventory holder, closing the inventory for all players.
                // - If this all happens in the same tick as the original InventoryCloseEvent,
                //   the server still considers the player (who already closed the inventory) as still viewing it.
                // - As a result, a second InventoryCloseEvent is fired unexpectedly.
                corpse.mainPart.remove()
            }
        }
    }
}