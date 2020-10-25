package ee.braffolk.factionsx.listener

import ee.braffolk.factionsx.VisualisationHandler
import net.prosavage.factionsx.event.FactionPreClaimEvent
import net.prosavage.factionsx.event.FactionUnClaimAllEvent
import net.prosavage.factionsx.event.FactionUnClaimEvent
import net.prosavage.factionsx.manager.GridManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkEvent


class ClaimListener(val visualisationHandler: VisualisationHandler) : Listener {
  val shapeCache = visualisationHandler.shapeCache
  val heightCache = visualisationHandler.visualiser.heightCache

  @EventHandler
  fun onUnclaim(event: FactionUnClaimEvent) {
    shapeCache.removeChunk(event.factionUnClaiming, event.fLocation)
    shapeCache.createFactionMesh(event.factionUnClaiming.id)
    heightCache.createFactionMesh(event.factionUnClaiming.id)
  }

  @EventHandler
  fun onUnclaimAll(event: FactionUnClaimAllEvent) {
    shapeCache.removeFactionChunks(event.unclaimingFaction)
    shapeCache.createFactionMesh(event.unclaimingFaction.id)
    heightCache.createFactionMesh(event.unclaimingFaction.id)
  }

  @EventHandler
  fun onClaim(event: FactionPreClaimEvent) {
    shapeCache.cacheChunk(event.factionClaiming, event.fLocation)
    shapeCache.createFactionMesh(event.factionClaiming.id)
    heightCache.createFactionMesh(event.factionClaiming.id)
    if(!event.claimedFaction.isSystemFaction()) {
      shapeCache.removeChunk(event.claimedFaction, event.fLocation)
      shapeCache.createFactionMesh(event.claimedFaction.id)
      heightCache.createFactionMesh(event.claimedFaction.id)
    }
  }

  @EventHandler
  fun onPlayerLogout(event: PlayerQuitEvent) {
    val player = event.player
    visualisationHandler.removePlayer(player)
  }

  @EventHandler
  fun onBlockBreak(event: BlockBreakEvent) {
    val faction = GridManager.getFactionAt(event.block.chunk)
    if(!faction.isSystemFaction()) {
      val world = event.block.world.name
      heightCache.createFactionMesh(world, faction.id)
    }
  }

  @EventHandler
  fun onBlockPlace(event: BlockPlaceEvent) {
    val faction = GridManager.getFactionAt(event.block.chunk)
    if(!faction.isSystemFaction()) {
      val world = event.block.world.name
      heightCache.createFactionMesh(world, faction.id)
    }
  }
}