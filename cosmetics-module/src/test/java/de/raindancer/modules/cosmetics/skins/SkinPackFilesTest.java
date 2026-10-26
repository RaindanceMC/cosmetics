package de.raindancer.modules.cosmetics.skins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shipped skins pack as the client will read it. The client only stitches {@code textures/item/} and
 * {@code textures/block/} into the atlases item models draw from: a texture anywhere else is in the pack and still
 * shows as the magenta-black checkerboard (deploy 20261026a, 404 of 449 looks).
 */
class SkinPackFilesTest {

    private static final String ROOT = "/de/raindancer/modules/cosmetics/pack/";
    private static final String NS = "rainsskins";

    private static List<String> files() throws IOException {
        try (InputStream in = SkinPackFilesTest.class.getResourceAsStream(ROOT + "files.txt")) {
            assertThat(in).as("files.txt").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().filter(l -> !l.isBlank()).toList();
        }
    }

    @Test
    @DisplayName("every file the index lists is in the jar")
    void indexMatchesJar() throws IOException {
        List<String> missing = files().stream().filter(f -> SkinPackFilesTest.class.getResource(ROOT + f) == null).toList();
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("our item textures sit where an atlas stitches them")
    void texturesInAtlasFolders() throws IOException {
        List<String> outside = files().stream()
                .filter(f -> f.startsWith("assets/" + NS + "/textures/") && f.endsWith(".png"))
                .map(f -> f.substring(("assets/" + NS + "/textures/").length()))
                .filter(f -> !f.startsWith("item/") && !f.startsWith("block/") && !f.startsWith("font/"))
                .toList();
        assertThat(outside).isEmpty();
    }

    @Test
    @DisplayName("every texture a model names is ours in an atlas folder, and shipped")
    void modelTexturesResolve() throws IOException {
        List<String> files = files();
        Set<String> shipped = new HashSet<>(files);
        List<String> wrong = new ArrayList<>();
        for (String file : files) {
            if (!file.startsWith("assets/" + NS + "/models/") || !file.endsWith(".json")) {
                continue;
            }
            JsonObject model;
            try (InputStream in = SkinPackFilesTest.class.getResourceAsStream(ROOT + file)) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("﻿", "");
                model = JsonParser.parseString(text).getAsJsonObject();
            }
            if (!model.has("textures") || !model.get("textures").isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                String ref = texture.getValue().getAsString();
                if (!ref.startsWith(NS + ":")) {
                    continue;
                }
                String path = ref.substring(NS.length() + 1);
                if (!(path.startsWith("item/") || path.startsWith("block/"))
                        || !shipped.contains("assets/" + NS + "/textures/" + path + ".png")) {
                    wrong.add(file + " -> " + ref);
                }
            }
        }
        assertThat(wrong).isEmpty();
    }
}
