"""The weapon-skin menus, painted as a coffee house's menu card: a thank-you for supporting the server.

Espresso wood around a cream card with coffee rings and a few beans, latte wells for the skins, a caramel strip for
the toolbar, dark roast for the navigation, the inventory in a coffee sack — and, where the game writes
"Inventory", the words THANK YOU FOR YOUR SUPPORT on the right. One painting per window height.

Uses Core's menu canvas (RainsCore/art/menus, checked out next to this repository).

    python3 -I menus.py <pack dir>
"""
import json
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', '..', '..', '..', 'RainsCore', 'art', 'menus'))
from canvas import Canvas, W, chest_well, inventory_well, hotbar_well, label_y, height, mix, wood, well, vignette  # noqa: E402

PACK = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'de',
                                                          'raindancer', 'modules', 'cosmetics', 'pack')
NS = 'rainsskins'

ESPRESSO = (52, 31, 19)
ESPRESSO_DARK = (32, 18, 11)
ESPRESSO_LIGHT = (96, 60, 38)
MOCHA = (111, 74, 46)
CARAMEL = (196, 140, 86)
CARAMEL_DARK = (140, 92, 50)
LATTE = (222, 196, 160)
LATTE_DARK = (176, 146, 108)
CREAM = (244, 230, 206)
CREAM_DARK = (226, 206, 172)
BURLAP = (170, 136, 92)
BURLAP_DARK = (124, 96, 60)
HEART = (214, 92, 96)
INK = (92, 58, 34)

# 3x5 capitals, enough for the thank-you
GLYPHS = {
    'A': ['.#.', '#.#', '###', '#.#', '#.#'], 'C': ['.##', '#..', '#..', '#..', '.##'],
    'F': ['###', '#..', '##.', '#..', '#..'], 'H': ['#.#', '#.#', '###', '#.#', '#.#'],
    'K': ['#.#', '#.#', '##.', '#.#', '#.#'], 'N': ['#.#', '###', '###', '###', '#.#'],
    'O': ['.#.', '#.#', '#.#', '#.#', '.#.'], 'P': ['##.', '#.#', '##.', '#..', '#..'],
    'R': ['##.', '#.#', '##.', '#.#', '#.#'], 'S': ['.##', '#..', '.#.', '..#', '##.'],
    'T': ['###', '.#.', '.#.', '.#.', '.#.'], 'U': ['#.#', '#.#', '#.#', '#.#', '###'],
    'Y': ['#.#', '#.#', '.#.', '.#.', '.#.'], ' ': ['.', '.', '.', '.', '.'],
}
HEART_GLYPH = ['#.#', '###', '###', '.#.', '...']


def text_width(text):
    return sum(len(GLYPHS[ch][0]) + 1 for ch in text) - 1


def write(c, x, y, text, colour, shadow):
    for ch in text:
        rows = GLYPHS[ch]
        for dy, row in enumerate(rows):
            for dx, cell in enumerate(row):
                if cell == '#':
                    c.put(x + dx + 1, y + dy + 1, shadow, 120)
                    c.put(x + dx, y + dy, colour)
        x += len(rows[0]) + 1
    return x


def heart(c, x, y):
    for dy, row in enumerate(HEART_GLYPH):
        for dx, cell in enumerate(row):
            if cell == '#':
                c.put(x + dx, y + dy, HEART)


def bean(c, x, y, flip):
    shape = ['.##.', '#..#', '#.##', '##.#', '#..#', '.##.'] if not flip else ['.##.', '#..#', '##.#', '#.##', '#..#', '.##.']
    for dy, row in enumerate(shape):
        for dx, cell in enumerate(row):
            if cell == '#':
                c.put(x + dx, y + dy, MOCHA)
            else:
                c.tint(x + dx, y + dy, ESPRESSO, 0.15)
    c.put(x + 1, y + 1, CARAMEL)


def ring(c, cx, cy, r, alpha):
    """A coffee cup's ring on the card."""
    for a in range(0, 360, 2):
        t = math.radians(a)
        for dr in (0, 0.6):
            x, y = round(cx + (r + dr) * math.cos(t)), round(cy + (r + dr) * math.sin(t))
            if 3 < x < W - 4:
                c.tint(x, y, MOCHA, alpha * (0.6 + 0.4 * math.sin(t * 3 + r)))


def cup(c, x, y):
    """A tiny cup with steam, at the end of the title band."""
    for dx in range(7):
        c.put(x + dx, y + 3, ESPRESSO_DARK)
        c.put(x + dx, y + 8, ESPRESSO_DARK)
    for dy in range(3, 9):
        c.put(x, y + dy, ESPRESSO_DARK)
        c.put(x + 6, y + dy, ESPRESSO_DARK)
    for dy in range(4, 8):
        for dx in range(1, 6):
            c.put(x + dx, y + dy, CREAM)
    for dx in range(1, 6):
        c.put(x + dx, y + 4, CARAMEL)
    c.put(x + 7, y + 5, ESPRESSO_DARK)
    c.put(x + 8, y + 5, ESPRESSO_DARK)
    c.put(x + 8, y + 6, ESPRESSO_DARK)
    c.put(x + 7, y + 7, ESPRESSO_DARK)
    for dx, dy in ((2, 2), (2, 1), (3, 0), (4, 2), (4, 1), (5, 0)):
        c.put(x + dx, y + dy, CREAM_DARK, 200)
    heart(c, x + 2, y + 5)


