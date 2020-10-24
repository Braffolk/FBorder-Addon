package ee.braffolk.factionsx

import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.manager.GridManager
import net.prosavage.factionsx.persist.data.FLocation
import org.bukkit.Bukkit
import org.bukkit.event.server.BroadcastMessageEvent
import java.util.*
import kotlin.collections.HashMap

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
      worldCaches.forEach { world, cache -> cache.removeFactionChunks(faction) }

  fun cacheFaction(faction: Faction) =
      worldCaches.forEach { (world, cache) -> cache.cacheFaction(faction) }

  fun createAllMeshes() =
      worldCaches.forEach { (world, cache) -> cache.createAllMeshes() }

  fun createFactionMesh(faction: Long) =
      worldCaches.forEach { (world, cache) -> cache.createFactionMesh(faction) }

  fun getFactionInnerCorners(faction: Faction, world: String): List<FLocation> =
      worldCaches[world]!!.getFactionInnerCorners(faction)

  fun getFactionOuterCorners(faction: Faction, world: String): List<FLocation> =
      worldCaches[world]!!.getFactionOuterCorners(faction)

  fun getFactionXLines(faction: Faction, world: String): List<Line>  =
      worldCaches[world]!!.getFactionXLines(faction)

  fun getFactionZLines(faction: Faction, world: String): List<Line>  =
      worldCaches[world]!!.getFactionZLines(faction)

  fun getFactionChunks(faction: Faction, world: String): List<FLocation>  =
      worldCaches[world]!!.getFactionChunks(faction)

  fun isCached(faction: Faction): Boolean =
      worldCaches.all { (world, cache) -> cache.isCached(faction) }
}