package de.randombyte.freeblocks.commands

import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text

abstract class PlayerExecutedCommand : CommandExecutor {
    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        if (src !is Player) throw CommandException(Text.of("Command must be executed by player!"))
        return executedByPlayer(src, args)
    }

    abstract fun executedByPlayer(player: Player, args: CommandContext): CommandResult
}