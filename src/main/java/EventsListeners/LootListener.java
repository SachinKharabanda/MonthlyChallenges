package EventsListeners;

import Challenges.Challenges;
import Challenges.Menu;
import ConfigManager.Messages;
import DBManager.DBManager;
import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

// PhatLoots import
import com.codisimus.plugins.phatloots.events.PlayerLootEvent;

public class LootListener implements Listener {

    private final DecemberChallenges plugin;
    private final DBManager dbManager;
    private final Messages messages;
    private final Menu menu;

    public LootListener(DecemberChallenges plugin, DBManager dbManager, Messages messages, Menu menu) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.messages = messages;
        this.menu = menu;
    }

    /**
     * Listen for PhatLoots loot events
     * This fires when a player successfully loots a PhatLoot chest
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLoot(PlayerLootEvent event) {
        Player player = event.getLooter();

        if (player == null) {
            return;
        }

        // Get current count before incrementing
        int currentCount = dbManager.getChestLootCount(player.getUniqueId());

        // Check if already completed
        if (Challenges.isChestChallengeComplete(currentCount)) {
            // Already completed, don't increment
            return;
        }

        // Increment the chest loot count
        dbManager.incrementChestLoot(player.getUniqueId());

        // Get new count after increment
        int newCount = dbManager.getChestLootCount(player.getUniqueId());

        // Check if they just completed the challenge
        if (Challenges.isChestChallengeComplete(newCount)) {
            // Send completion message
            String completionMsg = messages.getChallengeMessage(
                    Challenges.CHEST_CHALLENGE_ID,
                    "challenge-complete"
            );

            if (completionMsg != null && !completionMsg.isEmpty()) {
                player.sendMessage(completionMsg);
            }

            // Execute reward command
            menu.executeReward(player, Challenges.CHEST_CHALLENGE_ID);

            plugin.getLogger().info(player.getName() + " has completed the chest looting challenge!");
        }
    }
}