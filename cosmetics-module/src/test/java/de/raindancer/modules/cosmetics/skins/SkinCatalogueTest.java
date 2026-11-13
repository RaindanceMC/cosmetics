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
                    "CC-BY-SA-4.0", "Zlib", "MPL-2.0", "Unlicense", "BSD-3-Clause", "LGPL-3.0-only", "GPL-3.0-only", "WTFPL"));
            assertThat(SkinCatalogue.CATEGORIES).as(skin.id()).contains(skin.category());
            skin.looks().keySet().forEach(item -> assertThat(SkinCatalogue.kindOf(item)).as(item).isPresent());
        }
    }

    @Test
    @DisplayName("armour is skinnable by piece: any material's helmet takes a helmet look")
    void armourKinds() {
        assertThat(SkinCatalogue.kindOf("diamond_chestplate")).contains("chestplate");
        assertThat(SkinCatalogue.kindOf("leather_helmet")).contains("helmet");
        assertThat(SkinCatalogue.kindOf("turtle_helmet")).contains("helmet");
        assertThat(SkinCatalogue.kindOf("chainmail_boots")).contains("boots");
        assertThat(SkinCatalogue.kindOf("copper_leggings")).contains("leggings");
        assertThat(SkinCatalogue.kindOf("elytra")).isEmpty();
        assertThat(SkinCatalogue.isArmour("netherite_boots")).isTrue();
        assertThat(SkinCatalogue.isArmour("netherite_sword")).isFalse();
    }

    @Test
    @DisplayName("an armour skin carries its worn look; it goes on armour only, never on a weapon")
    void equipment() {
        SkinCatalogue catalogue = SkinCatalogue.read(new ByteArrayInputStream("""
                [{"id": "frost", "title": "Frost", "pack": 0, "packTitle": "Hyades", "category": "Armour",
                  "author": "Hyades", "license": "own", "url": "", "equipment": "rainsskins:frost",
                  "looks": {"iron_helmet": "rainsskins:armour/frost/helmet",
                            "iron_chestplate": "rainsskins:armour/frost/chestplate"}},
                 {"id": "katana", "title": "Katana", "pack": 1, "packTitle": "K", "category": "Scythe",
                  "author": "A", "license": "MIT", "url": "", "looks": {"diamond_sword": "rainsskins:k/s"}}]
                """.getBytes(StandardCharsets.UTF_8)));

        SkinCatalogue.Skin frost = catalogue.byId("frost").orElseThrow();
        assertThat(frost.equipmentFor("diamond_chestplate")).contains("rainsskins:frost");
        assertThat(frost.lookFor("golden_helmet")).contains("rainsskins:armour/frost/helmet");
        assertThat(frost.equipmentFor("diamond_sword")).isEmpty();
        assertThat(catalogue.byId("katana").orElseThrow().equipmentFor("diamond_sword")).isEmpty();
        assertThat(catalogue.offeredFor("leather_chestplate", Set.of())).extracting(SkinCatalogue.Skin::id)
                .containsExactly("frost");
    }

    @Test
    @DisplayName("every bundled armour skin ships its equipment asset and every layer texture it names")
    void bundledArmour() throws Exception {
        List<SkinCatalogue.Skin> armour = SkinCatalogue.bundled().all().stream()
                .filter(skin -> skin.equipment() != null).toList();
        assertThat(armour).hasSizeGreaterThan(40);
        List<String> files = SkinPack.files();
        for (SkinCatalogue.Skin skin : armour) {
            String asset = skin.equipment().substring(skin.equipment().indexOf(':') + 1);
            String json = "assets/rainsskins/equipment/" + asset + ".json";
            assertThat(files).as(skin.id()).contains(json);
            com.google.gson.JsonObject layers;
            try (java.io.InputStream in = SkinCatalogueTest.class.getResourceAsStream(
                    "/de/raindancer/modules/cosmetics/pack/" + json)) {
                layers = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonObject("layers");
            }
            assertThat(layers.keySet()).as(skin.id()).isNotEmpty().isSubsetOf("humanoid", "humanoid_leggings");
            for (String layer : layers.keySet()) {
                for (com.google.gson.JsonElement entry : layers.getAsJsonArray(layer)) {
                    String texture = entry.getAsJsonObject().get("texture").getAsString();
                    assertThat(files).as(skin.id()).contains("assets/rainsskins/textures/entity/equipment/" + layer + "/"
                            + texture.substring(texture.indexOf(':') + 1) + ".png");
                }
            }
            assertThat(skin.looks().keySet()).as(skin.id()).isNotEmpty().allMatch(SkinCatalogue::isArmour);
        }
    }

    @Test
    @DisplayName("a skin is for creators when the catalogue says so, and for everybody when it says nothing")
    void creatorFlag() {
        SkinCatalogue read = SkinCatalogue.read(new ByteArrayInputStream("""
                [{"id":"a","title":"A","pack":0,"packTitle":"p","category":"Twitch","author":"x","license":"l","url":"u",
                  "creator":true,"looks":{"diamond_sword":"rainsskins:a/diamond_sword"}},
                 {"id":"b","title":"B","pack":0,"packTitle":"p","category":"Fantasy","author":"x","license":"l","url":"u",
                  "looks":{"diamond_sword":"rainsskins:b/diamond_sword"}}]
                """.getBytes(StandardCharsets.UTF_8)));

        assertThat(read.byId("a").orElseThrow().creator()).isTrue();
        assertThat(read.byId("a").orElseThrow().unlockable()).as("nobody unlocks it, nobody gets it by surprise").isFalse();
        assertThat(read.byId("b").orElseThrow().creator()).isFalse();
        assertThat(read.byId("b").orElseThrow().unlockable()).isTrue();
    }

    /** What creators have: existing, openly licensed skins from the packs — nothing modelled for the server. */
    private static final List<String> CREATOR_WEAPONS = List.of("shadowstone-tools", "custom-3d-swords-dragon-blade",
            "glacial-gear");
    private static final List<String> CREATOR_ARMOUR = List.of("armour-purple-diamond-armor-diamond",
            "armour-sakura-hotbar-netherite", "armour-valkyrian-netherite-reforged-v2-netherite",
            "armour-netherite-armor-netherite");

    @Test
    @DisplayName("exactly the chosen skins are creator-only, in the Twitch category, with their packs' credits untouched")
    void creatorSkins() {
        SkinCatalogue catalogue = SkinCatalogue.bundled();
        List<String> flagged = catalogue.all().stream().filter(SkinCatalogue.Skin::creator).map(SkinCatalogue.Skin::id).toList();

        assertThat(flagged).containsExactlyInAnyOrderElementsOf(
                java.util.stream.Stream.concat(CREATOR_WEAPONS.stream(), CREATOR_ARMOUR.stream()).toList());
        for (String id : flagged) {
            SkinCatalogue.Skin skin = catalogue.byId(id).orElseThrow();
            assertThat(skin.category()).as(id).isEqualTo("Twitch");
            assertThat(skin.unlockable()).as(id).isFalse();
            assertThat(skin.author()).as(id + " keeps its author").isNotBlank().isNotEqualTo("Raindancer118");
            assertThat(skin.license()).as(id + " keeps its licence").isNotEqualTo("Own work");
            assertThat(skin.url()).as(id).startsWith("https://modrinth.com/resourcepack/");
        }
        assertThat(CREATOR_WEAPONS).allMatch(id -> catalogue.byId(id).orElseThrow().equipment() == null);
        assertThat(CREATOR_ARMOUR).allMatch(id -> catalogue.byId(id).orElseThrow().equipment() != null);
        assertThat(CREATOR_WEAPONS.stream().anyMatch(id -> catalogue.byId(id).orElseThrow().dresses("iron_sword"))).isTrue();
        assertThat(CREATOR_WEAPONS.stream().anyMatch(id -> catalogue.byId(id).orElseThrow().dresses("iron_axe"))).isTrue();
        assertThat(CREATOR_WEAPONS.stream().anyMatch(id -> catalogue.byId(id).orElseThrow().dresses("iron_pickaxe"))).isTrue();
    }

    @Test
    @DisplayName("every other skin is unchanged: armour stays in Armour, nobody's weapon skin moved, all of them unlockable")
    void otherSkinsUnchanged() {
        List<SkinCatalogue.Skin> others = SkinCatalogue.bundled().all().stream().filter(skin -> !skin.creator()).toList();

        assertThat(others.stream().filter(skin -> skin.equipment() != null)).hasSize(51)
                .allMatch(skin -> skin.category().equals("Armour"));
        assertThat(SkinCatalogue.bundled().all().stream().filter(skin -> skin.equipment() != null)).hasSize(55);
        assertThat(others).allMatch(skin -> !skin.category().equals("Twitch")).allMatch(SkinCatalogue.Skin::unlockable);
        assertThat(others).hasSize(124 - 7);
    }

    @Test
    @DisplayName("nothing modelled for the server is in the pack: the self-made Twitch skins are gone, files and all")
    void nothingSelfMade() throws Exception {
        assertThat(SkinCatalogue.bundled().all()).noneMatch(skin -> skin.id().startsWith("twitch-"));
        assertThat(SkinPack.files()).noneMatch(file -> file.contains("twitch"));
        assertThat(SkinCatalogue.bundled().all()).noneMatch(skin -> skin.author().equals("Raindancer118"));
    }

    @Test
    @DisplayName("a creator armour skin ships its worn layers like every other armour skin")
    void creatorArmourShips() throws Exception {
        List<String> files = SkinPack.files();
        for (String id : CREATOR_ARMOUR) {
            String asset = SkinCatalogue.bundled().byId(id).orElseThrow().equipment().substring("rainsskins:".length());
            assertThat(files).as(id).contains("assets/rainsskins/equipment/" + asset + ".json");
            assertThat(files.stream().filter(f -> f.startsWith("assets/rainsskins/textures/entity/equipment/")
                    && f.endsWith("/" + asset + ".png"))).as(id).isNotEmpty();
        }
    }
}
