"""Generate pixel-art SVG sources for the three Spirit Sword-line items.

Each sword is the vanilla 16x16 sword silhouette (VANILLA_ROWS in naming.py)
recolored from its theme palette, with minimal EXTRA accent pixels to hint
rarity/theme and the One-Qi beam muzzle.

The SVG is the editable design source; render_textures.py rasterizes these
files into 16x16 PNGs with resvg.

Usage:
    python generate_svgs.py
"""

from __future__ import annotations

from pathlib import Path

import naming

HERE = Path(__file__).resolve().parent
SVG_DIR = HERE / "svg"


def build_svg(name: str) -> str:
    size = naming.SIZES[name]
    palette = naming.PALETTES[name]
    flip = naming.FLIP.get(name, False)

    cells: list[tuple[int, int, str]] = []
    for y, row in enumerate(naming.VANILLA_ROWS):
        for x, symbol in enumerate(row):
            if symbol != ".":
                cells.append((x, y, symbol))
    # One-Qi is horizontally mirrored so its tip points upper-left.  The base
    # silhouette lives in pre-flip (vanilla) space and is flipped here; DETAIL
    # coordinates are already defined in final space, so they are NOT flipped.
    if flip:
        cells = [(size - 1 - x, y, symbol) for x, y, symbol in cells]
    for x, y, symbol in naming.DETAIL.get(name, []):
        cells.append((x, y, symbol))

    rects = []
    for x, y, symbol in cells:
        rects.append(
            f'    <rect x="{x}" y="{y}" width="1" height="1" '
            f'fill="{palette[symbol]}"/>'
        )

    title = name.replace("_", " ")
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {size} {size}" shape-rendering="crispEdges">\n'
        f'  <title>{title}</title>\n'
        f'  <desc>Vanilla-style sword silhouette recolored for {name}.</desc>\n'
        + "\n".join(rects)
        + "\n</svg>\n"
    )


def main() -> None:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    for name in naming.SWORDS:
        out = SVG_DIR / f"{name}.svg"
        out.write_text(build_svg(name), encoding="utf-8")
        print(f"wrote {out}")


if __name__ == "__main__":
    main()
