package com.wyno.orehider;

import com.wyno.orehider.analysis.DatabaseManager;
import com.wyno.orehider.analysis.MiningListener;
import com.wyno.orehider.modules.GhostManager;
import com.wyno.orehider.modules.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class OreHider extends JavaPlugin {

    private static OreHider instance;
    private PaperIntegrator integrator;
    private DiscordNotifier discord;
    private DatabaseManager database;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Core Systems
        this.database = new DatabaseManager(this);
        this.integrator = new PaperIntegrator(this);
        this.discord = new DiscordNotifier(this);
        
        // 2. Modules
        this.guiManager = new GuiManager(this);
        new GhostManager(this); // Initializes itself
        
        // 3. Listeners & Commands
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        
        getCommand("orehider").setExecutor(new OreHiderCommand(this));

        // 4. Integrity Check
        integrator.enforceGlobalConfig();
        discord.send(getConfig().getString("discord.messages.startup"));
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
        if (discord != null) discord.send(getConfig().getString("discord.messages.shutdown"));
    }

    public static OreHider getInstance() { return instance; }
    public PaperIntegrator getIntegrator() { return integrator; }
    public DiscordNotifier getDiscord() { return discord; }
    public DatabaseManager getDatabase() { return database; }
    public GuiManager getGuiManager() { return guiManager; }
}