package dev.djinq.djinqsort;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public final class DjinqSort extends JavaPlugin {
    private final Map<UUID, SortMode> modes = new HashMap<>();
    private final Map<UUID, Boolean> reverseOrder = new HashMap<>();
    private final InventorySorter sorter = new InventorySorter(this);
    private MessageManager messages;
    private YamlConfiguration activeConfig;
    private SortMode defaultMode;
    private boolean playerInventoryEnabled;
    private boolean containerShiftClickEnabled;
    private boolean joinHintEnabled;
    private long joinHintDelayTicks;
    private boolean defaultReverseOrder;

    @Override
    public void onEnable() {
        ResourceMigration.mergeMissingDefaults(this, "config.yml");
        ResourceMigration.mergeMissingDefaults(this, "messages.yml");
        messages = new MessageManager(this);
        if (!reloadPlugin()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SortCommand command = new SortCommand(this);
        PluginCommand sort = Objects.requireNonNull(getCommand("sort"));
        sort.setExecutor(command);
        sort.setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new SortListener(this), this);
        getServer().getConsoleSender().sendMessage(TextRenderer.render(
                "<#B3E342>djinqSort <#CACACA>made by <#B3E342>djinq "
                        + "<#CACACA>| <#B3E342>djinq.dev <#CACACA>| <#B3E342>Loaded"));
    }

    public boolean reloadPlugin() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(new File(getDataFolder(), "config.yml"));
            SortMode nextDefaultMode = SortMode.parse(config.getString("default-mode", "CATEGORY"));
            if (nextDefaultMode == null || nextDefaultMode == SortMode.REVERSE) {
                nextDefaultMode = SortMode.CATEGORY;
            }
            boolean nextPlayerInventoryEnabled = config.getBoolean("player-inventory.enabled", true);
            boolean nextContainerShiftClickEnabled = config.getBoolean("triggers.container-shift-right-click", true);
            boolean nextJoinHintEnabled = config.getBoolean("join-hint.enabled", true);
            long nextJoinHintDelayTicks = Math.max(0L, config.getLong("join-hint.delay-seconds", 120L)) * 20L;
            boolean nextDefaultReverseOrder = config.getBoolean("reverse-order", false);
            if (!messages.reload()) {
                return false;
            }

            activeConfig = config;
            defaultMode = nextDefaultMode;
            playerInventoryEnabled = nextPlayerInventoryEnabled;
            containerShiftClickEnabled = nextContainerShiftClickEnabled;
            joinHintEnabled = nextJoinHintEnabled;
            joinHintDelayTicks = nextJoinHintDelayTicks;
            defaultReverseOrder = nextDefaultReverseOrder;
            return true;
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
            getLogger().severe("Could not reload djinqSort configuration: " + exception.getMessage());
            return false;
        }
    }

    public SortMode modeOf(UUID playerId) {
        return modes.getOrDefault(playerId, defaultMode);
    }

    public void setMode(UUID playerId, SortMode mode) {
        modes.put(playerId, mode);
    }

    public boolean reverseOrderOf(UUID playerId) {
        return reverseOrder.getOrDefault(playerId, defaultReverseOrder);
    }

    public boolean toggleReverseOrder(UUID playerId) {
        boolean reverse = !reverseOrderOf(playerId);
        reverseOrder.put(playerId, reverse);
        return reverse;
    }

    public void removePlayer(UUID playerId) {
        modes.remove(playerId);
        reverseOrder.remove(playerId);
    }

    public boolean playerInventoryEnabled() {
        return playerInventoryEnabled;
    }

    public boolean containerShiftClickEnabled() {
        return containerShiftClickEnabled;
    }

    public boolean joinHintEnabled() {
        return joinHintEnabled;
    }

    public long joinHintDelayTicks() {
        return joinHintDelayTicks;
    }

    public boolean supportsContainer(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest chest) {
            return isPhysicalContainer(chest.getLeftSide(), inventory)
                    && isPhysicalContainer(chest.getRightSide(), inventory)
                    && activeConfig.getBoolean("containers.chest", true);
        }
        if (!isPhysicalContainer(holder, inventory)) {
            return false;
        }
        if (holder instanceof Chest) {
            return activeConfig.getBoolean("containers.chest", true);
        }
        return switch (inventory.getType()) {
            case CHEST -> activeConfig.getBoolean("containers.chest", true);
            case BARREL -> activeConfig.getBoolean("containers.barrel", true);
            case SHULKER_BOX -> activeConfig.getBoolean("containers.shulker-box", true);
            case HOPPER -> activeConfig.getBoolean("containers.hopper", true);
            case DROPPER -> activeConfig.getBoolean("containers.dropper", true);
            case DISPENSER -> activeConfig.getBoolean("containers.dispenser", true);
            default -> false;
        };
    }

    public MessageManager messages() {
        return messages;
    }

    public InventorySorter sorter() {
        return sorter;
    }

    private static boolean isPhysicalContainer(InventoryHolder holder, Inventory inventory) {
        if (!(holder instanceof Container container) || !container.isPlaced()) {
            return false;
        }
        if (!container.getWorld().isChunkLoaded(container.getX() >> 4, container.getZ() >> 4)) {
            return false;
        }
        return container.getBlock().getState() instanceof Container current
                && current.getInventory().equals(inventory);
    }
}
