"""Generate a BlockBench v5 java_item .bbmodel for the three Spirit Sword-line
items (灵剑 / 仙墟锻灵剑 / 一气归元剑).

Each sword is a single 16x16 (or 32x32) plane element textured with its own
embedded PNG, carrying the standard Minecraft handheld display transforms so
it previews like a real held item.  All three share one .bbmodel so the family
can be reviewed side by side.

Beam binding (一气归元剑): the One-Qi blade tip sits in the upper-right and
carries a white muzzle-flare core.  In game the beam fires from the player's
eye position along getLookAngle() (OneQiReturningOriginSwordItem.fireBeam), so
the handheld icon's tip direction matches the aim direction.

Usage:
    python build_bbmodel.py [--out path/to/spirit_swords.bbmodel]
"""

from __future__ import annotations

import argparse
import base64
import json
import uuid
from pathlib import Path

import naming

HERE = Path(__file__).resolve().parent
DEFAULT_OUT = HERE / "spirit_swords.bbmodel"
TEX_DIR = HERE / "textures"

# Standard Minecraft handheld display transforms (item/generated).
HANDHELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 55], "translation": [0, 4.25, 2.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, 55], "translation": [0, 4.25, 2.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}


def _png_data_uri(name: str) -> str:
    png = TEX_DIR / f"{name}.png"
    return "data:image/png;base64," + base64.b64encode(png.read_bytes()).decode("ascii")


def build_elements() -> list:
    elements = []
    for i, name in enumerate(naming.SWORDS):
        size = naming.SIZES[name]
        # Plane facing the viewer (north) with the texture on front/back.
        elements.append({
            "name": name,
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "render_order": "default",
            "allow_mirror_modeling": True,
            "type": "cube",
            "from": [0, 0, 0],
            "to": [size, size, 1],
            "autouv": 0,
            "color": 0,
            "origin": [size / 2, size / 2, 0],
            "faces": {
                "north": {"uv": [0, 0, size, size], "texture": i},
                "south": {"uv": [0, 0, size, size], "texture": i},
                "east": {"uv": [0, 0, 0, 0], "texture": None},
                "west": {"uv": [0, 0, 0, 0], "texture": None},
                "up": {"uv": [0, 0, 0, 0], "texture": None},
                "down": {"uv": [0, 0, 0, 0], "texture": None},
            },
            "uuid": str(uuid.uuid4()),
        })
    return elements


def build_textures() -> list:
    textures = []
    for i, name in enumerate(naming.SWORDS):
        size = naming.SIZES[name]
        relpath = f"textures/{name}.png"
        textures.append({
            "path": relpath,
            "name": name,
            "folder": "textures",
            "namespace": "",
            "id": str(i),
            "uv": [0, 0],
            "source": _png_data_uri(name),
            "width": size,
            "height": size,
            "relpath": relpath,
            "uuid": str(uuid.uuid4()),
        })
    return textures


def build_outliner() -> list:
    """v5 outliner: one root group per sword referencing its element uuid."""
    outliner = []
    for el in build_elements():
        outliner.append({"uuid": el["uuid"], "isOpen": True, "children": []})
    return outliner


def build_model() -> dict:
    elements = build_elements()
    textures = build_textures()
    outliner = []
    for el in elements:
        outliner.append({"uuid": el["uuid"], "isOpen": True, "children": []})
    return {
        "meta": {
            "format_version": "5.0.0",
            "model_format": "java_item",
            "box_uv": False,
        },
        "name": "spirit_swords",
        "model_identifier": naming.NAMESPACE + ":spirit_swords",
        "visible_box": [0, 0, 0, 32, 32, 16],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "unidentified": {},
        "duplicate_uuid": [],
        "elements": elements,
        "outliner": outliner,
        "resolution": {"width": 32, "height": 32},
        "textures": textures,
        "parent": "",
        "description": {
            "identifier": naming.NAMESPACE + ":spirit_swords",
            "name": "spirit_swords",
            "visible_box": [0, 0, 0, 32, 32, 16],
            "variable_placeholders": "",
            "variable_placeholder_buttons": [],
        },
        "display": HANDHELD_DISPLAY,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    args = parser.parse_args()

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        json.dumps(build_model(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
