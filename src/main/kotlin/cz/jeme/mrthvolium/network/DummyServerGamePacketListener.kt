package cz.jeme.mrthvolium.network

import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.CommonListenerCookie
import net.minecraft.server.network.ServerGamePacketListenerImpl

class DummyServerGamePacketListener(player: ServerPlayer) : ServerGamePacketListenerImpl(
    player.server,
    DummyConnection(),
    player,
    CommonListenerCookie(
        player.gameProfile,
        0,
        player.clientInformation(),
        false
    )
)