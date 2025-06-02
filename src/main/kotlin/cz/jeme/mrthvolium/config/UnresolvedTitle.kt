package cz.jeme.mrthvolium.config

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title

data class UnresolvedTitle(
    val title: String,
    val subtitle: String,
    val times: Title.Times
) {
    fun resolve(resolver: TagResolver) = Title.title(
        MiniMessage.miniMessage().deserialize(
            title,
            resolver
        ),
        MiniMessage.miniMessage().deserialize(
            subtitle,
            resolver
        ),
        times
    )
}