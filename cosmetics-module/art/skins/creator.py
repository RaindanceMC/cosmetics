"""Marks the skins that are for creators only: existing, openly licensed skins from the packs, not modelled here.

    python3 -I creator.py <module resources dir: .../de/raindancer/modules/cosmetics>

Sets "creator": true and the category "Twitch" on the listed skins in skins.json, and nothing else — author, licence,
url and credits stay the pack's. Run it again after convert.py, armour.py and shields.py, which rewrite skins.json
from the packs and know nothing of this; drops.py comes last. A creator skin is the creator node's alone (SkinRules.owns): not unlocked, not
rainscosmetics.skins.all, not Prime's.
"""
import json
import os
import sys

RES = sys.argv[1]

# id -> why it is here
CREATOR_SKINS = {
    'shadowstone-tools': 'Shadowstone Tools (onyx x netherite): a matching sword, axe and pickaxe in black with purple gems',
    'custom-3d-swords-dragon-blade': 'Custom 3D Swords, Dragon Blade: a purple lightning blade',
    'glacial-gear': 'Glacial Gear: a matching icy sword, axe and pickaxe, cyan like the glitch',
    'armour-purple-diamond-armor-diamond': 'Purple diamond armor: the whole set in Twitch purple',
    'armour-sakura-hotbar-netherite': 'S33R Sakura netherite: dark plate with pink and green glitch pixels',
    'armour-valkyrian-netherite-reforged-v2-netherite': 'Valkyrian netherite: dark plate with cyan and orange glow',
    'armour-netherite-armor-netherite': 'Netherite Armor: dark plate with cyan accents',
    'armour-ametrine-pvp-netherite': 'Ametrine netherite: plate fading from pink to Twitch purple',
    'armour-cherry-pvp-diamond': 'Cherry diamond: hot pink plate, loud on stream',
    'armour-vanillafrost-diamond': 'VanillaFrost diamond: clear ice plates, the armour to Glacial Gear',
}


def main():
    path = os.path.join(RES, 'skins.json')
    with open(path, encoding='utf-8') as f:
        skins = json.load(f)
    found = set()
    for skin in skins:
        if skin['id'] in CREATOR_SKINS:
            skin['creator'] = True
            skin['category'] = 'Twitch'
            found.add(skin['id'])
        else:
            skin.pop('creator', None)
    missing = set(CREATOR_SKINS) - found
    if missing:
        sys.exit('not in the catalogue: ' + ', '.join(sorted(missing)))
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(skins, f, indent=1, ensure_ascii=False)
        f.write('\n')
    print(len(found), 'creator skins')


if __name__ == '__main__':
    main()
