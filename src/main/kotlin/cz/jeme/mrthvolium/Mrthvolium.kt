package cz.jeme.mrthvolium

import cz.jeme.mrthvolium.corpse.CorpseManager
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

internal object Mrthvolium : JavaPlugin() {
    override fun onEnable() {
        if (!File(dataFolder, "config.yml").exists()) {
            saveDefaultConfig()
            // first start
            componentLogger.warn(
                Component.text(
                    "This plugin is not designed to be safely uninstalled.",
                    NamedTextColor.RED
                ).decorate(TextDecoration.BOLD)
            )
            componentLogger.warn(
                Component.text(
                    "By using $name, you acknowledge that once a corpse has been created, the plugin cannot be removed without leaving behind permanent traces.",
                    NamedTextColor.RED
                )
            )
            componentLogger.info(
                Component.text(
                    "✅ If you wish to keep using this plugin, please restart the server now.",
                    NamedTextColor.GREEN
                )
            )
            componentLogger.info(
                Component.text(
                    "❌ If you do not wish to use it, stop the server and delete the plugin file from the plugins folder.",
                    NamedTextColor.RED
                )
            )
            componentLogger.info("$name will now be disabled until the next server restart.")

            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        CorpseManager // initialize corpse manager
    }

    fun key(@KeyPattern.Value key: String): Key = NamespacedKey(this, key)
}
