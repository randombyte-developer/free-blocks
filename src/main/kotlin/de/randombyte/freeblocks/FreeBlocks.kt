package de.randombyte.freeblocks

import com.google.inject.Inject
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.block.SolidCubeProperty
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.plugin.Plugin

@Plugin(id = FreeBlocks.ID, name = FreeBlocks.NAME, version = FreeBlocks.VERSION, authors = arrayOf(FreeBlocks.AUTHOR))
class FreeBlocks @Inject constructor(val logger: Logger, @DefaultConfig(sharedRoot = true) val configLoader: ConfigurationLoader<CommentedConfigurationNode>) {
    companion object {
        const val ID = "freeblocks"
        const val NAME = "FreeBlocks"
        const val VERSION = "v0.1"
        const val AUTHOR = "RandomByte"
    }

    private val selectedFreeBlocks = mutableListOf<FreeBlock>()

    @Listener
    fun onInit(event: GameInitializationEvent) {
        FreeBlock.init(Cause.of(NamedCause.source(SpawnCause.builder().type(SpawnTypes.PLUGIN).build()),
                NamedCause.owner(Sponge.getPluginManager().fromInstance(this).get())), this)

        logger.info("$NAME loaded: $VERSION")
    }

    @Listener
    fun featherRightClick(event: InteractBlockEvent.Secondary, @First player: Player) {
        if (player.getItemInHand(HandTypes.MAIN_HAND).orElse(null)?.item?.equals(ItemTypes.FEATHER) ?: false) {
            if (player.getOrElse(Keys.IS_SNEAKING, false)) {
                // Switch block movement direction
            } else {
                // Select block
                if (event.targetBlock.getProperty(SolidCubeProperty::class.java).orElse(null)?.value ?: false) {
                    // Is representable by a FallingBlock and has the same hitbox as a shulker
                    val targetLocation = event.targetBlock.location.orElseThrow {
                        RuntimeException("Couldn't get location of block that was right clicked!")
                    }
                    val freeBlock = FreeBlock.create(targetLocation.add(0.0, 0.3, 0.0), targetLocation.block)
                }
            }
        }
    }
}