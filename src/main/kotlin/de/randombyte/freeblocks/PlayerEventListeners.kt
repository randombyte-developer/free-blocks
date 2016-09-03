package de.randombyte.freeblocks

import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.PropertyHolder
import org.spongepowered.api.data.property.block.SolidCubeProperty
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
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
    fun onFeatherRightClickSneakOnBlock(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (FreeBlocks.currentEditor?.equals(player.uniqueId) ?: false && player.isHoldingFeather() && player.isSneaking()) {
            FreeBlocks.currentMoveAxis = FreeBlocks.currentMoveAxis.cycleNext()
            player.sendMessage(Text.of("Switched to ${FreeBlocks.currentMoveAxis.name}-axis!"))
        }
    }

    @Listener
    fun onFeatherRightClickOnBlock(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetBlock.isSolid()) {
            if (!trySettingNewEditor(player)) return
            val targetLocation = event.targetBlock.location.orElseThrow {
                RuntimeException("Couldn't get location of block that was right clicked!")
            }
            FreeBlock.create(targetLocation.getCenter(), targetLocation.block).selected = true
        }
    }

    @Listener
    fun onFeatherRightClickOnShulker(event: InteractEntityEvent.Secondary.MainHand, @First player: Player) {
        if (player.isHoldingFeather() && !player.isSneaking() && event.targetEntity.type.equals(EntityTypes.SHULKER)) {
            if (!trySettingNewEditor(player)) return
            event.targetEntity.vehicle.ifPresent { vehicle ->
                FreeBlock.fromArmorStand(vehicle)?.apply { selected = !selected }
                if (FreeBlock.getSelectedBlocks().isEmpty()) FreeBlocks.currentEditor = null
            }
        }
    }

    @Listener
    fun onEditorScrolled(event: CurrentEditorScrolledEvent) {
        if (event.direction == 0) return

        FreeBlock.getSelectedBlocks().forEach { freeBlock ->
            val oldPosition = freeBlock.armorStand.location.position
            val movement = FreeBlocks.currentMoveAxis.toVector3d().mul(event.direction.toDouble() * 0.1)
            val newLocation = freeBlock.armorStand.location.setPosition(oldPosition.add(movement))
            freeBlock.armorStand.location = newLocation
        }
    }

    @Listener
    fun onLeave(event: ClientConnectionEvent.Disconnect) {
        if (FreeBlocks.currentEditor?.equals(event.targetEntity.uniqueId) ?: false) {
            FreeBlocks.currentEditor = null
            FreeBlock.getSelectedBlocks().forEach { it.selected = false }
        }
    }

    /**
     * @return True if successful, false if not
     */
    fun trySettingNewEditor(newEditor: Player): Boolean {
        if (FreeBlocks.currentEditor == null) {
            FreeBlocks.currentEditor = newEditor.uniqueId
            return true
        }
        // Is same UUID?
        return if (!FreeBlocks.currentEditor!!.equals(newEditor.uniqueId)) { // we don't expect modification from another thread
            newEditor.sendMessage(Text.of(TextColors.RED,
                    "There is currently another player using this plugin that selected at least one block!"))
            false
        } else true
    }
}