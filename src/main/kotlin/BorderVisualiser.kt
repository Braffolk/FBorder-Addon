package ee.braffolk.factionsx

import ee.braffolk.factionsx.listener.ClaimListener
import net.prosavage.factionsx.FactionsX
import net.prosavage.factionsx.addonframework.Addon
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager


class BorderVisualiser : Addon() {
  private val visualisationHandler = VisualisationHandler()
  private var claimListener = ClaimListener(visualisationHandler)

  @Override
  override fun onEnable() {
    logColored("Enabling Border Visualiser Addon!")
    FactionsX.baseCommand.addSubCommand(CmdVisualise(visualisationHandler))

    val pluginManager: PluginManager = Bukkit.getPluginManager()
    pluginManager.registerEvents(claimListener, FactionsX.instance)
  }

  @Override
  override fun onDisable() {
    logColored("Disabling Border Visualiser Addon!")
    FactionsX.baseCommand.removeSubCommand(CmdVisualise(visualisationHandler))
    visualisationHandler.stop()
    HandlerList.unregisterAll(claimListener);
  }
}