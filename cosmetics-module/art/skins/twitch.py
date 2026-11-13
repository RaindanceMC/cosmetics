"""The creators' Twitch skins — weapons and armour: our own pixel art, drawn here (nothing third-party to credit).

    python3 -I twitch.py <module resources dir: .../de/raindancer/modules/cosmetics>

Writes textures, models and item definitions of the skins `twitch-*` into the pack (namespace rainsskins), the worn
armour layers and equipment assets of the armour ones, adds them to skins.json flagged "creator": true (replacing any
earlier twitch-* entries) and to pack/files.txt. Run it again after convert.py and after armour.py, which rewrite
skins.json (and armour.py the equipment assets) from the downloaded packs and know nothing of these.

Palette: Twitch purple #9146FF with its light and dark; every few rows a glitch — the same pixel shifted aside in
cyan and magenta, like a bad RGB split.
"""
import json
import os
import sys

from PIL import Image

RES = sys.argv[1]
PACK = os.path.join(RES, 'pack')
NS = 'rainsskins'
TIERS = ['wooden', 'stone', 'copper', 'iron', 'golden', 'diamond', 'netherite']

PAL = {
    '.': None,
    'D': (14, 14, 16),
    'd': (38, 38, 44),
    'P': (0x91, 0x46, 0xFF),
    'L': (0xBF, 0x94, 0xFF),
    'S': (0x77, 0x2C, 0xE8),
    'G': (0x5C, 0x16, 0xC5),
    'W': (255, 255, 255),
    'C': (0x00, 0xE5, 0xFF),
    'M': (0xFF, 0x3C, 0x78),
}


def canvas():
    return [['.'] * 16 for _ in range(16)]


def put(grid, x, y, key):
    if 0 <= x < 16 and 0 <= y < 16:
        grid[y][x] = key


def sword(rgb):
    g = canvas()
    # blade: a five-wide diagonal band, tip at the top right (x + y = 15 is its spine)
    for y in range(1, 10):
        x = 15 - y
        edge = (y == 1)
        put(g, x - 2, y, 'D')
        put(g, x - 1, y, 'L')
        put(g, x, y, 'P')
        put(g, x + 1, y, 'S')
        put(g, x + 2, y, 'D')
        if edge:
            for dx in (-2, -1, 0, 1, 2):
                put(g, x + dx, y, '.')
            put(g, x, y, 'L')
            put(g, x + 1, y, 'D')
            put(g, x - 1, y, 'D')
    put(g, 14, 0, 'D')
    put(g, 15, 1, 'D')
    put(g, 14, 1, 'W')
    # guard across the blade's root, handle, pommel
    for k in range(-2, 3):
        put(g, 6 + k, 10 + k, 'G')
        put(g, 7 + k, 10 + k, 'D')
        put(g, 5 + k, 10 + k, 'D')
    put(g, 6, 10, 'P')
    for step, key in enumerate(['d', 'd', 'S']):
        put(g, 5 - step, 11 + step, key)
        put(g, 6 - step, 11 + step, 'D')
        put(g, 4 - step, 11 + step, 'D')
    put(g, 2, 14, 'P')
    put(g, 1, 14, 'D')
    put(g, 2, 15, 'D')
    put(g, 3, 14, 'D')
    # the glitch: rows where the blade is torn aside — cyan to the left, magenta to the right
    rows = (3, 6, 8) if rgb else (4, 7)
    for y in rows:
        x = 15 - y
        put(g, x - 3, y, 'C')
        put(g, x + 3, y, 'M')
        put(g, x - 2, y, 'L')
        put(g, x + 2, y, 'S')
    put(g, 11, 0, 'L')
    put(g, 15, 5, 'W')
    if rgb:
        for y in range(2, 10):
            x = 15 - y
            put(g, x, y, ['C', 'P', 'P', 'M', 'P', 'C', 'P', 'M'][y - 2])
    return g


