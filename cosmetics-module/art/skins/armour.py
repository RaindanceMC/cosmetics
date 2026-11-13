"""Turns downloaded armour resource packs into armour skins: one skin per pack and material, in the rainsskins
namespace, nothing of vanilla replaced.

    python3 -I armour.py <dir with the packs' zips and meta.json> <module resources dir> <vanilla client jar>

Worn layers come from the new layout (textures/entity/equipment/humanoid[_leggings]/<material>.png) or the old one
(textures/models/armor/<material>_layer_1|2.png); a skin offers only the pieces its layers actually paint. Icons are
the pack's own item textures where it has them, else the vanilla item. Leather stays dyeable, like vanilla's. A layer
the pack ships unchanged from vanilla (many packs carry every material and touch a few) is not a skin.
Re-running replaces every armour skin it made before.
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

PACKS_DIR, RES, VANILLA_JAR = sys.argv[1:4]
VANILLA = zipfile.ZipFile(VANILLA_JAR)
PACK_OUT = os.path.join(RES, 'pack')
NS = 'rainsskins'
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']
UNDYED_LEATHER = -6265536

# equipment name (new) -> (old layer name, item prefix, label)
MATERIALS = {
    'leather': ('leather', 'leather', 'Leather'),
    'chainmail': ('chainmail', 'chainmail', 'Chainmail'),
    'copper': ('copper', 'copper', 'Copper'),
    'iron': ('iron', 'iron', 'Iron'),
    'gold': ('gold', 'golden', 'Gold'),
    'diamond': ('diamond', 'diamond', 'Diamond'),
    'netherite': ('netherite', 'netherite', 'Netherite'),
    'turtle_scute': ('turtle', 'turtle', 'Turtle'),
}

# slug -> (title in menus, pack title, author, licence)
PACKS = {
    'jappa-styled-armor-retextures': ('Jappa', 'Jappa-Styled Armor Retextures', 'ATOMICtheCrow', 'CC-BY-4.0'),
    'valkyrian-netherite-reforged-v2': ('Valkyrian', 'Valkyrian Netherite Reforged V2', 'Orenxo', 'MIT'),
    'netherite-armor': ('Modern', 'Netherite Armor', 'ItsSory', 'MIT'),
    'frostburn': ('Frostburn', 'Frostburn', 'MayhemForge', 'MIT'),
    'purple-diamond-armor': ('Purple', 'Purple diamond armor', 'Mr_frugal', 'MIT'),
    'pink-iron': ('Pink', 'pink iron', 'QiroliOfficial', 'Apache-2.0'),
    'midas_gold': ('Midas', 'Gold Remastered', 'MidasDaEpik', 'MIT'),
    'better-armor-by-klimmer': ('Klimmer', 'Better armor', 'klimmer13631', 'MIT'),
    'izeexes-netherite-armor': ('Techno', "IZeeXeS's netherite armor", 'IZeeXeS', 'MIT'),
    'lisxmccreations': ('White', 'white netherite armor', 'LISXMC', 'MIT'),
    'plate-armor-plus': ('Plate', 'Plate Armor +', 'MrNoobNub', 'Unlicense'),
    'wooden-leather-armor': ('Wooden', 'Wooden Leather Armor', 'lord_bobbo', 'MIT'),
    'adinas-port-of-kits-cutie-flowers': ('Flowers', "Adina's Kit's Cutie Flowers", 'adinathedev', 'BSD-3-Clause'),
    'unobstructive-armor': ('Unobstructive', 'Unobstructive Armor', 'Antikyth', 'CC-BY-SA-4.0'),
    'mob-masks-armour': ('Mob Mask', 'Mob Masks Armour', 'SirJumpy8772', 'LGPL-3.0-only'),
    'life-series-armour-remake': ('Life Series', 'Life Series Armour Remake', 'thatrobin', 'MIT'),
    'sakura-hotbar': ('Sakura', 'S33R Sakura Hotbar', 'S33R_OF_PYR3', 'MIT'),
    'crystalcore': ('Crystal', 'CrystalCore', 'MR_POPCORN_9234', 'MIT'),
    'hardened-diamond-texture-pack': ('Hardened', 'Hardened Diamond Texture Pack', 'MemeThief', 'CC0-1.0'),
    'soups-knight-armor': ('Knight', "[Soup´s] Knight Armor", 's0up-Ilt0n', 'GPL-3.0-only'),
    'fionas-armors': ('Minimal', "Fiona's Minimal Armors", 'FionaBagel', 'WTFPL'),
}


def read_png(z, name):
    if name not in z.namelist():
        return None
    try:
        image = Image.open(io.BytesIO(z.read(name)))
        image.load()
        return image.convert('RGBA')
    except Exception:
        return None


def painted(image, box):
    """Whether anything is drawn in box (on the 64x32 grid, scaled to the image)."""
    s = image.width / 64
    if abs(image.height / image.width - 0.5) > 0.01:
        return False
    region = image.crop(tuple(round(v * s) for v in box))
    return region.getchannel('A').getbbox() is not None


def worn(z, material):
    old, _, _ = MATERIALS[material]
    root = 'assets/minecraft/textures/'
    layers = {}
    for layer, new_dir, old_suffix in (('humanoid', 'humanoid', '_layer_1'), ('humanoid_leggings', 'humanoid_leggings', '_layer_2')):
        image = read_png(z, f'{root}entity/equipment/{new_dir}/{material}.png') or read_png(z, f'{root}models/armor/{old}{old_suffix}.png')
        overlay = read_png(z, f'{root}entity/equipment/{new_dir}/{material}_overlay.png') or read_png(z, f'{root}models/armor/{old}{old_suffix}_overlay.png')
        if image is not None and abs(image.height / image.width - 0.5) <= 0.01 and not vanilla(image, f'{root}entity/equipment/{new_dir}/{material}.png'):
            layers[layer] = (image, overlay)
    return layers


def vanilla(image, name):
    """Whether image is vanilla's own texture, pixel for pixel."""
    original = read_png(VANILLA, name)
    return original is not None and original.size == image.size and original.tobytes() == image.tobytes()


