package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.RegionQuery
import io.mockk.*
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RegionQueryTests {
    @TempDir lateinit var directory: Path
    @Test fun aFastSegmentHitsABlockAcrossChunkBoundariesWithoutForeignReads() {
        val f = PlatformFixture(directory)
        val chunk = mockk<Chunk>()
        every { chunk.entities } returns emptyArray()
        every { f.legacy.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { assertChunk(f, firstArg(), secondArg()); true }
        every { f.legacy.world.getChunkAt(any<Int>(), any<Int>()) } returns chunk
        every { f.legacy.world.minHeight } returns 0
        every { f.legacy.world.maxHeight } returns 256
        val wall = mockk<org.bukkit.block.Block>()
        val air = mockk<org.bukkit.block.Block>()
        val wallShape = mockk<org.bukkit.util.VoxelShape>()
        val emptyShape = mockk<org.bukkit.util.VoxelShape>()
        every { wallShape.boundingBoxes } returns listOf(BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
        every { emptyShape.boundingBoxes } returns emptyList()
        every { wall.collisionShape } returns wallShape
        every { air.collisionShape } returns emptyShape
        every { f.legacy.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers {
            assertChunk(f, firstArg<Int>() shr 4, thirdArg<Int>() shr 4)
            if (firstArg<Int>() == 16) wall else air
        }
        val query = RegionQuery(f.scheduler, { f.legacy.world })
        val hits = query.collisions(SkillLocation(f.worldId, 0.0, 60.5, .5), SkillLocation(f.worldId, 32.0, 60.5, .5), 0.0).toCompletableFuture().join()
        assertEquals(1, hits.size)
        assertEquals(.5, hits[0].fraction(), 1e-9)
        assertEquals(SkillVector(-1.0, 0.0, 0.0), hits[0].normal())
        assertTrue(hits[0].actor().isEmpty)
    }
    @Test fun chunkQueriesSkipUnloadedChunksAndReadMovingEntitiesOnTheirOwnContexts() {
        val f = PlatformFixture(directory)
        val loaded = mockk<Chunk>()
        every { loaded.entities } answers { assertChunk(f, 0, 0); arrayOf(f.legacy.caster, f.legacy.victim) }
        every { f.legacy.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { assertChunk(f, firstArg(), secondArg()); firstArg<Int>() == 0 }
        every { f.legacy.world.getChunkAt(0, 0) } answers { assertChunk(f, 0, 0); loaded }
        every { f.legacy.caster.boundingBox } answers { f.scheduler.assertOwner(f.legacy.caster); BoundingBox(-.3, 60.0, -.3, .3, 61.8, .3) }
        every { f.legacy.victim.boundingBox } answers { f.scheduler.assertOwner(f.legacy.victim); BoundingBox(2.7, 60.0, -.3, 3.3, 61.8, .3) }
        val query = RegionQuery(f.scheduler, { f.scheduler.assertOwner("global"); f.legacy.world })
        val actors = query.entities(SkillBounds(f.worldId, 0.0, 59.0, 0.0, 31.0, 63.0, 1.0)).toCompletableFuture().join()
        assertEquals(setOf(f.legacy.playerId, f.legacy.victim.uniqueId), actors.map { it.id() }.toSet())
        verify(exactly = 0) { f.legacy.world.getChunkAt(1, 0) }
        verify(exactly = 0) { f.legacy.world.entities }
        every { f.legacy.victim.isValid } answers { f.scheduler.assertOwner(f.legacy.victim); false }
        assertTrue(query.snapshot(SkillActor.of(f.legacy.victim)).toCompletableFuture().join().isEmpty)
    }
    @Test fun immutableQueryShapesValidateWorldAndFiniteBoundaries() {
        val world = java.util.UUID.randomUUID()
        assertThrows(IllegalArgumentException::class.java) { SkillBounds(world, 2.0, 0.0, 0.0, 1.0, 1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { SkillBounds(world, 0.0, 0.0, 0.0, Double.NaN, 1.0, 1.0) }
        val f = PlatformFixture(directory)
        val query = RegionQuery(f.scheduler, { null })
        assertTrue(query.entities(SkillBounds(world, 0.0, 0.0, 0.0, 10.0, 10.0, 10.0)).toCompletableFuture().join().isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            query.collisions(SkillLocation(world, 0.0, 0.0, 0.0), SkillLocation(java.util.UUID.randomUUID(), 1.0, 1.0, 1.0), 0.0)
        }
    }
    private fun assertChunk(f: PlatformFixture, x: Int, z: Int) {
        val location = f.scheduler.current as? Location ?: fail("Chunk read outside location context")
        assertEquals(x, location.blockX shr 4); assertEquals(z, location.blockZ shr 4)
    }
}
