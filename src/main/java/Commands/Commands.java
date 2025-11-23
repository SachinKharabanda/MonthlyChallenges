package Commands;

import Challenges.Challenges;
import Challenges.Menu;
import ConfigManager.ConfigManager;
import ConfigManager.Messages;
import DBManager.DBManager;
import me.sachin.decemberChallenges.DecemberChallenges;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {

    private final DecemberChallenges plugin;
    private final ConfigManager configManager;
    private final Messages messages;
    private final DBManager dbManager;
    private final Menu menu;

    public Commands(DecemberChallenges plugin,
                    ConfigManager configManager,
                    Messages messages,
                    DBManager dbManager,
                    Menu menu) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = messages;
        this.dbManager = dbManager;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        // /challenges - Open GUI
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            menu.openGUI(player);
            return true;
        }

        // /challenges reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("challenges.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }

            configManager.reloadConfig();
            messages.reload();
            menu.reload();
            sender.sendMessage(messages.getSystemMessage("reload"));
            return true;
        }

        // /challenges reset <player> <id>
        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("challenges.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " reset <player> <id>");
                return true;
            }

            return handleReset(sender, args[1], args[2]);
        }

        // /challenges boost <player> <id> <amount>
        if (args[0].equalsIgnoreCase("boost")) {
            if (!sender.hasPermission("challenges.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " boost <player> <id> <amount>");
                return true;
            }

            return handleBoost(sender, args[1], args[2], args[3]);
        }

        // /challenges reduce <player> <id> <amount>
        if (args[0].equalsIgnoreCase("reduce")) {
            if (!sender.hasPermission("challenges.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " reduce <player> <id> <amount>");
                return true;
            }

            return handleReduce(sender, args[1], args[2], args[3]);
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [reload|reset|boost|reduce]");
        return true;
    }

    /**
     * Handle /challenges reset <player> <id>
     */
    private boolean handleReset(CommandSender sender, String targetName, String idStr) {
        // Get target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or not online.");
            return true;
        }

        // Parse challenge ID
        int challengeId;
        try {
            challengeId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be a number (1-5).");
            return true;
        }

        // Validate challenge ID
        if (challengeId < 1 || challengeId > 5) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be between 1 and 5.");
            return true;
        }

        // Reset the appropriate challenge
        switch (challengeId) {
            case Challenges.CHEST_CHALLENGE_ID:
                dbManager.resetChestLoot(target.getUniqueId());
                break;
            case Challenges.CRIMINAL_CHALLENGE_ID:
                dbManager.resetCriminalKills(target.getUniqueId());
                break;
            case Challenges.COPS_CHALLENGE_ID:
                dbManager.resetCopsKills(target.getUniqueId());
                break;
            case Challenges.PLAYER_CHALLENGE_ID:
                dbManager.resetPlayerKills(target.getUniqueId());
                break;
            case Challenges.BALANCE_CHALLENGE_ID:
                dbManager.setBalanceChallengeComplete(target.getUniqueId(), false);
                break;
        }

        // Send success message
        String message = messages.getSystemMessage("reset")
                .replace("{target}", target.getName())
                .replace("{targets}", target.getName())
                .replace("{id}", String.valueOf(challengeId));
        sender.sendMessage(message);

        return true;
    }

    /**
     * Handle /challenges boost <player> <id> <amount>
     */
    private boolean handleBoost(CommandSender sender, String targetName, String idStr, String amountStr) {
        // Get target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or not online.");
            return true;
        }

        // Parse challenge ID
        int challengeId;
        try {
            challengeId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be a number (1-5).");
            return true;
        }

        // Validate challenge ID
        if (challengeId < 1 || challengeId > 5) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be between 1 and 5.");
            return true;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount. Must be a number.");
            return true;
        }

        // Validate amount
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return true;
        }

        // Special handling for balance challenge (it's a boolean)
        if (challengeId == Challenges.BALANCE_CHALLENGE_ID) {
            dbManager.setBalanceChallengeComplete(target.getUniqueId(), true);
        } else {
            // Boost the appropriate challenge
            dbManager.boostProgress(target.getUniqueId(), challengeId, amount);
        }

        // Send success message
        String message = messages.getSystemMessage("boost")
                .replace("{target}", target.getName())
                .replace("{targets}", target.getName())
                .replace("{boost}", String.valueOf(amount))
                .replace("{id}", String.valueOf(challengeId));
        sender.sendMessage(message);

        return true;
    }

    /**
     * Handle /challenges reduce <player> <id> <amount>
     */
    private boolean handleReduce(CommandSender sender, String targetName, String idStr, String amountStr) {
        // Get target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found or not online.");
            return true;
        }

        // Parse challenge ID
        int challengeId;
        try {
            challengeId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be a number (1-5).");
            return true;
        }

        // Validate challenge ID
        if (challengeId < 1 || challengeId > 5) {
            sender.sendMessage(ChatColor.RED + "Invalid challenge ID. Must be between 1 and 5.");
            return true;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount. Must be a number.");
            return true;
        }

        // Validate amount
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Amount must be greater than 0.");
            return true;
        }

        // Special handling for balance challenge (it's a boolean)
        if (challengeId == Challenges.BALANCE_CHALLENGE_ID) {
            dbManager.setBalanceChallengeComplete(target.getUniqueId(), false);
        } else {
            // Reduce the appropriate challenge
            dbManager.reduceProgress(target.getUniqueId(), challengeId, amount);
        }

        // Send success message
        String message = messages.getSystemMessage("reduced")
                .replace("{target}", target.getName())
                .replace("{targets}", target.getName())
                .replace("{reduce}", String.valueOf(amount))
                .replace("{id}", String.valueOf(challengeId));
        sender.sendMessage(message);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        List<String> completions = new ArrayList<>();

        // First argument - subcommands
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            subcommands.add("reload");

            if (sender.hasPermission("challenges.admin")) {
                subcommands.add("reset");
                subcommands.add("boost");
                subcommands.add("reduce");
            }

            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Second argument - player names (for reset, boost, reduce)
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reset") ||
                    args[0].equalsIgnoreCase("boost") ||
                    args[0].equalsIgnoreCase("reduce")) {

                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Third argument - challenge IDs
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reset") ||
                    args[0].equalsIgnoreCase("boost") ||
                    args[0].equalsIgnoreCase("reduce")) {

                return Arrays.asList("1", "2", "3", "4", "5").stream()
                        .filter(id -> id.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }

        // Fourth argument - amount (for boost and reduce)
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("boost") || args[0].equalsIgnoreCase("reduce")) {
                return Arrays.asList("1", "10", "50", "100", "500", "1000");
            }
        }

        return Collections.emptyList();
    }
}