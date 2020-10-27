package ee.braffolk.factionsx.persist

import net.prosavage.factionsx.FactionsX
import net.prosavage.factionsx.addonframework.Addon
import java.io.File



class FBorderConfig: Config() {

  fun save(addon: Addon) {
    addon.configSerializer.save(this,
        File(addon.addonDataFolder, "border-config.json")
    )
  }

  fun load(addon: Addon) {
    FactionsX.baseCommand.getHelpInfo()
    addon.configSerializer.load(this,
        Config::class.java,
        File(addon.addonDataFolder, "border-config.json")
    )
  }

  companion object {
    val instance = FBorderConfig()
  }
}

open class Config {
  var visualisationInterval = 550L
  var colorAlly = mutableListOf(191, 255, 0)
  var colorHome = mutableListOf(191, 255, 0)
  var colorNeutral = mutableListOf(255, 255, 0)
  var colorTruce = mutableListOf(0, 0, 255)
  var colorEnemy = mutableListOf(255, 0, 0)
}