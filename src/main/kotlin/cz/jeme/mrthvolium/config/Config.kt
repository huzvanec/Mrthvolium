package cz.jeme.mrthvolium.config

import cz.jeme.mrthvolium.Mrthvolium
import org.bukkit.configuration.ConfigurationSection

object Config {
    private val config = Mrthvolium.config

    private fun section(
        path: String,
        accessor: (String) -> ConfigurationSection?
    ) = required(path, accessor)

    private fun <T> required(
        path: String,
        accessor: (String) -> T?
    ) = lazy {
        accessor(path) ?: throw IllegalArgumentException(
            "Invalid configuration value: '$path'"
        )
    }

    private fun <T> optional(
        path: String,
        accessor: (String) -> T?
    ) = lazy { accessor(path) }

    object Corpse {
        private val config by section("corpse", Config.config::getConfigurationSection)

        val name by optional("name", config::getString)
        val rotAfter by required("rot-after", config::getDurationTicks)
        val protection by required("protection", config::getCorpseProtection)
        val cacheCorpses by required("cache-corpses", config::getBoolean)
        val storeExperience by required("store-experience", config::getBoolean)
        val spawnInVoid by required("spawn-in-void", config::getBoolean)

        object Rotten {
            private val config by section("rotten", Corpse.config::getConfigurationSection)

            val name by optional("name", config::getString)
            val enabled by required("enabled", config::getBoolean)
            val protection by required("protection", config::getCorpseProtection)
            val despawnAfter by required("despawn-after", config::getDurationTicks)
            val skin by optional("skin", config::getSkin)
        }

        object Inventory {
            private val config by section("inventory", Corpse.config::getConfigurationSection)

            val name by required("name", config::getString)

            object LootButton {
                private val config by section("loot-button", Inventory.config::getConfigurationSection)

                val enabled by required("enabled", config::getBoolean)
                val itemModel by required("model", config::getKey)
                val itemName by required("name", config::getRichMessage)
                val itemLore by required("lore", config::getRichMessageList)
                val itemEnchantmentGlint by required("enchantment-glint", config::getBoolean)
            }
        }
    }

    object Debug {
        private val config by section("debug", Config.config::getConfigurationSection)

        val logCorpseCreation by required("log-corpse-creation", config::getBoolean)
        val logCorpseRemoval by required("log-corpse-removal", config::getBoolean)
    }
}