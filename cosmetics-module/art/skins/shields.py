"""Turns downloaded shield resource packs into shield skins, in the rainsskins namespace, nothing of vanilla replaced.

    python3 -I shields.py <dir with the packs' zips> <module resources dir: .../de/raindancer/modules/cosmetics>

Vanilla draws the shield with a special renderer (items/shield.json: "minecraft:special", type "minecraft:shield")
that takes no texture: it always paints entity/shield/base from its own atlas, so it cannot be skinned per item. A
skin's item definition therefore swaps it for a plain model with cuboids, picked by "minecraft:using_item" like
vanilla's (the blocking pose while in use), and without vanilla's upside-down transformation, which only fits the
entity renderer. Banner patterns do not show on a skinned shield. Textures go to textures/item/, the atlas item
models draw from.

A pack either brings its own shield models (they keep their display transforms), or only a texture in vanilla's
entity layout (entity/shield_base_nopattern.png): that one is put on the cuboid shield of Translucent Shield
(MIT), which maps vanilla's layout. Run after armour.py; re-running replaces every shield skin it made before.
"""
import io
import json
import os
import sys
import zipfile

PACKS_DIR, RES = sys.argv[1:3]
PACK_OUT = os.path.join(RES, 'pack')
NS = 'rainsskins'
FORMAT = 97                      # the 26.3 client's resource pack format, for picking a pack's overlays
CARRIER = 'translucent-shield'   # the cuboid shield texture-only packs are painted on
CARRIER_MODELS = ('assets/minecraft/models/item/shield.json', 'assets/minecraft/models/item/shield_blocking.json')

# slug -> (pack title, author, licence)
PACKS = {
    'custom-shields': ('Custom Shields', '6MHz', 'CC-BY-SA-4.0'),
    'golden-spruce-shield': ('Golden spruce shield', 'ThatCuteOne', 'CC-BY-4.0'),
    'warped-wood-shield': ('Warped Wood Shield', 'loomidex', 'MIT'),
    'shieldrecolor': ('Shield Recolor', 'veadex', 'CC0-1.0'),
    CARRIER: ('Translucent Shield', 'yassooof (model by caisem)', 'MIT'),
}

# id -> (title, pack slug, ('model', shield model, blocking model) or ('texture', texture in vanilla's layout))
SHIELDS = {
    'shield-custom-shields-creeper': ('Custom Shields: Creeper', 'custom-shields',
                                      ('model', 'item/6mhz_shields/creeper', 'item/6mhz_shields/creeper_blocking')),
    'shield-custom-shields-dark-red': ('Custom Shields: Dark Red', 'custom-shields',
                                       ('model', 'item/6mhz_shields/dark_red', 'item/6mhz_shields/dark_red_blocking')),
    'shield-custom-shields-mirror': ('Custom Shields: Mirror', 'custom-shields',
                                     ('model', 'item/6mhz_shields/mirror', 'item/6mhz_shields/mirror_blocking')),
    'shield-golden-spruce': ('Golden Spruce Shield', 'golden-spruce-shield',
                             ('texture', 'assets/minecraft/textures/entity/shield_base_nopattern.png')),
    'shield-custom-shields-rabbit': ('Custom Shields: Rabbit', 'custom-shields',
                                     ('model', 'item/6mhz_shields/rabbit', 'item/6mhz_shields/rabbit_blocking')),
    'shield-custom-shields-red': ('Custom Shields: Red', 'custom-shields',
                                  ('model', 'item/6mhz_shields/red', 'item/6mhz_shields/red_blocking')),
    'shield-cherry-wood': ('Cherry Wood Shield', 'shieldrecolor',
                           ('texture', 'assets/minecraft/textures/entity/shield_base_nopattern.png')),
    'shield-warped-wood': ('Warped Wood Shield', 'warped-wood-shield',
                           ('texture', 'assets/minecraft/textures/entity/shield_base_nopattern.png')),
}