def axe():
    rows = [
        '................',
        '...........DDD..',
        '..........DLLLD.',
        '.........DLPPPSD',
        '........DLPPPSD.',
        '........DPPSSD..',
        '.......DDDSD....',
        '......DdSD......',
        '.....DdSD.......',
        '....DdSD........',
        '...DdSD.........',
        '..DdSD..........',
        '..DPD...........',
        '..DD............',
        '................',
        '................',
    ]
    g = [list(r) for r in rows]
    for y, ahead in ((3, 'C'), (5, 'M')):
        put(g, 7, y, ahead)
    put(g, 13, 2, 'W')
    put(g, 14, 5, 'M')
    put(g, 8, 3, 'C')
    return g


def pick():
    rows = [
        '................',
        '.....DDDDDD.....',
        '...DDLLLLLPPD...',
        '..DLLPPPPPSSSD..',
        '.DLPPDDDDDDSSSD.',
        '.DPSD...DdSD.SD.',
        '..DD....DdSD.DD.',
        '........DdSD....',
        '.......DdSD.....',
        '.......DdSD.....',
        '......DdSD......',
        '......DdSD......',
        '.....DdSD.......',
        '.....DPD........',
        '.....DD.........',
        '................',
    ]
    g = [list(r) for r in rows]
    put(g, 1, 2, 'C')
    put(g, 0, 4, 'C')
    put(g, 14, 3, 'M')
    put(g, 15, 5, 'M')
    put(g, 8, 1, 'W')
    put(g, 12, 2, 'L')
    return g


SKINS = [
    # id, title, drawing, the items it dresses (every tier of each kind, so any held tier gets a look)
    ('twitch-glitchblade', 'Glitchblade', lambda: sword(False), 'sword'),
    ('twitch-glitchblade-rgb', 'Glitchblade RGB', lambda: sword(True), 'sword'),
    ('twitch-pixel-chopper', 'Pixel Chopper', axe, 'axe'),
    ('twitch-pixel-pick', 'Pixel Pick', pick, 'pickaxe'),
]


def render(grid):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, key in enumerate(row):
            colour = PAL[key]
            if colour:
                img.putpixel((x, y), colour + (255,))
    return img


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)


# ---------------------------------------------------------------------------------------------------- armour

NIGHT = {'P': (0x24, 0x24, 0x2B), 'L': (0x91, 0x46, 0xFF), 'S': (0x17, 0x17, 0x1C), 'G': (0x10, 0x10, 0x14),
         'D': (6, 6, 8)}


def palette(night):
    pal = dict(PAL)
    if night:
        pal.update(NIGHT)
    return pal


def mix(*parts):
    return sum(parts) & 0xFFFF


