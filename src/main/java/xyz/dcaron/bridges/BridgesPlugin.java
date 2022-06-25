package xyz.dcaron.bridges;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BridgesPlugin extends JavaPlugin {

    private static Plugin plugin;

    public static Plugin getPlugin() {
        return BridgesPlugin.plugin;
    }

    private static final Logger LOGGER = Logger.getLogger("Minecraft.xyz.dcaron.bridges");
    private static final String CONFIG_YML = "config.yml";

    public static void log(final String message, final Level level) {
        if (BridgesPlugin.LOGGER.isLoggable(level)) {
            BridgesPlugin.LOGGER.log(level, "[RetractableBridges] " + message);
        }
    }

    @Override
    public void onDisable() {
        BridgesPlugin.log("Plugin disabled.", Level.INFO);
    }

    @Override
    public void onEnable() {
        BridgesPlugin.log("Plugin enabled.", Level.INFO);
        BridgesPlugin.plugin = this;

        final BridgeOptions bridgeOptions = readConfigurationFiles();
        BridgesPlugin.log(bridgeOptions.getOptionsPrintable(), Level.INFO);

        final PluginManager pluginManager = super.getServer().getPluginManager();
        pluginManager.registerEvents(new BridgeBlockListener(bridgeOptions), this);
    }

    private BridgeOptions readConfigurationFiles() {
        final File configurationFile = new File(super.getDataFolder(), CONFIG_YML);
        firstRun(configurationFile);
        final FileConfiguration configData = YamlConfiguration.loadConfiguration(configurationFile);
        return new BridgeOptions(configData);
    }

    private void firstRun(final File configurationFile) {
        if (!configurationFile.exists()) {
            configurationFile.getParentFile().mkdirs();
            copyFile(getResource(CONFIG_YML), configurationFile);
        }
    }

    private void copyFile(InputStream in, File file) {
        try {
            final OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