def read_json(data):
    text = data.decode('utf-8-sig')
    return json.loads(text[:text.rfind('}') + 1])   # Translucent Shield ends its models with a // comment


class Zip:
    """A pack as the 26.3 client reads it: the overlays that apply, the last listed winning, over the root."""

    def __init__(self, path):
        self.z = zipfile.ZipFile(path)
        self.names = set(self.z.namelist())
        self.roots = ['']
        if 'pack.mcmeta' in self.names:
            for entry in read_json(self.z.read('pack.mcmeta')).get('overlays', {}).get('entries', []):
                low = entry.get('min_format', entry.get('formats', {}).get('min_inclusive', 0))
                high = entry.get('max_format', entry.get('formats', {}).get('max_inclusive', 999))
                low, high = (low[0] if isinstance(low, list) else low), (high[0] if isinstance(high, list) else high)
                if low <= FORMAT <= high:
                    self.roots.insert(0, entry['directory'] + '/')

    def find(self, name):
        return next((root + name for root in self.roots if root + name in self.names), None)

    def read(self, name):
        found = self.find(name)
        return None if found is None else self.z.read(found)


def split(ref):
    return ref.split(':', 1) if ':' in ref else ('minecraft', ref)


class Writer:
    def __init__(self):
        self.written = []

    def bytes(self, data, rel):
        path = os.path.join(PACK_OUT, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'wb') as f:
            f.write(data)
        self.written.append(rel)

    def json(self, data, rel):
        self.bytes((json.dumps(data, indent=1) + '\n').encode('utf-8'), rel)


def copy_model(pack, model_ref, sid, out, textures=None):
    """The pack's model in our namespace with its textures (and their animations) under textures/item/shields/<sid>/."""
    ns, path = split(model_ref)
    data = pack.read(f'assets/{ns}/models/{path}.json')
    if data is None:
        sys.exit(f'{sid}: {model_ref} is not in the pack')
    model = read_json(data)
    parent = model.get('parent')
    if isinstance(parent, str) and parent.startswith(('builtin/', 'minecraft:builtin/')):
        model.pop('parent')
    elif parent is not None:
        sys.exit(f'{sid}: {model_ref} has a parent ({parent}), which this script does not copy')
    mine = {}
    for key, ref in model.get('textures', {}).items():
        if ref.startswith('#'):
            mine[key] = ref
            continue
        if textures and key in textures:
            png, rel = textures[key]
        else:
            tns, tpath = split(ref)
            rel = f'assets/{tns}/textures/{tpath}.png'
            png = pack.read(rel)
        if png is None:
            sys.exit(f'{sid}: texture {ref} of {model_ref} is not in the pack')
        name = os.path.splitext(rel.split('/textures/', 1)[1])[0].replace('/', '_')
        ours = f'item/shields/{sid}/{name}'
        out.bytes(png, f'assets/{NS}/textures/{ours}.png')
        animation = pack.read(rel + '.mcmeta') if not (textures and key in textures) else None
        if animation is not None:
            out.bytes(animation, f'assets/{NS}/textures/{ours}.png.mcmeta')
        mine[key] = f'{NS}:{ours}'
    if mine:
        model['textures'] = mine
    model.pop('format_version', None)
    model.pop('credit', None)
    target = f'shields/{sid}/{os.path.basename(path)}'
    out.json(model, f'assets/{NS}/models/{target}.json')
    return f'{NS}:{target}'


