package cz.jeme.mrthvolium.persistence

import cz.jeme.mrthvolium.Mrthvolium
import cz.jeme.mrthvolium.util.toNamespacedKey
import io.papermc.paper.persistence.PersistentDataContainerView
import io.papermc.paper.persistence.PersistentDataViewHolder
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

interface PersistentData<P, C> : Keyed {
    companion object {
        @JvmStatic
        fun <P : Any, C : Any> data(
            key: Key,
            type: PersistentDataType<P, C>
        ): PersistentData<P, C> = PersistentDataImpl(
            key.toNamespacedKey(),
            type
        )

        // Corpse Interaction

        /**
         * Whether an entity is a corpse interaction or not.
         */
        @JvmStatic
        val IS_CORPSE_INTERACTION = data(
            Mrthvolium.key("is_corpse_interaction"),
            PersistentDataType.BOOLEAN
        )

        /**
         * UUID of the main part of the corpse (typically a text display showing the name).
         */
        @JvmStatic
        val CORPSE_MAIN_PART_UUID = data(
            Mrthvolium.key("corpse_main_part_uuid"),
            UUIDPersistentDataType
        )

        // Corpse Main Part (Text Display)

        /**
         * Whether an entity is a corpse main part or not.
         */
        @JvmStatic
        val IS_CORPSE_MAIN_PART = data(
            Mrthvolium.key("is_corpse_main_part"),
            PersistentDataType.BOOLEAN
        )

        /**
         * UUIDs of all interaction parts of the corpse.
         */
        @JvmStatic
        val CORPSE_INTERACTION_UUIDS = data(
            Mrthvolium.key("corpse_interaction_part_uuids"),
            PersistentDataType.LIST.listTypeFrom(UUIDPersistentDataType)
        )

        /**
         *  UUID of the player that died.
         */
        @JvmStatic
        val DEAD_PLAYER_UUID = data(
            Mrthvolium.key("dead_player_uuid"),
            UUIDPersistentDataType
        )

        /**
         * Name of the player that died.
         */
        @JvmStatic
        val DEAD_PLAYER_NAME = data(
            Mrthvolium.key("dead_player_name"),
            PersistentDataType.STRING
        )

        /**
         * 3D position (XYZ) of the corpse.
         */
        @JvmStatic
        val CORPSE_POSITION = data(
            Mrthvolium.key("corpse_position"),
            VectorPersistentDataType
        )

        /**
         * Selected hotbar slot (0–8) the player had at death.
         */
        @JvmStatic
        val CORPSE_SELECTED_SLOT = data(
            Mrthvolium.key("corpse_selected_slot"),
            PersistentDataType.INTEGER
        )

        /**
         * Value and Yggdrasil signature of the dead player's skin.
         */
        @JvmStatic
        val CORPSE_SKIN = data(
            Mrthvolium.key("corpse_skin"),
            CorpseSkinPersistentDataType
        )

        /**
         * Timestamp of death (epoch millis).
         */
        @JvmStatic
        val CORPSE_DEATH_TIMESTAMP = data(
            Mrthvolium.key("corpse_death_timestamp"),
            PersistentDataType.LONG
        )

        /**
         * Inventory contents of the corpse.
         */
        @JvmStatic
        val CORPSE_INVENTORY = data(
            Mrthvolium.key("corpse_inventory"),
            PersistentDataType.LIST.listTypeFrom(ItemStackPersistentDataType)
        )

        /**
         * Timestamp of corpse rotting (epoch millis).
         */
        @JvmStatic
        val CORPSE_ROT_TIMESTAMP = data(
            Mrthvolium.key("corpse_rot_timestamp"),
            PersistentDataType.LONG
        )

        // Inventory

        /**
         * Whether an item is a menu item and should be protected.
         */
        @JvmStatic
        val IS_MENU_ITEM = data(
            Mrthvolium.key("is_menu_item"),
            PersistentDataType.BOOLEAN
        )
    }

    val key: Key
    override fun key() = key

    val type: PersistentDataType<P, C>

    fun write(container: PersistentDataContainer, value: C)

    fun write(holder: PersistentDataHolder, value: C) = write(
        holder.persistentDataContainer,
        value
    )

    fun write(stack: ItemStack, value: C) {
        stack.editPersistentDataContainer { container ->
            write(container, value)
        }
    }

    fun read(container: PersistentDataContainerView): C?

    fun read(holder: PersistentDataViewHolder) = read(
        holder.persistentDataContainer
    )

    fun require(container: PersistentDataContainerView) = read(
        container
    ) ?: throw IllegalArgumentException(
        "Required persistent data not present in container: '${key.asString()}'"
    )

    fun require(holder: PersistentDataViewHolder) = require(
        holder.persistentDataContainer
    )

    fun check(container: PersistentDataContainerView): Boolean

    fun check(holder: PersistentDataViewHolder) = check(
        holder.persistentDataContainer
    )

    fun delete(container: PersistentDataContainer)

    fun delete(holder: PersistentDataHolder) {
        delete(holder.persistentDataContainer)
    }

    fun delete(stack: ItemStack) {
        stack.editPersistentDataContainer { container ->
            delete(container)
        }
    }

    private class PersistentDataImpl<P : Any, C : Any>(
        override val key: NamespacedKey,
        override val type: PersistentDataType<P, C>
    ) : PersistentData<P, C> {
        override fun write(container: PersistentDataContainer, value: C) {
            container.set<P, C>(key, type, value)
        }

        override fun read(container: PersistentDataContainerView): C? {
            return container.get(key, type)
        }

        override fun check(container: PersistentDataContainerView): Boolean {
            return container.has(key, type)
        }

        override fun delete(container: PersistentDataContainer) {
            container.remove(key)
        }
    }
}