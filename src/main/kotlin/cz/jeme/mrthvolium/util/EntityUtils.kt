package cz.jeme.mrthvolium.util

import cz.jeme.mrthvolium.persistence.PersistentData
import org.bukkit.entity.Entity
import org.bukkit.entity.TextDisplay

inline val Entity.isCorpseMainPart: Boolean
    get() = PersistentData.IS_CORPSE_MAIN_PART.read(this) ?: false

inline val Entity.isCorpseInteraction: Boolean
    get() = PersistentData.IS_CORPSE_INTERACTION.read(this) ?: false

inline val Entity.isCorpsePart: Boolean
    get() = isCorpseMainPart || isCorpseInteraction

inline val Entity.mainCorpsePart: TextDisplay?
    get() = when {
        isCorpseMainPart -> this as TextDisplay
        isCorpseInteraction -> {
            val uuid = PersistentData.CORPSE_MAIN_PART_UUID.read(this) ?: return null
            world.getEntity(uuid) as? TextDisplay
        }

        else -> null
    }