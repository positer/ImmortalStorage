# 灵剑/仙墟锻灵剑/一气归元剑 — Beautified Sword Family (design-only)

Design-only SVG + BlockBench deliverable for the three Spirit Sword-line items.
It does **not** modify the mod resource tree; it is the editable art source and
a BlockBench `java_item` preview model.

| Item | 中文 | Rarity | Purpose (mod) |
| --- | --- | --- | --- |
| `spirit_sword` | 灵剑 | UNCOMMON | growth sword: stage damage, tempering/quenching, Yuan payment |
| `immortal_ruin_forged_spirit_sword` | 仙墟锻灵剑 | EPIC | teleport + 2 s absolute restraint, portal particles |
| `one_qi_returning_origin_sword` | 一气归元剑 | EPIC | three-phase piercing beam weapon (BOW use, tip points upper-left) |

## Unified family style

All three swords share the **vanilla Minecraft sword silhouette**, transcribed
pixel-for-pixel from the vanilla `diamond_sword` 16x16 texture.  The silhouette
is kept faithful to vanilla — a 5-stop shaded blade, a **small guard bar at the
blade root**, a short tapered grip, and a pommel — so the icons read as native
Minecraft items.

**Detail is minimal and meaningful** (no random glyphs or wrap bands):

- Blade: 5 shading stops (`0` darkest outline -> `1` bright -> `2` mid ->
  `3` pale -> `4` black edge) give the metal a curved, lit surface.
- Guard: `g` dark gold / `G` bright gold, plus a single **guard gem** (`K`)
  per sword.
- One-Qi only: a **beam muzzle** at the tip (see below).

Per-sword palettes tied to rarity/purpose:

- **spirit_sword** — steel-blue blade, bronze guard, dark grip (growth,
  balanced). Tip points upper-right.
- **immortal_ruin_forged** — pale indigo/violet blade, violet guard (arcane
  teleport/restraint) plus a single arcane glint on the blade. Tip points
  upper-right.
- **one_qi_returning_origin** — teal/cyan blade, cyan guard, gold grip (beam
  weapon). **Horizontally mirrored** so the tip points **upper-left**, like a
  bow being aimed.

## 一气归元剑 beam binding (光束发射位置与方向)

The One-Qi sword is held like a bow (`UseAnim.BOW`), so its texture is
**horizontally flipped** (`FLIP` in `naming.py`) to point the blade tip
**upper-left**.  The beam is fired in game from `player.getEyePosition()` along
`getLookAngle().normalize()` (`OneQiReturningOriginSwordItem.fireBeam`), so the
upper-left tip is the aim direction; a **white muzzle core** (`(0,1)`,`(1,0)`)
plus a **cyan-white flare** (`(2,1)`,`(3,2)`) marks exactly where the beam
exits the sword.  The icon keeps a neutral "ready" muzzle; the three in-game
beam stages (thinner white -> thicker -> blue-core white-edge) are rendered at
runtime by the mod.

## Files

```text
design/swords/
├── naming.py                        shared item ids / sizes / palettes / accents
├── generate_svgs.py                 emits svg/<sword>.svg from the base silhouette
├── render_textures.py               rasterizes svg -> textures/<sword>.png (16/32)
├── build_bbmodel.py                 generates spirit_swords.bbmodel (java_item)
├── preview_family.py                side-by-side family comparison preview
├── spirit_swords.bbmodel            BlockBench v5 java_item model (all three swords)
├── svg/                             editable pixel-art SVG sources
├── textures/                        16x16 / 32x32 PNG item icons
└── preview/
    ├── spirit_swords_family.png     family comparison
    └── <sword>_8x.png               8x upscaled preview tiles
```

## Regenerate

```powershell
python generate_svgs.py
python render_textures.py
python build_bbmodel.py
python preview_family.py
```

Requires `resvg-py` (SVG -> PNG) and Pillow, same as the Advanced Xianqiao
Interface pipeline.

## Opening in BlockBench

Double-click `spirit_swords.bbmodel` (BlockBench v5, `java_item`).  It contains
three plane elements (16/32 px) with embedded base64 textures and the standard
Minecraft handheld display transforms, so you can review the family together.

## Later mod integration

The item models already point at `immortalstorage:item/<name>` (parent
`minecraft:item/handheld`, `layer0`).  To promote the art, copy
`textures/<name>.png` over
`project/neoforge-1.21.1-mdk/src/main/resources/assets/immortalstorage/textures/item/<name>.png`
for the three swords.  No model JSON changes are required.
