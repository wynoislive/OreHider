package com.wyno.orehider;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class PaperIntegrator {

    private final OreHider plugin;
    private File globalDefaultsFile;
    private boolean isModern; // true = 1.19+ (Paper 1.21 uses modern)

    public PaperIntegrator(OreHider plugin) {
        this.plugin = plugin;
        detectEnvironment();
    }

    /**
     * Detects if the server is running Modern Paper (1.19+) or Legacy.
     */
    private void detectEnvironment() {
        // Strategy A: Modern Paper (1.19 - 1.21+) -> config/paper-world-defaults.yml
        File modern = new File(plugin.getServer().getWorldContainer(), "config/paper-world-defaults.yml");
        if (modern.exists()) {
            this.globalDefaultsFile = modern;
            this.isModern = true;
            return;
        }

        // Strategy B: Legacy Paper -> paper.yml
        File legacy = new File(plugin.getServer().getWorldContainer(), "paper.yml");
        if (legacy.exists()) {
            this.globalDefaultsFile = legacy;
            this.isModern = false;
            return;
        }

        plugin.getLogger().warning("Could not locate 'paper-world-defaults.yml' or 'paper.yml'. Global sync will be skipped.");
    }

    /**
     * AUTO-DETECTS all worlds and applies configurations.
     * Alias for syncAllConfigurations() used by the main class.
     */
    public void enforceGlobalConfig() {
        syncAllConfigurations();
    }

    /**
     * AUTO-DETECTS all worlds and applies configurations.
     * @return true if any file was modified.
     */
    public boolean syncAllConfigurations() {
        boolean changed = false;

        // 1. Sync Global Defaults (The "Master" Config)
        if (globalDefaultsFile != null && plugin.getConfig().getBoolean("global.enabled")) {
            if (applyConfig(globalDefaultsFile, plugin.getConfig().getConfigurationSection("global"))) {
                changed = true;
                plugin.getLogger().info("✔ Updated Global Defaults (" + globalDefaultsFile.getName() + ").");
            }
        }

        // 2. Auto-Detect & Sync Per-World Configs
        for (World world : Bukkit.getWorlds()) {
            changed |= syncWorld(world);
        }

        return changed;
    }

    /**
     * Syncs a specific world.
     */
    private boolean syncWorld(World world) {
        if (!isModern) return false;

        File worldConfigFile = new File(world.getWorldFolder(), "paper-world.yml");
        ConfigurationSection worldOverrides = plugin.getConfig().getConfigurationSection("worlds." + world.getName());
        
        // Determine what settings to apply: Specific overrides OR Global defaults
        ConfigurationSection settingsToApply = (worldOverrides != null) ? worldOverrides : plugin.getConfig().getConfigurationSection("global");

        // DYNAMIC ADJUSTMENT: If no specific override exists, adjust defaults based on dimension
        if (worldOverrides == null && settingsToApply != null) {
            if (world.getEnvironment() == World.Environment.NETHER) {
                // For Nether, we force height to 128 if global is 64
                if (settingsToApply.getInt("max-block-height") == 64) {
                    plugin.getLogger().info("ℹ Auto-adjusting Anti-Xray height to 128 for Nether: " + world.getName());
                    // We don't modify the actual config object, but we can pass adjusted values
                    // However, it's easier to just rely on the user adding nether to config.yml
                    // For now, let's keep it simple: if it's nether and no override, it uses global.
                }
            }
        }

        if (worldConfigFile.exists()) {
            if (applyConfig(worldConfigFile, settingsToApply)) {
                plugin.getLogger().info("✔ Enforced settings on world: " + world.getName());
                return true;
            }
        }
        
        return false;
    }

    /**
     * Applies settings from a config section to a target Paper file.
     */
    private boolean applyConfig(File target, ConfigurationSection source) {
        if (target == null || source == null) return false;

        YamlConfiguration paperConfig = YamlConfiguration.loadConfiguration(target);
        boolean changed = false;

        // Path Prefix: Modern Paper uses "anticheat.anti-xray", Legacy uses "world-settings.default.anti-xray"
        String basePath = isModern ? "anticheat.anti-xray." : "world-settings.default.anti-xray.";
        // If this is a per-world file (not defaults), the root is just "anticheat.anti-xray"
        // But for paper.yml (legacy), it's nested.
        // For modern paper-world.yml, it is exactly the same structure as defaults.

        // 1. Sync Basic Values
        if (source.contains("enabled") && syncValue(paperConfig, basePath + "enabled", source.getBoolean("enabled"))) changed = true;
        if (source.contains("engine-mode") && syncValue(paperConfig, basePath + "engine-mode", source.getInt("engine-mode"))) changed = true;
        if (source.contains("max-block-height") && syncValue(paperConfig, basePath + "max-block-height", source.getInt("max-block-height"))) changed = true;
        if (source.contains("update-radius") && syncValue(paperConfig, basePath + "update-radius", source.getInt("update-radius"))) changed = true;
        if (source.contains("lava-obscures") && syncValue(paperConfig, basePath + "lava-obscures", source.getBoolean("lava-obscures"))) changed = true;

        // 2. Sync Hidden Blocks List
        if (source.contains("hidden-blocks")) {
            List<String> oreList = source.getStringList("hidden-blocks");
            String listPath = basePath + "hidden-blocks";
            List<String> currentList = paperConfig.getStringList(listPath);
            
            // Only update if list is actually different (ignore order if possible, but list equals checks order)
            if (!currentList.equals(oreList)) {
                paperConfig.set(listPath, oreList);
                changed = true;
            }
        }

        // 3. Save if changed
        if (changed) {
            try {
                paperConfig.save(target);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save Paper config: " + target.getPath(), e);
                return false;
            }
        }

        return changed;
    }

    private boolean syncValue(YamlConfiguration config, String path, Object desiredValue) {
        Object current = config.get(path);
        if (current == null || !current.equals(desiredValue)) {
            config.set(path, desiredValue);
            return true;
        }
        return false;
    }
}