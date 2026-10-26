package de.raindancer.modules.cosmetics.skins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkinCatalogueTest {

    private static final String JSON = """
            [
             {"id":"katana","title":"Simple Katana","pack":6,"packTitle":"Simple Katana Swords","category":"Katana & Blades",
              "author":"Someone","license":"MIT","url":"https://modrinth.com/resourcepack/simple-katana-swords",
              "looks":{"diamond_sword":"rainsskins:katana/diamond_sword","iron_sword":"rainsskins:katana/iron_sword"}},
             {"id":"scythe","title":"Cool Scythe","pack":12,"packTitle":"Cool Scythe","category":"Scythe",
              "author":"X","license":"MIT","url":"u","looks":{"netherite_sword":"rainsskins:scythe/netherite_sword"}},
             {"id":"bow3d","title":"3D Bow","pack":24,"packTitle":"3D Bow","category":"3D Classic",
              "author":"Y","license":"CC-BY-4.0","url":"u","looks":{"bow":"rainsskins:bow3d/bow"}},
             {"id":"picks","title":"Cute Pickaxes","pack":51,"packTitle":"Cute Pickaxes","category":"Cute & Fun",
              "author":"Z","license":"CC-BY-4.0","url":"u","looks":{"golden_pickaxe":"rainsskins:picks/golden_pickaxe"}}
            ]
            """;

    private static SkinCatalogue catalogue() {
        return SkinCatalogue.read(new ByteArrayInputStream(JSON.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("an item's kind is its weapon type, whatever its material; anything else has none")
    void kinds() {
        assertThat(SkinCatalogue.kindOf("diamond_sword")).contains("sword");
        assertThat(SkinCatalogue.kindOf("netherite_pickaxe")).contains("pickaxe");
        assertThat(SkinCatalogue.kindOf("copper_spear")).contains("spear");
        assertThat(SkinCatalogue.kindOf("golden_axe")).contains("axe");
        assertThat(SkinCatalogue.kindOf("bow")).contains("bow");
        assertThat(SkinCatalogue.kindOf("crossbow")).contains("crossbow");
        assertThat(SkinCatalogue.kindOf("mace")).contains("mace");
        assertThat(SkinCatalogue.kindOf("trident")).contains("trident");
        assertThat(SkinCatalogue.kindOf("diamond_hoe")).isEmpty();
        assertThat(SkinCatalogue.kindOf("bowl")).isEmpty();
        assertThat(SkinCatalogue.kindOf("stick")).isEmpty();
    }

    @Test
    @DisplayName("a weapon is only offered skins made for its own type, and only switched-on ones")
    void offered() {
        SkinCatalogue catalogue = catalogue();

        assertThat(catalogue.offeredFor("iron_sword", Set.of())).extracting(SkinCatalogue.Skin::id)
                .containsExactly("katana", "scythe");
        assertThat(catalogue.offeredFor("iron_sword", Set.of("scythe"))).extracting(SkinCatalogue.Skin::id)
                .containsExactly("katana");
        assertThat(catalogue.offeredFor("bow", Set.of())).extracting(SkinCatalogue.Skin::id).containsExactly("bow3d");
        assertThat(catalogue.offeredFor("crossbow", Set.of())).isEmpty();
        assertThat(catalogue.offeredFor("stick", Set.of())).isEmpty();
    }

    @Test
    @DisplayName("the look for an item is its own tier's, or another tier of the same type")
    void lookFor() {
        SkinCatalogue catalogue = catalogue();
        SkinCatalogue.Skin katana = catalogue.byId("katana").orElseThrow();

        assertThat(katana.lookFor("diamond_sword")).contains("rainsskins:katana/diamond_sword");
        assertThat(katana.lookFor("wooden_sword")).as("no wooden one: another sword's")
                .contains("rainsskins:katana/diamond_sword");
        assertThat(katana.lookFor("diamond_axe")).isEmpty();
        assertThat(catalogue.byId("picks").orElseThrow().lookFor("netherite_pickaxe"))
                .contains("rainsskins:picks/golden_pickaxe");
    }

    @Test
    @DisplayName("categories keep a fixed order, each listed once, empty ones left out")
    void categories() {
        assertThat(SkinCatalogue.categoriesOf(catalogue().offeredFor("diamond_sword", Set.of())))
                .containsExactly("Katana & Blades", "Scythe");
        assertThat(SkinCatalogue.categoriesOf(catalogue().all())).containsExactly("Katana & Blades", "Scythe", "3D Classic", "Cute & Fun");
    }

    @Test
    @DisplayName("the catalogue bundled with the module reads, and every skin has a look, a credit and a category")
    void bundled() {
        SkinCatalogue catalogue = SkinCatalogue.bundled();

        assertThat(catalogue.all()).hasSizeGreaterThan(50);
        for (SkinCatalogue.Skin skin : catalogue.all()) {
            assertThat(skin.looks()).as(skin.id()).isNotEmpty();
            assertThat(skin.author()).as(skin.id()).isNotBlank();
            assertThat(skin.license()).as(skin.id()).isIn(List.of("MIT", "CC-BY-4.0", "CC0-1.0", "Apache-2.0",
                    "CC-BY-SA-4.0", "Zlib", "MPL-2.0"));
            assertThat(SkinCatalogue.CATEGORIES).as(skin.id()).contains(skin.category());
            skin.looks().keySet().forEach(item -> assertThat(SkinCatalogue.kindOf(item)).as(item).isPresent());
        }
    }
}
