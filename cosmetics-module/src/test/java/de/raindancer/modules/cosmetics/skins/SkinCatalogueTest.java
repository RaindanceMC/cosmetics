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
            // Third-party art carries an open licence; the server's own (the creators' Twitch set) says so instead.
            List<String> licences = skin.creator() && skin.author().equals("Raindancer118")
                    ? List.of("Own work") : List.of("MIT", "CC-BY-4.0", "CC0-1.0", "Apache-2.0",
                    "CC-BY-SA-4.0", "Zlib", "MPL-2.0", "Unlicense", "BSD-3-Clause", "LGPL-3.0-only", "GPL-3.0-only", "WTFPL");
            assertThat(skin.license()).as(skin.id()).isIn(licences);
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

    @Test
    @DisplayName("the shipped Twitch set: purple weapon skins for creators — swords, an axe and a pickaxe, no armour")
    void shippedTwitchSet() {
        List<SkinCatalogue.Skin> twitch = SkinCatalogue.bundled().all().stream().filter(SkinCatalogue.Skin::creator).toList();

        assertThat(twitch).extracting(SkinCatalogue.Skin::title)
                .contains("Glitchblade", "Glitchblade RGB", "Pixel Chopper", "Pixel Pick", "Glitchplate", "Glitchplate Night");
        for (SkinCatalogue.Skin skin : twitch) {
            assertThat(skin.id()).startsWith("twitch-");
            assertThat(skin.category()).isEqualTo("Twitch");
            boolean armour = skin.equipment() != null;
            assertThat(skin.looks().keySet()).as(skin.id())
                    .allMatch(item -> SkinCatalogue.isArmour(item) == armour, "all armour or all weapons");
            assertThat(skin.looks().values()).as(skin.id()).allMatch(look -> look.startsWith("rainsskins:" + skin.id() + "/"));
            assertThat(skin.license()).as("our own work: no third party to credit").isEqualTo("Own work");
        }
        assertThat(twitch.stream().anyMatch(skin -> skin.dresses("iron_sword"))).isTrue();
        assertThat(twitch.stream().anyMatch(skin -> skin.dresses("netherite_axe"))).isTrue();
        assertThat(twitch.stream().anyMatch(skin -> skin.dresses("golden_pickaxe"))).isTrue();
        assertThat(SkinCatalogue.bundled().all().stream().filter(skin -> skin.id().startsWith("twitch-")))
                .as("everything called twitch is a creator skin").allMatch(SkinCatalogue.Skin::creator);
        assertThat(SkinCatalogue.CATEGORIES).contains("Twitch");
    }

    @Test
    @DisplayName("the Twitch armour: two full sets for creators, each worn as its own equipment asset, any material takes the look")
    void twitchArmour() throws Exception {
        List<SkinCatalogue.Skin> armour = SkinCatalogue.bundled().all().stream()
                .filter(skin -> skin.creator() && skin.equipment() != null).toList();

        assertThat(armour).extracting(SkinCatalogue.Skin::title).containsExactlyInAnyOrder("Glitchplate", "Glitchplate Night");
        List<String> files = SkinPack.files();
        for (SkinCatalogue.Skin skin : armour) {
            assertThat(skin.equipment()).isEqualTo("rainsskins:" + skin.id());
            for (String item : List.of("netherite_helmet", "iron_chestplate", "leather_leggings", "copper_boots", "turtle_helmet")) {
                assertThat(skin.dresses(item)).as(skin.id() + " " + item).isTrue();
                assertThat(skin.equipmentFor(item)).as(skin.id() + " " + item).contains(skin.equipment());
            }
            assertThat(skin.dresses("diamond_sword")).isFalse();
            assertThat(files).contains("assets/rainsskins/equipment/" + skin.id() + ".json",
                    "assets/rainsskins/textures/entity/equipment/humanoid/" + skin.id() + ".png",
                    "assets/rainsskins/textures/entity/equipment/humanoid_leggings/" + skin.id() + ".png");
            for (String piece : List.of("helmet", "chestplate", "leggings", "boots")) {
                assertThat(files).contains("assets/rainsskins/textures/item/" + skin.id() + "/" + piece + ".png",
                        "assets/rainsskins/models/" + skin.id() + "/" + piece + ".json",
                        "assets/rainsskins/items/" + skin.id() + "/" + piece + ".json");
            }
        }
    }

    private static java.awt.image.BufferedImage worn(String skin, String layer) throws Exception {
        try (java.io.InputStream in = SkinCatalogueTest.class.getResourceAsStream(
                "/de/raindancer/modules/cosmetics/pack/assets/rainsskins/textures/entity/equipment/" + layer + "/" + skin + ".png")) {
            return javax.imageio.ImageIO.read(in);
        }
    }

    private static boolean painted(java.awt.image.BufferedImage image, int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    @DisplayName("the worn layers are 64x32 and paint each piece where it is worn — boots only at the foot, leggings not at the foot")
    void twitchArmourLayers() throws Exception {
        for (String skin : List.of("twitch-glitchplate", "twitch-glitchplate-night")) {
            java.awt.image.BufferedImage outer = worn(skin, "humanoid");
            java.awt.image.BufferedImage inner = worn(skin, "humanoid_leggings");
            assertThat(outer.getWidth()).isEqualTo(64);
            assertThat(outer.getHeight()).isEqualTo(32);
            assertThat(inner.getWidth()).isEqualTo(64);
            assertThat(inner.getHeight()).isEqualTo(32);
            assertThat(painted(outer, 0, 0, 32, 16)).as(skin + " helmet").isTrue();
            assertThat(painted(outer, 16, 16, 40, 32)).as(skin + " chestplate body").isTrue();
            assertThat(painted(outer, 40, 16, 56, 32)).as(skin + " sleeves").isTrue();
            assertThat(painted(outer, 0, 28, 16, 32)).as(skin + " boots at the foot").isTrue();
            assertThat(painted(outer, 0, 20, 16, 28)).as(skin + " boots leave the legs bare").isFalse();
            assertThat(painted(inner, 0, 20, 16, 28)).as(skin + " leggings on the legs").isTrue();
            assertThat(painted(inner, 0, 28, 16, 32)).as(skin + " leggings stop above the foot").isFalse();
            assertThat(painted(inner, 16, 20, 40, 24)).as(skin + " belt").isTrue();
            assertThat(painted(inner, 0, 0, 64, 16)).as(skin + " no hat in the leggings layer").isFalse();
        }
    }
}
