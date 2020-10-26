package ee.braffolk.factionsx.cache

import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.persist.data.FLocation
import org.bukkit.Bukkit

data class Line(
    val p1: FLocation,
    val p2: FLocation
)


class ShapeCache {
  private val worldCaches = hashMapOf<String, WorldShapeCache>()

  init {
    Bukkit.getWorlds().forEach {
      Bukkit.broadcastMessage("caching world ${it.name}")
      worldCaches[it.name] = WorldShapeCache(it.name)
    }
  }

  fun cacheChunk(faction: Faction, location: FLocation) =
      worldCaches[location.world]!!.cacheChunk(faction, location)

  fun removeChunk(faction: Faction, location: FLocation) =
      worldCaches[location.world]!!.removeChunk(faction, location)

  fun removeFactionChunks(faction: Faction) =
      worldCaches.forEach { (_, cache) -> cache.removeFactionChunks(faction) }

  fun cacheFaction(faction: Faction) =
      worldCaches.forEach { (_, cache) -> cache.cacheFaction(faction) }

  fun createAllMeshes() =
      worldCaches.forEach { (_, cache) -> cache.createAllMeshes() }

  fun createFactionMesh(faction: Long) =
      worldCaches.forEach { (_, cache) -> cache.createFactionMesh(faction) }

  fun isCached(faction: Faction): Boolean =
      worldCaches.all { (_, cache) -> cache.isCached(faction) }

  fun getWorldCache(world: String): WorldShapeCache = worldCaches[world]!!
}