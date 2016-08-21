package de.randombyte.freeblocks

import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.PropertyHolder
import org.spongepowered.api.data.property.block.SolidCubeProperty
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.action.InteractEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.extent.Extent

class PlayerEventListeners {
    companion object {
        fun Player.isHoldingFeather() = getItemInHand(HandTypes.MAIN_HAND).orElse(null)?.item?.equals(ItemTypes.FEATHER) ?: false
        fun Player.isSneaking() = getOrElse(Keys.IS_SNEAKING, false)
        fun PropertyHolder.isSolid() = getProperty(SolidCubeProperty::class.java).orElse(null)?.value ?: false
        fun Location<out Extent>.getCenter() = Location(extent, blockPosition.toDouble().add(0.5, 0.0, 0.5))
    }

    @Listener
    fun onFeatherRightClick(event: InteractEvent, @First player: Player) {
        if (player.isHoldingFeather() && player.isSneaking()) {
            // Switch block movement direction
            player.sendMessage(Text.of("Switched direction"))
        }
    }

    @Listener
    fun onFeatherRightClickOnBlock(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetBlock.isSolid()) {
            val targetLocation = event.targetBlock.location.orElseThrow {
                RuntimeException("Couldn't get location of block that was right clicked!")
            }
            FreeBlock.create(targetLocation.getCenter(), targetLocation.block).selected = true
        }
    }

    @Listener
    fun onFeatherRightClickOnShulker(event: InteractEntityEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetEntity.type.equals(EntityTypes.SHULKER)) {
            event.targetEntity.vehicle.ifPresent { vehicle ->
                val freeBlock = FreeBlock.fromArmorStand(vehicle)
                if (freeBlock != null)
                    freeBlock.selected = !freeBlock.selected
            }
        }
    }
}