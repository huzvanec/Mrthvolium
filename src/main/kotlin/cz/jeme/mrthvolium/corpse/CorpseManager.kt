package cz.jeme.mrthvolium.corpse

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import cz.jeme.mrthvolium.Mrthvolium
import cz.jeme.mrthvolium.config.Config
import cz.jeme.mrthvolium.persistence.PersistentData
import cz.jeme.mrthvolium.util.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

object CorpseManager : Listener {
    // must not be mutated!
    private val emptyItem = ItemStack.empty()

    init {
        Bukkit.getPluginManager().registerEvents(
            this, Mrthvolium
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun PlayerJoinEvent.handle() {
        player.send(
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(
                Corpse.CORPSE_TEAM,
                true
            )
        )

        Corpse.corpses.forEach { it.sendSpawn(player) }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun EntityAddToWorldEvent.handle() {
        if (!entity.isCorpseMainPart) return
        val mainPart = entity as TextDisplay

        val corpse = Corpse.cachedOrNew(mainPart)

        // cfg
        val rotAfter = Config.Corpse.rotAfter
        val rottingEnabled = Config.Corpse.Rotten.enabled && rotAfter >= 0
        val despawnAfter = Config.Corpse.Rotten.despawnAfter
        val despawningEnabled = if (rottingEnabled) despawnAfter >= 0 else rotAfter >= 0

        val deathTime = PersistentData.CORPSE_DEATH_TIMESTAMP.require(mainPart)
        // ticks passed from the player dying
        val corpseTicks = (System.currentTimeMillis() - deathTime) / 50 // millis / 1000 * 20

        fun despawnCorpse() = Bukkit.getScheduler().runTask(
            Mrthvolium
        ) { ->
            // despawn on next tick to prevent attempts to remove the entity before the world is loaded 
            corpse.despawn(false)
        }

        if (corpse.rotten && rottingEnabled) {
            val rotTime = PersistentData.CORPSE_ROT_TIMESTAMP.require(mainPart)
            val rottenTicks = (System.currentTimeMillis() - rotTime) / 50

            if (despawningEnabled) {
                if (rottenTicks > despawnAfter) {
                    despawnCorpse()
                    return
                }
                corpse.despawnAfter(despawnAfter - rottenTicks)
            }
            Config.Corpse.Rotten.skin?.let { corpse.updateSkin(it) }
        } else {
            // revert to normal corpse if rotting was disabled
            if (corpse.rotten) PersistentData.CORPSE_ROT_TIMESTAMP.delete(mainPart)

            if (rottingEnabled) {
                // start rotting if enough time has passed
                if (corpseTicks > rotAfter) corpse.rot()
                else corpse.rotAfter(rotAfter - corpseTicks)
            } else if (despawningEnabled) {
                // if rotting is disabled, rotAfter option specifies when to despawn the corpse
                if (corpseTicks > rotAfter) {
                    despawnCorpse()
                    return
                }
                corpse.despawnAfter(rotAfter - corpseTicks)
            }
        }
        corpse.updateName()
        corpse.spawn()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun PlayerDeathEvent.handle() {
        val location = player.location
        val world = player.world

        if (player.inventory.all { it.isNullOrEmpty }) return

        val x = location.blockX
        val z = location.blockZ

        var y = location.blockY
        val minY = world.minHeight

        if (y < minY) {
            if (!Config.Corpse.spawnInVoid) return
            y = minY
        }
        while (y > minY && world.getBlockAt(x, y - 1, z).isPassable) y--

        val corpseMid = Location(
            world,
            location.x,
            y.toDouble(),
            location.z
        )

        world.spawn(
            corpseMid.clone().add(-0.5, 0.5, 0.0),
            TextDisplay::class.java
        ) { mainPart ->
            PersistentData.IS_CORPSE_MAIN_PART.write(
                mainPart,
                true
            )
            PersistentData.CORPSE_DEATH_TIMESTAMP.write(
                mainPart,
                System.currentTimeMillis()
            )
            PersistentData.DEAD_PLAYER_UUID.write(
                mainPart,
                player.uniqueId
            )
            PersistentData.DEAD_PLAYER_NAME.write(
                mainPart,
                player.name
            )
            PersistentData.CORPSE_POSITION.write(
                mainPart,
                corpseMid.toVector()
            )
            PersistentData.CORPSE_SELECTED_SLOT.write(
                mainPart,
                player.inventory.heldItemSlot
            )
            PersistentData.CORPSE_SKIN.write(
                mainPart,
                player.nms.gameProfile.properties["textures"].first()
            )
            if (Config.Corpse.storeExperience) {
                PersistentData.CORPSE_EXPERIENCE.write(
                    mainPart,
                    droppedExp
                )
                droppedExp = 0
            }
            PersistentData.CORPSE_INVENTORY.write(
                mainPart,
                player.inventory.map {
                    drops.remove(it) // prevent the player from dropping the items
                    it ?: emptyItem
                }
            )

            mainPart.billboard = Display.Billboard.CENTER
            mainPart.isSeeThrough = true
            mainPart.isDefaultBackground = true

            fun initInteraction(interaction: Interaction) {
                interaction.isResponsive = true
                interaction.interactionHeight = 0.5F

                PersistentData.IS_CORPSE_INTERACTION.write(
                    interaction,
                    true
                )
                PersistentData.CORPSE_MAIN_PART_UUID.write(
                    interaction,
                    mainPart.uniqueId
                )
            }

            val interactions = listOf(
                world.spawn(
                    corpseMid.clone().add(0.5, -0.25, 0.0),
                    Interaction::class.java,
                    ::initInteraction
                ),
                world.spawn(
                    corpseMid.clone().add(-0.5, -0.25, 0.0),
                    Interaction::class.java,
                    ::initInteraction
                )
            )

            PersistentData.CORPSE_INTERACTION_UUIDS.write(
                mainPart,
                interactions.map { it.uniqueId }
            )
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun EntityTeleportEvent.handle() {
        // prevent teleportation of corpse parts
        if (entity.isCorpsePart) isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerInteractEntityEvent.handle() {
        val mainPart = rightClicked.mainCorpsePart ?: return
        val corpse = Corpse.cachedOrNew(mainPart)
        val protection = if (corpse.rotten) Config.Corpse.Rotten.protection else Config.Corpse.protection
        if (
            protection.enabled &&
            !player.hasPermission("mrthvolium.bypass.protection") &&
            corpse.deadUuid != player.uniqueId
        ) {
            val resolver = CorpseTagResolvers.newProtectionForMainPart(mainPart)
            protection.title?.let { player.showTitle(it.resolve(resolver)) }
            protection.message?.let {
                player.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        it,
                        resolver
                    )
                )
            }
            protection.actionBar?.let {
                player.sendActionBar(
                    MiniMessage.miniMessage().deserialize(
                        it,
                        resolver
                    )
                )
            }
            protection.sound?.let { player.world.playSound(it, mainPart) }
            return
        }
        player.openInventory(corpse.inventoryHolder.inventory)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun EntityRemoveEvent.handle() {
        when (cause) {
            EntityRemoveEvent.Cause.PLUGIN,
            EntityRemoveEvent.Cause.DEATH -> {
                val mainPart = entity.mainCorpsePart ?: return
                CorpseTagResolvers.remove(mainPart)
                // remove all interactions
                PersistentData.CORPSE_INTERACTION_UUIDS.require(mainPart).forEach {
                    val interaction = mainPart.world.getEntity(it) ?: return@forEach
                    // remove data to prevent calling this event again
                    PersistentData.IS_CORPSE_INTERACTION.delete(interaction)
                    interaction.remove()
                }
                // remove corpse and drop items and exp
                Corpse.cachedOnly(mainPart)?.let {
                    it.poof()
                    it.remove(dropItems = true, dropExperience = true)
                }

                // remove main part
                // remove data to prevent calling this event again
                PersistentData.IS_CORPSE_MAIN_PART.delete(mainPart)
                mainPart.remove()
            }

            EntityRemoveEvent.Cause.UNLOAD -> {
                if (!Config.Corpse.cacheCorpses && entity.isCorpseMainPart) {
                    // if corpse caching is disabled, remove corpse on unload
                    Corpse.cachedOnly(entity as TextDisplay)?.remove(
                        dropItems = false, dropExperience = false
                    )
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun InventoryClickEvent.handle() {
        val holder = inventory.getHolder(false)
        if (holder !is CorpseInventoryHolder) return
        holder.onClick(this)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun InventoryCloseEvent.handle() {
        val holder = inventory.getHolder(false)
        if (holder !is CorpseInventoryHolder) return
        holder.onClose(this)
    }
}