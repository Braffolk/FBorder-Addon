package ee.braffolk.factionsx.listener

import ee.braffolk.factionsx.VisualisationHandler
import net.prosavage.factionsx.event.FactionPreClaimEvent
import net.prosavage.factionsx.event.FactionUnClaimAllEvent
import net.prosavage.factionsx.event.FactionUnClaimEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent


class ClaimListener(val visualisationHandler: VisualisationHandler) : Listener {
  val shapeCache = visualisationHandler.shapeCache

  @EventHandler
  fun onUnclaim(event: FactionUnClaimEvent) {
    shapeCache.removeChunk(event.factionUnClaiming, event.fLocation)
    shapeCache.createFactionMesh(event.factionUnClaiming.id)
  }

  @EventHandler
  fun onUnclaimAll(event: FactionUnClaimAllEvent) {
    shapeCache.removeFactionChunks(event.unclaimingFaction)
    shapeCache.createFactionMesh(event.unclaimingFaction.id)
  }

  @EventHandler
  fun onClaim(event: FactionPreClaimEvent) {
    shapeCache.cacheFaction(event.factionClaiming)
    shapeCache.cacheChunk(event.factionClaiming, event.fLocation)
    shapeCache.createFactionMesh(event.factionClaiming.id)
  }

  @EventHandler
  fun onPlayerLogout(event: PlayerQuitEvent) {
    val player = event.player
    visualisationHandler.removePlayer(player)
  }
}