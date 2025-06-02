package cz.jeme.mrthvolium.config

import net.kyori.adventure.sound.Sound

data class CorpseProtection(
    val enabled: Boolean,
    val message: String?,
    val actionBar: String?,
    val title: UnresolvedTitle?,
    val sound: Sound?
)