def paint(rows):
    h = height(rows)
    c = Canvas(W, h, seed=200 + rows)
    rng = random.Random(rows)
    wood(c, 0, 0, W, h, ESPRESSO, ESPRESSO_DARK, ESPRESSO_LIGHT, seed=rows, plank=None)
    c.outline(0, 0, W, h, ESPRESSO_DARK)
    c.round_corners(2)

    # the card: from the top to the bottom of the chest rows
    bottom = chest_well(rows - 1, 0)[1] + 19
    x0, y0, x1, y1 = 3, 3, W - 3, bottom + 1
    for y in range(y0, y1):
        for x in range(x0, x1):
            n = (math.sin(x * 0.23 + y * 0.11) + math.sin(x * 0.07 - y * 0.19)) * 0.04 + rng.uniform(-0.03, 0.03)
            c.put(x, y, mix(CREAM, CREAM_DARK, max(0, min(1, 0.2 + n))))
    vignette(c, x0, y0, x1, y1, CARAMEL_DARK, strength=0.35, reach=6)
    c.outline(x0, y0, x1, y1, CARAMEL_DARK)
    for cx, cy, r in ((W - 26, y0 + 30, 13), (22, y1 - 20, 10)):
        ring(c, cx, cy, r, 0.22)
    for bx, by, flip in ((W - 14, y0 + 4, False), (W - 20, y0 + 7, True), (6, y1 - 9, True)):
        bean(c, bx, by, flip)

    # the title band, a caramel ribbon with the cup at its end
    for y in range(5, 15):
        for x in range(6, W - 7):
            c.tint(x, y, CARAMEL, 0.35)
    c.hline(6, W - 7, 15, CARAMEL_DARK)
    cup(c, W - 22, 4)

    for r in range(rows):
        for col in range(9):
            x, y = chest_well(r, col)
            if rows == 6 and r == 5:
                well(c, x, y, ESPRESSO, ESPRESSO_DARK, ESPRESSO_LIGHT, depth=0.4)
            elif rows == 6 and r == 4:
                well(c, x, y, CARAMEL, CARAMEL_DARK, CREAM, depth=0.3)
            else:
                well(c, x, y, LATTE, LATTE_DARK, CREAM, depth=0.3)

    # the strip the game writes "Inventory" on, and the thank-you on its right
    ly = label_y(rows)
    for y in range(ly - 1, ly + 10):
        for x in range(5, W - 5):
            c.put(x, y, mix(CREAM, CREAM_DARK, 0.25))
    c.hline(5, W - 5, ly - 2, CARAMEL_DARK)
    c.hline(5, W - 5, ly + 10, CARAMEL_DARK)
    message = 'THANK YOU FOR YOUR SUPPORT'
    end = W - 8 - 4
    x = end - text_width(message)
    write(c, x, ly + 2, message, INK, CARAMEL)
    heart(c, end + 1, ly + 2)

    # the inventory in a coffee sack
    sack_top = ly + 11
    for y in range(sack_top, h - 3):
        for x in range(3, W - 3):
            weave = ((x + y) % 2) * 0.08 + ((x // 2 + y // 2) % 2) * 0.05
            c.put(x, y, mix(BURLAP, BURLAP_DARK, 0.25 + weave + rng.uniform(-0.03, 0.03)))
    for r in range(3):
        for col in range(9):
            well(c, *inventory_well(rows, r, col), BURLAP_DARK, ESPRESSO, LATTE, depth=0.35)
    for col in range(9):
        well(c, *hotbar_well(rows, col), BURLAP_DARK, ESPRESSO, LATTE, depth=0.35)
    return c


def main():
    providers = []
    written = []
    for rows in range(1, 7):
        c = paint(rows)
        rel = f'assets/{NS}/textures/font/menu/menu_{rows}.png'
        path = os.path.join(PACK, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        c.save(path)
        written.append(rel)
        providers.append({'type': 'bitmap', 'file': f'{NS}:font/menu/menu_{rows}.png', 'ascent': 13,
                          'height': c.h, 'chars': [chr(0xE000 + rows)]})
    providers.append({'type': 'space', 'advances': {'': -8, '': -169}})
    rel = f'assets/{NS}/font/menu.json'
    os.makedirs(os.path.join(PACK, f'assets/{NS}/font'), exist_ok=True)
    with open(os.path.join(PACK, rel), 'w', encoding='utf-8') as f:
        json.dump({'providers': providers}, f, indent=1)
    written.append(rel)
    index = os.path.join(PACK, 'files.txt')
    known = set(open(index).read().split()) if os.path.exists(index) else set()
    with open(index, 'w') as f:
        f.write('\n'.join(sorted(known | set(written))) + '\n')
    from PIL import Image
    paint(6).img.resize((W * 3, height(6) * 3), Image.NEAREST).save(os.path.join(HERE, 'menu-preview.png'))
    print('\n'.join(written))


if __name__ == '__main__':
    main()
