package Hooks;

import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class LootHook {

    private final DecemberChallenges plugin;
    private boolean phatLootsEnabled = false;

    public LootHook(DecemberChallenges plugin) {
        this.plugin = plugin;
        checkPhatLoots();
    }

    /**
     * Check if PhatLoots is installed and enabled
     */
    private void checkPhatLoots() {
        Plugin phatLoots = Bukkit.getPluginManager().getPlugin("PhatLoots");

        if (phatLoots != null && phatLoots.isEnabled()) {
            phatLootsEnabled = true;
            plugin.getLogger().info("Successfully hooked into PhatLoots!");
        } else {
            phatLootsEnabled = false;
            plugin.getLogger().warning("PhatLoots not found! Chest looting challenge will not work.");
        }
    }

    /**
     * Check if PhatLoots is available
     */
    public boolean isPhatLootsEnabled() {
        return phatLootsEnabled;
    }

    /**
     * Get the PhatLoots plugin instance (if available)
     */
    public Plugin getPhatLootsPlugin() {
        if (phatLootsEnabled) {
            return Bukkit.getPluginManager().getPlugin("PhatLoots");
        }
        return null;
    }
}