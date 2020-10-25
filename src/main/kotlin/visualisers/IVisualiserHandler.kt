package ee.braffolk.factionsx.visualisers
import ee.braffolk.factionsx.VisualisationPerformance
import ee.braffolk.factionsx.cache.ShapeCache
import org.bukkit.entity.Player

interface IVisualiserHandler {
  val shapeCache: ShapeCache
  fun visualise(player: Player, visualisationPerformance: VisualisationPerformance)
}