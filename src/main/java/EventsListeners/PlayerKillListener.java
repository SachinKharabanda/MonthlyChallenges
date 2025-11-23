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
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerKillListener implements Listener {

    private final DecemberChallenges plugin;
    private final DBManager dbManager;
    private final Messages messages;
    private final Menu menu;

    public PlayerKillListener(DecemberChallenges plugin,
                              DBManager dbManager,
                              Messages messages,
                              Menu menu) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.messages = messages;
        this.menu = menu;
    }

    /**
     * Track player kills for the challenge.
     * Only counts if a player kills another player (not suicide).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // No killer or killer is the victim (suicide)
        if (killer == null || killer.equals(victim)) {
            return;
        }

        // Get current count before incrementing
        int current = dbManager.getPlayerKillCount(killer.getUniqueId());

        // Already completed
        if (Challenges.isPlayerChallengeComplete(current)) {
            return;
        }

        // Increment player kill count
        dbManager.incrementPlayerKills(killer.getUniqueId());

        // Get new count after increment
        int newCount = dbManager.getPlayerKillCount(killer.getUniqueId());

        // Check if they just completed the challenge
        if (Challenges.isPlayerChallengeComplete(newCount)) {
            String completionMsg = messages.getChallengeMessage(
                    Challenges.PLAYER_CHALLENGE_ID,
                    "challenge-complete"
            );

            if (completionMsg != null && !completionMsg.isEmpty()) {
                killer.sendMessage(completionMsg);
            }

            menu.executeReward(killer, Challenges.PLAYER_CHALLENGE_ID);
            plugin.getLogger().info("[DecemberChallenges] " + killer.getName() + " has completed the player killing challenge!");
        }
    }
}