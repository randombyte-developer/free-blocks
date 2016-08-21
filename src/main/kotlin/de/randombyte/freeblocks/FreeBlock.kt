package de.randombyte.freeblocks

import com.flowpowered.math.vector.Vector3d
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

        private val _selectedBlocks = mutableListOf<FreeBlock>()
        fun getSelectedBlocks() = _selectedBlocks.toList()

        /**
         * Called once at server startup.
         */
        fun init(spawnCause: Cause, worldModiferCause: Cause, pluginInstance: Any) {
            if (initialized) throw IllegalStateException("Can't initialize twice!")
            this.spawnCause = spawnCause
            this.worldModiferCause = worldModiferCause
            Task.builder().execute { ->
                getLoadedFreeBlocks().forEach { freeBlock ->
                    preventFallingBlockTurningIntoNormalBlock(freeBlock.fallingBlock)
                }
            }.intervalTicks(1).submit(pluginInstance)
            Task.builder().intervalTicks(2000000000).execute { -> // Before FALL_TIME Int.Min_VALUE runs out
                getLoadedFreeBlocks().forEach { freeBlock ->
                    resetFallTime(freeBlock.fallingBlock) // Prevents despawning of FallingBlock
                }
            }
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
            rotation = Vector3d.ZERO // without explicitly setting this it is always a bit off
        })

        /**
         * Spawns a falling-sand [Entity] at [location]. The [blockState] defines the appearance of the entity.
         * @return The spawned entity
         * @throws [RuntimeException] if the entity couldn't be spawned
         */
        private fun spawnFallingBlock(location: Location<out Extent>, blockState: BlockState): Entity =
                spawnEntity(location, EntityTypes.FALLING_BLOCK, {
                    offer(Keys.FALLING_BLOCK_STATE, blockState)
                    resetFallTime(this)
                    preventFallingBlockTurningIntoNormalBlock(this)
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
                    .add(PotionEffect.of(PotionEffectTypes.INVISIBILITY, 0, Int.MAX_VALUE)))
            rotation = Vector3d.ZERO // without explicitly setting this it is always a bit off
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

        private fun resetFallTime(fallingBlock: Entity) = fallingBlock.offer(Keys.FALL_TIME, Int.MIN_VALUE)

        private fun preventFallingBlockTurningIntoNormalBlock(fallingBlock: Entity) {
            val location = fallingBlock.location
            fallingBlock.location = Location(location.extent, location.x, -1.0, location.z)
        }
    }

    var selected = false
        set(value) {
            if (value) _selectedBlocks += this else _selectedBlocks -= this
            shulker.offer(Keys.GLOWING, value)
            field = value
        }

    // The armorStand is the identification of a FreeBlock
    override fun equals(other: Any?) = other is FreeBlock && other.armorStand.uniqueId.equals(armorStand.uniqueId)
}