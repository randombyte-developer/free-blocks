package de.randombyte.freeblocks

import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent

class CurrentEditorScrolledEvent(private val player: Player, val direction: Int, private val cause: Cause) : TargetPlayerEvent {
    override fun getCause() = cause
    override fun getTargetEntity() = player
}