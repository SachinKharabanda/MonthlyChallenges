package me.sachin.decemberChallenges;

import Challenges.Menu;
import Commands.Commands;
import ConfigManager.ConfigManager;
import ConfigManager.Messages;
import DBManager.DBManager;
import EventsListeners.CopsListener;
import EventsListeners.LootListener;
import EventsListeners.MythicListener;
import EventsListeners.PlayerKillListener;
import Hooks.CopsHook;
import Hooks.LootHook;
import Hooks.MythicHook;
import Hooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DecemberChallenges extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private DBManager dbManager;
    private Menu menu;

    private LootHook lootHook;
    private MythicHook mythicHook;
    private CopsHook copsHook;
    private VaultHook vaultHook;

    @Override
    public void onEnable() {
        // Ensure default config.yml exists
        saveDefaultConfig();

        // Managers
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.dbManager = new DBManager(this);

        // Database
        dbManager.init();

        // Menu GUI
        this.menu = new Menu(this, configManager, messages, dbManager);

        // Hooks
        this.vaultHook = new VaultHook(this);
        this.lootHook = new LootHook(this);
        this.mythicHook = new MythicHook(this);
        this.copsHook = new CopsHook(this);

        // Listeners
        registerListeners();

        // /challenges command
        Commands commands = new Commands(this, configManager, messages, dbManager, menu);
        PluginCommand cmd = getCommand("challenges");
        if (cmd != null) {
            cmd.setExecutor(commands);
            cmd.setTabCompleter(commands);
        } else {
            getLogger().severe("[DecemberChallenges] Command 'challenges' not defined in plugin.yml!");
        }

        getLogger().info("[DecemberChallenges] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            dbManager.close();
        }
        getLogger().info("[DecemberChallenges] Plugin disabled.");
    }

    private void registerListeners() {
        // PhatLoots listener (requires hook)
        if (lootHook != null && lootHook.isPhatLootsEnabled()) {
            Bukkit.getPluginManager().registerEvents(
                    new LootListener(this, dbManager, messages, menu),
                    this
            );
            getLogger().info("[DecemberChallenges] LootListener registered successfully!");
        }

        // MythicMobs listener (requires hook)
        if (mythicHook != null && mythicHook.isMythicMobsEnabled()) {
            Bukkit.getPluginManager().registerEvents(
                    new MythicListener(this, dbManager, messages, menu),
                    this
            );
            getLogger().info("[DecemberChallenges] MythicListener registered successfully!");
        }

        // Cops listener (requires hook)
        if (copsHook != null && copsHook.isCopsEnabled()) {
            Bukkit.getPluginManager().registerEvents(
                    new CopsListener(this, dbManager, messages, menu),
                    this
            );
            getLogger().info("[DecemberChallenges] CopsListener registered successfully!");
        }

        // Player kill listener (no hook needed - built into Bukkit)
        Bukkit.getPluginManager().registerEvents(
                new PlayerKillListener(this, dbManager, messages, menu),
                this
        );
        getLogger().info("[DecemberChallenges] PlayerKillListener registered successfully!");
    }

    // Getters (in case you want them later)

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public DBManager getDbManager() {
        return dbManager;
    }

    public Menu getMenu() {
        return menu;
    }

    public LootHook getLootHook() {
        return lootHook;
    }

    public MythicHook getMythicHook() {
        return mythicHook;
    }

    public CopsHook getCopsHook() {
        return copsHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }
}