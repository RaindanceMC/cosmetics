package de.raindancer.modules.cosmetics.flair;

import de.raindancer.core.ui.identity.JoinLines;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Optional;
import java.util.function.Predicate;

/** What of somebody's flair is worn, given the nodes they hold. The one place that asks. */
public final class FlairRule {

    /** The tag's colour when none is picked. */
    public static final int PRIME_GOLD = 0xFFC93C;

    public boolean may(FlairOption option, Predicate<String> has) {
        return has.test(option.node());
    }

    /** {@code stored} with everything dropped that they may not wear — an option gone, a perk that ran out. */
    public FlairChoice effective(FlairChoice stored, Predicate<String> has) {
        FlairChoice kept = FlairChoice.EMPTY;
        for (var pick : stored.picks().entrySet()) {
            Optional<FlairOption> option = FlairCatalogue.find(pick.getKey(), pick.getValue());
            if (option.isPresent() && may(option.get(), has)) {
                kept = kept.with(pick.getKey(), option.get().id());
            }
        }
        return kept;
    }

    /** The symbol in its colour; none without a symbol. */
    public Optional<Component> tag(FlairChoice effective) {
        Optional<FlairOption> symbol = effective.get(FlairCategory.TAG)
                .flatMap(id -> FlairCatalogue.find(FlairCategory.TAG, id));
        if (symbol.isEmpty()) {
            return Optional.empty();
        }
        int rgb = effective.get(FlairCategory.COLOUR).flatMap(id -> FlairCatalogue.find(FlairCategory.COLOUR, id))
                .map(FlairOption::rgb).orElse(PRIME_GOLD);
        return Optional.of(Component.text(symbol.get().text(), TextColor.color(rgb)));
    }

    public Optional<String> line(FlairChoice effective, JoinLines.Kind kind) {
        return effective.get(FlairCategory.JOIN).flatMap(id -> FlairCatalogue.find(FlairCategory.JOIN, id))
                .map(option -> kind == JoinLines.Kind.JOIN ? option.text() : option.text2());
    }

    /** The teleport sound they picked: its {@code text} is the departure key and {@code text2} the arrival's. */
    public Optional<FlairOption> teleport(FlairChoice effective) {
        return effective.get(FlairCategory.TELEPORT).flatMap(id -> FlairCatalogue.find(FlairCategory.TELEPORT, id));
    }

    public Optional<FlairEffect> effect(FlairChoice effective, FlairCategory category) {
        return effective.get(category).flatMap(id -> FlairCatalogue.find(category, id)).map(FlairOption::effect);
    }
}
