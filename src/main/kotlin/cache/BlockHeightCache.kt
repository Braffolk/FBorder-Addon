package ee.braffolk.factionsx.cache

import net.prosavage.factionsx.manager.FactionManager
import org.bukkit.Bukkit
import java.util.concurrent.CopyOnWriteArrayList

class BlockHeightCache(private val shapeCache: ShapeCache) {
  private val worldCaches = hashMapOf<String, WorldBlockHeightCache>()

  init {
    Bukkit.getWorlds().forEach {
      worldCaches[it.name] = WorldBlockHeightCache(shapeCache.getWorldCache(it.name))
    }
  }

  fun createFactionMesh(faction: Long) {
    worldCaches.forEach { (world, cache) ->
      cache.createFactionMesh(faction)
    }
  }

  fun createFactionMesh(world: String, faction: Long) {
    worldCaches[world]!!.createFactionMesh(faction)
  }

  fun updateChunkMesh(world: String, faction: Long, x: Int, z: Int) {
    worldCaches[world]!!.updateChunkMesh(faction, x, z)
  }

  fun createAllMeshes() {
    FactionManager.getFactions().forEach {
      if (it.id != FactionManager.WILDERNESS_ID) {
        createFactionMesh(it.id)
      }
    }
  }

  fun getFactionXHeights(faction: Long, world: String): CopyOnWriteArrayList<FChunkHeights> {
    return worldCaches[world]!!.getFactionXHeights(faction)
  }

  fun getFactionZHeights(faction: Long, world: String): CopyOnWriteArrayList<FChunkHeights> {
    return worldCaches[world]!!.getFactionZHeights(faction)
  }

  fun isCached(faction: Long): Boolean = worldCaches.all { (_, cache) -> cache.isCached(faction) }
}