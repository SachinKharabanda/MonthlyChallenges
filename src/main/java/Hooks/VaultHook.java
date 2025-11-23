package Hooks;

import me.sachin.decemberChallenges.DecemberChallenges;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final DecemberChallenges plugin;
    private Economy economy = null;

    public VaultHook(DecemberChallenges plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Setup Vault economy integration
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("[DecemberChallenges] Vault not found! Balance challenge will not work.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("[DecemberChallenges] Vault economy provider not found!");
            return;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("[DecemberChallenges] Hooked into Vault economy successfully!");
    }

    /**
     * Check if Vault economy is available
     */
    public boolean isEconomyEnabled() {
        return economy != null;
    }

    /**
     * Get a player's balance
     */
    public double getBalance(Player player) {
        if (economy == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * Get the Economy instance
     */
    public Economy getEconomy() {
        return economy;
    }
}