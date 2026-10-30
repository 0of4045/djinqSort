package dev.djinq.djinqsort;

import java.util.Locale;

public enum SortMode {
    CATEGORY,
    MATERIAL,
    ALPHABETICAL,
    QUANTITY,
    DURABILITY,
    REVERSE;

    public static SortMode parse(String input) {
        try {
            return valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
