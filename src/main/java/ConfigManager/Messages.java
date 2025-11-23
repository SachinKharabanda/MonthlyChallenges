package ConfigManager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Handles messages.yml loading & access.
 */
public class Messages {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;

    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        createFile();
        loadValues();
    }

    private void createFile() {
        file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    private void loadValues() {
        this.prefix = color(config.getString("prefix", "&7[&cChallenges&7]&f "));
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        loadValues();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSystemMessage(String key) {
        String path = "system-messages." + key;
        String raw = config.getString(path);
        if (raw == null) {
            return color(prefix + "&cMissing system message for key: " + key);
        }
        raw = raw.replace("{prefix}", prefix);
        return color(raw);
    }

    /**
     * Fetch a challenge-specific message.
     *
     * @param id   challenge id (1-5)
     * @param type message type, e.g. "challenge-completed", "challenge-remaining", "challenge-complete"
     */
    public String getChallengeMessage(int id, String type) {
        String path = "challenge-messages." + id + "." + type;
        String raw = config.getString(path);
        if (raw == null) {
            return color(prefix + "&cMissing challenge message for id " + id + " and type " + type);
        }
        raw = raw.replace("{prefix}", prefix);
        return color(raw);
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
