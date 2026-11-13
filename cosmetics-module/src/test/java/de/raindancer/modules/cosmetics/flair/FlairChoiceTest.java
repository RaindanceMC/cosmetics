package de.raindancer.modules.cosmetics.flair;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlairChoiceTest {

    @Test
    @DisplayName("what is picked is written in a fixed order and read back the same")
    void roundTrip() {
        FlairChoice choice = FlairChoice.EMPTY.with(FlairCategory.KILL, "fireworks").with(FlairCategory.TAG, "star")
                .with(FlairCategory.COLOUR, "gold");

        assertThat(choice.format()).isEqualTo("tag=star;colour=gold;kill=fireworks");
        assertThat(FlairChoice.parse(choice.format())).isEqualTo(choice);
    }

    @Test
    @DisplayName("nothing picked is nothing kept")
    void empty() {
        assertThat(FlairChoice.EMPTY.format()).isNull();
        assertThat(FlairChoice.parse(null)).isEqualTo(FlairChoice.EMPTY);
        assertThat(FlairChoice.parse("")).isEqualTo(FlairChoice.EMPTY);
        assertThat(FlairChoice.EMPTY.with(FlairCategory.JOIN, "royal").without(FlairCategory.JOIN).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("garbage in the data costs only the garbage")
    void forgiving() {
        FlairChoice read = FlairChoice.parse("tag=star;;nonsense;wings=big;join=;death=ghost;=x");

        assertThat(read.get(FlairCategory.TAG)).contains("star");
        assertThat(read.get(FlairCategory.DEATH)).contains("ghost");
        assertThat(read.get(FlairCategory.JOIN)).isEmpty();
        assertThat(read.format()).isEqualTo("tag=star;death=ghost");
    }

    @Test
    @DisplayName("categories are found by the word a command uses")
    void categoriesByName() {
        assertThat(FlairCategory.of("Kill")).contains(FlairCategory.KILL);
        assertThat(FlairCategory.of("colour")).contains(FlairCategory.COLOUR);
        assertThat(FlairCategory.of("color")).contains(FlairCategory.COLOUR);
        assertThat(FlairCategory.of("wings")).isEmpty();
    }
}
