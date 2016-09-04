package de.randombyte.freeblocks

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.event.cause.NamedCause
import org.spongepowered.api.item.inventory.entity.Hotbar
import org.spongepowered.api.scheduler.Task
import org.spongepowered.api.service.user.UserStorageService
import java.util.*

object ScrollListener {
    private var initialized = false

    fun init(plugin: FreeBlocks) {
        if (initialized) throw IllegalStateException("Can't initialize twice!")
        Task.builder()
                .intervalTicks(1)
                .execute { ->
                    if (FreeBlocks.currentEditor == null) return@execute
                    val direction = getScrollingDirection()
                    if (Math.abs(direction) > 3) return@execute // Scrolled too fast todo
                    val event = CurrentEditorScrolledEvent(
                            FreeBlocks.currentEditor!!.toPlayer(),
                            direction,
                            Cause.of(NamedCause.source(plugin)))
                    Sponge.getEventManager().post(event)
                }.submit(plugin)
        initialized = true
    }

    private var lastEditor: UUID? = null
    private var lastSelectedHotbarSlot = -1

    /**
     * Returns what the currentEditor has done.
     * @return > 0 if scrolled up, == 0 if not scrolled, < 0 if scrolled down
     */
    val hotbarCapacity = 9
    private fun getScrollingDirection(): Int {
        val selectedSlot = getSelectedHotbarSlot()
        val direction = if (lastSelectedHotbarSlot == 0 && selectedSlot == hotbarCapacity - 1) {
            // cursor jumped from left to right
            -1
        } else if (lastSelectedHotbarSlot == hotbarCapacity - 1 && selectedSlot == 0) {
            // cursor jumped from right to left
            +1
        } else selectedSlot - lastSelectedHotbarSlot
        lastSelectedHotbarSlot = selectedSlot
        return direction
    }

    private fun getSelectedHotbarSlot(): Int {
        if (FreeBlocks.currentEditor == null) throw RuntimeException("currentEditor is null!")
        val currentSelectedHotbarSlot = FreeBlocks.currentEditor!!.toPlayer()
                .inventory.query<Hotbar>(Hotbar::class.java).selectedSlotIndex
        if (lastEditor == null) {
            lastEditor = FreeBlocks.currentEditor
            lastSelectedHotbarSlot = currentSelectedHotbarSlot
        }
        return currentSelectedHotbarSlot
    }

    /**
     * Tries to get the [Player] from the [UUID].
     * @throws [RuntimeException] if player wasn't found or is offline
     * @return The player
     */
    private fun UUID.toPlayer(): Player {
        return Sponge.getServiceManager().provide(UserStorageService::class.java).get().get(this).orElseThrow {
            RuntimeException("Didn't find player $this!")
        }.player.orElseThrow {
            RuntimeException("Player $this is currently offline!")
        }
    }
}