def plate(img, pal, x0, y0, w, h, edge=True):
    """One face of the worn model: lit top row, purple body, shaded lower rows, dark outline, and glitched rows where
    the same row is shifted a pixel with cyan on its left and magenta on its right."""
    def px(x, y, key):
        img.putpixel((x0 + x, y0 + y), pal[key] + (255,))
    for y in range(h):
        for x in range(w):
            key = 'P'
            if y == 0:
                key = 'L'
            elif y >= h - max(1, h // 4):
                key = 'S'
            if y == h - 1:
                key = 'G'
            if edge and (x == 0 or x == w - 1):
                key = 'D'
            px(x, y, key)
    seed = x0 * 7 + y0 * 13
    for y in range(2, h - 1):
        if mix(seed, y * 5) % 9 == 0 and w >= 4:
            px(0, y, 'C')
            px(w - 1, y, 'M')
            px(1, y, 'L')
            px(w - 2, y, 'S')
    if w >= 4 and h >= 4 and mix(seed, 3) % 5 == 0:
        px(1 + mix(seed, 1) % (w - 2), 1 + mix(seed, 2) % (h - 2), 'W')


def worn_layers(night):
    pal = palette(night)
    outer = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    inner = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    # helmet: head box at (0,0)
    for x, y, w, h in ((8, 0, 8, 8), (16, 0, 8, 8), (0, 8, 8, 8), (8, 8, 8, 8), (16, 8, 8, 8), (24, 8, 8, 8)):
        plate(outer, pal, x, y, w, h)
    for x in range(10, 14):          # the visor on the front: a dark slit with two glowing eyes
        for y in (11, 12):
            outer.putpixel((x, y), pal['D'] + (255,))
    outer.putpixel((10, 11), pal['C'] + (255,))
    outer.putpixel((13, 11), pal['C'] + (255,))
    outer.putpixel((11, 12), pal['M'] + (255,))
    # chestplate: body at (16,16), arms at (40,16)
    for x, y, w, h in ((20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12),
                       (44, 16, 4, 4), (48, 16, 4, 4), (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12)):
        plate(outer, pal, x, y, w, h)
    for dx, dy in ((3, 3), (4, 3), (3, 4), (4, 4)):   # a lit core on the chest
        outer.putpixel((20 + dx, 20 + dy), pal['W' if (dx + dy) % 2 else 'L'] + (255,))
    # boots: only the foot of the leg box at (0,16) — the rest is the player's own leg
    for x, y, w, h in ((0, 28, 4, 4), (4, 28, 4, 4), (8, 28, 4, 4), (12, 28, 4, 4), (8, 16, 4, 4)):
        plate(outer, pal, x, y, w, h)
    # leggings: the leg box down to the knee and the belt round the body — the foot stays bare
    for x, y, w, h in ((0, 20, 4, 8), (4, 20, 4, 8), (8, 20, 4, 8), (12, 20, 4, 8), (4, 16, 4, 4),
                       (20, 16, 8, 4), (16, 20, 4, 4), (20, 20, 8, 4), (28, 20, 4, 4), (32, 20, 8, 4)):
        plate(inner, pal, x, y, w, h)
    inner.putpixel((23, 21), pal['W'] + (255,))
    inner.putpixel((24, 21), pal['W'] + (255,))
    return outer, inner


ICONS = {
    'helmet': [
        '................', '................', '....DDDDDDDD....', '...DLLLLLLPPD...', '..DLPPPPPPPSSD..',
        '..DLPPPPPPPSSD..', '..DPPDDDDDDPSD..', '..DPDCDDDDMDSD..', '..DPPDDDDDDSSD..', '..DPSSD..DSSSD..',
        '..DDDDD..DDDDD..', '................', '................', '................', '................', '................'],
    'chestplate': [
        '................', '..DDD......DDD..', '.DLLPD....DPPSD.', '.DLPPDDDDDDPPSD.', '.DPPPPLLLLPPSSD.',
        '.DDPPPPPPPPSSDD.', '..DPPPPWWPPPSD..', '..DPPPPWLPPPSD..', '..DCPPPPPPPMSD..', '..DPPPPPPPPSSD..',
        '..DPSSSSSSSSSD..', '..DDDDDDDDDDDD..', '................', '................', '................', '................'],
    'leggings': [
        '................', '..DDDDDDDDDDDD..', '..DLLLLLLLLPPD..', '..DGGGGWGGGGGD..', '..DLPPPPPPPPSD..',
        '..DPPPPDDPPPSD..', '..DPPPD..DPPSD..', '..DCPPD..DPPMD..', '..DPPPD..DPPSD..', '..DPPSD..DPSSD..',
        '..DPPSD..DPSSD..', '..DDDDD..DDDDD..', '................', '................', '................', '................'],
    'boots': [
        '................',
        '................',
        '................',
        '................',
        '................',
        '..DDDD....DDDD..',
        '..DLPD....DLPD..',
        '..DPPD....DPPD..',
        '..DCPD....DPMD..',
        '.DDPPDD..DDPPDD.',
        '.DLPPPSD.DLPPPSD',
        '.DDDDDDD.DDDDDDD',
        '................',
        '................',
        '................',
        '................'],
}


def icon(piece, night):
    pal = palette(night)
    grid = [list(r.ljust(16, '.')[:16]) for r in ICONS[piece]]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, key in enumerate(row):
            if pal.get(key):
                img.putpixel((x, y), pal[key] + (255,))
    return img


ARMOUR = [
    ('twitch-glitchplate', 'Glitchplate', False),
    ('twitch-glitchplate-night', 'Glitchplate Night', True),
]
PIECES = ['helmet', 'chestplate', 'leggings', 'boots']


def make_armour(files, entries):
    for skin_id, title, night in ARMOUR:
        outer, inner = worn_layers(night)
        for layer, image in (('humanoid', outer), ('humanoid_leggings', inner)):
            rel = f'assets/{NS}/textures/entity/equipment/{layer}/{skin_id}.png'
            os.makedirs(os.path.dirname(os.path.join(PACK, rel)), exist_ok=True)
            image.save(os.path.join(PACK, rel))
            files.append(rel)
        equipment = {'layers': {layer: [{'texture': f'{NS}:{skin_id}'}] for layer in ('humanoid', 'humanoid_leggings')}}
        rel = f'assets/{NS}/equipment/{skin_id}.json'
        write(os.path.join(PACK, rel), json.dumps(equipment, indent=1) + '\n')
        files.append(rel)
        looks = {}
        for piece in PIECES:
            tex = f'textures/item/{skin_id}/{piece}.png'
            path = os.path.join(PACK, 'assets', NS, tex)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            icon(piece, night).save(path)
            write(os.path.join(PACK, 'assets', NS, f'models/{skin_id}/{piece}.json'), json.dumps(
                {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{skin_id}/{piece}'}}, indent=1))
            write(os.path.join(PACK, 'assets', NS, f'items/{skin_id}/{piece}.json'), json.dumps(
                {'model': {'type': 'minecraft:model', 'model': f'{NS}:{skin_id}/{piece}'}}))
            files += [f'assets/{NS}/{tex}', f'assets/{NS}/models/{skin_id}/{piece}.json',
                      f'assets/{NS}/items/{skin_id}/{piece}.json']
            # One look per piece: any material's piece of that kind takes it (SkinCatalogue falls back by kind).
            looks[f'diamond_{piece}'] = f'{NS}:{skin_id}/{piece}'
        entries.append({'id': skin_id, 'title': title, 'pack': -1, 'packTitle': 'Twitch set (own work)',
                        'category': 'Twitch', 'author': 'Raindancer118', 'license': 'Own work',
                        'url': 'https://github.com/RaindanceMC/cosmetics', 'looks': looks,
                        'equipment': f'{NS}:{skin_id}', 'creator': True})


def main():
    files = []
    entries = []
    for skin_id, title, draw, kind in SKINS:
        img = render(draw())
        looks = {}
        for tier in TIERS:
            item = f'{tier}_{kind}'
            tex = f'textures/item/{skin_id}/{item}.png'
            path = os.path.join(PACK, 'assets', NS, tex)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path)
            model = {'parent': 'minecraft:item/handheld',
                     'textures': {'layer0': f'{NS}:item/{skin_id}/{item}'}}
            write(os.path.join(PACK, 'assets', NS, f'models/{skin_id}/{item}.json'), json.dumps(model, indent=1))
            definition = {'model': {'type': 'minecraft:model', 'model': f'{NS}:{skin_id}/{item}'}}
            write(os.path.join(PACK, 'assets', NS, f'items/{skin_id}/{item}.json'), json.dumps(definition))
            files += [f'assets/{NS}/{tex}', f'assets/{NS}/models/{skin_id}/{item}.json',
                      f'assets/{NS}/items/{skin_id}/{item}.json']
            looks[item] = f'{NS}:{skin_id}/{item}'
        entries.append({'id': skin_id, 'title': title, 'pack': -1, 'packTitle': 'Twitch set (own work)',
                        'category': 'Twitch', 'author': 'Raindancer118', 'license': 'Own work',
                        'url': 'https://github.com/RaindanceMC/cosmetics', 'looks': looks, 'creator': True})

    make_armour(files, entries)

    catalogue = os.path.join(RES, 'skins.json')
    with open(catalogue, encoding='utf-8') as f:
        skins = [s for s in json.load(f) if not s['id'].startswith('twitch-')]
    skins = entries + skins
    with open(catalogue, 'w', encoding='utf-8') as f:
        json.dump(skins, f, indent=1, ensure_ascii=False)
        f.write('\n')

    index = os.path.join(PACK, 'files.txt')
    with open(index, encoding='utf-8') as f:
        kept = [l.rstrip('\n') for l in f if l.strip() and '/twitch-' not in l]
    with open(index, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sorted(set(kept + files))) + '\n')
    print(len(entries), 'skins,', len(files), 'files')


if __name__ == '__main__':
    main()
