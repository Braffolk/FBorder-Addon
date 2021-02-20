package ee.braffolk.factionsx.listener

import ee.braffolk.factionsx.VisualisationHandler
import net.prosavage.factionsx.event.FactionPreClaimEvent
import net.prosavage.factionsx.event.FactionUnClaimAllEvent
import net.prosavage.factionsx.event.FactionUnClaimEvent
import net.prosavage.factionsx.manager.FactionManager
import net.prosavage.factionsx.manager.GridManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent


class BorderClaimListener(val visualisationHandler: VisualisationHandler) : Listener {
  val shapeCache = visualisationHandler.shapeCache
  val heightCache = visualisationHandler.visualiser.heightCache

  @EventHandler
  fun onUnclaim(event: FactionUnClaimEvent) {
    if (event.factionUnClaiming.id != FactionManager.WILDERNESS_ID) {
      shapeCache.removeChunk(event.factionUnClaiming, event.fLocation)
      shapeCache.createFactionMesh(event.factionUnClaiming.id)
      heightCache.createFactionMesh(event.factionUnClaiming.id)
    }
  }

  @EventHandler
  fun onUnclaimAll(event: FactionUnClaimAllEvent) {
    if (event.unclaimingFaction.id != FactionManager.WILDERNESS_ID) {
      shapeCache.removeFactionChunks(event.unclaimingFaction)
      shapeCache.createFactionMesh(event.unclaimingFaction.id)
      heightCache.createFactionMesh(event.unclaimingFaction.id)
    }
  }

  @EventHandler
  fun onClaim(event: FactionPreClaimEvent) {
    if (event.factionClaiming.id != FactionManager.WILDERNESS_ID) {
      shapeCache.cacheChunk(event.factionClaiming, event.fLocation)
      shapeCache.createFactionMesh(event.factionClaiming.id)
      heightCache.createFactionMesh(event.factionClaiming.id)
    }

    if (event.claimedFaction.id != FactionManager.WILDERNESS_ID) {
      shapeCache.removeChunk(event.claimedFaction, event.fLocation)
      shapeCache.createFactionMesh(event.claimedFaction.id)
      heightCache.createFactionMesh(event.claimedFaction.id)
    }
  }

  @EventHandler
  fun onBlockBreak(event: BlockBreakEvent) {
    val faction = GridManager.getFactionAt(event.block.chunk)
    if (faction.id != FactionManager.WILDERNESS_ID) {
      val world = event.block.world.name
      if(!shapeCache.isCached(faction)) {
        shapeCache.cacheFaction(faction)
        shapeCache.createFactionMesh(faction.id)
      }
      if(!heightCache.isCached(faction.id)) {
        heightCache.createFactionMesh(world, faction.id)
      } else {
        // only update the changed chunk
        val chunk = event.block.location.chunk
        heightCache.updateChunkMesh(world, faction.id, chunk.x, chunk.z)
      }
    }
  }

  @EventHandler
  fun onBlockPlace(event: BlockPlaceEvent) {
    val faction = GridManager.getFactionAt(event.block.chunk)
    if (faction.id != FactionManager.WILDERNESS_ID) {
      val world = event.block.world.name
      if(!shapeCache.isCached(faction)) {
        shapeCache.cacheFaction(faction)
        shapeCache.createFactionMesh(faction.id)
      }
      if(!heightCache.isCached(faction.id)) {
        heightCache.createFactionMesh(world, faction.id)
      } else {
        // only update the changed chunk
        val chunk = event.block.location.chunk
        heightCache.updateChunkMesh(world, faction.id, chunk.x, chunk.z)
      }
    }
  }
}