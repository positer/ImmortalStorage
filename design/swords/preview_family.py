"""Build a side-by-side family comparison preview of the three swords.

Lays the rendered PNGs (native size) on one canvas with captions so the
shared silhouette + per-sword theme can be reviewed at a glance.

Usage:
    python preview_family.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

import naming

HERE = Path(__file__).resolve().parent
TEX_DIR = HERE / "textures"
PREVIEW_DIR = HERE / "preview"
OUT = PREVIEW_DIR / "spirit_swords_family.png"

SCALE = 8  # upscale each icon for readability


def main() -> None:
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    tiles = []
    for name in naming.SWORDS:
        img = Image.open(TEX_DIR / f"{name}.png").convert("RGBA")
        tiles.append((name, img))

    gap = 24
    margin = 20
    label_h = 28
    max_w = max(img.width for _, img in tiles)
    max_h = max(img.height for _, img in tiles)
    total_w = margin * 2 + len(tiles) * max_w * SCALE + gap * (len(tiles) - 1)
    total_h = margin * 2 + label_h + max_h * SCALE

    canvas = Image.new("RGBA", (total_w, total_h), (24, 28, 38, 255))
    draw = ImageDraw.Draw(canvas)

    x = margin
    for name, img in tiles:
        up = img.resize((img.width * SCALE, img.height * SCALE), Image.Resampling.NEAREST)
        canvas.alpha_composite(up, (x, margin))
        draw.text((x, margin + up.height + 6), name, fill=(235, 240, 250, 255))
        x += up.width + gap

    canvas.save(OUT)
    print(f"wrote {OUT} ({canvas.size})")


if __name__ == "__main__":
    main()
