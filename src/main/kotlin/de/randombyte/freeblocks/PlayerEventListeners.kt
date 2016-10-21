package de.randombyte.freeblocks

import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.property.PropertyHolder
import org.spongepowered.api.data.property.block.SolidCubeProperty
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.action.InteractEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.entity.InteractEntityEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.event.filter.type.Include
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.chat.ChatTypes
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.extent.Extent

/**
 * Manages all player interactions(creating, moving, destroying FreeBlocks, etc.).
 *
 * The MainHand version of all events is used to only have the event fired once for one action. If
 * the superclass/superinterface would be used(e.g. InteractBlockEvent.Secondary instead of
 * InteractBlockEvent.Secondary.MainHand) the event gets fired two times. That's a problem for some
 * interactions like de/selecting a FreeBlock.
 * But there is one exception: InteractEvent.Primary, which gets fired only once. One could use the
 * MainHand version but there might be cases where only the OffHand version gets fired.
 */
class PlayerEventListeners {
    companion object {
        fun Player.isInEditMode() = FreeBlocks.currentEditor?.equals(uniqueId) ?: false
        fun Player.isSneaking() = getOrElse(Keys.IS_SNEAKING, false)
        fun PropertyHolder.isSolid() = getProperty(SolidCubeProperty::class.java).orElse(null)?.value ?: false
        fun Location<out Extent>.getCenter() = Location(extent, blockPosition.toDouble().add(0.5, 0.0, 0.5))
    }

    @Listener
    fun onRightClickOnBlock(event: InteractBlockEvent.Secondary.MainHand, @First player: Player) {
        if (player.run { isInEditMode() && !isSneaking() } && event.targetBlock.isSolid()) {
            val targetLocation = event.targetBlock.location.orElseThrow {
                RuntimeException("Couldn't get location of block that was right clicked!")
            }
            FreeBlock.create(targetLocation.getCenter(), targetLocation.block).selected = true
        }
    }

    @Listener
    fun onRightClickOnShulker(event: InteractEntityEvent.Secondary.MainHand, @First player: Player) {
        if (player.run { isInEditMode() && !isSneaking() } && event.targetEntity.type == EntityTypes.SHULKER) {
            FreeBlock.fromPassengerEntity(event.targetEntity)?.apply { selected = !selected }
        }
    }

    @Listener
    fun onLeftClickOnShulker(event: InteractEntityEvent.Primary, @First player: Player) {
        if (player.run { isInEditMode() && !isSneaking() } && event.targetEntity.type == EntityTypes.SHULKER) {
            FreeBlock.fromPassengerEntity(event.targetEntity)?.remove()
        }
    }

    @Listener
    @Include(InteractBlockEvent.Secondary.MainHand::class, InteractEntityEvent.Secondary.MainHand::class)
    fun onRightClickSneak(event: InteractEvent, @First player: Player) {
        if (player.run { isInEditMode() && isSneaking() }) {
            FreeBlocks.currentMoveAxis = FreeBlocks.currentMoveAxis.cycleNext()
            player.sendMessage(ChatTypes.ACTION_BAR, Text.of(FreeBlocks.LOGO,
                    TextColors.YELLOW, " Switched to ${FreeBlocks.currentMoveAxis.name}-axis!"))
        }
    }

    @Listener
    fun onEditorScrolled(event: CurrentEditorScrolledEvent) {
        if (event.direction == 0) return

        if (!event.targetEntity.isSneaking()) {
            FreeBlock.getSelectedBlocks().forEach { freeBlock ->
                val oldPosition = freeBlock.armorStand.location.position
                val movement = FreeBlocks.currentMoveAxis.toVector3d()
                        .mul(event.direction.toDouble() * FreeBlocks.movementSpeeds[FreeBlocks.currentMoveSpeedIndex])
                freeBlock.armorStand.location = freeBlock.armorStand.location.setPosition(oldPosition.add(movement))
            }
        }
    }

    @Listener
    fun onEditorSneakScrolled(event: CurrentEditorScrolledEvent) {
        if (event.targetEntity.run { isInEditMode() && isSneaking() }) {
            val nextMoveSpeedIndex = (FreeBlocks.currentMoveSpeedIndex + event.direction)
                    .coerceIn(0, FreeBlocks.movementSpeeds.lastIndex)

            if (FreeBlocks.currentMoveSpeedIndex != nextMoveSpeedIndex) {
                event.targetEntity.sendMessage(ChatTypes.ACTION_BAR, Text.of(FreeBlocks.LOGO, TextColors.YELLOW,
                        " Movement speed: ${FreeBlocks.movementSpeeds[nextMoveSpeedIndex]}"))
            }

            FreeBlocks.currentMoveSpeedIndex = nextMoveSpeedIndex
        }
    }

    @Listener
    fun onLeave(event: ClientConnectionEvent.Disconnect) {
        FreeBlocks.resetPlayer(event.targetEntity)
    }
}