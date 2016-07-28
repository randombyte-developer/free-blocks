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
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.extent.Extent

class FreeBlock private constructor(val location: Location<out Extent>, val armorStand: Entity, val fallingBlock: Entity, val shulker: Entity) {
    companion object {
        lateinit var spawnCause: Cause

        var selectedFreeBlocks = listOf<FreeBlock>()
            private set

        /**
         * Once called at server startup.
         */
        fun init(spawnCause: Cause, pluginInstance: Any) {
            this.spawnCause = spawnCause
            Sponge.getScheduler().createTaskBuilder().execute { ->
                resetFallTime() // Prevents despawning of FallingBlocks
            }.intervalTicks(1).submit(pluginInstance)
        }

        fun fromArmorStand(armorStand: Entity): FreeBlock? {
            fun List<Entity>.findByType(type: EntityType) = find { it.type.equals(type) }

            val shulker = armorStand.passengers.findByType(EntityTypes.SHULKER)
            val fallingBlock = armorStand.passengers.findByType(EntityTypes.FALLING_BLOCK)
            return if (armorStand.passengers.size == 2 && shulker != null && fallingBlock != null) {
                val freeBlock = FreeBlock(armorStand.location, armorStand, fallingBlock, shulker)
                freeBlock.selected = shulker.getOrElse(Keys.GLOWING, false)
                return freeBlock
            } else null
        }

        fun create(location: Location<out Extent>, blockState: BlockState): FreeBlock {
            if (location.hasBlock()) location.block = BlockTypes.AIR.defaultState
            val armorStand = spawnArmorStand(location)
            val fallingBlock = spawnFallingBlock(location, blockState)
            val shulker = spawnShulker(location)
            armorStand.addPassenger(fallingBlock)
            armorStand.addPassenger(shulker)
            return FreeBlock(location, armorStand, fallingBlock, shulker)
        }

        private fun spawnEntity(location: Location<out Extent>, entityType: EntityType,
                                modifyEntityFunc: (Entity) -> Unit): Entity {
            val entity = location.extent.createEntity(entityType, location.position).orElseThrow {
                RuntimeException("Couldn't create ${entityType.name} at $location!")
            }
            modifyEntityFunc.invoke(entity)
            if (!location.extent.spawnEntity(entity, spawnCause))
                throw RuntimeException("Couldn't spawn ${entityType.name} at $location!")
            return entity
        }

        private fun spawnArmorStand(location: Location<out Extent>) = spawnEntity(location, EntityTypes.ARMOR_STAND, {
            it.offer(Keys.INVISIBLE, true)
            it.offer(Keys.ARMOR_STAND_HAS_GRAVITY, false)
            it.offer(Keys.ARMOR_STAND_MARKER, true)
        })

        private fun spawnFallingBlock(location: Location<out Extent>, blockState: BlockState) = spawnEntity(location, EntityTypes.FALLING_BLOCK, {
            it.offer(Keys.FALL_TIME, Integer.MIN_VALUE)
            it.offer(Keys.FALLING_BLOCK_STATE, blockState)
        })

        private fun spawnShulker(location: Location<out Extent>) = spawnEntity(location, EntityTypes.SHULKER, {
            it.offer(Keys.AI_ENABLED, false)
            it.offer(Keys.IS_SILENT, true)
            it.offer(Keys.PERSISTS, true)
            it.offer(it.getOrCreate(PotionEffectData::class.java).get().effects()
                    .add(PotionEffect.of(PotionEffectTypes.INVISIBILITY, 0, Integer.MAX_VALUE)))
        })

        private fun resetFallTime() {
            Sponge.getServer().worlds.forEach { world ->
                world.loadedChunks.forEach { chunk ->
                    chunk.entities.filter { it.type.equals(EntityTypes.FALLING_BLOCK) }.forEach { fallingBlock ->
                        fallingBlock.offer(Keys.FALL_TIME, 1)
                    }
                }
            }
        }
    }

    var selected = false
        set(value) {
            val selectedIndex = selectedFreeBlocks.indexOfFirst { it.armorStand.uniqueId.equals(armorStand.uniqueId) }
            if (selectedIndex < 0 && value) selectedFreeBlocks += this
            else if (selectedIndex > 0 && !value) selectedFreeBlocks -= selectedFreeBlocks[selectedIndex]
            shulker.offer(Keys.GLOWING, value)
            field = value
        }
}