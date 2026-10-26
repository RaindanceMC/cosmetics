package de.raindancer.modules.cosmetics.skins;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every weapon skin there is: made by art/skins/convert.py from openly licensed resource packs, its models in the
 * {@code rainsskins} namespace of the module's pack. A skin dresses one or more weapon types, a look per item.
 */
public final class SkinCatalogue {

    /** The categories in the order the menu shows them. */
    public static final List<String> CATEGORIES = List.of("Katana & Blades", "Scythe", "Fantasy", "3D Classic", "Modern",
            "Cute & Fun");

    private static final Pattern WEAPON = Pattern.compile(
            "^(?:(?:wooden|stone|copper|iron|golden|diamond|netherite)_(sword|axe|pickaxe|spear)|(mace|trident|bow|crossbow))$");

    /** @param looks item id (diamond_sword) to the item model that dresses it */
    public record Skin(String id, String title, int pack, String packTitle, String category, String author,
                       String license, String url, Map<String, String> looks) {

        /** The look for this item: its own, or another of the same weapon type's. */
        public Optional<String> lookFor(String item) {
            String own = looks.get(item);
            if (own != null) {
                return Optional.of(own);
            }
            Optional<String> kind = kindOf(item);
            return kind.flatMap(k -> looks.entrySet().stream()
                    .filter(entry -> kindOf(entry.getKey()).filter(k::equals).isPresent())
                    .map(Map.Entry::getValue).findFirst());
        }

        public boolean dresses(String item) {
            return lookFor(item).isPresent();
        }
    }

    private final List<Skin> skins;
    private final Map<String, Skin> byId = new LinkedHashMap<>();

    private SkinCatalogue(List<Skin> skins) {
        this.skins = List.copyOf(skins);
        skins.forEach(skin -> byId.put(skin.id(), skin));
    }

    public static SkinCatalogue read(InputStream in) {
        List<Skin> read = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                new TypeToken<List<Skin>>() { }.getType());
        return new SkinCatalogue(read == null ? List.of() : read);
    }

    /** The catalogue that ships with the module, next to its pack. */
    public static SkinCatalogue bundled() {
        InputStream in = SkinCatalogue.class.getResourceAsStream("/de/raindancer/modules/cosmetics/skins.json");
        return in == null ? new SkinCatalogue(List.of()) : read(in);
    }

    /** {@code sword} for any sword, {@code bow} for a bow; nothing for what has no skins. */
    public static Optional<String> kindOf(String item) {
        if (item == null) {
            return Optional.empty();
        }
        Matcher matcher = WEAPON.matcher(item);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
    }

    public List<Skin> all() {
        return skins;
    }

    public Optional<Skin> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** What this item may wear: skins for its weapon type, minus those an owner switched off. */
    public List<Skin> offeredFor(String item, Set<String> switchedOff) {
        return skins.stream().filter(skin -> !switchedOff.contains(skin.id()) && skin.dresses(item)).toList();
    }

    public static List<String> categoriesOf(Collection<Skin> skins) {
        List<String> found = new ArrayList<>();
        for (String category : CATEGORIES) {
            if (skins.stream().anyMatch(skin -> skin.category().equals(category))) {
                found.add(category);
            }
        }
        skins.stream().map(Skin::category).filter(category -> !found.contains(category)).distinct().forEach(found::add);
        return found;
    }
}
