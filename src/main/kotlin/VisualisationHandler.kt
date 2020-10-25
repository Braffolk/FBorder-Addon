package ee.braffolk.factionsx

import net.prosavage.factionsx.core.Faction
import net.prosavage.factionsx.manager.FactionManager
import net.prosavage.factionsx.util.Relation
import net.prosavage.factionsx.util.getFPlayer
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.lang.Long.max
import java.util.*
import kotlin.math.*


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
            } else {
              playerList.remove(it)
            }
          }
        } catch (e: Exception) {
          Bukkit.broadcastMessage("error occured with border visualisation: ${e.message}")
        }
      }
    }, 250, 250)
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

  fun stop() {
    timer.cancel()
  }

  fun visualise(player: Player) {
    val worldName = player.world.name

    val eyeY = player.eyeLocation.y.toLong()
    val verticalRange = max(0, (eyeY - 32) / 64 * 64)..min((eyeY + 64) / 64 * 64, 256)
    val maxRenderDistance = (Bukkit.getViewDistance() * 16).toDouble().pow(2).toLong()

    val playerPerformanceF = when (playerPerformanceMap[player.uniqueId]) {
      VisualisationPerformance.Fast -> 4.0
      VisualisationPerformance.Normal -> 2.0
      VisualisationPerformance.Fancy -> 1.0
      else -> 2.0
    }

    FactionManager.getFactions().forEach { faction ->
      if (!faction.isSystemFaction()) {
        if (!shapeCache.isCached(faction)) {
          shapeCache.cacheFaction(faction)
          shapeCache.createFactionMesh(faction.id)
        }
        val factionColor = getPlayerRelationColor(player, faction)
        val factionColorDark = Color.fromRGB(
            (factionColor.red - 96).coerceAtLeast(0),
            (factionColor.green - 96).coerceAtLeast(0),
            (factionColor.blue - 48).coerceAtLeast(0)
        )

        // get all lines and planes to be rendered
        val outerCorners = shapeCache.getFactionOuterCorners(faction, worldName)
        val innerCorners = shapeCache.getFactionInnerCorners(faction, worldName)

        val zLines = shapeCache.getFactionZLines(faction, worldName)
        val xLines = shapeCache.getFactionXLines(faction, worldName)

        val horisontalPlanes = shapeCache.getFactionChunks(faction, worldName)

        val shapeCount = outerCorners.size + innerCorners.size + xLines.size + zLines.size + horisontalPlanes.size
        val dynamicPerformanceF = (log(shapeCount.toDouble(), 8.0 - playerPerformanceF) - 2.0).coerceAtLeast(0.0)
        val performanceF = playerPerformanceF + dynamicPerformanceF

        // dusts for top and bottom planes
        val calculateStep = { diff: Double -> max(performanceF, abs(diff) / (10 - performanceF)) }
        val bottomStep = calculateStep((eyeY - verticalRange.first).toDouble())
        val topStep = calculateStep((eyeY - verticalRange.last).toDouble())

        val bottomDust = Particle.DustOptions(factionColor, bottomStep.toFloat())
        val topDust = Particle.DustOptions(factionColor, topStep.toFloat())

        // visualise vertical corner lines
        listOf(outerCorners, innerCorners).zip(listOf(factionColor, factionColorDark))
            .forEach { (lines, wantedColor) ->
              lines.zip(lines.map {
                val loc = Location(player.world, it.x.toDouble(), eyeY.toDouble(), it.z.toDouble())
                player.location.distanceSquared(loc)
              })
                  .filter { it.second < maxRenderDistance }
                  .forEach { (line, distance) ->
                    var y = verticalRange.first.toDouble()
                    val step = (distance / (maxRenderDistance * (dynamicPerformanceF + 1.0)) * 16 + performanceF).coerceAtLeast(1.0)
                    while (true) {
                      if (y > verticalRange.last) { break }
                      val color = if(Math.random() < 0.2) { Color.BLACK } else { wantedColor }

                      val dust = Particle.DustOptions(color, step.coerceAtLeast(2.0).toFloat())

                      player.spawnParticle(
                          Particle.REDSTONE,
                          Location(player.world, line.x.toDouble(), y, line.z.toDouble()),
                          0,
                          dust
                      )
                      y += step
                    }
                  }
            }

        // horisontal line at eye height
        listOf(zLines, xLines).forEach { lines ->
          lines
              .zip(lines.map {
                val loc = Location(player.world,
                    (it.p1.x + it.p2.x) * 0.5,
                    eyeY.toDouble(),
                    (it.p1.z + it.p2.z) * 0.5)
                player.location.distanceSquared(loc)
              })
              .filter { (_, distance) -> distance < maxRenderDistance }
              .forEach { (line, distance) ->
                val size = (distance / maxRenderDistance * 16).coerceAtLeast(2.0)
                val eyeDust = Particle.DustOptions(factionColor, size.toFloat())
                val step = (distance / (maxRenderDistance * (dynamicPerformanceF + 1.0)) * 16 + performanceF).coerceAtLeast(1.0)

                for (x in line.p1.x..line.p2.x step step.toLong()) {
                  for (z in line.p1.z..line.p2.z step step.toLong()) {
                    createParticle(player, x, eyeY + 1, z, eyeDust)
                  }
                }
              }
        }

        // bottom and top plane
        horisontalPlanes
            .zip(horisontalPlanes.map {
              val loc = Location(player.world, (it.x * 16 + 8).toDouble(), eyeY.toDouble(), (it.z * 16 + 8).toDouble())
              player.location.distanceSquared(loc)
            })
            .filter { (loc, distance) -> distance < maxRenderDistance }
            .forEach { (loc, distance) ->
              var step = floor(5.0 - performanceF / 2 - dynamicPerformanceF * distance/maxRenderDistance).coerceAtLeast(2.0)
              repeat(step.toInt()) { x ->
                repeat(step.toInt()) { z ->
                  val px = loc.x * 16 + (x / (step - 1) * 16.0)
                  val pz = loc.z * 16 + (z / (step - 1) * 16.0)
                  createParticle(player, px, verticalRange.last.toDouble(), pz, topDust)
                }
              }
              step = 2.0
              repeat(step.toInt()) { x ->
                repeat(step.toInt()) { z ->
                  val px = loc.x * 16 + (x / (step - 1) * 16.0)
                  val pz = loc.z * 16 + (z / (step - 1) * 16.0)
                  createParticle(player, px, verticalRange.first.toDouble(), pz, bottomDust)
                }
              }
            }
      }
    }
  }

  private fun getPlayerRelationColor(player: Player, toFaction: Faction): Color {
    return if (player.getFPlayer().hasFaction()) {
      val playerFaction = player.getFPlayer().getFaction()
      if (playerFaction.id == toFaction.id) {
        Color.LIME
      } else {
        when (playerFaction.getRelationTo(toFaction)) {
          Relation.ALLY -> Color.LIME
          Relation.ENEMY -> Color.RED
          Relation.NEUTRAL -> Color.fromRGB(140, 140, 140)
          Relation.TRUCE -> Color.WHITE
        }
      }
    } else {
      Color.fromRGB(140, 140, 140)
    }
  }

  private fun createParticle(player: Player, x: Long, y: Long, z: Long, dust: Particle.DustOptions) {
    createParticle(player, x.toDouble(), y.toDouble(), z.toDouble(), dust)
  }

  private fun createParticle(player: Player, x: Double, y: Double, z: Double, dust: Particle.DustOptions) {
    player.spawnParticle(
        Particle.REDSTONE,
        Location(player.world, x, y, z),
        0,
        dust
    )
  }
}