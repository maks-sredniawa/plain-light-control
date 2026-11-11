"""The mark: a light bulb with a round hole for its glass, the single definition of this app's identity.

Both generators import it, so the launcher icon, the notification icon and the feature graphic
cannot drift into being different marks. Provenance: `art/README.md`.

The geometry is two subpaths: the shape, and the hole punched out of it. Coordinates are in
arbitrary units around an origin at the mark's centre; callers fit them to their own canvas with
`fit()`. y grows downward, as it does in both SVG and PIL.
"""

import re
from dataclasses import dataclass

# The only copy of the icon's colours: make-launcher-icon.py writes ic_launcher_background.xml from GROUND.
GROUND = (0xF9, 0xBE, 0x11)
MARK_INK = (0x00, 0x36, 0x3C)


@dataclass(frozen=True)
class Subpath:
    """One closed contour, as cubic path data.

    `d` is always wound CLOCKWISE on screen (positive shoelace area with y downward). A hole is
    stored the same way and reversed at read time by `nodes()`, so it is subtracted by the non-zero
    fill rule that both SVG and VectorDrawable use by default. That is what makes the hole a real
    hole rather than a shape painted in the background colour — and it is why it survives the
    monochrome layer, where Android tints everything one flat colour and a painted hole would vanish.
    """

    d: str
    hole: bool = False


MARK = (
    "M-33.46,21.16"
        "C-48.84,7.31 -54.1,-14.59 -46.68,-33.92C-39.26,-53.24 -20.7,-66 -0,-66"
        "C20.7,-66 39.26,-53.24 46.68,-33.92C54.1,-14.59 48.84,7.31 33.46,21.16"
        "C25.28,28.52 25,26.16 25,36.16C25,44.44 25,52.72 25,61"
        "C25,65.97 20.97,70 16,70C5.33,70 -5.33,70 -16,70"
        "C-20.97,70 -25,65.97 -25,61C-25,52.72 -25,44.44 -25,36.16"
        "C-25,26.16 -25.28,28.52 -33.46,21.16"
        "Z"
)

HOLE = (
    "M29,-16"
        "C29,0.02 16.02,13 0,13C-16.02,13 -29,0.02 -29,-16"
        "C-29,-32.02 -16.02,-45 -0,-45C16.02,-45 29,-32.02 29,-16"
        "Z"
)

PARTS = (
    Subpath(MARK),
    Subpath(HOLE, hole=True),
)

_NUM = re.compile(r"-?\d*\.?\d+(?:e-?\d+)?")


def _points(d):
    """Path data -> the flat control-point list: 1 start point plus 3 per cubic segment."""
    pts = []
    for chunk in re.finditer(r"[MC]([^MCZz]*)", d):
        nums = [float(v) for v in _NUM.findall(chunk.group(1))]
        pts += list(zip(nums[0::2], nums[1::2]))
    return pts


def nodes(e, tf=lambda p: p):
    """Control points, caller-transformed, wound for fill or hole."""
    pts = _points(e.d)
    if e.hole:
        pts.reverse()
    return [tf(p) for p in pts]


def path_data(e, tf=lambda p: p, precision=2):
    """SVG/VectorDrawable path data for one subpath."""
    n = nodes(e, tf)

    def f(p):
        return f"{round(p[0], precision):g},{round(p[1], precision):g}"

    out = [f"M{f(n[0])}"]
    for i in range(1, len(n), 3):
        out.append("C" + " ".join(f(n[i + j]) for j in range(3)))
    return "".join(out) + "Z"


def outline(e, tf=lambda p: p, steps=32):
    """The same subpath flattened to a polygon, for renderers that can't draw curves."""
    n = nodes(e, tf)
    pts = []
    for i in range(1, len(n), 3):
        (x0, y0), (x1, y1), (x2, y2), (x3, y3) = n[i - 1], n[i], n[i + 1], n[i + 2]
        for s in range(steps):
            t = s / steps
            u = 1 - t
            pts.append((
                u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3,
            ))
    return pts


def bounds(parts=PARTS):
    """Bounding box of the silhouette. Holes are ignored — a hole cannot extend the mark."""
    xs, ys = [], []
    for e in parts:
        if e.hole:
            continue
        for x, y in outline(e):
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(canvas, art_size, parts=PARTS):
    """A transform placing the mark centred on a square canvas at a given longest-side size.

    Returns a function mapping mark coordinates to canvas coordinates.
    """
    x0, y0, x1, y1 = bounds(parts)
    scale = art_size / max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return lambda p: (canvas / 2 + (p[0] - cx) * scale, canvas / 2 + (p[1] - cy) * scale)