def main():
    packs = {}
    for slug in PACKS:
        path = os.path.join(PACKS_DIR, slug + '.zip')
        if not os.path.exists(path):
            sys.exit(f'{slug}.zip is missing in {PACKS_DIR}: every shield pack is needed, it rebuilds them all')
        packs[slug] = Zip(path)
    catalogue_path = os.path.join(RES, 'skins.json')
    catalogue = [s for s in json.load(open(catalogue_path, encoding='utf-8')) if not s['id'].startswith('shield-')]
    files_path = os.path.join(PACK_OUT, 'files.txt')
    files = [line.strip() for line in open(files_path) if line.strip()]
    ours = (f'assets/{NS}/items/shields/', f'assets/{NS}/models/shields/', f'assets/{NS}/textures/item/shields/')
    for rel in files:
        if rel.startswith(ours) and os.path.exists(os.path.join(PACK_OUT, rel)):
            os.remove(os.path.join(PACK_OUT, rel))
    files = [f for f in files if not f.startswith(ours)]
    for top in ours:
        for folder, _, _ in sorted(os.walk(os.path.join(PACK_OUT, top)), reverse=True):
            if not os.listdir(folder):
                os.rmdir(folder)
    out, skins, used = Writer(), [], []
    for sid, (title, slug, source) in SHIELDS.items():
        pack_title, author, licence = PACKS[slug]
        pack = packs[slug]
        if source[0] == 'model':
            normal = copy_model(pack, source[1], sid, out)
            blocking = copy_model(pack, source[2], sid, out)
            used.append(slug)
        else:
            png = pack.read(source[1])
            if png is None:
                sys.exit(f'{sid}: {source[1]} is not in the pack')
            carrier = packs[CARRIER]
            painted = {'shield': (png, source[1]), 'particle': (png, source[1])}
            normal = copy_model(carrier, CARRIER_MODELS[0][len('assets/minecraft/models/'):-5], sid, out, painted)
            blocking = copy_model(carrier, CARRIER_MODELS[1][len('assets/minecraft/models/'):-5], sid, out, painted)
            used += [slug, CARRIER]
        out.json({'model': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                            'on_false': {'type': 'minecraft:model', 'model': normal},
                            'on_true': {'type': 'minecraft:model', 'model': blocking}}},
                 f'assets/{NS}/items/shields/{sid}.json')
        skins.append({'id': sid, 'title': title, 'pack': 0, 'packTitle': pack_title, 'category': 'Shields',
                      'author': author, 'license': licence, 'url': f'https://modrinth.com/resourcepack/{slug}',
                      'looks': {'shield': f'{NS}:shields/{sid}'}})
    armour_end = next((i for i, s in enumerate(catalogue) if not s['id'].startswith('armour-')), len(catalogue))
    catalogue = catalogue[:armour_end] + skins + catalogue[armour_end:]
    with open(catalogue_path, 'w', encoding='utf-8') as f:
        json.dump(catalogue, f, indent=1, ensure_ascii=False)
        f.write('\n')
    with open(files_path, 'w') as f:
        f.write('\n'.join(sorted(set(files + out.written))) + '\n')
    meta = json.load(open(os.path.join(PACKS_DIR, 'meta.json')))
    credits = [f"{PACKS[slug][0]} by {PACKS[slug][1]} ({PACKS[slug][2]}), version {meta.get(slug, {}).get('version', '?')}, "
               f'https://modrinth.com/resourcepack/{slug}' for slug in PACKS if slug in used]
    credits_path = os.path.join(PACK_OUT, f'assets/{NS}/CREDITS.txt')
    text = open(credits_path, encoding='utf-8').read()
    marker = '\nShield skins'
    text = text.split(marker)[0].rstrip('\n') + '\n' + marker + (
        ': the packs\' shield models and textures copied into the rainsskins namespace, the textures under '
        'textures/item/, and wired to item definitions that switch to the blocking model while the shield is in use. '
        'A texture made for vanilla\'s shield is painted on Translucent Shield\'s cuboid model. Nothing else was '
        'changed; the converted CC-BY-SA files stay under CC-BY-SA-4.0.\n' + '\n'.join(credits) + '\n')
    open(credits_path, 'w', encoding='utf-8').write(text)
    print(f'{len(skins)} shield skins, {len(out.written)} files')


if __name__ == '__main__':
    main()
