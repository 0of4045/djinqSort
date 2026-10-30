package dev.djinq.djinqsort;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class TextRenderer {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("(?i)[&\\u00A7]#([0-9a-f]{6})");
    private static final Pattern LEGACY = Pattern.compile("(?i)[&\\u00A7]([0-9a-fk-or])");

    private TextRenderer() {
    }

    public static Component render(String input) {
        String text = replace(HEX, input, match -> "<reset><color:#" + match.group(1) + ">");
        return MINI_MESSAGE.deserialize(replace(LEGACY, text, match -> legacy(match.group(1).charAt(0))));
    }

    private static String replace(Pattern pattern, String input, Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(input == null ? "" : input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.apply(matcher)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String legacy(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
}
