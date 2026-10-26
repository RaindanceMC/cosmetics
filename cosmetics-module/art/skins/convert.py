"""Turns downloaded weapon resource packs into skins: one namespace (rainsskins), nothing of vanilla replaced.

    python3 -I convert.py <packs.json> <unpacked dir> <vanilla dir> <out pack dir> <out catalogue.json>

For every weapon, Minecraft's own item definition (from the 26.3 client) is the template; every vanilla model it
names is swapped for the pack's model or texture where the pack has one. A pack's own item definition wins; one
that selects on the custom name (a rename variant), and OptiFine CIT files, become one skin per variant.
"""
import copy
import json
import os
import re
import shutil
import sys

PACKS, UNPACKED, VANILLA, OUT, CATALOGUE = sys.argv[1:6]
NS = 'rainsskins'
TIERS = ['wooden', 'stone', 'copper', 'iron', 'golden', 'diamond', 'netherite']
TARGETS = [f'{t}_{k}' for k in ('sword', 'axe', 'pickaxe', 'spear') for t in TIERS] + ['mace', 'trident', 'bow', 'crossbow']
GUNS = re.compile(r'\b(gun|rifle|shotgun|pistol|revolver|rpg|m4|ak-?47|musket|glock)\b', re.I)


def kind_of(target):
    return target.split('_')[-1] if '_' in target else target


def load_json(path):
    with open(path, encoding='utf-8-sig') as f:
        return json.load(f)


def vanilla_item(target):
    return load_json(os.path.join(VANILLA, 'assets/minecraft/items', target + '.json'))


VANILLA_TEXTURES = set()
with open(os.path.join(VANILLA, 'textures.txt')) as f:
    for line in f:
        line = line.strip()
        if line.endswith('.png'):
            VANILLA_TEXTURES.add(line[len('assets/minecraft/textures/'):-4])


def split_ref(ref):
    ref = ref.lstrip('#') if not ref.startswith('#') else ref
    if ':' in ref:
        ns, path = ref.split(':', 1)
    else:
        ns, path = 'minecraft', ref
    return ns, path


