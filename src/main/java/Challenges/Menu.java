package Challenges;

import ConfigManager.ConfigManager;
import ConfigManager.Messages;
import DBManager.DBManager;
import Hooks.VaultHook;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.sachin.decemberChallenges.DecemberChallenges;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

public class Menu {

    private final DecemberChallenges plugin;
    private final ConfigManager configManager;
    private final Messages messages;
    private final DBManager dbManager;

    // Cache for menu configuration
    private int menuSize;
    private boolean closeOnInteract;
    private Map<Integer, ChallengeMenuItem> menuItems;

    public Menu(DecemberChallenges plugin,
                ConfigManager configManager,
                Messages messages,
                DBManager dbManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = messages;
        this.dbManager = dbManager;
        loadMenuConfig();
    }

    /**
     * Load menu configuration from config.yml
     */
    public void loadMenuConfig() {
        menuItems = new HashMap<>();

        menuSize = configManager.getConfig().getInt("menu-size", 27);
        closeOnInteract = configManager.getConfig().getBoolean("close-menu-upon-interact", true);

        ConfigurationSection itemsSection = configManager.getConfig().getConfigurationSection("menu-items");
        if (itemsSection == null) {
            plugin.getLogger().warning("No menu-items found in config.yml!");
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            ChallengeMenuItem menuItem = new ChallengeMenuItem();

            // Get challenge ID (check both challenge-id and game-id for typo compatibility)
            menuItem.challengeId = itemSection.getInt("challenge-id", -1);
            if (menuItem.challengeId == -1) {
                menuItem.challengeId = itemSection.getInt("game-id", -1);
            }

            menuItem.material = Material.getMaterial(itemSection.getString("material", "STONE"));
            menuItem.textureValue = itemSection.getString("value", "");
            menuItem.position = itemSection.getInt("position", 0);
            menuItem.displayName = itemSection.getString("display-name", "&eChallenge");
            menuItem.displayLore = itemSection.getStringList("display-lore");
            menuItem.rewardCommand = itemSection.getString("reward-console-command", "");

            if (menuItem.challengeId != -1) {
                menuItems.put(menuItem.challengeId, menuItem);
            }
        }

        plugin.getLogger().info("Loaded " + menuItems.size() + " challenge menu items");
    }

    /**
     * Reload menu configuration
     */
    public void reload() {
        loadMenuConfig();
    }

    /**
     * Open the challenges GUI for a player
     */
    public void openGUI(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text(color("&8Challenges")))
                .rows(menuSize / 9)
                .disableAllInteractions() // Prevent all item movements
                .create();

        // Add all challenge items
        for (Map.Entry<Integer, ChallengeMenuItem> entry : menuItems.entrySet()) {
            int challengeId = entry.getKey();
            ChallengeMenuItem menuItem = entry.getValue();

            ItemStack item = createChallengeItem(player, challengeId, menuItem);
            GuiItem guiItem = new GuiItem(item, event -> {
                event.setCancelled(true); // Ensure click is cancelled
                handleChallengeClick(player, challengeId);
            });

            gui.setItem(menuItem.position-1, guiItem);
        }

        // Fill empty slots with glass panes
        //gui.getFiller().fillBorder(new GuiItem(createFillerItem()));

