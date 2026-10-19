#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
st_to_mst.py — 严格像素色系替换：design/st_original_blue → design/mst

对每张 32x32 PNG 按精确 RGB 查找表（LUT）逐像素替换，不做近似匹配：
  1. 边框灰白色系（紫灰描边 4 阶 + 灰白高光 3 阶，共 7 阶）
     → 深灰黑色系（纯灰 r=g=b 7 阶，保持明度层次，整体压暗）
  2. 核心浅蓝色系（3 阶）→ 灰白色系（3 阶，沿用 kst 版本配色，保证风格一致）
  3. 透明像素、alpha 通道严格保持不变
  4. 严格校验：任何非透明像素若未命中映射表 → 判失败（防止遗漏色）

输出到 design/mst/，文件名沿用 kst 惯例的容量名：
  655567 → 1k, 655568 → 4k, 655569 → 16k, 655570 → 64k, 655571 → 256k

用法：python design/st_to_mst.py
"""

import os
import sys
from collections import Counter

from PIL import Image

# ---------------------------------------------------------------------------
# 严格色系映射表：源 RGB -> 目标 RGB（仅当像素 RGB 精确相等时才替换）
#
# 边框源色（7 阶，按 ITU-R 601 亮度排序）：
#   亮度 0.259/0.314/0.435/0.561/0.696/0.802/0.949
#   线性重映射到深灰黑亮度区间 [0.05, 0.45]，色相归零（纯灰）。
# 核心源色（3 阶）：
#   目标沿用 kst 版本已确立的灰白亮色系 (246/214/186 阶)，与 kst 完全一致。
# ---------------------------------------------------------------------------
BORDER_LUT = {
    (65, 63, 84):   (13, 13, 13),     # 紫灰最暗（外描边）-> 近黑
    (77, 77, 103):  (21, 21, 21),     # 紫灰暗        -> 深黑
    (105, 109, 136): (39, 39, 39),    # 紫灰中        -> 深灰黑
    (135, 143, 165): (57, 57, 57),    # 紫灰亮        -> 深灰
    (173, 176, 196): (77, 77, 77),    # 灰蓝过渡      -> 深灰
    (203, 204, 212): (93, 93, 93),    # 浅灰          -> 中深灰
    (242, 242, 242): (115, 115, 115), # 边框高光白    -> 深灰黑最亮阶
}

CORE_LUT = {
    (218, 255, 255): (246, 246, 248),  # 极浅青蓝（高光）-> 亮白
    (156, 211, 255): (214, 214, 220),  # 浅蓝（主体）    -> 亮灰白
    (125, 169, 210): (186, 186, 194),  # 蓝灰（暗部）    -> 灰白
}

LUT = {}
LUT.update(BORDER_LUT)
LUT.update(CORE_LUT)

# 输出文件名：与 kst 目录既有惯例一致（容量名）
NAME_MAP = {
    "655567.png": "1k.png",
    "655568.png": "4k.png",
    "655569.png": "16k.png",
    "655570.png": "64k.png",
    "655571.png": "256k.png",
}

DESIGN_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(DESIGN_DIR, "st_original_blue")
OUT_DIR = os.path.join(DESIGN_DIR, "mst")


def recolor_image(src_path):
    """严格 LUT 替换单张图片，返回 (图像, 替换像素数, 替换前计数, 替换后计数, 未命中色集合)。"""
    im = Image.open(src_path).convert("RGBA")
    w, h = im.size
    px = im.load()

    before = Counter()
    after = Counter()
    replaced = 0
    unmapped = set()

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                before[("transparent",)] += 1
                after[("transparent",)] += 1
                continue
            before[(r, g, b)] += 1
            target = LUT.get((r, g, b))
            if target is None:
                unmapped.add((r, g, b, a))  # 严格模式：非透明像素必须全部命中映射表
                after[(r, g, b)] += 1
                continue
            px[x, y] = target + (a,)
            replaced += 1
            after[target] += 1

    return im, replaced, before, after, unmapped


def main():
    if not os.path.isdir(SRC_DIR):
        sys.exit(f"错误：找不到源目录 {SRC_DIR}")

    pngs = sorted(f for f in os.listdir(SRC_DIR) if f.lower().endswith(".png"))
    if not pngs:
        sys.exit(f"错误：{SRC_DIR} 中没有 PNG 图片")

    os.makedirs(OUT_DIR, exist_ok=True)
    print(f"源目录 : {SRC_DIR}")
    print(f"输出目录: {OUT_DIR}")
    print(f"严格映射表 ({len(LUT)} 组，边框 {len(BORDER_LUT)} + 核心 {len(CORE_LUT)}):")
    for s, d in LUT.items():
        print(f"  {s} -> {d}")
    print("-" * 70)

    all_ok = True
    for fn in pngs:
        src = os.path.join(SRC_DIR, fn)
        out_fn = NAME_MAP.get(fn, fn)
        dst = os.path.join(OUT_DIR, out_fn)

        im, replaced, before, after, unmapped = recolor_image(src)

        # 校验 1：所有源色归零
        residual = sum(after[c] for c in LUT if c in after)
        # 校验 2：无未命中颜色
        strict_ok = not unmapped
        # 校验 3：alpha 与透明像素不变、总像素数不变
        total_ok = (after[("transparent",)] == before[("transparent",)]
                    and sum(after.values()) == sum(before.values()))
        ok = residual == 0 and strict_ok and total_ok
        all_ok &= ok

        if ok:
            im.save(dst)
            print(f"[OK] {fn} -> {out_fn}  替换像素 {replaced}")
        else:
            print(f"[FAIL] {fn}  未写入输出")
            if residual:
                print(f"      !! 替换后仍有源色残留 x{residual}")
            if unmapped:
                print(f"      !! 未命中映射表的非透明色（含 alpha）: {sorted(unmapped)}")
            if not total_ok:
                print(f"      !! 像素总数/透明像素数不一致")
        for s, d in LUT.items():
            n = before[s]
            if n:
                print(f"      {s} -> {d} : {n} 像素")

    print("-" * 70)
    print("全部通过：边框→深灰黑、核心→灰白，其余像素严格不变" if all_ok
          else "存在校验失败项，未写入对应输出，请检查！")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
