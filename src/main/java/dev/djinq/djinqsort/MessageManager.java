package dev.djinq.djinqsort;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageManager {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private String prefix;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean reload() {
        try {
            YamlConfiguration nextMessages = new YamlConfiguration();
            nextMessages.load(new File(plugin.getDataFolder(), "messages.yml"));
            String nextPrefix = nextMessages.getString("prefix");
            if (nextPrefix == null || nextMessages.getConfigurationSection("messages") == null) {
                throw new InvalidConfigurationException("messages.yml is missing its prefix or messages section.");
            }

            messages = nextMessages;
            prefix = nextPrefix;
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not load messages.yml: " + exception.getMessage());
            return false;
        }
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> values) {
        sender.sendMessage(TextRenderer.render(prefix + message(key), values));
    }

    public void sendWithoutPrefix(CommandSender sender, String key) {
        sender.sendMessage(TextRenderer.render(message(key), Map.of()));
    }

    private String message(String key) {
        return messages.getString("messages." + key, "<#E34242>Missing message.");
    }
}
