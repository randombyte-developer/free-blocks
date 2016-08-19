package de.randombyte.freeblocks

import com.google.inject.Inject
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.PropertyHolder
import org.spongepowered.api.data.property.block.SolidCubeProperty
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.action.InteractEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.text.Text

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

        logger.info("$NAME loaded: $VERSION")
    }

    fun Player.isHoldingFeather() = getItemInHand(HandTypes.MAIN_HAND).orElse(null)?.item?.equals(ItemTypes.FEATHER) ?: false
    fun Player.isSneaking() = getOrElse(Keys.IS_SNEAKING, false)
    fun PropertyHolder.isSolid() = getProperty(SolidCubeProperty::class.java).orElse(null)?.value ?: false

    @Listener
    fun featherRightClick(event: InteractEvent, @First player: Player) {
        if (player.isHoldingFeather() && player.isSneaking()) {
            // Switch block movement direction
            player.sendMessage(Text.of("Switched direction"))
        }
    }

    @Listener
    fun featherRightClickOnBlock(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetBlock.isSolid()) {
            val targetLocation = event.targetBlock.location.orElseThrow {
                RuntimeException("Couldn't get location of block that was right clicked!")
            }
            FreeBlock.create(targetLocation.add(0.0, 0.3, 0.0), targetLocation.block).selected = true
        }
    }

    @Listener
    fun featherRightClickOnShulker(event: InteractEntityEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetEntity.type.equals(EntityTypes.SHULKER)) {
            event.targetEntity.vehicle.ifPresent { vehicle ->
                val freeBlock = FreeBlock.fromArmorStand(vehicle)
                if (freeBlock != null)
                    freeBlock.selected = !freeBlock.selected
            }
        }
    }
}