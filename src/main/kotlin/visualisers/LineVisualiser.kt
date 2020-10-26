package ee.braffolk.factionsx.visualisers

import ee.braffolk.factionsx.VisualisationHandler
import ee.braffolk.factionsx.VisualisationPerformance
import ee.braffolk.factionsx.cache.BlockHeightCache
import ee.braffolk.factionsx.cache.ShapeCache
import ee.braffolk.factionsx.standardDeviation
import kotlinx.coroutines.*
import net.prosavage.factionsx.manager.FactionManager
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.pow
import kotlin.system.measureTimeMillis

class LineVisualiser(override val shapeCache: ShapeCache) : IVisualiserHandler {
  val heightCache = BlockHeightCache(shapeCache)
  private val maxRenderDistance = (Bukkit.getViewDistance() * 16).toDouble().pow(2).toLong()
  val maxDustSize = 16.0

  override fun visualise(player: Player, visualisationPerformance: VisualisationPerformance) {
    val eyeY = player.eyeLocation.y.toLong()
    val playerDir = player.location.direction.normalize()
    val playerPos = player.location.toVector()

    val playerPerformanceF = when (visualisationPerformance) {
      VisualisationPerformance.Fast -> 4.0
      VisualisationPerformance.Normal -> 2.0
      VisualisationPerformance.Fancy -> 1.0
    }
    val iPlayerPerformanceF = playerPerformanceF.toInt()

    val operations = FactionManager.getFactions().flatMap { faction ->
      if (!faction.isSystemFaction()) {
        if (!shapeCache.isCached(faction)) {
          shapeCache.cacheFaction(faction)
          shapeCache.createFactionMesh(faction.id)
        }
        if (!heightCache.isCached(faction.id)) {
          heightCache.createFactionMesh(faction.id)
        }

        val worldName = player.world.name
        val factionColor = VisualisationHandler.getPlayerRelationColor(player, faction)

        val xLines = heightCache.getFactionXHeights(faction.id, worldName)
        val zLines = heightCache.getFactionZHeights(faction.id, worldName)

        val shouldRender = listOf(
            { v: Vector -> v.z.toInt().rem(iPlayerPerformanceF) == 0 },
            { v: Vector -> v.x.toInt().rem(iPlayerPerformanceF) == 0 }
        )
        listOf(xLines, zLines).flatMapIndexed { index, l ->
          val lines = l.filter { it.heights.isNotEmpty() }
          lines.zip(lines.map {
            val loc = Location(player.world, (it.location.x * 16.0 + 8.0), eyeY.toDouble(), (it.location.z * 16.0 + 8.0))
            player.location.distanceSquared(loc)
          })
              .filter { (chunk, distance) -> distance < maxRenderDistance }
              .filter { (chunk, _) ->
                val listY = chunk.heights.map { it.y }
                val avgY = listY.average()
                val standardDeviation = listY.standardDeviation(avgY)

                val linePos = Vector(chunk.location.x * 16.0 + 8.0, avgY, chunk.location.z * 16.0 + 8.0)

                if(standardDeviation > 30) {
                  val lineDirFlat = linePos.clone().setY(eyeY.toFloat())
                      .subtract(playerPos.setY(eyeY.toFloat())).normalize()
                  playerDir.angle(lineDirFlat) < Math.PI * 0.5
                } else {
                  val lineDir = linePos.clone().subtract(playerPos).normalize()
                  playerDir.angle(lineDir) < Math.PI * 0.5
                }
              }
              .flatMap { (chunk, distance) ->
                val size = (distance / maxRenderDistance * maxDustSize)
                val dust = Particle.DustOptions(if (Math.random() < 0.33) Color.BLACK else factionColor, size.coerceAtLeast(2.0).toFloat())

                chunk.heights
                    .filter { v -> shouldRender[index](v) }
                    .map { v ->
                      { VisualisationHandler.createParticle(player, v.x + 0.5, v.y + 1.5, v.z + 0.5, dust) }
                    }
              }
        }
      } else {
        listOf()
      }
    }.toMutableList()


    if (operations.isNotEmpty()) {
      GlobalScope.launch {
        sendAndWait(operations, 0)
      }
    }
  }

  suspend fun sendAndWait(operations: MutableList<() -> Unit>, lastTime: Long) {
    delay((VisualisationHandler.visualisationInterval / operations.size) - lastTime)

    val time = measureTimeMillis { operations.removeAt(0)() }
    if (operations.isNotEmpty()) {
      sendAndWait(operations, time)
    }
  }
}


