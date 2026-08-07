"""Rasterize the sword SVG sources into 16x16 / 32x32 PNG textures.

Uses resvg-py (bundled native resvg) for pixel-exact SVG->PNG, matching the
interface pipeline.  Also writes 8x upscaled preview tiles for visual QA.

Usage:
    python render_textures.py
"""

from __future__ import annotations

from pathlib import Path

import resvg_py
from PIL import Image

import naming

HERE = Path(__file__).resolve().parent
SVG_DIR = HERE / "svg"
TEX_DIR = HERE / "textures"
PREVIEW_DIR = HERE / "preview"

UPSCALE = 8


def render_svg(svg_path: Path, out_png: Path, px: int) -> None:
    png_bytes = resvg_py.svg_to_bytes(
        svg_path=str(svg_path),
        width=px,
        height=px,
        shape_rendering="crisp_edges",
    )
    img = Image.open(__import__("io").BytesIO(png_bytes)).convert("RGBA")
    if img.size != (px, px):
        img = img.resize((px, px), Image.Resampling.NEAREST)
    out_png.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_png)

    preview = img.resize((px * UPSCALE, px * UPSCALE), Image.Resampling.NEAREST)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    preview.save(PREVIEW_DIR / f"{svg_path.stem}_{UPSCALE}x.png")
    return img


def main() -> None:
    for name in naming.SWORDS:
        px = naming.SIZES[name]
        img = render_svg(SVG_DIR / f"{name}.svg", TEX_DIR / f"{name}.png", px)
        opaque = sum(1 for p in img.getdata() if p[3] > 0)
        print(f"ok textures/{name}.png {img.size} opaque={opaque}/{px*px}")


if __name__ == "__main__":
    main()
