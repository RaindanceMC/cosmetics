package de.raindancer.modules.cosmetics.model;

import java.util.List;

/**
 * Which of the other plugins' doors fit on the cosmetics page. When there are more than places, the last place
 * becomes a way to the rest, so no plugin's cosmetic silently falls off the page.
 */
public record DoorLayout<T>(List<T> onPage, List<T> more) {

    public static <T> DoorLayout<T> of(List<T> doors, int places) {
        if (doors.size() <= places) {
            return new DoorLayout<>(List.copyOf(doors), List.of());
        }
        int shown = Math.max(0, places - 1);
        return new DoorLayout<>(List.copyOf(doors.subList(0, shown)), List.copyOf(doors.subList(shown, doors.size())));
    }

    /** Columns for {@code count} doors in one row, centred and symmetric; at most five, columns 2 to 6. */
    public static List<Integer> rowColumns(int count) {
        return switch (Math.min(Math.max(count, 0), 5)) {
            case 0 -> List.of();
            case 1 -> List.of(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(2, 4, 6);
            case 4 -> List.of(2, 3, 5, 6);
            default -> List.of(2, 3, 4, 5, 6);
        };
    }
}
