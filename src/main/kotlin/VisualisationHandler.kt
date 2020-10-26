package ee.braffolk.factionsx

import ee.braffolk.factionsx.cache.ShapeCache
import ee.braffolk.factionsx.persist.FBorderConfig
import ee.braffolk.factionsx.visualisers.LineVisualiser
import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.manager.FactionManager
import net.prosavage.factionsx.util.Relation
import net.prosavage.factionsx.util.getFPlayer
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.*


enum class VisualisationPerformance() {
  Fast,
  Normal,
  Fancy
}

class VisualisationHandler {
  val shapeCache = ShapeCache()
  val playerList = mutableListOf<UUID>()
  private val playerPerformanceMap = hashMapOf<UUID, VisualisationPerformance>()
  private val timer = Timer()
  val visualiser = LineVisualiser(shapeCache)

  init {
    FactionManager.getFactions().forEach { faction ->
      if (!faction.isSystemFaction()) {
        shapeCache.cacheFaction(faction)
      }
    }

    shapeCache.createAllMeshes()

    timer.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        try {
          playerList.forEach {
            val player = Bukkit.getPlayer(it)
            if (player != null && player.isOnline) {
              visualise(player)
            }
          }
        } catch (e: Exception) {
          Bukkit.broadcastMessage("error occured with border visualisation: ${e.message} - ${e.stackTraceToString()}")
        }
      }
    }, 1, visualisationInterval)
  }

  fun addPlayer(player: Player, performance: VisualisationPerformance) {
    playerList.add(player.uniqueId)
    playerPerformanceMap[player.uniqueId] = performance
  }

  fun hasPlayer(player: Player): Boolean {
    return playerList.contains(player.uniqueId)
  }

  fun removePlayer(player: Player) {
    if (playerList.contains(player.uniqueId)) {
      playerList.remove(player.uniqueId)
    }
  }

  fun getPlayerPerformance(player: Player): VisualisationPerformance {
    return playerPerformanceMap[player.uniqueId]!!
  }

  fun stop() {
    timer.cancel()
  }

  fun visualise(player: Player) {
    visualiser.visualise(player, playerPerformanceMap[player.uniqueId]!!)
  }

  companion object {
    fun createParticle(player: Player, x: Long, y: Long, z: Long, dust: Particle.DustOptions) {
      createParticle(player, x.toDouble(), y.toDouble(), z.toDouble(), dust)
    }

    fun createParticle(player: Player, x: Double, y: Double, z: Double, dust: Particle.DustOptions) {
      player.spawnParticle(
          Particle.REDSTONE,
          Location(player.world, x, y, z),
          0,
          dust
      )
    }

    fun getPlayerRelationColor(player: Player, toFaction: Faction): Color {
      val config = FBorderConfig.instance
      val color = if (player.getFPlayer().hasFaction()) {
        val playerFaction = player.getFPlayer().getFaction()
        if (playerFaction.id == toFaction.id) {
          config.colorHome
        } else {
          when (playerFaction.getRelationTo(toFaction)) {
            Relation.ALLY -> config.colorAlly
            Relation.ENEMY -> config.colorEnemy
            Relation.NEUTRAL -> config.colorNeutral
            Relation.TRUCE -> config.colorTruce
          }
        }
      } else {
        config.colorNeutral
      }
      return Color.fromRGB(color[0], color[1], color[2])
    }

    val visualisationInterval = FBorderConfig.instance.visualisationInterval
  }
}