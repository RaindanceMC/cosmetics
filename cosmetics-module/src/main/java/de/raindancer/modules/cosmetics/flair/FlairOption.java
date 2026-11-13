package de.raindancer.modules.cosmetics.flair;

/**
 * One thing to pick in a {@link FlairCategory}, behind {@code node}.
 *
 * @param text   a tag's symbol; a join line's template for joining ({@code <player>} is the name)
 * @param text2  a join line's template for leaving
 * @param rgb    a tag colour
 * @param effect a kill or death effect
 */
public record FlairOption(FlairCategory category, String id, String title, String node, String text, String text2,
                          Integer rgb, FlairEffect effect) {

    static FlairOption tag(String id, String title, String node, String glyph) {
        return new FlairOption(FlairCategory.TAG, id, title, node, glyph, null, null, null);
    }

    static FlairOption colour(String id, String title, String node, int rgb) {
        return new FlairOption(FlairCategory.COLOUR, id, title, node, null, null, rgb, null);
    }

    static FlairOption lines(String id, String title, String node, String join, String quit) {
        return new FlairOption(FlairCategory.JOIN, id, title, node, join, quit, null, null);
    }

    static FlairOption effect(FlairCategory category, String id, String title, String node, FlairEffect effect) {
        return new FlairOption(category, id, title, node, null, null, null, effect);
    }
}
