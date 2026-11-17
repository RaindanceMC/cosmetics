package de.raindancer.modules.cosmetics.screen;

import de.raindancer.core.ui.dialog.DialogAnswers;
import de.raindancer.core.ui.dialog.DialogPage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CosmeticsDialogTest {

    private final List<DialogPage> shown = new ArrayList<>();
    private final Player viewer = mock(Player.class);

    @BeforeEach
    void standIn() {
        org.mockito.Mockito.when(viewer.isOnline()).thenReturn(true);
        DialogPage.runtime(new DialogPage.Runtime() {
            @Override
            public void show(Player player, DialogPage page) {
                shown.add(page);
            }

            @Override
            public void onPlayerThread(Player player, Runnable task) {
                task.run();
            }
        });
    }

    @AfterEach
    void game() {
        DialogPage.runtime(null);
    }

    private static String plain(net.kyori.adventure.text.Component text) {
        return PlainTextComponentSerializer.plainText().serialize(text);
    }

    private static List<String> labels(DialogPage page) {
        return page.pressable().stream().map(button -> plain(button.label())).toList();
    }

    @Test
    @DisplayName("every cosmetic is a button in the order given, under the Cosmetics title, with what is worn above")
    void everyCosmeticAButton() {
        AtomicInteger pressed = new AtomicInteger();
        DialogPage page = CosmeticsDialog.page(UUID.randomUUID(), "Ada", "<gray>Particle: <white>Flame", List.of(
                CosmeticsDialog.Entry.open("Name style", "Plain right now.", pressed::incrementAndGet),
                CosmeticsDialog.Entry.open("Particles", "Flame.", pressed::incrementAndGet),
                CosmeticsDialog.Entry.open("Chat colour", "Yours: plain.", pressed::incrementAndGet)));

        assertThat(plain(page.title())).contains("Cosmetics");
        assertThat(page.plainText()).contains("Ada").contains("Particle: Flame");
        assertThat(labels(page)).containsExactly("Name style", "Particles", "Chat colour");

        page.button("Particles").press(viewer, DialogAnswers.none());
        assertThat(pressed).hasValue(1);
    }

    @Test
    @DisplayName("a locked cosmetic is drawn greyed with its reason and pressing it does nothing but say why")
    void lockedSaysWhy() {
        AtomicInteger pressed = new AtomicInteger();
        DialogPage page = CosmeticsDialog.page(UUID.randomUUID(), "Ada", "", List.of(
                CosmeticsDialog.Entry.locked("Weapon skins ☕", "New looks for your weapons.",
                        "For supporters. See /support", pressed::incrementAndGet)));

        DialogPage.Button skins = page.button("Weapon skins");
        assertThat(skins.allowed()).isFalse();
        skins.press(viewer, DialogAnswers.none());
        assertThat(pressed).hasValue(0);
        assertThat(shown).last().extracting(DialogPage::notice).isEqualTo("For supporters. See /support");
    }

    @Test
    @DisplayName("another plugin's door becomes a button: its name, its lines as the tooltip, its lock as the reason")
    void doorAsButton() {
        AtomicInteger opened = new AtomicInteger();
        CosmeticsDialog.Entry open = CosmeticsDialog.Entry.door("Rod skins",
                List.of("A look for the rod in your hand.", "Hold a rod and click."), Optional.empty(),
                opened::incrementAndGet);
        assertThat(open.allowed()).isTrue();
        assertThat(open.tooltip()).contains("A look for the rod in your hand.").contains("Hold a rod and click.");
        open.press().run();
        assertThat(opened).hasValue(1);

        CosmeticsDialog.Entry locked = CosmeticsDialog.Entry.door("Pet looks", List.of(), Optional.of("A Prime perk"),
                opened::incrementAndGet);
        assertThat(locked.allowed()).isFalse();
        assertThat(locked.refusal()).isEqualTo("A Prime perk");
    }

    @Test
    @DisplayName("clearing asks first: Yes clears and goes back, No only goes back")
    void clearAsks() {
        AtomicInteger cleared = new AtomicInteger();
        AtomicInteger back = new AtomicInteger();
        DialogPage ask = CosmeticsDialog.confirmClear("Clear your cosmetics?", List.of("Your name goes back to plain."),
                cleared::incrementAndGet, back::incrementAndGet);

        assertThat(ask.isConfirmation()).isTrue();
        assertThat(ask.plainText()).contains("Your name goes back to plain.");
        ask.pressable().get(1).press(viewer, DialogAnswers.none());
        assertThat(cleared).hasValue(0);
        assertThat(back).hasValue(1);
        ask.pressable().get(0).press(viewer, DialogAnswers.none());
        assertThat(cleared).hasValue(1);
        assertThat(back).hasValue(2);
    }

    @Test
    @DisplayName("Unlocks is there only while the server sells something: switched on and with a price set")
    void unlocksOnlyWhenSold() {
        assertThat(CosmeticsDialog.showsUnlocks(true, true)).isTrue();
        assertThat(CosmeticsDialog.showsUnlocks(false, true)).as("selling switched off").isFalse();
        assertThat(CosmeticsDialog.showsUnlocks(true, false)).as("no price set").isFalse();
    }
}
