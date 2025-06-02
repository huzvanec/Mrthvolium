package cz.jeme.mrthvolium

import cz.jeme.mrthvolium.corpse.CorpseManager
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

internal object Mrthvolium : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        
        CorpseManager // initialize corpse manager
    }

    fun key(@KeyPattern.Value key: String): Key = NamespacedKey(this, key)
}
