package EventsListeners;

import Challenges.Challenges;
import Challenges.Menu;
import ConfigManager.Messages;
import DBManager.DBManager;
import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CopsListener implements Listener {

    private final DecemberChallenges plugin;
    private final DBManager dbManager;
    private final Messages messages;
    private final Menu menu;

    public CopsListener(DecemberChallenges plugin,
                        DBManager dbManager,
                        Messages messages,
                        Menu menu) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.messages = messages;
        this.menu = menu;
    }

    /**
     * Any entity with the "cops_npc" metadata killed by a player counts as a cop kill.
     * This ties directly into the Cops plugin tagging of NPCs.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCopDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!entity.hasMetadata("cops_npc")) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        int current = dbManager.getCopsKillCount(killer.getUniqueId());

        if (Challenges.isCopsChallengeComplete(current)) {
            return;
        }

        dbManager.incrementCopsKills(killer.getUniqueId());

        int newCount = dbManager.getCopsKillCount(killer.getUniqueId());

        if (Challenges.isCopsChallengeComplete(newCount)) {
            String completionMsg = messages.getChallengeMessage(
                    Challenges.COPS_CHALLENGE_ID,
                    "challenge-complete"
            );

            if (completionMsg != null && !completionMsg.isEmpty()) {
                killer.sendMessage(completionMsg);
            }

            menu.executeReward(killer, Challenges.COPS_CHALLENGE_ID);
            plugin.getLogger().info("[DecemberChallenges] " + killer.getName() + " has completed the cops killing challenge!");
        }
    }
}