class Pack:
    """One unpacked pack; copies what a skin needs into our namespace, under the skin's own folder."""

    def __init__(self, root, skin):
        self.root = root
        self.skin = skin
        self.copied = {}
        self.problems = []
        self.problems_for = {}
        self.target = None
        self.here = []   # folders of the models being copied, for refs written relative to them

    def relative(self, ref, ext):
        """A ref written relative to the model naming it (./x, or a bare name in an OptiFine folder)."""
        if not self.here:
            return None
        bare = ref.split(':', 1)[1] if ':' in ref else ref
        if not (bare.startswith('./') or bare.startswith('../') or '/' not in bare):
            return None
        full = os.path.normpath(os.path.join(self.here[-1], bare + ext))
        if full.startswith(os.path.normpath(self.root)) and os.path.isfile(full):
            return full
        return None

    def file(self, ns, kind, path, ext):
        for p in (path, path.replace('item/', 'items/', 1), path.replace('items/', 'item/', 1)):
            full = os.path.join(self.root, 'assets', ns, kind, p + ext)
            if os.path.isfile(full):
                return full
        return None

    def has_model(self, ref):
        ns, path = split_ref(ref)
        return self.relative(ref, '.json') is not None or self.file(ns, 'models', path, '.json') is not None

    def has_texture(self, ref):
        ns, path = split_ref(ref)
        return self.relative(ref, '.png') is not None or self.file(ns, 'textures', path, '.png') is not None

    def texture(self, ref):
        """Our name for a texture: the pack's copy, or the vanilla one left alone."""
        if ref.startswith('#'):
            return ref
        ns, path = split_ref(ref)
        src = self.relative(ref, '.png') or self.file(ns, 'textures', path, '.png')
        if src is not None and self.relative(ref, '.png'):
            path = os.path.relpath(src[:-4], self.root).replace(os.sep, '/')
        if src is None:
            if ns == 'minecraft' and (path in VANILLA_TEXTURES or path.replace('items/', 'item/', 1) in VANILLA_TEXTURES):
                return 'minecraft:' + path.replace('items/', 'item/', 1)
            self.problems.append('missing texture ' + ref)
            self.problems_for.setdefault(self.target, []).append('missing texture ' + ref)
            return 'minecraft:item/barrier'
        ours = f'{self.skin}/{ns}/{path}'
        dest = os.path.join(OUT, 'assets', NS, 'textures', ours + '.png')
        if dest not in self.copied:
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            shutil.copyfile(src, dest)
            if os.path.isfile(src + '.mcmeta'):
                shutil.copyfile(src + '.mcmeta', dest + '.mcmeta')
            self.copied[dest] = True
        return f'{NS}:{ours}'

    def model(self, ref):
        """Our name for a model: the pack's copy with its parents and textures, or the vanilla one left alone."""
        ns, path = split_ref(ref)
        src = self.relative(ref, '.json') or self.file(ns, 'models', path, '.json')
        if src is None:
            return ref if ':' in ref else 'minecraft:' + ref
        if self.relative(ref, '.json'):
            path = os.path.relpath(src[:-5], self.root).replace(os.sep, '/')
        ours = f'{self.skin}/{ns}/{path}'
        dest = os.path.join(OUT, 'assets', NS, 'models', ours + '.json')
        if dest in self.copied:
            return f'{NS}:{ours}'
        self.copied[dest] = True
        try:
            data = load_json(src)
        except Exception as broken:
            self.problems.append(f'unreadable model {ref}: {broken}')
            return 'minecraft:item/barrier'
        data.pop('overrides', None)
        self.here.append(os.path.dirname(src))
        try:
            if isinstance(data.get('parent'), str) and not data['parent'].startswith('builtin/'):
                data['parent'] = self.model(data['parent'])
            if isinstance(data.get('textures'), dict):
                data['textures'] = {k: self.texture(v) if isinstance(v, str) else v for k, v in data['textures'].items()}
        finally:
            self.here.pop()
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'w') as f:
            json.dump(data, f, separators=(',', ':'))
        return f'{NS}:{ours}'

    def from_texture(self, vanilla_model, texture_ref):
        """A vanilla model given the pack's texture for it: what a texture-only pack amounts to."""
        ns, path = split_ref(vanilla_model)
        base = load_json(os.path.join(VANILLA, 'assets/minecraft/models', path + '.json'))
        ours = f'{self.skin}/model/{path}'
        dest = os.path.join(OUT, 'assets', NS, 'models', ours + '.json')
        model = {'parent': base.get('parent', 'minecraft:item/handheld'), 'textures': dict(base.get('textures', {}))}
        if ':' not in model['parent']:
            model['parent'] = 'minecraft:' + model['parent']
        mine = self.texture(texture_ref)
        for key, value in list(model['textures'].items()):
            if split_ref(value)[1].replace('items/', 'item/') == split_ref(texture_ref)[1].replace('items/', 'item/'):
                model['textures'][key] = mine
        if 'layer0' not in model['textures']:
            model['textures']['layer0'] = mine
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'w') as f:
            json.dump(model, f, separators=(',', ':'))
        return f'{NS}:{ours}'


def vanilla_model_textures(ref):
    ns, path = split_ref(ref)
    try:
        return [v for v in load_json(os.path.join(VANILLA, 'assets/minecraft/models', path + '.json')).get('textures', {}).values()]
    except FileNotFoundError:
        return []


def swap(node, pack, override=None):
    """The vanilla definition with every model the pack provides swapped in; returns whether anything was."""
    changed = False
    if isinstance(node, dict):
        if node.get('type') in ('minecraft:model', 'model') and isinstance(node.get('model'), str):
            ref = node['model']
            replacement = override(ref) if override else None
            if replacement:
                node['model'] = replacement
                changed = True
            elif pack.has_model(ref):
                node['model'] = pack.model(ref)
                changed = True
            else:
                for tex in vanilla_model_textures(ref):
                    if pack.has_texture(tex):
                        node['model'] = pack.from_texture(ref, tex)
                        changed = True
                        break
        if node.get('type') in ('minecraft:special', 'special') and isinstance(node.get('base'), str):
            base = node['base']
            if pack.has_model(base):
                # The game draws a special model (the thrown-style trident) itself; a pack's own 3D model replaces it.
                node.clear()
                node.update({'type': 'minecraft:model', 'model': pack.model(base)})
                changed = True
        for value in list(node.values()):
            changed = swap(value, pack, override) or changed
    elif isinstance(node, list):
        for value in node:
            changed = swap(value, pack, override) or changed
    return changed


def rewrite_own(node, pack):
    """A pack's own item definition, every model it names copied into our namespace."""
    if isinstance(node, dict):
        if node.get('type') in ('minecraft:model', 'model') and isinstance(node.get('model'), str):
            node['model'] = pack.model(node['model'])
        if node.get('type') in ('minecraft:special', 'special') and isinstance(node.get('base'), str):
            node['base'] = pack.model(node['base'])
        for value in node.values():
            rewrite_own(value, pack)
    elif isinstance(node, list):
        for value in node:
            rewrite_own(value, pack)


