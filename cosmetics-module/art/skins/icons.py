"""Draws Minecraft item models as the inventory shows them: the model's own 'gui' display transform, its cuboids
with their textures, orthographic, lit like the game's item lighting. Flat items are their layered textures.

    render_icon(item_definition_ref, size) -> PIL.Image
"""
import json
import math
import os

from PIL import Image, ImageDraw

ROOTS = []           # searched in order: our pack, the vanilla client extract
VANILLA_TEX = '/tmp/mcfont-tex'


def find(ns, kind, path, ext):
    for root in ROOTS:
        f = os.path.join(root, 'assets', ns, kind, path + ext)
        if os.path.isfile(f):
            return f
    return None


def split(ref):
    return ref.split(':', 1) if ':' in ref else ('minecraft', ref)


_tex = {}


def texture(ref):
    if ref in _tex:
        return _tex[ref]
    ns, path = split(ref)
    f = find(ns, 'textures', path, '.png')
    img = Image.open(f).convert('RGBA') if f else Image.new('RGBA', (16, 16), (255, 0, 255, 255))
    if img.height > img.width:      # an animation strip: its first frame
        img = img.crop((0, 0, img.width, img.width))
    _tex[ref] = img
    return img


def load_model(ref):
    """The model with its parents merged: elements, textures, display."""
    ns, path = split(ref)
    if path.startswith('builtin/'):
        return {'builtin': path}
    f = find(ns, 'models', path, '.json')
    if f is None:
        return {}
    data = json.load(open(f, encoding='utf-8-sig'))
    merged = {}
    if 'parent' in data:
        merged = load_model(data['parent'])
    out = dict(merged)
    tex = dict(merged.get('textures', {}))
    tex.update(data.get('textures', {}))
    out['textures'] = tex
    disp = dict(merged.get('display', {}))
    disp.update(data.get('display', {}))
    out['display'] = disp
    if 'elements' in data:
        out['elements'] = data['elements']
    if merged.get('builtin') and 'elements' not in data:
        out['builtin'] = merged['builtin']
    return out


def resolve(textures, key, depth=0):
    value = textures.get(key.lstrip('#'))
    if value is None or depth > 8:
        return None
    return resolve(textures, value, depth + 1) if value.startswith('#') else value


def item_model_ref(definition_ref):
    """The model an item definition shows in the inventory: the gui case of a select, the idle side otherwise."""
    ns, path = split(definition_ref)
    f = find(ns, 'items', path, '.json')
    node = json.load(open(f))['model']

    def walk(n):
        t = n.get('type', '').replace('minecraft:', '')
        if t == 'model':
            return n['model']
        if t == 'special':
            return n.get('base')
        if t == 'condition':
            return walk(n['on_false'])
        if t == 'select':
            for case in n.get('cases', []):
                when = case['when'] if isinstance(case['when'], list) else [case['when']]
                if 'gui' in when:
                    return walk(case['model'])
            return walk(n.get('fallback') or n['cases'][0]['model'])
        if t == 'range_dispatch':
            return walk(n.get('fallback') or n['entries'][0]['model'])
        if t == 'composite':
            return walk(n['models'][0])
        return None
    return walk(node)


def rot_matrix(rx, ry, rz):
    def rx_(a):
        c, s = math.cos(a), math.sin(a)
        return [[1, 0, 0], [0, c, -s], [0, s, c]]

    def ry_(a):
        c, s = math.cos(a), math.sin(a)
        return [[c, 0, s], [0, 1, 0], [-s, 0, c]]

    def rz_(a):
        c, s = math.cos(a), math.sin(a)
        return [[c, -s, 0], [s, c, 0], [0, 0, 1]]

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    # the game applies x, then y, then z (as a quaternion from euler angles in XYZ order)
    return mul(mul(rx_(math.radians(rx)), ry_(math.radians(ry))), rz_(math.radians(rz)))


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


FACES = {
    'north': ([1, 1, 0], [0, 1, 0], [0, 0, 0], [1, 0, 0]),
    'south': ([0, 1, 1], [1, 1, 1], [1, 0, 1], [0, 0, 1]),
    'west': ([0, 1, 0], [0, 1, 1], [0, 0, 1], [0, 0, 0]),
    'east': ([1, 1, 1], [1, 1, 0], [1, 0, 0], [1, 0, 1]),
    'up': ([0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]),
    'down': ([0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]),
}
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.6, 'east': 0.6}


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
            'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
            'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[face]


def flat(model, size):
    layers = sorted(k for k in model.get('textures', {}) if k.startswith('layer'))
    out = Image.new('RGBA', (16, 16))
    for key in layers:
        ref = resolve(model['textures'], key)
        if ref:
            out.alpha_composite(texture(ref).resize((16, 16), Image.NEAREST))
    return out.resize((size, size), Image.NEAREST)


