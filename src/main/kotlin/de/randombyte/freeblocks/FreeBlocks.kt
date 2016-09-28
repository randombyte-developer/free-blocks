package de.randombyte.freeblocks

import com.google.inject.Inject
import de.randombyte.freeblocks.PlayerEventListeners.Companion.isInEditMode
import de.randombyte.freeblocks.commands.EditCommand
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes
import org.spongepowered.api.event.entity.DamageEntityEvent
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.scheduler.Task
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.Axis
import java.util.*
import java.util.concurrent.TimeUnit

@Plugin(id = FreeBlocks.ID, name = FreeBlocks.NAME, version = FreeBlocks.VERSION, authors = arrayOf(FreeBlocks.AUTHOR))
class FreeBlocks @Inject constructor(val logger: Logger, @DefaultConfig(sharedRoot = true) val configLoader: ConfigurationLoader<CommentedConfigurationNode>) {
    companion object {
        const val ID = "freeblocks"
        const val NAME = "FreeBlocks"
        const val VERSION = "v0.1"
        const val AUTHOR = "RandomByte"

        const val DEBUG = false

        var currentEditor: UUID? = null
        var currentMoveAxis: Axis = Axis.X
        var currentMoveSpeedIndex: Int = 12

        val movementSpeeds =
                listOf(0.005, 0.01, 0.02, 0.03, 0.05, 0.08, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.8, 1.0, 2.0, 5.0, 10.0)

        var blockedDamagePerSecond = 0

        val LOGO = Text.of(TextColors.BLUE, "[FreeBlocks]")

        /**
         * If this player is the editor it will be reset and all blocks become unselected.
         *
         * @return if player was editor
         */
        fun resetPlayer(player: Player): Boolean {
            return if (player.isInEditMode()) {
                FreeBlocks.currentEditor = null
                FreeBlock.getSelectedBlocks().forEach { it.selected = false }
                true
            } else false
        }
    }

    @Listener
    fun onInit(event: GameInitializationEvent) {
        val spawnCause = Cause.of(NamedCause.source(SpawnCause.builder().type(SpawnTypes.PLUGIN).build()),
                NamedCause.owner(Sponge.getPluginManager().fromInstance(this).get()))
        val worldModifierCause = Cause.of(NamedCause.source(Sponge.getPluginManager().fromInstance(this).get()))

        FreeBlock.init(spawnCause, worldModifierCause, this)
        ScrollListener.init(this)

        Sponge.getEventManager().registerListeners(this, PlayerEventListeners())

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .child(CommandSpec.builder()
                        .permission("freeblocks.edit")
                        .executor(EditCommand())
                        .build(), "edit")
                .build(), "freeblocks")

        logger.info("$NAME loaded: $VERSION")

        if (DEBUG) {
            Task.builder().async().interval(1, TimeUnit.SECONDS).execute { ->
                logger.info("Blocked damage events per second: $blockedDamagePerSecond")
                blockedDamagePerSecond = 0
            }.submit(this)
        }
    }

    /**
     * Prevents a [FreeBlock] from taking damage(lava, fire, water, suffocating in blocks...)
     */
    @Listener
    fun onDamageFreeBlock(event: DamageEntityEvent) {
        if (FreeBlock.getLoadedFreeBlocks().any { freeBlock ->
            event.targetEntity == freeBlock.armorStand ||
            event.targetEntity == freeBlock.fallingBlock ||
            event.targetEntity == freeBlock.shulker
        }) {
            event.isCancelled = true
            if (DEBUG) blockedDamagePerSecond++
        }
    }
}