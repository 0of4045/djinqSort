package dev.djinq.djinqsort;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class DjinqSort extends JavaPlugin {
    private final Map<UUID, SortMode> modes = new HashMap<>();
    private final Map<UUID, Boolean> reverseOrder = new HashMap<>();
    private final InventorySorter sorter = new InventorySorter(this);
    private MessageManager messages;
    private SortMode defaultMode;
    private boolean playerInventoryEnabled;
    private boolean middleClickEnabled;
    private boolean containerShiftClickEnabled;
    private boolean joinHintEnabled;
    private long joinHintDelayTicks;
    private boolean defaultReverseOrder;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ResourceMigration.mergeMissingDefaults(this, "config.yml");
        ResourceMigration.mergeMissingDefaults(this, "messages.yml");
        reloadPlugin();

        SortCommand command = new SortCommand(this);
        PluginCommand sort = Objects.requireNonNull(getCommand("sort"));
        sort.setExecutor(command);
        sort.setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new SortListener(this), this);
        getServer().getConsoleSender().sendMessage(TextRenderer.render(
                "<#B3E342>djinqSort <#CACACA>made by <#B3E342>djinq "
                        + "<#CACACA>| <#B3E342>djinq.dev <#CACACA>| <#B3E342>Loaded"));
    }

    public void reloadPlugin() {
        reloadConfig();
        messages = new MessageManager(this);
        defaultMode = SortMode.parse(getConfig().getString("default-mode", "CATEGORY"));
        if (defaultMode == null || defaultMode == SortMode.REVERSE) {
            defaultMode = SortMode.CATEGORY;
        }
        playerInventoryEnabled = getConfig().getBoolean("player-inventory.enabled", true);
        middleClickEnabled = getConfig().getBoolean("triggers.middle-click", false);
        containerShiftClickEnabled = getConfig().getBoolean("triggers.container-shift-right-click", true);
        joinHintEnabled = getConfig().getBoolean("join-hint.enabled", true);
        joinHintDelayTicks = Math.max(0L, getConfig().getLong("join-hint.delay-seconds", 120L)) * 20L;
        defaultReverseOrder = getConfig().getBoolean("reverse-order", false);
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

    public boolean middleClickEnabled() {
        return middleClickEnabled;
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
        if (inventory.getHolder() instanceof Chest || inventory.getHolder() instanceof DoubleChest) {
            return getConfig().getBoolean("containers.chest", true);
        }
        return switch (inventory.getType()) {
            case CHEST -> getConfig().getBoolean("containers.chest", true);
            case BARREL -> getConfig().getBoolean("containers.barrel", true);
            case SHULKER_BOX -> getConfig().getBoolean("containers.shulker-box", true);
            case HOPPER -> getConfig().getBoolean("containers.hopper", true);
            case DROPPER -> getConfig().getBoolean("containers.dropper", true);
            case DISPENSER -> getConfig().getBoolean("containers.dispenser", true);
            default -> false;
        };
    }

    public MessageManager messages() {
        return messages;
    }

    public InventorySorter sorter() {
        return sorter;
    }
}
