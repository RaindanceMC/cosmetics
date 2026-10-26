"""Checks a converted skins pack: every item definition's models, their parents and textures must exist.

    python3 -I validate.py <pack dir> <vanilla dir> <catalogue.json>
"""
import json, os, sys

PACK, VANILLA, CATALOGUE = sys.argv[1:4]
vanilla_textures = {l.strip()[len('assets/minecraft/textures/'):-4] for l in open(os.path.join(VANILLA, 'textures.txt')) if l.strip().endswith('.png')}


def split(ref):
    return ref.split(':', 1) if ':' in ref else ('minecraft', ref)


def model_file(ref):
    ns, path = split(ref)
    for root in (PACK, VANILLA):
        f = os.path.join(root, 'assets', ns, 'models', path + '.json')
        if os.path.isfile(f):
            return f
    return None


def check_model(ref, seen, problems):
    if ref in seen or ref.startswith('builtin/') or ref.startswith('minecraft:builtin/'):
        return
    seen.add(ref)
    f = model_file(ref)
    if f is None:
        problems.append('model ' + ref)
        return
    data = json.load(open(f, encoding='utf-8-sig'))
    if isinstance(data.get('parent'), str):
        check_model(data['parent'], seen, problems)
    for value in (data.get('textures') or {}).values():
        if not isinstance(value, str) or value.startswith('#'):
            continue
        ns, path = split(value)
        if value == 'minecraft:item/barrier':
            problems.append('texture missing in the pack (stand-in barrier)')
            continue
        if ns == 'minecraft' and path in vanilla_textures:
            continue
        if not os.path.isfile(os.path.join(PACK, 'assets', ns, 'textures', path + '.png')):
            problems.append('texture ' + value)


def refs(node):
    if isinstance(node, dict):
        if isinstance(node.get('model'), str):
            yield node['model']
        if isinstance(node.get('base'), str):
            yield node['base']
        for v in node.values():
            yield from refs(v)
    elif isinstance(node, list):
        for v in node:
            yield from refs(v)


catalogue = json.load(open(CATALOGUE))
fix = '--fix' in sys.argv
bad = 0
for skin in catalogue:
    for target, key in skin['looks'].items():
        ns, path = split(key)
        f = os.path.join(PACK, 'assets', ns, 'items', path + '.json')
        problems = []
        if not os.path.isfile(f):
            problems.append('item definition ' + key)
        else:
            seen = set()
            for ref in refs(json.load(open(f))):
                check_model(ref, seen, problems)
        if problems:
            bad += 1
            print(f"{skin['id']} {target}: {sorted(set(problems))[:4]}")
            if fix:
                skin['looks'][target] = None
if fix:
    for skin in catalogue:
        skin['looks'] = {t: k for t, k in skin['looks'].items() if k}
    catalogue = [s for s in catalogue if s['looks']]
    json.dump(catalogue, open(CATALOGUE, 'w'), indent=1)
    print(f"dropped {bad} broken look(s); {len(catalogue)} skins, {sum(len(s['looks']) for s in catalogue)} looks left")
    sys.exit(0)
print(f"{len(catalogue)} skins, {sum(len(s['looks']) for s in catalogue)} looks, {bad} broken")
sys.exit(1 if bad else 0)
