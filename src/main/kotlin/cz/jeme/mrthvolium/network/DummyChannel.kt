package cz.jeme.mrthvolium.network

import io.netty.channel.*
import java.net.SocketAddress

class DummyChannel : AbstractChannel(null) {
    private val config: ChannelConfig by lazy {
        DefaultChannelConfig(this)
            .setAutoRead(true)
    }

    private val metadata: ChannelMetadata by lazy {
        ChannelMetadata(true)
    }

    override fun config() = config

    override fun isOpen() = false

    override fun isActive() = false

    override fun metadata() = metadata

    override fun newUnsafe() = null

    override fun isCompatible(loop: EventLoop?) = false

    override fun localAddress0() = null

    override fun remoteAddress0() = null

    override fun doBind(localAddress: SocketAddress?) {}

    override fun doDisconnect() {}

    override fun doClose() {}

    override fun doBeginRead() {}

    override fun doWrite(`in`: ChannelOutboundBuffer?) {}
}