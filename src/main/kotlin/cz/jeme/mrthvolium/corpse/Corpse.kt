package cz.jeme.mrthvolium.corpse

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.datafixers.util.Pair
import cz.jeme.mrthvolium.Mrthvolium
import cz.jeme.mrthvolium.config.Config
import cz.jeme.mrthvolium.network.DummyServerGamePacketListener
import cz.jeme.mrthvolium.persistence.PersistentData
import cz.jeme.mrthvolium.util.CorpseTagResolvers
import cz.jeme.mrthvolium.util.nms
import cz.jeme.mrthvolium.util.send
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ParticleStatus
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.ChatVisiblity
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.*
import net.minecraft.world.entity.ExperienceOrb as NmsExperienceOrb
import net.minecraft.world.entity.player.Player as NmsPlayer

class Corpse private constructor(val mainPart: TextDisplay) {
    companion object {
        private val cache = hashMapOf<UUID, Corpse>()
        val corpses: Collection<Corpse> = cache.values

        @JvmStatic
        fun cachedOrNew(mainPart: TextDisplay) = cache.computeIfAbsent(mainPart.uniqueId) { Corpse(mainPart) }

        @JvmStatic
        fun cachedOnly(mainPart: TextDisplay) = cache[mainPart.uniqueId]

        val CORPSE_TEAM: PlayerTeam by lazy {
            val scoreboard = Bukkit.getScoreboardManager().mainScoreboard.nms
            PlayerTeam(scoreboard, "Corpses").apply {
                nameTagVisibility = Team.Visibility.NEVER
                collisionRule = Team.CollisionRule.NEVER
            }
        }

        // https://minecraft.wiki/w/Java_Edition_protocol/Packets#Client_Information_(configuration)
        private val CLIENT_INFORMATION: ClientInformation = ClientInformation(
            "en_us",
            2,
            ChatVisiblity.FULL,
            true,
            0x7F, // 0111 1111 - enable all skin features
            NmsPlayer.DEFAULT_MAIN_HAND,
            false,
            false,
            ParticleStatus.ALL
        )

        private val PLAYER_EQUIPMENT = EnumSet.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
        )
    }

    var spawned: Boolean = false
        private set
    var removed: Boolean = false
        private set

    private var rotTask: BukkitTask? = null
    private var despawnTask: BukkitTask? = null

    private fun assertNotRemoved() {
        if (removed) throw IllegalStateException("Corpse removed")
    }

    fun <C> requirePersistent(data: PersistentData<*, C>): C {
        assertNotRemoved()
        return data.require(mainPart)
    }

    fun <C> readPersistent(data: PersistentData<*, C>): C? {
        assertNotRemoved()
        return data.read(mainPart)
    }

    fun checkPersistent(data: PersistentData<*, *>): Boolean {
        assertNotRemoved()
        return data.check(mainPart)
    }

    val world: World = mainPart.world
    val inventoryHolder = CorpseInventoryHolder(this)

    val position: Vector = requirePersistent(PersistentData.CORPSE_POSITION)
    val selectedSlot: Int = requirePersistent(PersistentData.CORPSE_SELECTED_SLOT)
    val deadUuid: UUID = requirePersistent(PersistentData.DEAD_PLAYER_UUID)
    val experience: Int by lazy { readPersistent(PersistentData.CORPSE_EXPERIENCE) ?: 0 }
    val rotten: Boolean get() /* dynamic */ = checkPersistent(PersistentData.CORPSE_ROT_TIMESTAMP)

    private val uuid: UUID = UUID.randomUUID() // private to prevent accidental usage
    var skin: Property = requirePersistent(PersistentData.CORPSE_SKIN)
        private set
    val gameProfile: GameProfile = GameProfile(uuid, "Corpse")
    val body: ServerPlayer = ServerPlayer(
        Bukkit.getServer().nms,
        world.nms,
        gameProfile,
        CLIENT_INFORMATION
    ).apply {
        connection = DummyServerGamePacketListener(this)
        setPos(
            position.x + 1, // center the body
            position.y - 0.01, // move slightly down to remove weird shadow
            position.z
        )
        pose = Pose.SLEEPING
        inventory.selectedSlot = selectedSlot
    }

    init {
        updateSkin(skin)
        updateEquipment()
        if (Config.Debug.logCorpseCreation)
            Mrthvolium.componentLogger.info("Created corpse at $position in world '${world.name}'")
    }

    internal fun sendSpawn(player: Player) {
        assertNotRemoved()
        player.send(
            ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                body
            ),
            ClientboundAddEntityPacket(
                body.id,
                uuid,
                body.x,
                body.y,
                body.z,
                body.xRot,
                body.yRot,
                body.type,
                0,
                body.deltaMovement,
                body.yHeadRot.toDouble()
            ),
            ClientboundSetEntityDataPacket(
                body.id,
                body.entityData.nonDefaultValues
                    ?: emptyList<SynchedEntityData.DataValue<*>>()
            ),
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                CORPSE_TEAM,
                gameProfile.name,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        )
        sendEquipment(player)
    }

    fun spawn() {
        assertNotRemoved()
        spawned = true
        Bukkit.getOnlinePlayers().forEach(::sendSpawn)
    }

    fun remove(dropItems: Boolean, dropExperience: Boolean) {
        if (spawned) Bukkit.getOnlinePlayers().forEach(::sendRemove)
        if (dropExperience && Config.Corpse.storeExperience && experience > 0) NmsExperienceOrb.awardWithDirection(
            world.nms,
            position.nms,
            Vec3.ZERO,
            experience,
            ExperienceOrb.SpawnReason.PLAYER_DEATH,
            null,
            null
        )
        removed = true
        rotTask?.cancel()
        despawnTask?.cancel()
        cache.remove(mainPart.uniqueId)
        inventoryHolder.remove(dropItems)
        if (Config.Debug.logCorpseRemoval)
            Mrthvolium.componentLogger.info("Removed corpse at $position in world '${world.name}'")
    }

    private fun sendRemove(player: Player) {
        assertNotRemoved()
        player.send(
            ClientboundPlayerInfoRemovePacket(
                listOf(uuid)
            ),
            ClientboundRemoveEntitiesPacket(
                body.id
            )
        )
    }

    fun updateSkin(skin: Property) {
        assertNotRemoved()
        this.skin = skin
        gameProfile.properties.removeAll("textures")
        gameProfile.properties.put("textures", skin)

        if (spawned) Bukkit.getOnlinePlayers().forEach {
            sendRemove(it)
            sendSpawn(it)
        }
    }

    private fun sendEquipment(player: Player) {
        assertNotRemoved()
        player.send(
            ClientboundSetEquipmentPacket(
                body.id,
                PLAYER_EQUIPMENT.map { slot ->
                    Pair(slot, body.getItemBySlot(slot))
                }
            )
        )
    }

    fun updateEquipment() {
        assertNotRemoved()
        PLAYER_EQUIPMENT.forEach { slot ->
            body.setItemSlot(slot, inventoryHolder.getEquipment(slot).nms)
        }
        if (spawned) Bukkit.getOnlinePlayers().forEach(::sendEquipment)
    }

    fun poof() {
        assertNotRemoved()
        val offset = 0.25

        world.spawnParticle(
            Particle.POOF,
            position.x,
            position.y,
            position.z,
            20,
            offset, offset, offset,
            body.random.nextGaussian() * 0.02
        )
    }

    fun rot() {
        assertNotRemoved()
        Config.Corpse.Rotten.skin?.let { updateSkin(it) }
        PersistentData.CORPSE_ROT_TIMESTAMP.write(mainPart, System.currentTimeMillis())
        val despawnAfter = Config.Corpse.Rotten.despawnAfter
        if (despawnAfter >= 0) despawnAfter(despawnAfter)
        updateName()
    }

    fun rotAfter(ticks: Long) {
        assertNotRemoved()
        rotTask = Bukkit.getScheduler().runTaskLater(
            Mrthvolium,
            ::rot,
            ticks
        )
    }

    fun despawn(poof: Boolean = true) {
        assertNotRemoved()
        if (poof) poof()
        remove(dropItems = false, dropExperience = false)
        mainPart.remove()
    }

    fun despawnAfter(ticks: Long) {
        assertNotRemoved()
        despawnTask = Bukkit.getScheduler().runTaskLater(
            Mrthvolium,
            ::despawn,
            ticks
        )
    }

    fun updateName() {
        assertNotRemoved()
        val name = if (rotten) Config.Corpse.Rotten.name else Config.Corpse.name
        mainPart.text(
            name?.let {
                MiniMessage.miniMessage().deserialize(
                    it,
                    CorpseTagResolvers.cachedOrNewNameForMainPart(mainPart)
                )
            }
        )
    }
}