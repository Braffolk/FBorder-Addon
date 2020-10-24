# FBorderVisualiser-Addon
Minecraft plugin [FactionsX](https://www.spigotmc.org/resources/factionsx.83459/) addon that visualises faction borders using particles and colours them according to faction relations.

![faction with borders visualised](https://i.imgur.com/5i72ii2.png "Example faction")

The visualisation can be enabled by calling /f visualise-borders or /f v-b. The command also has a performance option

- /f visualise Fast - fewer particles
- /f visualise Normal - normal amount of particles
- /f visualise Fancy - a lot of particles

# How to install
Drop the .jar inside the `/plugins/FactionsX/addons` folder

# Known issues
- The plugin can be laggy in massive factions that havent made their claims as one shape. This will be solved by reducing the amount of particles depending on how many lines will be visualised for the player
