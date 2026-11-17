"""Marks the skins that come with a monthly Prime drop: existing, openly licensed skins from the packs.

    python3 -I drops.py <module resources dir: .../de/raindancer/modules/cosmetics>

Sets "drop": "<month>" on the listed skins in skins.json and nothing else. Such a skin is opened only by its own node
(rainscosmetics.skin.<id>), which prime-drops.yml gives the Prime members of that month (SkinRules.owns): not by
rainscosmetics.skins.all, not by an unlock. Run it last, after convert.py, armour.py, shields.py and creator.py.
Prints the entries for prime-drops.yml; PrimeDrops.SHIPPED carries the same (PrimeDropsTest checks both agree).
"""
import json
import os
import sys

RES = sys.argv[1]

DROPS = {
    '2027-01': ('Shieldwall', ['shield-custom-shields-creeper', 'shield-custom-shields-dark-red',
                              'shield-custom-shields-mirror', 'shield-golden-spruce']),
    '2027-02': ('Hold the Line', ['shield-custom-shields-rabbit', 'shield-custom-shields-red',
                                  'shield-cherry-wood', 'shield-warped-wood']),
    '2027-03': ('Crown and Plate', ['armour-even-better-armor-and-tools-gold', 'armour-platinum-diamond',
                                    'armour-carrorate-craft-gears-netherite']),
}


def main():
    path = os.path.join(RES, 'skins.json')
    with open(path, encoding='utf-8') as f:
        skins = json.load(f)
    month_of = {sid: month for month, (_, ids) in DROPS.items() for sid in ids}
    found = set()
    for skin in skins:
        if skin['id'] in month_of:
            skin['drop'] = month_of[skin['id']]
            found.add(skin['id'])
        else:
            skin.pop('drop', None)
    missing = set(month_of) - found
    if missing:
        sys.exit('not in the catalogue: ' + ', '.join(sorted(missing)))
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(skins, f, indent=1, ensure_ascii=False)
        f.write('\n')
    for month, (title, ids) in DROPS.items():
        print(f'  "{month}":\n    title: {title}\n    unlocks: [' + ', '.join(f'rainscosmetics.skin.{i}' for i in ids) + ']')


if __name__ == '__main__':
    main()
