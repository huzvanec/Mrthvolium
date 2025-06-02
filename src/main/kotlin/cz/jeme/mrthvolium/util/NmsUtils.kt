package cz.jeme.mrthvolium.util

import net.minecraft.network.protocol.Packet
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.scoreboard.CraftScoreboard
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Scoreboard
import net.minecraft.world.entity.EquipmentSlot as NMSEquipmentSlot
import net.minecraft.world.item.ItemStack as NMSItemStack
import net.minecraft.world.scores.Scoreboard as NMSScoreboard

inline val Player.nms: ServerPlayer
    get() = (this as CraftPlayer).handle

inline val Player.connection: ServerGamePacketListenerImpl
    get() = nms.connection

fun Player.send(first: Packet<*>, vararg rest: Packet<*>) {
    connection.send(first)
    rest.forEach { connection.send(it) }
}

inline val Server.nms: DedicatedServer
    get() = (this as CraftServer).server

inline val Scoreboard.nms: NMSScoreboard
    get() = (this as CraftScoreboard).handle

inline val World.nms: ServerLevel
    get() = (this as CraftWorld).handle

inline val ItemStack?.nms: NMSItemStack
    get() = (this as? CraftItemStack)?.handle ?: CraftItemStack.asNMSCopy(this)

inline val EquipmentSlot.nms: NMSEquipmentSlot
    get() = CraftEquipmentSlot.getNMS(this)