package dev.djinq.djinqsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public final class InventorySorter {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private final DjinqSort plugin;
    private final Set<Inventory> sorting = Collections.newSetFromMap(new IdentityHashMap<>());

    public InventorySorter(DjinqSort plugin) {
        this.plugin = plugin;
    }

    public Result sortPlayerInventory(Player player, SortMode mode, boolean reverse) {
        if (!plugin.playerInventoryEnabled()) {
            return Result.UNSUPPORTED;
        }
        return sort(player.getInventory(), 9, 36, mode, reverse);
    }

    public Result sortContainer(Inventory inventory, SortMode mode, boolean reverse) {
        if (!plugin.supportsContainer(inventory)) {
            return Result.UNSUPPORTED;
        }
        return sort(inventory, 0, inventory.getSize(), mode, reverse);
    }

    private Result sort(Inventory inventory, int firstSlot, int lastSlot, SortMode mode, boolean reverse) {
        if (!sorting.add(inventory)) {
            return Result.BUSY;
        }
        try {
            ItemStack[] before = snapshot(inventory, firstSlot, lastSlot);
            List<Entry> entries = entries(before);
            if (entries.isEmpty()) {
                return Result.EMPTY;
            }

            if (mode != SortMode.QUANTITY) {
                entries.sort(comparator(mode, reverse));
            }
            ItemStack[] sorted = sortedSlots(entries, before.length, mode, reverse);
            if (sorted == null || !matches(inventory, firstSlot, before)) {
                return Result.FAILED;
            }

            try {
                for (int index = 0; index < sorted.length; index++) {
                    inventory.setItem(firstSlot + index, sorted[index]);
                }
            } catch (RuntimeException exception) {
                restore(inventory, firstSlot, before);
                plugin.getLogger().warning("Could not apply inventory sort: " + exception.getMessage());
                return Result.FAILED;
            }
            return Result.SORTED;
        } finally {
            sorting.remove(inventory);
        }
    }

    private static ItemStack[] snapshot(Inventory inventory, int firstSlot, int lastSlot) {
        ItemStack[] snapshot = new ItemStack[lastSlot - firstSlot];
        for (int index = 0; index < snapshot.length; index++) {
            ItemStack item = inventory.getItem(firstSlot + index);
            snapshot[index] = isEmpty(item) ? null : item.clone();
        }
        return snapshot;
    }

    private static List<Entry> entries(ItemStack[] snapshot) {
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < snapshot.length; index++) {
            if (!isEmpty(snapshot[index])) {
                entries.add(new Entry(snapshot[index], index));
            }
        }
        return entries;
    }

    private static Comparator<Entry> comparator(SortMode mode, boolean reverse) {
        Comparator<Entry> comparator = switch (mode) {
            case CATEGORY -> Comparator.comparingInt((Entry entry) -> category(entry.item().getType()));
            case ALPHABETICAL -> Comparator.comparing(
                    (Entry entry) -> itemName(entry.item()), String.CASE_INSENSITIVE_ORDER);
            case QUANTITY -> Comparator.comparingInt((Entry entry) -> entry.item().getAmount()).reversed();
            case DURABILITY -> Comparator.comparingInt((Entry entry) -> remainingDurability(entry.item())).reversed();
            case MATERIAL, REVERSE -> Comparator.comparing(entry -> materialName(entry.item()));
        };
        if (mode != SortMode.MATERIAL && mode != SortMode.REVERSE) {
            comparator = comparator.thenComparing(entry -> materialName(entry.item()));
        }
        if (reverse) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparingInt(Entry::index);
    }

    private static ItemStack[] sortedSlots(List<Entry> entries, int size, SortMode mode, boolean reverse) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Entry entry : entries) {
            addStack(stacks, entry.item().clone());
        }
        if (stacks.size() > size) {
            return null;
        }
        if (mode == SortMode.QUANTITY) {
            Comparator<ItemStack> quantities = Comparator.comparingInt(ItemStack::getAmount).reversed();
            quantities = quantities.thenComparing(InventorySorter::materialName);
            if (reverse) {
                quantities = quantities.reversed();
            }
            stacks.sort(quantities);
        }
        ItemStack[] sorted = new ItemStack[size];
        for (int index = 0; index < stacks.size(); index++) {
            sorted[index] = stacks.get(index);
        }
        return sorted;
    }

    private static void addStack(List<ItemStack> stacks, ItemStack incoming) {
        for (ItemStack current : stacks) {
            if (!current.isSimilar(incoming) || current.getAmount() >= current.getMaxStackSize()) {
                continue;
            }
            int moved = Math.min(current.getMaxStackSize() - current.getAmount(), incoming.getAmount());
            current.setAmount(current.getAmount() + moved);
            incoming.setAmount(incoming.getAmount() - moved);
            if (incoming.getAmount() == 0) {
                return;
            }
        }
        stacks.add(incoming);
    }

    private static boolean matches(Inventory inventory, int firstSlot, ItemStack[] expected) {
        for (int index = 0; index < expected.length; index++) {
            ItemStack current = inventory.getItem(firstSlot + index);
            ItemStack original = expected[index];
            if (isEmpty(current) != isEmpty(original)) {
                return false;
            }
            if (!isEmpty(current) && (current.getAmount() != original.getAmount() || !current.isSimilar(original))) {
                return false;
            }
        }
        return true;
    }

    private static void restore(Inventory inventory, int firstSlot, ItemStack[] snapshot) {
        for (int index = 0; index < snapshot.length; index++) {
            inventory.setItem(firstSlot + index, snapshot[index]);
        }
    }

    private static int category(Material material) {
        if (material.isBlock()) {
            return 0;
        }
        if (material.isEdible()) {
            return 1;
        }
        if (material.getMaxDurability() > 0) {
            return 2;
        }
        if (material.name().endsWith("_SPAWN_EGG")) {
            return 3;
        }
        return 4;
    }

    private static String materialName(ItemStack item) {
        return item.getType().getKey().asString();
    }

    private static String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() ? PLAIN_TEXT.serialize(meta.displayName()) : materialName(item);
    }

    private static int remainingDurability(ItemStack item) {
        if (item.getType().getMaxDurability() <= 0) {
            return -1;
        }
        ItemMeta meta = item.getItemMeta();
        int damage = meta instanceof Damageable damageable ? damageable.getDamage() : 0;
        return item.getType().getMaxDurability() - damage;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private record Entry(ItemStack item, int index) {
    }

    public enum Result {
        SORTED,
        EMPTY,
        UNSUPPORTED,
        BUSY,
        FAILED
    }
}
