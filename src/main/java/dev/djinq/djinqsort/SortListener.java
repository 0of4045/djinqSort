package dev.djinq.djinqsort;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class SortListener implements Listener {
    private final DjinqSort plugin;
    private final Set<UUID> queued = new HashSet<>();

    public SortListener(DjinqSort plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !isEmpty(event.getCursor())) {
            return;
        }

        if (event.getClick() != ClickType.MIDDLE || !plugin.middleClickEnabled()
                || !player.hasPermission("djinqsort.middleclick")) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        boolean playerInventory = clicked.equals(player.getInventory()) && event.getSlot() >= 9 && event.getSlot() < 36;
        boolean container = clicked.equals(event.getView().getTopInventory()) && plugin.supportsContainer(clicked);
        if (!playerInventory && !container) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!queued.add(playerId)) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> sort(player, clicked, playerInventory, playerId));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onContainerShiftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking() || !plugin.containerShiftClickEnabled()
                || !player.hasPermission("djinqsort.containerclick")) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof InventoryHolder holder)) {
            return;
        }
        Inventory target = holder.getInventory();
        if (!plugin.supportsContainer(target)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!queued.add(playerId)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> sort(player, target, false, playerId));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.joinHintEnabled() || !event.getPlayer().hasPermission("djinqsort.containerclick")) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.hasPermission("djinqsort.containerclick")) {
                plugin.messages().sendWithoutPrefix(player, "hint");
            }
        }, plugin.joinHintDelayTicks());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        queued.remove(playerId);
        plugin.removePlayer(playerId);
    }

    private void sort(Player player, Inventory target, boolean playerInventory, UUID playerId) {
        queued.remove(playerId);
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        if (!playerInventory && !player.getOpenInventory().getTopInventory().equals(target)) {
            return;
        }
        SortMode mode = plugin.modeOf(playerId);
        boolean reverse = plugin.reverseOrderOf(playerId);
        if (playerInventory) {
            plugin.sorter().sortPlayerInventory(player, mode, reverse);
        } else {
            plugin.sorter().sortContainer(target, mode, reverse);
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
