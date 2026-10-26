package de.raindancer.modules.cosmetics.skins;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

/** A skin on an item: the tag in its data, and the model it shows. No decisions here — {@link SkinRules} makes them. */
public final class SkinItems {

    private static final NamespacedKey SKIN = new NamespacedKey("rainscosmetics", "skin");
    private static final NamespacedKey OWNER = new NamespacedKey("rainscosmetics", "skin_owner");

    private SkinItems() {
    }

    public static Optional<SkinTag> tagOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer data = stack.getItemMeta().getPersistentDataContainer();
        String skin = data.get(SKIN, PersistentDataType.STRING);
        String owner = data.get(OWNER, PersistentDataType.STRING);
        if (skin == null || owner == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SkinTag(skin, UUID.fromString(owner)));
        } catch (IllegalArgumentException broken) {
            return Optional.empty();
        }
    }

    public static void tag(ItemStack stack, SkinTag tag) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(SKIN, PersistentDataType.STRING, tag.skin());
        meta.getPersistentDataContainer().set(OWNER, PersistentDataType.STRING, tag.owner().toString());
        stack.setItemMeta(meta);
    }

    public static void untag(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().remove(SKIN);
        meta.getPersistentDataContainer().remove(OWNER);
        stack.setItemMeta(meta);
    }

    /** The model set on the item, as {@code namespace:path}; null when it shows its own. */
    public static String currentModel(ItemStack stack) {
        if (!stack.isDataOverridden(DataComponentTypes.ITEM_MODEL)) {
            return null;
        }
        Key model = stack.getData(DataComponentTypes.ITEM_MODEL);
        return model == null ? null : model.asString();
    }

    /** @return whether the stack changed */
    public static boolean apply(ItemStack stack, SkinRules.Look look) {
        String current = currentModel(stack);
        switch (look.kind()) {
            case SHOW -> {
                if (look.model().equals(current)) {
                    return false;
                }
                stack.setData(DataComponentTypes.ITEM_MODEL, Key.key(look.model()));
                return true;
            }
            case HIDE -> {
                if (current == null || !current.startsWith(SkinRules.OUR_NAMESPACE + ":")) {
                    return false;
                }
                stack.resetData(DataComponentTypes.ITEM_MODEL);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
