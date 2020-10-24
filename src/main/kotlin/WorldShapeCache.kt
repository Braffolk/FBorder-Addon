package ee.braffolk.factionsx

import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.manager.GridManager
import net.prosavage.factionsx.persist.data.FLocation
import java.util.*
import kotlin.collections.HashMap

class WorldShapeCache(val world: String) {
  private val factionChunks = HashMap<Long, LinkedList<FLocation>>()
  private val factionVerticalLines = HashMap<Long, List<FLocation>>()
  private val factionOuterCorners = HashMap<Long, List<FLocation>>()
  private val factionInnerCorners = HashMap<Long, List<FLocation>>()
  private val factionXLines = HashMap<Long, List<Line>>()
  private val factionZLines = HashMap<Long, List<Line>>()

  init {
    createAllMeshes()
  }

  fun cacheChunk(faction: Faction, location: FLocation) {
    if(!factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id] = LinkedList()
    }
    factionChunks[faction.id]!!.add(location)
  }

  fun removeChunk(faction: Faction, location: FLocation) {
    if(factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id]!!.remove(location)
    }
  }

  fun removeFactionChunks(faction: Faction) {
    if(factionChunks.containsKey(faction.id)) {
      factionChunks[faction.id]!!.clear()
    }
  }

  fun cacheFaction(faction: Faction) {
    factionChunks[faction.id] = LinkedList()
    GridManager.getAllClaims(faction)
        .filter { it.world == world }
        .forEach { location ->
          cacheChunk(faction, location)
        }
  }

  fun createAllMeshes() {
    factionChunks.forEach { (faction, chunks) ->
      createFactionMesh(faction)
    }
  }

  fun createFactionMesh(faction: Long) {
    val chunks = factionChunks[faction]!!

    factionOuterCorners[faction] = chunks.flatMap { c ->
      val pairs = listOf(
          FLocation(c.x - 1, c.z - 1, c.world),
          FLocation(c.x + 1, c.z + 1, c.world),
          FLocation(c.x - 1, c.z + 1, c.world),
          FLocation(c.x + 1, c.z - 1, c.world)
      )
      pairs
          .filter { p ->
            // outer corner
            chunks.none {
              it.world == c.world  &&(
                  (it.x == p.x && it.z == p.z || it.x == c.x && it.z == p.z || it.x == p.x && it.z == c.z))
            }
          }
          .map { FLocation(
              c.x * 16 + if (it.x > c.x) 16 else 0,
              c.z * 16 + if (it.z > c.z) 16 else 0,
              c.world
          ) }
    }

    factionInnerCorners[faction] = chunks.flatMap { c ->
      val pairs = listOf(
          FLocation(c.x - 1, c.z - 1, c.world),
          FLocation(c.x + 1, c.z + 1, c.world),
          FLocation(c.x - 1, c.z + 1, c.world),
          FLocation(c.x + 1, c.z - 1, c.world)
      )
      pairs
          .filter { p ->
            // inner corner
            chunks.none { it.world == c.world && it.x == p.x && it.z == p.z } &&
                chunks.any { it.world == c.world && (it.x == p.x && it.z == c.z) } &&
                chunks.any { it.world == c.world && (it.x == c.x && it.z == p.z) }
          }
          .map { FLocation(
              c.x * 16 + if (it.x > c.x) 16 else 0,
              c.z * 16 + if (it.z > c.z) 16 else 0,
              c.world
          ) }
    }

    factionVerticalLines[faction] = (factionOuterCorners[faction]!!.toTypedArray() + factionInnerCorners[faction]!!.toTypedArray()).toList()

    factionXLines[faction] = chunks.flatMap { c ->
      val pairs = listOf(FLocation(c.x - 1, c.z, c.world), FLocation(c.x + 1, c.z, c.world), )
      pairs
          .filter { p -> chunks.none { it.x == p.x && it.z == p.z } }
          .map { Line(
              FLocation(c.x * 16 + if (it.x > c.x) 16 else 0, c.z * 16, c.world),
              FLocation(c.x * 16 + if (it.x > c.x) 16 else 0, c.z * 16 + 16, c.world)
          ) }
    }
    factionZLines[faction] = chunks.flatMap { c ->
      val pairs = listOf(FLocation(c.x, c.z - 1, c.world), FLocation(c.x, c.z + 1, c.world), )
      pairs
          .filter { p -> chunks.none { it.x == p.x && it.z == p.z } }
          .map { Line(
              FLocation(c.x * 16, c.z * 16 + if (it.z > c.z) 16 else 0, c.world),
              FLocation(c.x * 16 + 16, c.z * 16 + 16 + if (it.z > c.z) 16 else 0, c.world)
          ) }
    }
  }

  fun getFactionInnerCorners(faction: Faction): List<FLocation> {
    return factionInnerCorners[faction.id]!!
  }

  fun getFactionOuterCorners(faction: Faction): List<FLocation> {
    return factionOuterCorners[faction.id]!!
  }

  fun getFactionXLines(faction: Faction): List<Line> {
    return factionXLines[faction.id]!!
  }

  fun getFactionZLines(faction: Faction): List<Line> {
    return factionZLines[faction.id]!!
  }

  fun getFactionChunks(faction: Faction): List<FLocation> {
    return factionChunks[faction.id]!!
  }

  fun isCached(faction: Faction): Boolean {
    return factionChunks.containsKey(faction.id)
  }
}