def name_variants(definition):
    """A definition that picks its look by the item's custom name: the cases, and what is left without one."""
    model = definition.get('model', {})
    if model.get('type') in ('minecraft:select', 'select') and model.get('property') in ('minecraft:component', 'component') \
            and model.get('component') in ('minecraft:custom_name', 'custom_name'):
        out = []
        for case in model.get('cases', []):
            when = case.get('when')
            names = when if isinstance(when, list) else [when]
            label = next((n if isinstance(n, str) else n.get('text', '') for n in names if n), '')
            label = re.sub(r'[\u0400-\u04FF]+', '', label).strip() or label
            out.append((label, case['model']))
        return out, model.get('fallback')
    return [], None


def write_item(skin, target, definition):
    dest = os.path.join(OUT, 'assets', NS, 'items', skin, target + '.json')
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, 'w') as f:
        json.dump(definition, f, separators=(',', ':'))
    return f'{NS}:{skin}/{target}'


def slugify(text):
    return re.sub(r'[^a-z0-9]+', '-', text.lower()).strip('-')[:40] or 'look'


def properties(path):
    out = {}
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#') or '=' not in line:
                continue
            key, value = line.split('=', 1)
            out[key.strip()] = value.strip()
    return out


def cit_variants(root):
    """OptiFine CIT files: (label, items, model ref or texture ref) for each."""
    out = []
    for dirpath, _, files in os.walk(root):
        for name in sorted(files):
            if not name.endswith('.properties') or '/cit' not in dirpath.replace('\\', '/'):
                continue
            props = {k: v.encode('latin-1', 'backslashreplace').decode('unicode_escape', 'replace') if '\\u' in v else v
                     for k, v in properties(os.path.join(dirpath, name)).items()}
            if props.get('type', 'item') != 'item':
                continue
            items = [i.replace('minecraft:', '') for i in re.split(r'[\s,]+', props.get('items', props.get('matchItems', ''))) if i]
            label = re.sub(r'^(i)?(regex|pattern|ipattern|iregex):', '', props.get('nbt.display.Name', name[:-11]))
            label = re.sub(r'[\\^$.*+?()\[\]{}|]', '', label).strip() or name[:-11]
            label = re.sub(r'[_]+', ' ', label).strip().title()
            # Some packs name a variant twice, Latin and Cyrillic run together; the menu shows the Latin
            label = re.sub(r'[\u0400-\u04FF]+', '', label).strip() or label
            rel = os.path.relpath(dirpath, os.path.join(root, 'assets'))
            ns = rel.split(os.sep)[0]
            base = '/'.join(rel.split(os.sep)[1:])

            def ref_of(value, kind):
                value = value.replace('\\', '/')
                if value.endswith('.json') or value.endswith('.png'):
                    value = value.rsplit('.', 1)[0]
                if value.startswith('./'):
                    value = value[2:]
                if ':' in value:
                    return value
                if '/' not in value or value.startswith('optifine'):
                    # relative to the properties file; models/textures folders do not apply under optifine/
                    return f'{ns}:{base}/{value}' if not value.startswith('optifine') else f'{ns}:{value}'
                return f'{ns}:{value}'
            per_model = {}
            for key, value in props.items():
                if key.startswith('model.') or key.startswith('texture.'):
                    which = key.split('.', 1)[1].replace('.standby', '').replace('_standby', '')
                    per_model[(key.split('.')[0], which)] = ref_of(value, '')
            model = props.get('model')
            texture = props.get('texture')
            if not model and not texture and not per_model:
                texture = name[:-11]
            out.append((label, items, ref_of(model, '') if model else None, ref_of(texture, '') if texture else None,
                        per_model, dirpath))
    return out


class CitPack(Pack):
    """CIT paths point outside models/ and textures/ (optifine/cit/...); resolve those too."""

    def file(self, ns, kind, path, ext):
        found = super().file(ns, kind, path, ext)
        if found:
            return found
        full = os.path.join(self.root, 'assets', ns, path + ext)
        return full if os.path.isfile(full) else None


# Packs that bring items of their own rather than replacing vanilla ones: which vanilla weapon each one dresses.
OWN_ITEMS = {
}


