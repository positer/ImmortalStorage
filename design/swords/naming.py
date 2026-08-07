"""Shared naming for the three Spirit Sword-line items.

Design-only deliverable for:
  - spirit_sword                      (灵剑, UNCOMMON growth sword)
  - immortal_ruin_forged_spirit_sword (仙墟锻灵剑, EPIC teleport/restraint)
  - one_qi_returning_origin_sword     (一气归元剑, EPIC three-phase beam)

STYLE: vanilla (原版) 16x16 pixel style, faithful to the real Minecraft
diamond_sword.  The silhouette is transcribed pixel-for-pixel from the vanilla
texture: blade tip upper-right with 5-stop shading, a SMALL guard bar at the
blade root (3x3), a short tapered grip, and a pommel.  Detail is kept minimal
and meaningful — only a single guard gem per sword and, for the One-Qi, the
beam muzzle — never random glyphs or wrap bands, so the icons stay readable.
"""

# Canonical item ids (must match the mod registrations).
SWORDS = [
    "spirit_sword",
    "immortal_ruin_forged_spirit_sword",
    "one_qi_returning_origin_sword",
]

# Canvas size per sword: all 16x16 in vanilla style.
SIZES = {name: 16 for name in SWORDS}

# Resource namespace for later mod integration.
NAMESPACE = "immortalstorage"

# Horizontal flip (mirror) for a sword so its blade tip points LEFT instead of
# right.  The One-Qi Returning Origin Sword is held like a bow (UseAnim.BOW)
# aiming forward, so its tip points upper-LEFT and the beam leaves the upper
# left tip.  FLIP is applied to the full 16x16 canvas (x -> 15-x), which also
# mirrors the DETAIL pixels (defined in final space below).
FLIP = {
    "one_qi_returning_origin_sword": True,
}

# Vanilla diamond_sword silhouette (16x16), transcribed pixel-for-pixel from
# the vanilla texture.  Symbols are material stops:
#   Blade: 0 darkest outline | 1 bright | 2 mid | 3 pale highlight | 4 black edge
#   Guard: g dark gold | G bright gold
#   Grip:  h grip shade
VANILLA_ROWS = [
    ".............000",
    "............0114",
    "...........01214",
    "..........01214.",
    ".........01234..",
    "........01234...",
    "..00...03234....",
    "..0h0.03234.....",
    "...0243234......",
    "...022334.......",
    "....0204.......",
    "...gG4004......",
    "..gG400004......",
    "00G04....44......",
    "0h4.............",
    "444.............",
]

# Per-sword meaningful details (x, y, symbol) in FINAL canvas space
# (post-FLIP).  Kept to one guard gem; the One-Qi adds its beam muzzle.
DETAIL = {
    "spirit_sword": [
        (4, 11, "K"),  # guard gem (white)
    ],
    "immortal_ruin_forged_spirit_sword": [
        (4, 11, "K"),  # guard gem (white)
        (8, 7, "L"),   # single arcane glint on the upper blade
    ],
    "one_qi_returning_origin_sword": [
        (11, 11, "K"),  # guard gem (white), mirrored to the right side
        (0, 1, "W"),    # beam muzzle core at the upper-left tip
        (1, 0, "W"),
        (2, 1, "L"),    # cyan-white flare beside the muzzle
    ],
}

# Per-sword theme palette (symbol -> hex).  Symbol set used by the shared
# silhouette: 0 1 2 3 4 (blade stops) g G (guard) h (grip) K (gem)
# L (glint/flare) W (muzzle white).
PALETTES = {
    "spirit_sword": {
        # steel-blue blade, violet guard, bronze grip
        "0": "#1A2B3C", "1": "#9FB8D6", "2": "#6E87A3", "3": "#C6D8EE",
        "4": "#31455C", "g": "#7A5C2E", "G": "#D9AE54", "h": "#5A4329",
        "K": "#FFFFFF", "L": "#9DD9FF", "W": "#FFFFFF",
    },
    "immortal_ruin_forged_spirit_sword": {
        # pale indigo/violet blade, violet guard, deep grip
        "0": "#232359", "1": "#A6A6FF", "2": "#6A6AE0", "3": "#C9C9FF",
        "4": "#3A3A8A", "g": "#5151BE", "G": "#B6B7FF", "h": "#3B4A8A",
        "K": "#FFFFFF", "L": "#D0C8FF", "W": "#FFFFFF",
    },
    "one_qi_returning_origin_sword": {
        # teal/cyan blade, blue guard, gold grip
        "0": "#0B3A2E", "1": "#4EC9F0", "2": "#159FE8", "3": "#7FE4FA",
        "4": "#0E5A4A", "g": "#1E7A8A", "G": "#55F2DA", "h": "#5F4A1E",
        "K": "#E9FFFF", "L": "#55F2DA", "W": "#FFFFFF",
    },
}

# Beam-emission design notes for One-Qi Returning Origin Sword (一气归元剑).
# In-game the beam is fired from player.getEyePosition() along
# getLookAngle().normalize() (OneQiReturningOriginSwordItem.fireBeam).  The
# sword is held like a bow (UseAnim.BOW) aiming forward, so its blade tip
# points upper-LEFT (achieved by FLIP) and the beam leaves the upper-left tip.
# The DETAIL pixels place a white muzzle core at the tip plus a cyan-white
# flare, marking the beam origin and direction on the icon.  The three runtime
# stages (thin white -> thicker -> blue-core white-edge) are rendered by the
# mod, not baked into the icon.
BEAM_MUZZLE_TIP = (0, 1)  # 16-space (post-FLIP): white core at the tip.
