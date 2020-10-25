package ee.braffolk.factionsx.visualisers

import ee.braffolk.factionsx.VisualisationHandler.Companion.createParticle
import ee.braffolk.factionsx.VisualisationHandler.Companion.getPlayerRelationColor
import ee.braffolk.factionsx.VisualisationPerformance
import ee.braffolk.factionsx.cache.ShapeCache
import net.prosavage.factionsx.manager.FactionManager
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.lang.Long
import kotlin.math.*

class BoxVisualiser(override val shapeCache: ShapeCache) : IVisualiserHandler {
  override fun visualise(player: Player, visualisationPerformance: VisualisationPerformance) {
    val worldName = player.world.name

    val eyeY = player.eyeLocation.y.toLong()
    val verticalRange = Long.max(0, (eyeY - 32) / 64 * 64)..min((eyeY + 64) / 64 * 64, 256)
    val maxRenderDistance = (Bukkit.getViewDistance() * 16).toDouble().pow(2).toLong()

    val playerPerformanceF = when (visualisationPerformance) {
      VisualisationPerformance.Fast -> 4.0
      VisualisationPerformance.Normal -> 2.0
      VisualisationPerformance.Fancy -> 1.0
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
}