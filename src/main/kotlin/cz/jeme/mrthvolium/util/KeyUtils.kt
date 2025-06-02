package cz.jeme.mrthvolium.util

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey

fun Key.toNamespacedKey(): NamespacedKey = this as? NamespacedKey
    ?: NamespacedKey(namespace(), value())