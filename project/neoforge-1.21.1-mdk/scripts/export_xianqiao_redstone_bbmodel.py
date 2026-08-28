#!/usr/bin/env python3
"""Deterministically export the Xianqiao redstone interface from its bbmodel."""

import argparse
import base64
import json
from pathlib import Path


def compact_json(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("bbmodel", type=Path)
    parser.add_argument("resources", type=Path)
    args = parser.parse_args()

    source = json.loads(args.bbmodel.read_text(encoding="utf-8-sig"))
    resolution = source["resolution"]
    width = int(resolution["width"])
    height = int(resolution["height"])
    if width <= 0 or height <= 0 or width % 16 or height % 16:
        raise ValueError("bbmodel resolution must be a positive multiple of 16")
    if len(source["elements"]) != 1:
        raise ValueError("expected the supplied one-cube Blockbench model")

    element = source["elements"][0]
    face_scale_x = 16.0 / width
    face_scale_y = 16.0 / height
    faces = {}
    for direction, face in element["faces"].items():
        uv = face["uv"]
        normalized = [
            uv[0] * face_scale_x, uv[1] * face_scale_y,
            uv[2] * face_scale_x, uv[3] * face_scale_y,
        ]
        normalized = [int(value) if value.is_integer() else value for value in normalized]
        encoded = {"uv": normalized, "texture": "#all"}
        if face.get("rotation"):
            encoded["rotation"] = face["rotation"]
        faces[direction] = encoded

    block_models = args.resources / "assets/immortalstorage/models/block"
    block_textures = args.resources / "assets/immortalstorage/textures/block"
    block_models.mkdir(parents=True, exist_ok=True)
    block_textures.mkdir(parents=True, exist_ok=True)
    texture_by_name = {entry["name"]: entry for entry in source["textures"]}
    # The supplied bbmodel's two embedded texture labels are visually reversed:
    # its "activated" image is the unpowered appearance and vice versa. Keep
    # runtime resource names aligned with the actual block state so blockstate,
    # item model, redstone output and light emission retain one clear meaning.
    source_texture_by_runtime_state = {
        "inactivated": "activated",
        "activated": "inactivated",
    }
    for state in ("inactivated", "activated"):
        source_texture_name = source_texture_by_runtime_state[state]
        texture = texture_by_name[source_texture_name]
        prefix = "data:image/png;base64,"
        if not texture["source"].startswith(prefix):
            raise ValueError(f"{source_texture_name} is not an embedded PNG")
        (block_textures / f"xianqiao_redstone_interface_{state}.png").write_bytes(
            base64.b64decode(texture["source"][len(prefix):]))
        model = {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": f"immortalstorage:block/xianqiao_redstone_interface_{state}",
                "all": f"immortalstorage:block/xianqiao_redstone_interface_{state}",
            },
            "elements": [{"from": element["from"], "to": element["to"], "faces": faces}],
        }
        (block_models / f"xianqiao_redstone_interface_{state}.json").write_text(
            compact_json(model), encoding="utf-8")

    blockstates = args.resources / "assets/immortalstorage/blockstates"
    item_models = args.resources / "assets/immortalstorage/models/item"
    blockstates.mkdir(parents=True, exist_ok=True)
    item_models.mkdir(parents=True, exist_ok=True)
    (blockstates / "xianqiao_redstone_interface.json").write_text(compact_json({
        "variants": {
            "activated=false": {"model": "immortalstorage:block/xianqiao_redstone_interface_inactivated"},
            "activated=true": {"model": "immortalstorage:block/xianqiao_redstone_interface_activated"},
        }
    }), encoding="utf-8")
    (item_models / "xianqiao_redstone_interface.json").write_text(compact_json({
        "parent": "immortalstorage:block/xianqiao_redstone_interface_inactivated"
    }), encoding="utf-8")


if __name__ == "__main__":
    main()
