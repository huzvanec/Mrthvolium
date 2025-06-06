package cz.jeme.mrthvolium.config

import com.mojang.authlib.properties.Property
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ComponentDecoder
import net.kyori.adventure.title.Title
import org.bukkit.configuration.ConfigurationSection
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JDuration

fun ConfigurationSection.getKey(path: String): Key? {
    if (!isKey(path)) return null
    return Key.key(getString(path)!!)
}

fun ConfigurationSection.getKeyList(path: String): List<Key> = getStringList(path)
    .filter { Key.parseable(it) }
    .map { Key.key(it) }

fun ConfigurationSection.isKey(path: String): Boolean {
    return Key.parseable(getString(path))
}

fun ConfigurationSection.getSkin(path: String): Property? {
    if (!isSkin(path)) return null
    return Property(
        "textures",
        getString("$path.value")!!,
        getString("$path.signature")!!
    )
}

fun ConfigurationSection.isSkin(path: String): Boolean {
    return isString("$path.value") && isString("$path.signature")
}


fun ConfigurationSection.getRichMessageList(path: String): List<Component> {
    return getComponentList(path, MiniMessage.miniMessage())
}

fun <C : Component> ConfigurationSection.getComponentList(
    path: String,
    decoder: ComponentDecoder<in String, C>
): List<Component> {
    return getStringList(path).map { decoder.deserialize(it) }
}

fun ConfigurationSection.getDuration(path: String): JDuration? {
    return Duration.parseOrNull(getString(path) ?: return null)?.toJavaDuration()
}

fun ConfigurationSection.getDurationTicks(path: String): Long? {
    return getDuration(path)?.let { it.toMillis() / 50 }
}

fun ConfigurationSection.isDuration(path: String): Boolean {
    return isString(path) && Duration.parseOrNull(path) != null
}

fun ConfigurationSection.getTitleTimes(path: String): Title.Times? {
    return Title.Times.times(
        getDuration("$path.fade-in") ?: return null,
        getDuration("$path.stay") ?: return null,
        getDuration("$path.fade-out") ?: return null
    )
}

fun ConfigurationSection.isTitleTimes(path: String): Boolean {
    return isDuration("$path.fade-in") &&
            isDuration("$path.stay") &&
            isDuration("$path.fade-out")
}

fun ConfigurationSection.getTitle(path: String): Title? {
    return Title.title(
        getRichMessage("$path.title") ?: return null,
        getRichMessage("$path.subtitle") ?: return null,
        getTitleTimes("$path.times") ?: return null
    )
}

fun ConfigurationSection.isTitle(path: String): Boolean {
    return isString("$path.title") &&
            isString("$path.subtitle") &&
            isTitleTimes("$path.times")
}

fun ConfigurationSection.getUnresolvedTitle(path: String): UnresolvedTitle? {
    return UnresolvedTitle(
        getString("$path.title") ?: return null,
        getString("$path.subtitle") ?: return null,
        getTitleTimes("$path.times") ?: return null
    )
}

fun ConfigurationSection.getCorpseProtection(path: String): CorpseProtection {
    return CorpseProtection(
        getBoolean("$path.enabled"),
        getString("$path.message"),
        getString("$path.action-bar"),
        getUnresolvedTitle("$path.title"),
        getSound("$path.sound")
    )
}

fun ConfigurationSection.getSoundSource(path: String): Sound.Source? {
    return Sound.Source.NAMES.value(getString(path) ?: return null)
}

fun ConfigurationSection.getSound(path: String): Sound? {
    if (!isDouble("$path.volume") || !isDouble("$path.pitch"))
        return null
    return Sound.sound(
        getKey("$path.sound") ?: return null,
        getSoundSource("$path.source") ?: return null,
        getDouble("$path.volume").toFloat(),
        getDouble("$path.pitch").toFloat()
    )
}