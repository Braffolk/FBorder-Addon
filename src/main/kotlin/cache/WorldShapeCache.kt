package ee.braffolk.factionsx.cache

import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.manager.FactionManager
import net.prosavage.factionsx.manager.GridManager
import net.prosavage.factionsx.persist.data.FLocation
import org.bukkit.util.BoundingBox
import java.util.*
import kotlin.collections.HashMap

class WorldShapeCache(val world: String) {
  private val factionChunks = HashMap<Long, LinkedList<FLocation>>()
  private val factionXLines = HashMap<Long, List<Line>>()
  private val factionZLines = HashMap<Long, List<Line>>()
  private val factionBbox = HashMap<Long, BoundingBox>()
  private val chunkXLines = HashMap<Pair<Int, Int>, List<Line>>()
  private val chunkZLines = HashMap<Pair<Int, Int>, List<Line>>()

  init {
    createAllMeshes()
  }

  fun cacheChunk(faction: Faction, location: FLocation) {
    if (!factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id] = LinkedList()
    }
    factionChunks[faction.id]!!.add(location)
  }

  fun removeChunk(faction: Faction, location: FLocation) {
    if (factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id]!!.remove(location)
    }
  }

  fun removeFactionChunks(faction: Faction) {
    if (factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id]!!.clear()
    }
  }

  fun cacheFaction(faction: Long) = cacheFaction(FactionManager.getFaction(faction))

  fun cacheFaction(faction: Faction) {
    factionChunks[faction.id] = LinkedList()
    GridManager.getAllClaims(faction)
        .filter { it.world == world }
        .forEach { location ->
          cacheChunk(faction, location)
        }
  }

  fun createAllMeshes() {
    FactionManager.getFactions().forEach {
      if (it.id != FactionManager.WILDERNESS_ID) {
        createFactionMesh(it.id)
      }
    }
  }

  fun createFactionMesh(faction: Long) {
    if(!factionChunks.containsKey(faction)) {
      cacheFaction(faction)
    }
    val chunks = factionChunks[faction]!!

    if(chunks.isNotEmpty()) {
      factionBbox[faction] = BoundingBox(
          chunks.minOf { it.x * 16.0 }, 0.0, chunks.minOf { it.z * 16.0 },
          chunks.maxOf { it.x * 16.0 + 16.0 }, 256.0, chunks.maxOf { it.z * 16.0 + 16.0 }
      )
    } else {
      factionBbox[faction] = BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    factionXLines[faction] = chunks.flatMap { c ->
      val pairs = listOf(FLocation(c.x - 1, c.z, c.world), FLocation(c.x + 1, c.z, c.world))
          .filter { p -> chunks.none { it.x == p.x && it.z == p.z } }
          .map {
            val x = c.x * 16 + if (it.x > c.x) 15 else 0
            Line(
                FLocation(x, c.z * 16, c.world),
                FLocation(x, c.z * 16 + 15, c.world)
            )
          }
      chunkXLines[Pair(c.x.toInt(), c.z.toInt())] = pairs
      pairs
    }
    factionZLines[faction] = chunks.flatMap { c ->
      val pairs = listOf(FLocation(c.x, c.z - 1, c.world), FLocation(c.x, c.z + 1, c.world))
          .filter { p -> chunks.none { it.x == p.x && it.z == p.z } }
          .map {
            val z = c.z * 16 + if (it.z > c.z) 15 else 0
            Line(
                FLocation(c.x * 16, z, c.world),
                FLocation(c.x * 16 + 15, z, c.world)
            )
          }
      chunkZLines[Pair(c.x.toInt(), c.z.toInt())] = pairs
      pairs
    }
  }

  fun getFactionXLines(faction: Long): List<Line> = factionXLines[faction]!!
  fun getFactionZLines(faction: Long): List<Line> = factionZLines[faction]!!
  fun getChunkXLines(x: Int, z: Int): List<Line> = chunkXLines[Pair(x, z)]!!
  fun getChunkZLines(x: Int, z: Int): List<Line> = chunkZLines[Pair(x, z)]!!
  fun isChunkCached(x: Long, z: Long): Boolean =
    chunkXLines.containsKey(Pair(x, z)) && chunkZLines.containsKey(Pair(x, z))

  fun getFactionBbox(faction: Long): BoundingBox = factionBbox[faction]!!

  fun getFactionChunks(faction: Faction): List<FLocation> {
    return factionChunks[faction.id]!!
  }

  fun isCached(faction: Faction): Boolean = isCached(faction.id)
  fun isCached(faction: Long): Boolean {
    return factionChunks.containsKey(faction) &&
        factionXLines.containsKey(faction) &&
        factionZLines.containsKey(faction) &&
        factionBbox.containsKey(faction)
  }
}