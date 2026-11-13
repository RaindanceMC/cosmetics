"""The creators' Twitch weapon skins: our own pixel art, drawn here (nothing third-party, so nothing to credit).

    python3 -I twitch.py <module resources dir: .../de/raindancer/modules/cosmetics>

Writes textures, models and item definitions of the skins `twitch-*` into the pack (namespace rainsskins), adds them
to skins.json flagged "creator": true (replacing any earlier twitch-* entries) and to pack/files.txt. Run it again
after convert.py, which rewrites skins.json from the downloaded packs and knows nothing of these.

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

    catalogue = os.path.join(RES, 'skins.json')
    with open(catalogue, encoding='utf-8') as f:
        skins = [s for s in json.load(f) if not s['id'].startswith('twitch-')]
    skins = entries + skins
    with open(catalogue, 'w', encoding='utf-8') as f:
        json.dump(skins, f, indent=1, ensure_ascii=False)
        f.write('\n')

    index = os.path.join(PACK, 'files.txt')
    with open(index, encoding='utf-8') as f:
        kept = [l.rstrip('\n') for l in f if l.strip() and f'/{NS}/textures/item/twitch-' not in l
                and f'/{NS}/models/twitch-' not in l and f'/{NS}/items/twitch-' not in l]
    with open(index, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sorted(set(kept + files))) + '\n')
    print(len(entries), 'skins,', len(files), 'files')


if __name__ == '__main__':
    main()
