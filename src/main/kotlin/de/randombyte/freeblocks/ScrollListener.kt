package de.randombyte.freeblocks

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.filter.cause.Root
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent
import org.spongepowered.api.item.inventory.entity.Hotbar
import org.spongepowered.api.service.user.UserStorageService
import java.util.*

object ScrollListener {
    private lateinit var plugin: FreeBlocks

    fun init(plugin: FreeBlocks) {
        this.plugin = plugin
        Sponge.getEventManager().registerListeners(plugin, this)
    }

    private var lastEditor: UUID? = null
    private var lastSelectedHotbarSlot = -1

    @Listener
    fun onChangeSelectedSlot(event: ChangeInventoryEvent.Held, @Root player: Player) {
        if (FreeBlocks.currentEditor == null) return
        val direction = getScrollingDirection()
        if (direction == 0 || Math.abs(direction) > 3) return // Scrolled too fast todo
        val scrolledEvent = CurrentEditorScrolledEvent(
                FreeBlocks.currentEditor!!.toPlayer(),
                direction,
                event.cause)
        Sponge.getEventManager().post(scrolledEvent)
    }

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