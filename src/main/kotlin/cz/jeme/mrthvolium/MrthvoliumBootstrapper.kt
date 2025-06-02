package cz.jeme.mrthvolium

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext

@Suppress("UnstableApiUsage")
internal class MrthvoliumBootstrapper : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {}

    override fun createPlugin(context: PluginProviderContext) = Mrthvolium
}