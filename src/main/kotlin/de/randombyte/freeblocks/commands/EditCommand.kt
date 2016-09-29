package de.randombyte.freeblocks.commands

import de.randombyte.freeblocks.FreeBlocks
import de.randombyte.freeblocks.PlayerEventListeners.Companion.isInEditMode
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.service.user.UserStorageService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import java.util.*

class EditCommand : PlayerExecutedCommand() {
    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        if (FreeBlocks.resetPlayer(player)) {
            player.sendMessage(Text.of(FreeBlocks.LOGO, TextColors.YELLOW, " You left the edit mode!"))
            return CommandResult.success()
        } else if (trySettingNewEditor(player)) {
            player.sendMessage(Text.of(FreeBlocks.LOGO, TextColors.GREEN, " Your are in edit mode!"))
            player.sendMessage(Text.of(TextColors.YELLOW, "Right click blocks to select, scroll to move, " +
                            "shift right click to change axis, shift scroll to change speed"))
            return CommandResult.success()
        }

        // We can assume that currentEditor is non-null because we are after trySettingNewEditor()
        val currentEditorName = FreeBlocks.currentEditor!!.toUser().name
        throw CommandException(
                Text.of("Couldn't set player as editor: $currentEditorName currently is the editor!"))
    }

    /**
     * Tries to set [newEditor] as the current editor. For replacing the current editor the previous
     * one first has to be removed.
     *
     * @return True if successful, false if not
     */
    private fun trySettingNewEditor(newEditor: Player): Boolean {
        return if (FreeBlocks.currentEditor == null) {
            FreeBlocks.currentEditor = newEditor.uniqueId
            true
        } else newEditor.isInEditMode()
    }

    private fun getUserStoragService() = Sponge.getServiceManager()
            .provide(UserStorageService::class.java)
            .orElseThrow { CommandException(Text.of("UserStorageService not available!")) }

    private fun UUID.toUser(): User = getUserStoragService()
            .get(FreeBlocks.currentEditor)
            .orElseThrow {
                CommandException(Text.of("Couldn't find user with UUID ${FreeBlocks.currentEditor}!"))
            }
}