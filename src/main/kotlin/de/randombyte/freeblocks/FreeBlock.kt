package de.randombyte.freeblocks

import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData
import org.spongepowered.api.effect.potion.PotionEffect
import org.spongepowered.api.effect.potion.PotionEffectTypes
import org.spongepowered.api.entity.Entity
import org.spongepowered.api.entity.EntityType
import org.spongepowered.api.entity.EntityTypes
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.scheduler.Task
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.extent.Extent

class FreeBlock private constructor(val armorStand: Entity, val fallingBlock: Entity, val shulker: Entity) {
    companion object {
        private var initialized = false
        private lateinit var spawnCause: Cause
        private lateinit var worldModiferCause: Cause

        var selectedFreeBlocks = listOf<FreeBlock>()
            private set

        /**
         * Called once at server startup.
         */
        fun init(spawnCause: Cause, worldModiferCause: Cause, pluginInstance: Any) {
            if (initialized) throw IllegalStateException("Can't initialize twice!")
            this.spawnCause = spawnCause
            this.worldModiferCause = worldModiferCause
            Task.builder().execute { ->
                resetFallTime() // Prevents despawning of FallingBlocks
            }.intervalTicks(1).submit(pluginInstance)
            initialized = true
        }

        /**
         * @return An existing [FreeBlock] where [armorStand] belongs to the structure or null
         */
        fun fromArmorStand(armorStand: Entity): FreeBlock? {
            fun List<Entity>.findByType(type: EntityType) = find { it.type.equals(type) }

            if (armorStand.passengers.size != 2) return null // fail-fast
            val shulker = armorStand.passengers.findByType(EntityTypes.SHULKER)
            val fallingBlock = armorStand.passengers.findByType(EntityTypes.FALLING_BLOCK)

            return if (shulker != null && fallingBlock != null) {
                val freeBlock = FreeBlock(armorStand, fallingBlock, shulker)
                freeBlock.selected = shulker.getOrElse(Keys.GLOWING, false)
                return freeBlock
            } else null
        }

        /**
         * Removes an existing block at [location] and builds the armorStand-shulker-fallingSand structure. The [blockState]
         * defines the appearance of the falling sand.
         * @return The [FreeBlock] that was generated
         */
        fun create(location: Location<out Extent>, blockState: BlockState): FreeBlock {
            if (location.hasBlock()) location.setBlock(BlockTypes.AIR.defaultState, worldModiferCause)

            val armorStand = spawnArmorStand(location)
            val fallingBlock = spawnFallingBlock(location, blockState)
            val shulker = spawnShulker(location)
            armorStand.addPassenger(fallingBlock)
            armorStand.addPassenger(shulker)
            return FreeBlock(armorStand, fallingBlock, shulker)
        }

        /**
         * Spawns an [Entity] at [location] of type [entityType].
         * @return The spawned entity
         * @throws [RuntimeException] if the entity couldn't be spawned
         */
        private fun spawnEntity(location: Location<out Extent>, entityType: EntityType, modifyEntityFunc: Entity.() -> Unit): Entity {
            val entity = location.extent.createEntity(entityType, location.position)
            modifyEntityFunc.invoke(entity)
            if (!location.extent.spawnEntity(entity, spawnCause))
                throw RuntimeException("Couldn't spawn ${entityType.name} at $location!")
            return entity
        }

        /**
         * Spawns a floating, invisible ArmorStand at [location].
         * @return The spawned entity
         * @throws [RuntimeException] if the entity couldn't be spawned
         */
        private fun spawnArmorStand(location: Location<out Extent>): Entity = spawnEntity(location, EntityTypes.ARMOR_STAND, {
            offer(Keys.INVISIBLE, true)
            offer(Keys.ARMOR_STAND_HAS_GRAVITY, false)
            offer(Keys.ARMOR_STAND_MARKER, true)
        })

        /**
         * Spawns a falling-sand [Entity] at [location]. The [blockState] defines the appearance of the entity.
         * @return The spawned entity
         * @throws [RuntimeException] if the entity couldn't be spawned
         */
        private fun spawnFallingBlock(location: Location<out Extent>, blockState: BlockState): Entity =
                spawnEntity(location, EntityTypes.FALLING_BLOCK, {
                    offer(Keys.FALL_TIME, Integer.MIN_VALUE)
                    offer(Keys.FALLING_BLOCK_STATE, blockState)
        })

        /**
         * Spawns an AI-disabled, silent, invisible and persistent Shulker at [location].
         * @return The spawned entity
         * @throws [RuntimeException] if the entity couldn't be spawned
         */
        private fun spawnShulker(location: Location<out Extent>) = spawnEntity(location, EntityTypes.SHULKER, {
            offer(Keys.AI_ENABLED, false)
            offer(Keys.IS_SILENT, true)
            offer(Keys.PERSISTS, true)
            offer(getOrCreate(PotionEffectData::class.java).get().effects()
                    .add(PotionEffect.of(PotionEffectTypes.INVISIBILITY, 0, Integer.MAX_VALUE)))
        })

        /**
         * @return A [List] of [FreeBlock]s of every loaded chunk on the server.
         */
        fun getLoadedFreeBlocks(): List<FreeBlock> {
            return Sponge.getServer().worlds.map { world ->
                world.loadedChunks.map { chunk -> // Ensure that only loaded chunks are handled to improve performance
                    chunk.entities
                            .filter { it.type.equals(EntityTypes.ARMOR_STAND) }
                            .mapNotNull { fromArmorStand(it) }
                }.flatten()
            }.flatten()
        }

        /**
         * Resets the fall time of [freeBlocks] to 1.
         */
        private fun resetFallTime(vararg freeBlocks: FreeBlock) = freeBlocks.forEach { it.fallingBlock.offer(Keys.FALL_TIME, 1) }
    }

    // glowing effect
    var selected = false
        set(value) {
            val selectedIndex = selectedFreeBlocks.indexOfFirst { it.armorStand.uniqueId.equals(armorStand.uniqueId) }
            if (selectedIndex < 0 && value) {
                selectedFreeBlocks += this
            } else if (selectedIndex > 0 && !value) {
                selectedFreeBlocks -= selectedFreeBlocks[selectedIndex]
            }

            if (field != value) {
                shulker.offer(Keys.GLOWING, value)
                field = value
            }
        }
}