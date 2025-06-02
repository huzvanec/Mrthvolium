package cz.jeme.mrthvolium.network

import net.minecraft.network.Connection
import net.minecraft.network.protocol.PacketFlow

class DummyConnection() : Connection(PacketFlow.SERVERBOUND) {
    init {
        channel = DummyChannel()
        address = DummySocketAddress()
    }
}