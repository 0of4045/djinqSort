package dev.djinq.djinqsort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SortCommand implements CommandExecutor, TabCompleter {
    private final DjinqSort plugin;

    public SortCommand(DjinqSort plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        if (!sender.hasPermission("djinqsort.command")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            return sortPlayerInventory(sender);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "mode" -> setMode(sender, args);
            default -> usage(sender);
        };
    }

    private static List<String> complete(String input, List<String> values) {
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                matches.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return matches;
    }

    private record SortState(SortMode mode, boolean reverse, String label) {
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean canChangeMode = sender.hasPermission("djinqsort.command") && sender.hasPermission("djinqsort.mode");
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (canChangeMode) {
                options.add("mode");
            }
            if (sender.hasPermission("djinqsort.reload")) {
                options.add("reload");
            }
            return complete(args[0], options);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode") && canChangeMode) {
            List<String> modes = new ArrayList<>();
            for (SortMode mode : SortMode.values()) {
                modes.add(mode.name());
            }
            return complete(args[1], modes);
        }
        return List.of();
    }

    private boolean sortPlayerInventory(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        SortState state = state(player);
        sendResult(sender, plugin.sorter().sortPlayerInventory(player, state.mode(), state.reverse()), state.label());
        return true;
    }

    private boolean setMode(CommandSender sender, String[] args) {
        if (!sender.hasPermission("djinqsort.mode")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return true;
        }
        if (args.length != 2) {
            return usage(sender);
        }
        SortMode mode = SortMode.parse(args[1]);
        if (mode == null) {
            plugin.messages().send(sender, "invalid-mode", Map.of("mode", args[1]));
            return true;
        }
        if (mode == SortMode.REVERSE) {
            plugin.toggleReverseOrder(player.getUniqueId());
            plugin.messages().send(sender, "mode-set", Map.of("mode", state(player).label()));
            return true;
        }
        plugin.setMode(player.getUniqueId(), mode);
        plugin.messages().send(sender, "mode-set", Map.of("mode", mode.name()));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("djinqsort.reload")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        plugin.messages().send(sender, plugin.reloadPlugin() ? "reload-success" : "reload-failed");
        return true;
    }

    private boolean usage(CommandSender sender) {
        plugin.messages().send(sender, "usage");
        return true;
    }

    private void sendResult(CommandSender sender, InventorySorter.Result result, String mode) {
        switch (result) {
            case SORTED -> plugin.messages().send(sender, "sorted", Map.of("mode", mode));
            case EMPTY -> plugin.messages().send(sender, "empty");
            case UNSUPPORTED -> plugin.messages().send(sender, "player-inventory-disabled");
            case BUSY -> plugin.messages().send(sender, "sorting-busy");
            case FAILED -> plugin.messages().send(sender, "sorting-failed");
        }
    }

    private SortState state(Player player) {
        SortMode mode = plugin.modeOf(player.getUniqueId());
        boolean reverse = plugin.reverseOrderOf(player.getUniqueId());
        return new SortState(mode, reverse, reverse ? mode.name() + " (REVERSE)" : mode.name());
    }

}
