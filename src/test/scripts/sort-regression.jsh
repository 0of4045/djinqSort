// Run after mvn verify and dependency:build-classpath -Dmdep.outputFile=target/verification-classpath.txt.
// jshell --class-path "target/classes;<classpath file contents>" src/test/scripts/sort-regression.jsh
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import dev.djinq.djinqsort.InventorySorter;
import dev.djinq.djinqsort.SortMode;

// Small API test doubles: no server, runtime dependency or production class is added.
ItemStack stack(Material material, int count, String metadata) {
    return new ItemStack() {
        private int amount = count;

        public Material getType() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int value) {
            amount = value;
        }

        public int getMaxStackSize() {
            return 64;
        }

        public boolean isSimilar(ItemStack other) {
            return other != null && other.getType() == material && other.toString().equals(metadata);
        }

        public ItemStack clone() {
            return stack(material, amount, metadata);
        }

        public String toString() {
            return metadata;
        }
    };
}

void require(boolean condition, String message) {
    if (!condition) {
        throw new AssertionError(message);
    }
}

// Build entries directly: Material.isAir() requires Paper's live registry, unrelated to stack ordering.
List<Object> entries(ItemStack[] items) throws Exception {
    Constructor<?> constructor = Class.forName("dev.djinq.djinqsort.InventorySorter$Entry")
            .getDeclaredConstructor(ItemStack.class, int.class);
    constructor.setAccessible(true);
    List<Object> values = new ArrayList<>();
    for (int index = 0; index < items.length; index++) {
        if (items[index] != null) {
            values.add(constructor.newInstance(items[index], index));
        }
    }
    return values;
}

try {
    Method sortedSlots = InventorySorter.class.getDeclaredMethod(
            "sortedSlots", List.class, int.class, SortMode.class, boolean.class);
    sortedSlots.setAccessible(true);
    for (boolean reverse : List.of(false, true)) {
        ItemStack[] before = {
            stack(Material.STONE, 32, "plain"),
            stack(Material.STONE, 32, "plain"),
            stack(Material.DIRT, 48, "plain"),
            stack(Material.STONE, 20, "named"),
            stack(Material.STONE, 20, "named"),
            null
        };
        List<?> values = entries(before);
        ItemStack[] sorted = (ItemStack[]) sortedSlots.invoke(
                null, values, before.length, SortMode.QUANTITY, reverse);
        int[] quantities = Arrays.stream(sorted).filter(item -> item != null).mapToInt(ItemStack::getAmount).toArray();
        require(Arrays.equals(quantities, reverse ? new int[]{40, 48, 64} : new int[]{64, 48, 40}),
                "Final merged quantities or sorting direction changed");
        require(before[0].getAmount() == 32 && before[1].getAmount() == 32 && before[3].getAmount() == 20,
                "Calculation modified the inventory snapshot");
        require(sorted[reverse ? 0 : 2].toString().equals("named"), "Different metadata was merged");
        require(Arrays.stream(sorted).filter(item -> item != null).mapToInt(ItemStack::getAmount).sum() == 152,
                "Item quantities were not preserved");

        ItemStack[] partial = {
            stack(Material.STONE, 50, "plain"), stack(Material.STONE, 40, "plain"), stack(Material.DIRT, 48, "plain")
        };
        ItemStack[] result = (ItemStack[]) sortedSlots.invoke(
                null, entries(partial), partial.length, SortMode.QUANTITY, reverse);
        int[] amounts = Arrays.stream(result).mapToInt(ItemStack::getAmount).toArray();
        require(Arrays.equals(amounts, reverse ? new int[]{26, 48, 64} : new int[]{64, 48, 26}),
                "Partial stacks were remerged after quantity sorting");
    }

    // Source guards complement the quantity checks; interaction/protection integration needs Paper runtime testing.
    String listener = Files.readString(Path.of("src/main/java/dev/djinq/djinqsort/SortListener.java"));
    require(!listener.contains("getOpenInventory()"), "Container shortcut must not require an open inventory");
    require(listener.contains("event.useInteractedBlock() == Result.DENY"), "Final protection check was removed");
    require(listener.contains("!current.getInventory().equals(target)"), "Deferred target revalidation was removed");
    require(listener.contains("!block.getWorld().isChunkLoaded("), "Chunk revalidation was removed");
    require(listener.contains("!player.getWorld().equals(block.getWorld())"), "World revalidation was removed");
    require(listener.contains("!player.hasPermission(\"djinqsort.containerclick\")"), "Permission check was removed");
    require(!listener.contains("InventoryClickEvent"), "Sorting must not intercept inventory clicks");
    String main = Files.readString(Path.of("src/main/java/dev/djinq/djinqsort/DjinqSort.java"));
    require(!main.contains("middleClick"), "Removed shortcut settings must not be loaded");
    String config = Files.readString(Path.of("src/main/resources/config.yml"));
    require(!config.contains("middle-click"), "Removed shortcut must not appear in default configuration");
    String descriptor = Files.readString(Path.of("src/main/resources/plugin.yml"));
    require(!descriptor.contains("djinqsort.middleclick"), "Removed shortcut permission must not be registered");
    String sorter = Files.readString(Path.of("src/main/java/dev/djinq/djinqsort/InventorySorter.java"));
    require(sorter.contains("if (mode != SortMode.QUANTITY)"), "Quantity mode must not presort unmerged stacks");
    System.out.println("SORT_REGRESSION_CHECKS_PASSED: quantity directions, merging, metadata separation, snapshots and interaction source guards.");
} catch (Throwable exception) {
    exception.printStackTrace();
    System.exit(1);
}
/exit
