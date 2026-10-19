#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
st_recolor.py — 严格像素色系替换：核心部分浅蓝色系 → 灰白亮色系

处理 design/st/ 下的每一张 PNG（32x32 像素贴图）：
  仅将核心宝石区域的三阶浅蓝色系按精确 RGB 一一映射为灰白亮色系，
  其余颜色（紫灰蓝阴影、灰阶、高光、透明像素）与 alpha 通道严格保持不变。

严格性保证：
  - 使用精确颜色查找表（LUT），不做色相/明度近似匹配，不碰相近色；
  - 只重写 RGB，alpha 原样保留；
  - 替换前自动备份原图到 design/st_original_blue/；
  - 替换后逐图校验：源色归零、其余颜色计数不变。

用法：python design/st_recolor.py
"""

import os
import sys
from collections import Counter

from PIL import Image

# ---------------------------------------------------------------------------
# 严格色系映射表：源 RGB -> 目标 RGB（仅当像素 RGB 精确相等时才替换）
# 源色为 design/st 各图核心宝石区的浅蓝色系三阶；目标为灰白亮色系三阶，
# 保持明度层级（高光 > 主体 > 暗部），且整体比周边灰阶更亮。
# ---------------------------------------------------------------------------
RECOLOR_LUT = {
    (218, 255, 255): (246, 246, 248),   # 极浅青蓝（高光）-> 亮白
    (156, 211, 255): (214, 214, 220),   # 浅蓝（主体）    -> 亮灰白
    (125, 169, 210): (186, 186, 194),   # 蓝灰（暗部）    -> 灰白
}

DESIGN_DIR = os.path.dirname(os.path.abspath(__file__))
ST_DIR = os.path.join(DESIGN_DIR, "st")
BACKUP_DIR = os.path.join(DESIGN_DIR, "st_original_blue")


def recolor_image(src_path):
    """严格 LUT 替换单张图片，返回 (替换像素数, 替换前颜色计数, 替换后颜色计数)。"""
    im = Image.open(src_path).convert("RGBA")
    w, h = im.size
    px = im.load()

    before = Counter()
    after = Counter()
    replaced = 0

    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                before[("transparent",)] += 1
                continue
            before[(r, g, b)] += 1
            target = RECOLOR_LUT.get((r, g, b))
            if target is not None:
                px[x, y] = target + (a,)
                replaced += 1
                after[target] += 1
            else:
                after[(r, g, b)] += 1

    return im, replaced, before, after


def main():
    if not os.path.isdir(ST_DIR):
        sys.exit(f"错误：找不到目录 {ST_DIR}")

    pngs = sorted(f for f in os.listdir(ST_DIR) if f.lower().endswith(".png"))
    if not pngs:
        sys.exit(f"错误：{ST_DIR} 中没有 PNG 图片")

    os.makedirs(BACKUP_DIR, exist_ok=True)
    print(f"源目录 : {ST_DIR}")
    print(f"备份目录: {BACKUP_DIR}")
    print(f"严格映射表 ({len(RECOLOR_LUT)} 组):")
    for s, d in RECOLOR_LUT.items():
        print(f"  {s} -> {d}")
    print("-" * 70)

    all_ok = True
    for fn in pngs:
        src = os.path.join(ST_DIR, fn)

        # 1) 备份原图
        backup_path = os.path.join(BACKUP_DIR, fn)
        Image.open(src).save(backup_path)

        # 2) 严格替换
        im, replaced, before, after = recolor_image(src)

        # 3) 校验：替换后源色必须归零，其余颜色计数必须不变
        residual = sum(after[c] for c in RECOLOR_LUT if c in after)
        intact_ok = True
        for c, n in before.items():
            if c in RECOLOR_LUT:
                continue  # 源色被替换，允许消失
            if after[c] != n:
                intact_ok = False
        ok = (residual == 0) and intact_ok and \
             (sum(after.values()) == sum(before.values()))
        all_ok &= ok

        # 4) 写回原位置
        im.save(src)

        print(f"[{'OK' if ok else 'FAIL'}] {fn}  {im.size[0]}x{im.size[1]}  "
              f"替换像素 {replaced}")
        for s, d in RECOLOR_LUT.items():
            n = before[s]
            if n:
                print(f"      {s} -> {d} : {n} 像素")
        if not ok:
            print(f"      !! 校验失败：残留源色 {residual}，其余色 intact={intact_ok}")

    print("-" * 70)
    print("全部通过：所有源色已替换且其余像素严格不变" if all_ok
          else "存在校验失败项，请检查！")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
