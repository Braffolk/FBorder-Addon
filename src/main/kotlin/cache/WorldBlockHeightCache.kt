package ee.braffolk.factionsx.cache

import net.prosavage.factionsx.manager.FactionManager
import net.prosavage.factionsx.persist.data.FLocation
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.absoluteValue

data class FChunkHeights(val location: FLocation, val heights: CopyOnWriteArrayList<Vector>)

class WorldBlockHeightCache(private val shapeCache: WorldShapeCache) {
  private val factionXHeights = HashMap<Long, CopyOnWriteArrayList<FChunkHeights>>()
  private val factionZHeights = HashMap<Long, CopyOnWriteArrayList<FChunkHeights>>()

  init {
    createAllMeshes()
  }

  fun createFactionMesh(faction: Long) {
    if(!shapeCache.isCached(faction)) {
      shapeCache.cacheFaction(faction)
      shapeCache.createFactionMesh(faction)
    }
    storeLines(faction, shapeCache.getFactionXLines(faction), factionXHeights)
    storeLines(faction, shapeCache.getFactionZLines(faction), factionZHeights)
  }

  fun updateChunkMesh(faction: Long, x: Int, z: Int) {
    if(!shapeCache.isCached(faction)) {
      shapeCache.cacheFaction(faction)
      shapeCache.createFactionMesh(faction)
    }
    val location = Pair(x, z)
    updateChunkLines(faction, location, shapeCache.getChunkXLines(x, z), factionXHeights)
    updateChunkLines(faction, location, shapeCache.getChunkZLines(x, z), factionZHeights)
  }

  fun createAllMeshes() {
    FactionManager.getFactions().forEach {
      if (it.id != FactionManager.WILDERNESS_ID) {
        createFactionMesh(it.id)
      }
    }
  }

  fun getFactionXHeights(faction: Long): CopyOnWriteArrayList<FChunkHeights> {
    return factionXHeights[faction]!!
  }

  fun getFactionZHeights(faction: Long): CopyOnWriteArrayList<FChunkHeights> {
    return factionZHeights[faction]!!
  }

  fun isCached(faction: Long): Boolean {
    return factionXHeights.containsKey(faction) && factionZHeights.containsKey(faction)
  }

  private fun storeLines(faction: Long, lines: List<Line>, map: HashMap<Long, CopyOnWriteArrayList<FChunkHeights>>) {
    if(!map.containsKey(faction)) {
      map[faction] = CopyOnWriteArrayList()
    } else {
      map[faction]?.clear()
    }

    addLinesToFactionList(lines, map[faction]!!)
  }

  private fun updateChunkLines(faction: Long, location: Pair<Int, Int>, lines: List<Line>, map: HashMap<Long, CopyOnWriteArrayList<FChunkHeights>>) {
    map[faction] = CopyOnWriteArrayList(
      map[faction]!!
        .filter { it.location.x.toInt() != location.first || it.location.z.toInt() != location.second }
    )

    addLinesToFactionList(lines, map[faction]!!)
  }

  private fun addLinesToFactionList(lines: List<Line>, list: CopyOnWriteArrayList<FChunkHeights>) {
    lines.forEach {
      val world = Bukkit.getWorld(it.p1.world)!!
      val chunk = world.getBlockAt(it.p1.x.toInt(), 0, it.p1.z.toInt()).chunk
      val location = FLocation(chunk.x.toLong(), chunk.z.toLong(), world.name)
      val chunkHeights = FChunkHeights(location, CopyOnWriteArrayList())
      val x1 = chunk.x * 16
      val z1 = chunk.z * 16

      for(x in it.p1.x.toInt()..it.p2.x.toInt()) {
        for(z in it.p1.z.toInt()..it.p2.z.toInt()) {
          val bx = (x - x1).absoluteValue
          val bz = (z - z1).absoluteValue
          var prev1 = chunk.getBlock(bx, 1, bz)
          var prev2 = chunk.getBlock(bx, 0, bz)
          for(y in 2 until world.maxHeight) {
            val block = chunk.getBlock(bx, y, bz)

            if(!prev2.isEmpty && prev1.isEmpty && block.isEmpty) {
              chunkHeights.heights.add(Vector(x, y-2, z))
            }
            prev2 = prev1
            prev1 = block
          }
        }
      }
      list.add(chunkHeights)
    }
  }
}