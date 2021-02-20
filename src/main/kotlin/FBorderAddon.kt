package ee.braffolk.factionsx

import ee.braffolk.factionsx.cmd.CmdVisualise
import ee.braffolk.factionsx.listener.BorderClaimListener
import ee.braffolk.factionsx.persist.FBorderConfig
import net.prosavage.factionsx.FactionsX
import net.prosavage.factionsx.addonframework.Addon
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.PluginManager


class FBorderAddon : Addon() {
  private val visualisationHandler = VisualisationHandler()
  private var claimListener = BorderClaimListener(visualisationHandler)

  val config = FBorderConfig.instance //FBorderConfig()

  @Override
  override fun onEnable() {
    logColored("Enabling Border Visualiser Addon!")
    FactionsX.baseCommand.addSubCommand(CmdVisualise(visualisationHandler))

    val pluginManager: PluginManager = Bukkit.getPluginManager()
    pluginManager.registerEvents(claimListener, FactionsX.instance)
    config.load(this)
  }

  @Override
  override fun onDisable() {
    logColored("Disabling Border Visualiser Addon!")
    FactionsX.baseCommand.removeSubCommand(CmdVisualise(visualisationHandler))
    visualisationHandler.stop()
    HandlerList.unregisterAll(claimListener)

    // Load first to read changes from file, then save.
    config.load(this)
    config.save(this)
  }
}