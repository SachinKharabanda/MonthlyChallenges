package Hooks;

import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MythicHook {

    private final DecemberChallenges plugin;
    private boolean mythicMobsEnabled = false;

    public MythicHook(DecemberChallenges plugin) {
        this.plugin = plugin;
        checkMythicMobs();
    }

    private void checkMythicMobs() {
        Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");

        if (mythic != null && mythic.isEnabled()) {
            mythicMobsEnabled = true;
            plugin.getLogger().info("[DecemberChallenges] Successfully hooked into MythicMobs.");
        } else {
            mythicMobsEnabled = false;
            plugin.getLogger().warning("[DecemberChallenges] MythicMobs not found or not enabled. Criminal challenge will be disabled.");
        }
    }

    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }

    public Plugin getMythicMobsPlugin() {
        if (!mythicMobsEnabled) return null;
        return Bukkit.getPluginManager().getPlugin("MythicMobs");
    }
}
