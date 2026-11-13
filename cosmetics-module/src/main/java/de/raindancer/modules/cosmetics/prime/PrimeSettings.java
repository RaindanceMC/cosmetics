package de.raindancer.modules.cosmetics.prime;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

@Settings(id = "cosmetics-prime", topics = {
        @Topic(path = "cosmetics/prime", title = "Prime", icon = Material.NETHER_STAR),
})
public record PrimeSettings(

        @In("cosmetics/prime") @Title("Cosmetics are a Prime perk")
        @Describe("On: coloured names, particles, teleport looks and weapon/armour skins are for Prime, staff and "
                + "creators only, and Prime's monthly drops are worn. Off: everybody has the everyday cosmetics and "
                + "skins are a thank-you for coffee supporters.")
        @Key("prime.only")
        boolean primeOnly) {

    public static final PrimeSettings DEFAULTS = new PrimeSettings(false);
}