def render_model(model, size):
    if 'elements' not in model:
        return flat(model, size)
    gui = model.get('display', {}).get('gui', {})
    rot = gui.get('rotation', [0, 0, 0])
    tr = gui.get('translation', [0, 0, 0])
    sc = gui.get('scale', [1, 1, 1])
    m = rot_matrix(*rot)
    k = size / 16.0
    polys = []
    for el in model['elements']:
        f, t = el['from'], el['to']
        er = el.get('rotation')
        for face, spec in el.get('faces', {}).items():
            corners = []
            for c in FACES[face]:
                p = [f[i] if c[i] == 0 else t[i] for i in range(3)]
                if er:
                    o = er.get('origin', [8, 8, 8])
                    q = [p[i] - o[i] for i in range(3)]
                    # one axis and an angle, or (newer models) an angle per axis, applied x, y, z
                    turns = [(er['axis'], er.get('angle', 0))] if 'axis' in er else \
                        [(ax, er.get(ax, 0)) for ax in ('x', 'y', 'z') if er.get(ax)]
                    for axis, angle in turns:
                        a = math.radians(angle)
                        ca, sa = math.cos(a), math.sin(a)
                        if axis == 'x':
                            q = [q[0], q[1] * ca - q[2] * sa, q[1] * sa + q[2] * ca]
                        elif axis == 'y':
                            q = [q[0] * ca + q[2] * sa, q[1], -q[0] * sa + q[2] * ca]
                        else:
                            q = [q[0] * ca - q[1] * sa, q[0] * sa + q[1] * ca, q[2]]
                    p = [q[i] + o[i] for i in range(3)]
                # centre, scale, rotate, translate — as the display transform does
                q = [(p[i] - 8) * sc[i] for i in range(3)]
                q = apply(m, q)
                q = [q[0] + tr[0], q[1] + tr[1], q[2] + tr[2]]
                corners.append(q)
            ref = resolve(model.get('textures', {}), spec.get('texture', ''))
            if not ref:
                continue
            uv = spec.get('uv') or default_uv(face, f, t)
            depth = sum(c[2] for c in corners) / 4
            normal = [0, 0, 0]
            ux = [corners[1][i] - corners[0][i] for i in range(3)]
            vy = [corners[3][i] - corners[0][i] for i in range(3)]
            normal = [ux[1] * vy[2] - ux[2] * vy[1], ux[2] * vy[0] - ux[0] * vy[2], ux[0] * vy[1] - ux[1] * vy[0]]
            if normal[2] < 0:
                continue
            polys.append((depth, corners, ref, uv, spec.get('rotation', 0), SHADE[face]))
    polys.sort(key=lambda p: p[0])
    big = 4
    W = size * big
    canvas = Image.new('RGBA', (W, W))
    for _, corners, ref, uv, uvrot, shade in polys:
        tex = texture(ref)
        tw, th = tex.size
        u0, v0, u1, v1 = [c * tw / 16.0 for c in uv[:2]] + [c * tw / 16.0 for c in uv[2:]]
        quad_uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        r = (uvrot // 90) % 4
        quad_uv = quad_uv[r:] + quad_uv[:r]
        pts = [((c[0] * k + size / 2) * big, (size / 2 - c[1] * k) * big) for c in corners]
        draw_quad(canvas, tex, pts, quad_uv, shade)
    return canvas.resize((size, size), Image.LANCZOS)


def draw_quad(canvas, tex, pts, uvs, shade):
    """Fills a projected quad by mapping each pixel back into the texture (bilinear interpolation of the quad)."""
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    x0, x1 = max(0, int(min(xs))), min(canvas.width - 1, int(max(xs)) + 1)
    y0, y1 = max(0, int(min(ys))), min(canvas.height - 1, int(max(ys)) + 1)
    (ax, ay), (bx, by), (cx, cy), (dx, dy) = pts
    src = tex.load()
    dst = canvas.load()
    tw, th = tex.size
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            st = inverse_bilinear(x + 0.5, y + 0.5, ax, ay, bx, by, cx, cy, dx, dy)
            if st is None:
                continue
            s, t = st
            u = (1 - t) * ((1 - s) * uvs[0][0] + s * uvs[1][0]) + t * ((1 - s) * uvs[3][0] + s * uvs[2][0])
            v = (1 - t) * ((1 - s) * uvs[0][1] + s * uvs[1][1]) + t * ((1 - s) * uvs[3][1] + s * uvs[2][1])
            ui = min(tw - 1, max(0, int(u)))
            vi = min(th - 1, max(0, int(v)))
            r, g, b, a = src[ui, vi]
            if a < 10:
                continue
            dst[x, y] = (int(r * shade), int(g * shade), int(b * shade), 255)


def inverse_bilinear(px, py, ax, ay, bx, by, cx, cy, dx, dy):
    # quad a(0,0) b(1,0) c(1,1) d(0,1)
    ex, ey = bx - ax, by - ay
    fx, fy = dx - ax, dy - ay
    gx, gy = ax - bx + cx - dx, ay - by + cy - dy
    hx, hy = px - ax, py - ay
    k2 = gx * fy - gy * fx
    k1 = ex * fy - ey * fx + hx * gy - hy * gx
    k0 = hx * ey - hy * ex
    if abs(k2) < 1e-9:
        if abs(k1) < 1e-9:
            return None
        v = -k0 / k1
    else:
        w = k1 * k1 - 4 * k0 * k2
        if w < 0:
            return None
        w = math.sqrt(w)
        v = (-k1 - w) / (2 * k2)
        if not (-0.001 <= v <= 1.001):
            v = (-k1 + w) / (2 * k2)
    denom_x = ex + gx * v
    denom_y = ey + gy * v
    if abs(denom_x) > abs(denom_y):
        if abs(denom_x) < 1e-9:
            return None
        u = (hx - fx * v) / denom_x
    else:
        if abs(denom_y) < 1e-9:
            return None
        u = (hy - fy * v) / denom_y
    if -0.001 <= u <= 1.001 and -0.001 <= v <= 1.001:
        return (u, v)
    return None


def render_icon(definition_ref, size=48):
    ref = item_model_ref(definition_ref)
    return render_model(load_model(ref), size)