def convert(meta):
    root = os.path.join(UNPACKED, f"{meta['n']:02d}")
    base_id = slugify(meta['slug'])
    skins = []
    report = []

    def emit(skin_id, title, looks, pack):
        broken = {t for t in looks if any(p.startswith('missing') for p in pack.problems_for.get(t, []))}
        for t in broken:
            report.append(f'{skin_id}: {t} left out (a texture it needs is not in the pack)')
        looks = {t: v for t, v in looks.items() if t not in broken}
        if looks:
            skins.append(dict(id=skin_id, title=title, pack=meta['n'], packTitle=meta['title'], category=meta['cat'],
                              author=meta['author'], license=meta['license'],
                              url=f"https://modrinth.com/resourcepack/{meta['slug']}", looks=looks))
        report.extend(f'{skin_id}: {p}' for p in pack.problems)

    # 1. the pack's own item definitions, split by rename where they select on the name
    plain_looks, variant_looks = {}, {}
    pack = Pack(root, base_id)
    for target in TARGETS:
        pack.target = target
        own = os.path.join(root, 'assets/minecraft/items', target + '.json')
        if os.path.isfile(own):
            try:
                definition = load_json(own)
            except Exception as broken:
                report.append(f'{base_id}: unreadable {target}: {broken}')
                continue
            cases, fallback = name_variants(definition)
            if cases:
                for label, model in cases:
                    vid = f'{base_id}-{slugify(label)}'
                    vpack = Pack(root, vid)
                    vpack.target = target
                    node = {'model': copy.deepcopy(model)}
                    rewrite_own(node, vpack)
                    swap(node, vpack)
                    variant_looks.setdefault((vid, label), {})[target] = write_item(vid, target, node)
                    report.extend(f'{vid}: {p}' for p in vpack.problems)
                if fallback:
                    node = {'model': copy.deepcopy(fallback)}
                    rewrite_own(node, pack)
                    swap(node, pack)
                    if json.dumps(node) != json.dumps({'model': vanilla_item(target)['model']}):
                        plain_looks[target] = write_item(base_id, target, node)
            else:
                node = copy.deepcopy(definition)
                rewrite_own(node, pack)
                swap(node, pack)
                if json.dumps(node, sort_keys=True) != json.dumps(vanilla_item(target), sort_keys=True):
                    plain_looks[target] = write_item(base_id, target, node)
            continue
        # 2. Minecraft's own definition with the pack's models and textures swapped in
        node = copy.deepcopy(vanilla_item(target))
        if swap(node, pack):
            plain_looks[target] = write_item(base_id, target, node)
    for target, model_ref in OWN_ITEMS.get(meta['slug'], {}).items():
        pack.target = target
        if pack.has_model(model_ref):
            plain_looks[target] = write_item(base_id, target, {'model': {'type': 'minecraft:model', 'model': pack.model(model_ref)}})
    title = re.sub(r'\s*[\(\[].*?[\)\]]', '', meta['title']).strip()
    emit(base_id, title, plain_looks, pack)
    for (vid, label), looks in variant_looks.items():
        emit(vid, f'{title}: {label}', looks, Pack(root, vid))

    # 3. OptiFine CIT variants — the same looks again for older clients, when the pack has modern ones
    for label, items, model_ref, texture_ref, per_model, folder in ([] if variant_looks else cit_variants(root)):
        if GUNS.search(label):
            report.append(f'{base_id}: left out "{label}" (a gun)')
            continue
        vid = f'{base_id}-{slugify(label)}'
        while any(s['id'] == vid for s in skins):
            vid += '-2'
        cpack = CitPack(root, vid)
        cpack.here.append(folder)
        looks = {}
        for target in [i for i in items if i in TARGETS]:
            def override(ref, target=target):
                name = split_ref(ref)[1].split('/')[-1]
                chosen_model = per_model.get(('model', name)) or (model_ref if name == target else None)
                chosen_texture = per_model.get(('texture', name)) or (texture_ref if name == target else None)
                if chosen_model and cpack.has_model(chosen_model):
                    return cpack.model(chosen_model)
                if chosen_texture and cpack.has_texture(chosen_texture):
                    return cpack.from_texture(ref, chosen_texture)
                return None
            cpack.target = target
            node = copy.deepcopy(vanilla_item(target))
            if swap(node, cpack, override):
                looks[target] = write_item(vid, target, node)
        emit(vid, f'{title}: {label}', looks, cpack)
    return skins, report