def pieces(layers, material):
    found = []
    outer = layers.get('humanoid', (None,))[0]
    inner = layers.get('humanoid_leggings', (None,))[0]
    if outer is not None and (painted(outer, (0, 0, 32, 16)) or painted(outer, (32, 0, 64, 16))):
        found.append('helmet')
    if material != 'turtle_scute':
        if outer is not None and (painted(outer, (16, 16, 40, 32)) or painted(outer, (40, 16, 56, 32))):
            found.append('chestplate')
        if inner is not None and (painted(inner, (0, 16, 16, 32)) or painted(inner, (16, 16, 40, 32))):
            found.append('leggings')
        if outer is not None and painted(outer, (0, 16, 16, 32)):
            found.append('boots')
    return found


def icon(z, prefix, piece):
    root = 'assets/minecraft/textures/'
    old_prefix = 'gold' if prefix == 'golden' else prefix
    for name in (f'{root}item/{prefix}_{piece}.png', f'{root}items/{old_prefix}_{piece}.png'):
        image = read_png(z, name)
        if image is not None and image.width == image.height:
            overlay = read_png(z, name.replace('.png', '_overlay.png'))
            return image, overlay
    return None, None


def save(image, rel, written):
    path = os.path.join(PACK_OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)
    written.append(rel)


def save_json(data, rel, written):
    path = os.path.join(PACK_OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=1)
        f.write('\n')
    written.append(rel)