        gui.open(player);
    }

    /**
     * Create an ItemStack for a challenge with current progress
     */
    private ItemStack createChallengeItem(Player player, int challengeId, ChallengeMenuItem menuItem) {
        ItemStack item;

        // Create player head with custom texture
        if (menuItem.material == Material.PLAYER_HEAD && !menuItem.textureValue.isEmpty()) {
            item = createCustomHead(menuItem.textureValue);
        } else {
            item = new ItemStack(menuItem.material != null ? menuItem.material : Material.STONE);
        }

        // Get current progress
        int current = getCurrentProgress(player, challengeId);
        int max = getMaxProgress(challengeId);
        int remaining = Math.max(0, max - current);
        boolean isComplete = isComplete(player, challengeId);

        // Set display name
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(menuItem.displayName));

            // Set lore with placeholders
            List<String> lore = new ArrayList<>();
            for (String line : menuItem.displayLore) {
                String processed = replacePlaceholders(line, player, challengeId, current, max, remaining);
                lore.add(color(processed));
            }

            // Add completion status
           /* lore.add("");
            if (isComplete) {
                lore.add(color("&a&l✔ COMPLETED"));
            } else {
                lore.add(color("&e&l⚠ IN PROGRESS"));
            }*/

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a custom player head with Base64 texture using Paper's PlayerProfile API
     */
    private ItemStack createCustomHead(String base64Texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                // Use Paper's PlayerProfile API (available in Paper 1.21.4)
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();

                // Decode base64 to get the texture URL
                String decoded = new String(Base64.getDecoder().decode(base64Texture));

                // Extract URL from JSON (format: {"textures":{"SKIN":{"url":"http://..."}}}
                String urlString = decoded.substring(decoded.indexOf("http://"), decoded.lastIndexOf("\""));

                textures.setSkin(new URL(urlString));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);

            } catch (MalformedURLException | IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to set custom head texture: " + e.getMessage());
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Create filler item (black stained glass pane)
     */
    /*private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }*/

    /**
     * Handle when a player clicks on a challenge item
     */
    private void handleChallengeClick(Player player, int challengeId) {
        boolean isComplete = isComplete(player, challengeId);

        // Special handling for balance challenge
        if (challengeId == Challenges.BALANCE_CHALLENGE_ID) {
            if (isComplete) {
                // Already completed - send completed message
                String message = messages.getChallengeMessage(challengeId, "challenge-completed");
                player.sendMessage(message);
            } else {
                // Check current balance
                double balance = getPlayerBalance(player);

                if (balance >= Challenges.BALANCE_GOAL) {
                    // Player has enough money! Mark as complete and give reward
                    dbManager.setBalanceChallengeComplete(player.getUniqueId(), true);

                    // Send completion message
                    String message = messages.getChallengeMessage(challengeId, "challenge-complete");
                    message = replacePlaceholders(message, player, challengeId, 0, 0, 0);
                    player.sendMessage(message);

                    // Execute reward command
                    executeReward(player, challengeId);
                } else {
                    // Not enough money yet - send remaining message
                    String message = messages.getChallengeMessage(challengeId, "challenge-remaining");
                    message = replacePlaceholders(message, player, challengeId, 0, 0, 0);
                    player.sendMessage(message);
                }
            }
        } else {
            // Normal challenge handling
            if (isComplete) {
                // Already completed - send completed message
                String message = messages.getChallengeMessage(challengeId, "challenge-completed");
                player.sendMessage(message);
            } else {
                // Not complete yet - send remaining message
                int current = getCurrentProgress(player, challengeId);
                int max = getMaxProgress(challengeId);
                int remaining = Math.max(0, max - current);

                String message = messages.getChallengeMessage(challengeId, "challenge-remaining");
                message = replacePlaceholders(message, player, challengeId, current, max, remaining);
                player.sendMessage(message);
            }
        }

        // Close GUI if configured
        if (closeOnInteract) {
            player.closeInventory();
        }
    }

    /**
     * Replace placeholders in a string
     */
    private String replacePlaceholders(String text, Player player, int challengeId,
                                       int current, int max, int remaining) {
        text = text.replace("{prefix}", messages.getPrefix());

        // Challenge-specific placeholders
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                text = text.replace("{chests_looted}", String.valueOf(current));
                text = text.replace("{chests_to_be_looted}", String.valueOf(max));
                text = text.replace("{chests_remaining}", String.valueOf(remaining));
                break;

            case Challenges.CRIMINAL_CHALLENGE_ID:
                text = text.replace("{criminals_killed}", String.valueOf(current));
                text = text.replace("{criminals_to_be_killed}", String.valueOf(max));
                text = text.replace("{criminals_remaining}", String.valueOf(remaining));
                break;

            case Challenges.COPS_CHALLENGE_ID:
                text = text.replace("{cops_killed}", String.valueOf(current));
                text = text.replace("{cops_to_be_killed}", String.valueOf(max));
                text = text.replace("{cops_remaining}", String.valueOf(remaining));
                break;

            case Challenges.PLAYER_CHALLENGE_ID:
                text = text.replace("{players_killed}", String.valueOf(current));
                text = text.replace("{players_to_be_killed}", String.valueOf(max));
                text = text.replace("{kills_remaining}", String.valueOf(remaining));
                break;

            case Challenges.BALANCE_CHALLENGE_ID:
                double balance = getPlayerBalance(player);
                DecimalFormat df = new DecimalFormat("#,###");
                text = text.replace("{balance_formatted}", df.format(balance));
                text = text.replace("{money_remaining}", df.format(Math.max(0, Challenges.BALANCE_GOAL - balance)));
                break;
        }

        text = text.replace("{player}", player.getName());

        return text;
    }

    /**
     * Get current progress for a challenge
     */
    private int getCurrentProgress(Player player, int challengeId) {
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                return dbManager.getChestLootCount(player.getUniqueId());
            case Challenges.CRIMINAL_CHALLENGE_ID:
                return dbManager.getCriminalKillCount(player.getUniqueId());
            case Challenges.COPS_CHALLENGE_ID:
                return dbManager.getCopsKillCount(player.getUniqueId());
            case Challenges.PLAYER_CHALLENGE_ID:
                return dbManager.getPlayerKillCount(player.getUniqueId());
            case Challenges.BALANCE_CHALLENGE_ID:
                return dbManager.isBalanceChallengeComplete(player.getUniqueId()) ? 1 : 0;
            default:
                return 0;
        }
    }

    /**
     * Get maximum progress for a challenge
     */
    private int getMaxProgress(int challengeId) {
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                return Challenges.CHEST_GOAL;
            case Challenges.CRIMINAL_CHALLENGE_ID:
                return Challenges.CRIMINAL_GOAL;
            case Challenges.COPS_CHALLENGE_ID:
                return Challenges.COPS_GOAL;
            case Challenges.PLAYER_CHALLENGE_ID:
                return Challenges.PLAYER_GOAL;
            case Challenges.BALANCE_CHALLENGE_ID:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Check if a challenge is complete
     */
    private boolean isComplete(Player player, int challengeId) {
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                return Challenges.isChestChallengeComplete(dbManager.getChestLootCount(player.getUniqueId()));
            case Challenges.CRIMINAL_CHALLENGE_ID:
                return Challenges.isCriminalChallengeComplete(dbManager.getCriminalKillCount(player.getUniqueId()));
            case Challenges.COPS_CHALLENGE_ID:
                return Challenges.isCopsChallengeComplete(dbManager.getCopsKillCount(player.getUniqueId()));
            case Challenges.PLAYER_CHALLENGE_ID:
                return Challenges.isPlayerChallengeComplete(dbManager.getPlayerKillCount(player.getUniqueId()));
            case Challenges.BALANCE_CHALLENGE_ID:
                return dbManager.isBalanceChallengeComplete(player.getUniqueId());
            default:
                return false;
        }
    }

    /**
     * Get player's balance from Vault (if available)
     */
    private double getPlayerBalance(Player player) {
        VaultHook vaultHook = plugin.getVaultHook();
        if (vaultHook != null && vaultHook.isEconomyEnabled()) {
            return vaultHook.getBalance(player);
        }
        return 0.0;
    }

    /**
     * Execute reward command for a challenge
     */
    public void executeReward(Player player, int challengeId) {
        ChallengeMenuItem menuItem = menuItems.get(challengeId);
        if (menuItem == null || menuItem.rewardCommand.isEmpty()) {
            return;
        }

        String command = menuItem.rewardCommand.replace("{player}", player.getName());

        // Execute command from console
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("Executed reward command for " + player.getName() +
                    ": " + command);
        });
    }

    /**
     * Translate color codes
     */
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Inner class to hold menu item data
     */
    private static class ChallengeMenuItem {
        int challengeId;
        Material material;
        String textureValue;
        int position;
        String displayName;
        List<String> displayLore;
        String rewardCommand;
    }
}