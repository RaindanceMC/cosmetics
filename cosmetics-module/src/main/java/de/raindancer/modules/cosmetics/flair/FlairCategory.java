package de.raindancer.modules.cosmetics.flair;

import java.util.Locale;
import java.util.Optional;

/** The things a player may choose on the flair page, one option each. */
public enum FlairCategory {
    /** The symbol in front of the name. */
    TAG("tag", "Tag"),
    /** The tag's colour. */
    COLOUR("colour", "Tag colour"),
    /** The lines said when they join and leave. */
    JOIN("join", "Join and leave"),
    /** What plays where they win a fight. */
    KILL("kill", "Kill effect"),
    /** What plays where they die. */
    DEATH("death", "Death effect");

    private final String id;
    private final String title;

    FlairCategory(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public static Optional<FlairCategory> of(String word) {
        if (word == null) {
            return Optional.empty();
        }
        String wanted = word.trim().toLowerCase(Locale.ROOT);
        if (wanted.equals("color")) {
            wanted = "colour";
        }
        for (FlairCategory category : values()) {
            if (category.id.equals(wanted)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