def main():
    meta = json.load(open(os.path.join(PACKS_DIR, 'meta.json')))
    catalogue_path = os.path.join(RES, 'skins.json')
    catalogue = [skin for skin in json.load(open(catalogue_path, encoding='utf-8')) if skin.get('category') != 'Armour']
    files_path = os.path.join(PACK_OUT, 'files.txt')
    files = [line.strip() for line in open(files_path) if line.strip()]
    files = [f for f in files if not f.startswith(f'assets/{NS}/equipment/') and '/armour/' not in f
             and not f.startswith(f'assets/{NS}/textures/entity/equipment/')]
    written, skins, credits, skipped = [], [], [], []
    for slug, (short, pack_title, author, licence) in PACKS.items():
        zip_path = os.path.join(PACKS_DIR, slug + '.zip')
        if not os.path.exists(zip_path):
            skipped.append(f'{slug}: not downloaded')
            continue
        z = zipfile.ZipFile(zip_path)
        made = 0
        for material, (_, prefix, label) in MATERIALS.items():
            layers = worn(z, material)
            covered = pieces(layers, material)
            if not covered:
                continue
            sid = f'{slug}-{material}'.replace('_', '-')
            asset = f'armour/{sid}'
            equipment = {}
            for layer, (image, overlay) in layers.items():
                save(image, f'assets/{NS}/textures/entity/equipment/{layer}/{asset}.png', written)
                entry = {'texture': f'{NS}:{asset}'}
                if material == 'leather':
                    entry['dyeable'] = {'color_when_undyed': UNDYED_LEATHER}
                equipment[layer] = [entry]
                if overlay is not None:
                    save(overlay, f'assets/{NS}/textures/entity/equipment/{layer}/{asset}_overlay.png', written)
                    equipment[layer].append({'texture': f'{NS}:{asset}_overlay'})
            save_json({'layers': equipment}, f'assets/{NS}/equipment/{asset}.json', written)
            looks = {}
            for piece in covered:
                item = f'{prefix}_{piece}'
                image, overlay = icon(z, prefix, piece)
                if image is None:
                    looks[item] = f'minecraft:{item}'
                    continue
                texture = f'item/armour/{sid}/{piece}'
                save(image, f'assets/{NS}/textures/{texture}.png', written)
                textures = {'layer0': f'{NS}:{texture}'}
                if overlay is not None:
                    save(overlay, f'assets/{NS}/textures/{texture}_overlay.png', written)
                    textures['layer1'] = f'{NS}:{texture}_overlay'
                save_json({'parent': 'minecraft:item/generated', 'textures': textures},
                          f'assets/{NS}/models/armour/{sid}/{piece}.json', written)
                model = {'type': 'minecraft:model', 'model': f'{NS}:armour/{sid}/{piece}'}
                if material == 'leather':
                    model['tints'] = [{'type': 'minecraft:dye', 'default': UNDYED_LEATHER}]
                save_json({'model': model}, f'assets/{NS}/items/armour/{sid}/{piece}.json', written)
                looks[item] = f'{NS}:armour/{sid}/{piece}'
            skins.append({'id': f'armour-{sid}', 'title': f'{short} {label}', 'pack': 0, 'packTitle': pack_title,
                          'category': 'Armour', 'author': author, 'license': licence,
                          'url': f'https://modrinth.com/resourcepack/{slug}', 'looks': looks,
                          'equipment': f'{NS}:{asset}'})
            made += 1
        if made:
            version = meta.get(slug, {}).get('version', '?')
            credits.append(f'{pack_title} by {author} ({licence}), version {version}, '
                           f'https://modrinth.com/resourcepack/{slug}')
        else:
            skipped.append(f'{slug}: no armour layers a vanilla client can show')
    with open(catalogue_path, 'w', encoding='utf-8') as f:
        json.dump(skins + catalogue, f, indent=1, ensure_ascii=False)
        f.write('\n')
    with open(files_path, 'w') as f:
        f.write('\n'.join(sorted(set(files + written))) + '\n')
    credits_path = os.path.join(PACK_OUT, f'assets/{NS}/CREDITS.txt')
    text = open(credits_path, encoding='utf-8').read()
    marker = '\nArmour skins'
    text = text.split(marker)[0].rstrip('\n') + '\n' + marker + (
        ', converted the same way into worn layers (equipment assets) and icons:\n' + '\n'.join(credits) + '\n')
    open(credits_path, 'w', encoding='utf-8').write(text)
    print(f'{len(skins)} armour skins from {len(credits)} packs, {len(written)} files')
    for line in skipped:
        print('skipped', line)


if __name__ == '__main__':
    main()
