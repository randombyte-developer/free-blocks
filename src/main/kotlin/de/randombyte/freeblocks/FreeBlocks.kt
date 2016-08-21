package de.randombyte.freeblocks

import com.google.inject.Inject
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(id = FreeBlocks.ID, name = FreeBlocks.NAME, version = FreeBlocks.VERSION, authors = arrayOf(FreeBlocks.AUTHOR))
class FreeBlocks @Inject constructor(val logger: Logger, @DefaultConfig(sharedRoot = true) val configLoader: ConfigurationLoader<CommentedConfigurationNode>) {
    companion object {
        const val ID = "freeblocks"
        const val NAME = "FreeBlocks"
        const val VERSION = "v0.1"
        const val AUTHOR = "RandomByte"
    }

    @Listener
    fun onInit(event: GameInitializationEvent) {
        val spawnCause = Cause.of(NamedCause.source(SpawnCause.builder().type(SpawnTypes.PLUGIN).build()),
                NamedCause.owner(Sponge.getPluginManager().fromInstance(this).get()))
        val worldModifierCause = Cause.of(NamedCause.source(Sponge.getPluginManager().fromInstance(this).get()))
        FreeBlock.init(spawnCause, worldModifierCause, this)

        Sponge.getEventManager().registerListeners(this, PlayerEventListeners())

        logger.info("$NAME loaded: $VERSION")
    }

    /**
     * Prevents a [FreeBlock] from taking damage(lava, fire, water, suffocating in blocks...)
     */
    @Listener
    fun onDamageShulker(event: DamageEntityEvent) {
        event.isCancelled = FreeBlock.getLoadedFreeBlocks().any { freeBlock ->
            event.targetEntity.equals(freeBlock.armorStand) ||
            event.targetEntity.equals(freeBlock.fallingBlock) ||
            event.targetEntity.equals(freeBlock.shulker)
        }
        logger.info("Cancelled: ${event.isCancelled}")
    }
}