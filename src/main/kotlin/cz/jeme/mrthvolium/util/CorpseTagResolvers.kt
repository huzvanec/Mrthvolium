package cz.jeme.mrthvolium.util

import cz.jeme.mrthvolium.config.Config
import cz.jeme.mrthvolium.corpse.Corpse
import cz.jeme.mrthvolium.persistence.PersistentData
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.TagPattern
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.apache.commons.lang3.time.DurationFormatUtils
import org.bukkit.entity.TextDisplay
import java.time.Instant
import java.time.ZoneId
import java.util.*

object CorpseTagResolvers {
    // Map of corpse main part uuid -> tag resolver
    private val cache = hashMapOf<UUID, TagResolver>()

    @JvmStatic
    fun cachedOrNewNameForMainPart(mainPart: TextDisplay) = cache.computeIfAbsent(mainPart.uniqueId) {
        val deadName = PersistentData.DEAD_PLAYER_NAME.require(mainPart)
        val pos = PersistentData.CORPSE_POSITION.require(mainPart)
        val timestamp = PersistentData.CORPSE_DEATH_TIMESTAMP.require(mainPart)

        TagResolver.resolver(
            Placeholder.unparsed("player_name", deadName),
            Formatter.number("corpse_x", pos.x),
            Formatter.number("corpse_y", pos.y),
            Formatter.number("corpse_z", pos.z),
            Formatter.date("death_stamp", Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())),
        )
    }

    @JvmStatic
    fun newProtectionForMainPart(mainPart: TextDisplay): TagResolver {
        val deadName = PersistentData.DEAD_PLAYER_NAME.require(mainPart)
        val deathTime = PersistentData.CORPSE_DEATH_TIMESTAMP.require(mainPart)
        val corpseMillis = (System.currentTimeMillis() - deathTime)
        val rotAfterMillis = Config.Corpse.rotAfter * 50

        return TagResolver.resolver(
            Placeholder.unparsed("player_name", deadName),
            durationResolver(
                "rot_time",
                rotAfterMillis - corpseMillis
            )
        )
    }

    private fun durationResolver(@TagPattern key: String, duration: Long): TagResolver {
        return TagResolver.resolver(key) { argumentQueue: ArgumentQueue, context: Context ->
            val format = argumentQueue.popOr("Format expected.").value()
            Tag.inserting(
                context.deserialize(
                    DurationFormatUtils.formatDuration(
                        duration,
                        format,
                        true
                    )
                )
            )
        }
    }

    @JvmStatic
    fun remove(mainPart: TextDisplay) = cache.remove(mainPart.uniqueId)

    @JvmStatic
    fun cachedOrNewNameForMainPart(corpse: Corpse) = cachedOrNewNameForMainPart(corpse.mainPart)
}