package Hooks;

import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class CopsHook {

    private final DecemberChallenges plugin;
    private boolean copsEnabled = false;

    public CopsHook(DecemberChallenges plugin) {
        this.plugin = plugin;
        checkCops();
    }

    private void checkCops() {
        Plugin cops = Bukkit.getPluginManager().getPlugin("Cops");

        if (cops != null && cops.isEnabled()) {
            copsEnabled = true;
            plugin.getLogger().info("[DecemberChallenges] Successfully hooked into Cops.");
        } else {
            copsEnabled = false;
            plugin.getLogger().warning("[DecemberChallenges] Cops not found or not enabled. Cops challenge will be disabled.");
        }
    }

    public boolean isCopsEnabled() {
        return copsEnabled;
    }

    public Plugin getCopsPlugin() {
        if (!copsEnabled) return null;
        return Bukkit.getPluginManager().getPlugin("Cops");
    }
}
