package de.raindancer.modules.cosmetics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoorLayoutTest {

    @Test
    @DisplayName("as many doors as places: every door gets one, nothing is left over")
    void fits() {
        DoorLayout<String> layout = DoorLayout.of(List.of("a", "b", "c"), 3);
        assertThat(layout.onPage()).containsExactly("a", "b", "c");
        assertThat(layout.more()).isEmpty();
    }

    @Test
    @DisplayName("one door too many: the last place becomes a way to the rest, which holds the last two")
    void overflow() {
        DoorLayout<String> layout = DoorLayout.of(List.of("a", "b", "c", "d"), 3);
        assertThat(layout.onPage()).containsExactly("a", "b");
        assertThat(layout.more()).containsExactly("c", "d");
    }

    @Test
    @DisplayName("no doors, or no places: nothing on the page and no way to more")
    void empty() {
        assertThat(DoorLayout.of(List.of(), 6).onPage()).isEmpty();
        assertThat(DoorLayout.of(List.of(), 6).more()).isEmpty();
        DoorLayout<String> nowhere = DoorLayout.of(List.of("a"), 0);
        assertThat(nowhere.onPage()).isEmpty();
        assertThat(nowhere.more()).containsExactly("a");
    }

    @Test
    @DisplayName("a row of doors sits in the middle, evenly, left to right")
    void rowColumns() {
        assertThat(DoorLayout.rowColumns(0)).isEmpty();
        assertThat(DoorLayout.rowColumns(1)).containsExactly(4);
        assertThat(DoorLayout.rowColumns(2)).containsExactly(3, 5);
        assertThat(DoorLayout.rowColumns(3)).containsExactly(2, 4, 6);
        assertThat(DoorLayout.rowColumns(4)).containsExactly(2, 3, 5, 6);
        assertThat(DoorLayout.rowColumns(5)).containsExactly(2, 3, 4, 5, 6);
        assertThat(DoorLayout.rowColumns(9)).containsExactly(2, 3, 4, 5, 6);
    }
}
