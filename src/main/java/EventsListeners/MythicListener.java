package EventsListeners;

import Challenges.Challenges;
import Challenges.Menu;
import ConfigManager.Messages;
import DBManager.DBManager;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MythicListener implements Listener {

    private final DecemberChallenges plugin;
    private final DBManager dbManager;
    private final Messages messages;
    private final Menu menu;

    public MythicListener(DecemberChallenges plugin,
                          DBManager dbManager,
                          Messages messages,
                          Menu menu) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.messages = messages;
        this.menu = menu;
    }

    /**
     * Any MythicMob killed by a player counts as a "criminal" kill.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        LivingEntity killer = event.getKiller();
        if (!(killer instanceof Player)) {
            return;
        }

        Player player = (Player) killer;

        int current = dbManager.getCriminalKillCount(player.getUniqueId());

        if (Challenges.isCriminalChallengeComplete(current)) {
            return;
        }

        dbManager.incrementCriminalKills(player.getUniqueId());

        int newCount = dbManager.getCriminalKillCount(player.getUniqueId());

        if (Challenges.isCriminalChallengeComplete(newCount)) {
            String completionMsg = messages.getChallengeMessage(
                    Challenges.CRIMINAL_CHALLENGE_ID,
                    "challenge-complete"
            );

            if (completionMsg != null && !completionMsg.isEmpty()) {
                player.sendMessage(completionMsg);
            }

            menu.executeReward(player, Challenges.CRIMINAL_CHALLENGE_ID);
            plugin.getLogger().info("[DecemberChallenges] " + player.getName() + " has completed the criminal killing challenge!");
        }
    }
}
