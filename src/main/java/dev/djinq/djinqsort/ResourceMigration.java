package dev.djinq.djinqsort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class ResourceMigration {
    private ResourceMigration() {
    }

    static void mergeMissingDefaults(JavaPlugin plugin, String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.isFile()) {
            plugin.saveResource(resourceName, false);
            return;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration defaults = loadDefaults(plugin, resourceName);
        boolean changed = false;

        for (String path : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(path) || current.contains(path)) {
                continue;
            }

            current.set(path, defaults.get(path));
            changed = true;
        }

        if (!changed) {
            return;
        }

        try {
            Files.copy(file.toPath(), backup(file).toPath(), StandardCopyOption.REPLACE_EXISTING);
            current.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not migrate " + resourceName + ".", exception);
        }
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin, String resourceName) {
        try (InputStream input = Objects.requireNonNull(plugin.getResource(resourceName))) {
            return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled " + resourceName + ".", exception);
        }
    }

    private static File backup(File file) {
        return new File(file.getParentFile(), file.getName() + ".bak");
    }
}
