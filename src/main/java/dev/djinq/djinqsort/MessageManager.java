package dev.djinq.djinqsort;

import java.util.Map;
import net.kyori.adventure.text.Component;
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
        sender.sendMessage(render(prefix + messages.getString("messages." + key, "&cMissing message: " + key), values));
    }

    public void sendWithoutPrefix(CommandSender sender, String key) {
        sender.sendMessage(render(messages.getString("messages." + key, "&cMissing message: " + key), Map.of()));
    }

    private static Component render(String text, Map<String, String> values) {
        String rendered = text == null ? "" : text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return TextRenderer.render(rendered);
    }
}