def signature(skin):
    """What a skin looks like, whatever folder it was copied to: its item definitions with its own id taken out."""
    parts = []
    for target, key in sorted(skin['looks'].items()):
        path = os.path.join(OUT, 'assets', NS, 'items', key.split(':', 1)[1] + '.json')
        folder = key.split(':', 1)[1].split('/')[0]
        with open(path) as f:
            parts.append(target + '=' + f.read().replace(folder + '/', '*/'))
    return '\n'.join(parts)


def model_texts(skin):
    """The models and textures a skin's looks name, as content, for telling two variants apart."""
    folder = next(iter(skin['looks'].values())).split(':', 1)[1].split('/')[0]
    root = os.path.join(OUT, 'assets', NS)
    out = []
    for kind in ('models', 'textures'):
        base = os.path.join(root, kind, folder)
        for dirpath, _, names in os.walk(base):
            for name in sorted(names):
                with open(os.path.join(dirpath, name), 'rb') as f:
                    out.append(os.path.relpath(os.path.join(dirpath, name), base) + ':' + str(hash(f.read())))
    return '\n'.join(sorted(out))


def icon_marks(skin):
    """What each look shows in the inventory, rendered: two variants that only differ in name look the same."""
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import icons
    icons.ROOTS[:] = [OUT, VANILLA]
    marks = []
    for target, key in sorted(skin['looks'].items()):
        try:
            marks.append(target + ':' + icons.render_icon(key, 32).tobytes().hex()[:4000])
        except Exception as unrenderable:
            marks.append(target + ':?' + str(unrenderable))
    return '\n'.join(marks)


def tidy(catalogue):
    merged, by_name = [], {}
    for skin in catalogue:
        key = (skin['pack'], skin['title'])
        if key in by_name:
            by_name[key]['looks'].update({t: k for t, k in skin['looks'].items() if t not in by_name[key]['looks']})
            continue
        by_name[key] = skin
        merged.append(skin)
    kept, seen = [], set()
    for skin in merged:
        mark = (skin['pack'], signature(skin), model_texts(skin))
        drawn = (skin['pack'], icon_marks(skin))
        if mark in seen or drawn in seen:
            print(f"  same look as an earlier variant, left out: {skin['title']}")
            continue
        seen.add(mark)
        seen.add(drawn)
        kept.append(skin)
    return kept


def write_credits(metas, catalogue):
    """CC-BY asks for the author, the licence and that the work was changed; the others are thanked all the same."""
    used = {s['pack'] for s in catalogue}
    lines = ['Weapon skins in this pack, by the people who made them.',
             'Every pack was converted for RainsCosmetics: its models and textures were copied into the rainsskins',
             'namespace and wired to item definitions, so they dress single items instead of replacing vanilla ones.',
             'Nothing else was changed. Licences: https://spdx.org/licenses/', '']
    for meta in metas:
        if meta['n'] in used:
            lines.append(f"{meta['title']} by {meta['author']} ({meta['license']}), version {meta['version']}, "
                         f"https://modrinth.com/resourcepack/{meta['slug']}")
    path = os.path.join(OUT, 'assets', NS, 'CREDITS.txt')
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')


def write_index():
    """pack.mcmeta and files.txt — the list Core's JarPack copies out of the jar."""
    with open(os.path.join(OUT, 'pack.mcmeta'), 'w') as f:
        json.dump({'pack': {'description': 'Rain\'s Cosmetics — weapon skins and their menus', 'min_format': 88,
                            'max_format': 97}}, f, indent=1)
    files = []
    for dirpath, _, names in os.walk(OUT):
        for name in names:
            rel = os.path.relpath(os.path.join(dirpath, name), OUT).replace(os.sep, '/')
            if rel != 'files.txt':
                files.append(rel)
    with open(os.path.join(OUT, 'files.txt'), 'w') as f:
        f.write('\n'.join(sorted(files)) + '\n')


def main():
    metas = json.load(open(PACKS))
    if os.path.isdir(os.path.join(OUT, 'assets', NS)):
        shutil.rmtree(os.path.join(OUT, 'assets', NS))
    catalogue, problems = [], []
    for meta in metas:
        skins, report = convert(meta)
        catalogue.extend(skins)
        problems.extend(report)
        kinds = sorted({kind_of(t) for s in skins for t in s['looks']})
        print(f"{meta['n']:2} {meta['slug'][:30]:30} skins={len(skins):3} {','.join(kinds)}")
    catalogue = tidy(catalogue)
    with open(CATALOGUE, 'w') as f:
        json.dump(catalogue, f, indent=1)
    write_credits(metas, catalogue)
    write_index()
    print('skins', len(catalogue), 'problems', len(problems))
    for p in problems[:60]:
        print('  !', p)


main()
