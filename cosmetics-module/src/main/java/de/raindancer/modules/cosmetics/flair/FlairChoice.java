package de.raindancer.modules.cosmetics.flair;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** What one player picked, one option id per category. Written as {@code tag=star;kill=fireworks}. */
public record FlairChoice(Map<FlairCategory, String> picks) {

    public static final FlairChoice EMPTY = new FlairChoice(Map.of());

    public FlairChoice {
        picks = picks.isEmpty() ? Map.of() : java.util.Collections.unmodifiableMap(new EnumMap<>(picks));
    }

    public Optional<String> get(FlairCategory category) {
        return Optional.ofNullable(picks.get(category));
    }

    public FlairChoice with(FlairCategory category, String id) {
        Map<FlairCategory, String> next = new EnumMap<>(FlairCategory.class);
        next.putAll(picks);
        next.put(category, id);
        return new FlairChoice(next);
    }

    public FlairChoice without(FlairCategory category) {
        Map<FlairCategory, String> next = new EnumMap<>(FlairCategory.class);
        next.putAll(picks);
        next.remove(category);
        return new FlairChoice(next);
    }

    public boolean isEmpty() {
        return picks.isEmpty();
    }

    /** Null for nothing: nothing is kept for somebody who picked nothing. */
    public String format() {
        if (picks.isEmpty()) {
            return null;
        }
        StringBuilder written = new StringBuilder();
        picks.forEach((category, id) -> {
            if (written.length() > 0) {
                written.append(';');
            }
            written.append(category.id()).append('=').append(id);
        });
        return written.toString();
    }

    public static FlairChoice parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return EMPTY;
        }
        Map<FlairCategory, String> read = new EnumMap<>(FlairCategory.class);
        for (String part : stored.split(";")) {
            int at = part.indexOf('=');
            if (at <= 0) {
                continue;
            }
            String id = part.substring(at + 1).strip();
            Optional<FlairCategory> category = FlairCategory.of(part.substring(0, at));
            if (category.isPresent() && !id.isEmpty()) {
                read.put(category.get(), id);
            }
        }
        return new FlairChoice(read);
    }
}
