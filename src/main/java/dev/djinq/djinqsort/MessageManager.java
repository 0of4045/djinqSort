package dev.djinq.djinqsort;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public final class MessageManager {
    private final FileConfiguration messages;
    private final String prefix;

    public MessageManager(DjinqSort plugin) {
        messages = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), "messages.yml"));
        prefix = messages.getString("prefix", "&8[&bSorting&8] &7");
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> values) {
        sender.sendMessage(TextRenderer.render(prefix + messages.getString("messages." + key, "&cMissing message: " + key), values));
    }

    public void sendWithoutPrefix(CommandSender sender, String key) {
        sender.sendMessage(TextRenderer.render(messages.getString("messages." + key, "&cMissing message: " + key), Map.of()));
    }
}
