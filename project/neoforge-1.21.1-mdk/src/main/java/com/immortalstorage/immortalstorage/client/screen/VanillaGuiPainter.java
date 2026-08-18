package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;

final class VanillaGuiPainter {
    private static final ResourceLocation VANILLA_SLOT =
            ResourceLocation.withDefaultNamespace("container/slot");
    private static final ResourceLocation CRAFTING_TABLE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final ResourceLocation FURNACE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final ResourceLocation FURNACE_BURN_PROGRESS =
            ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation IMMORTAL_FURNACE_LIT_PROGRESS =
            ResourceLocation.fromNamespaceAndPath(
                    "immortalstorage", "container/immortal_furnace/lit_progress");
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_2 = 0xFFD8D8D8;
    private static final int PANEL_3 = 0xFF8B8B8B;
    private static final int PANEL_4 = 0xFFB7B7B7;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int MID = 0xFF8B8B8B;
    private static final int DARK = 0xFF373737;
    private static final int BLACK = 0xFF111111;
    private static final int DISABLED = 0xFF787878;
    private static final int CYAN = 0xFF63B7BE;
    private static final int CYAN_DARK = 0xFF2F7178;
    private static final int GREEN = 0xFF6EAD62;
    private static final int GREEN_DARK = 0xFF3D7138;
    private static final int RED = 0xFFC43D32;
    private static final int GOLD = 0xFFD4A443;

    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        chamferFill(g, x, y, w, h, PANEL);
        g.hLine(x + 1, x + w - 2, y, LIGHT);
        g.vLine(x, y + 1, y + h - 2, LIGHT);
        g.hLine(x + 1, x + w - 2, y + h - 1, DARK);
        g.vLine(x + w - 1, y + 1, y + h - 2, DARK);
        g.hLine(x + 1, x + w - 2, y + 1, PANEL_2);
        g.vLine(x + 1, y + 1, y + h - 2, PANEL_2);
        g.hLine(x + 1, x + w - 2, y + h - 2, MID);
        g.vLine(x + w - 2, y + 1, y + h - 2, MID);
    }

    static void sophisticatedBackpackPanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        panel(g, x, y, w, h);

        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);
        g.fill(x + 2, y + 2, x + w - 2, y + 18, PANEL_2);
        g.fill(x + 4, y + 4, x + w - 4, y + 7, 0x66FFFFFF);
        g.hLine(x + 3, x + w - 4, y + 18, MID);
        g.fill(x + 8, y + 19, x + 170, y + 110, PANEL_4);
        g.hLine(x + 8, x + 169, y + 19, LIGHT);
        g.vLine(x + 8, y + 19, y + 109, LIGHT);
        g.hLine(x + 8, x + 169, y + 110, MID);
        g.vLine(x + 170, y + 19, y + 110, MID);

        g.fill(x + 171, y + 19, x + w - 12, y + 110, PANEL_3);
        g.hLine(x + 171, x + w - 13, y + 19, MID);
        g.vLine(x + 171, y + 19, y + 110, MID);
        g.hLine(x + 171, x + w - 13, y + 110, LIGHT);
        g.vLine(x + w - 12, y + 19, y + 110, LIGHT);

        g.fill(x + 2, y + 120, x + w - 2, y + 134, PANEL_2);
        g.hLine(x + 3, x + w - 4, y + 119, LIGHT);
        g.hLine(x + 3, x + w - 4, y + 134, MID);
        g.fill(x + 9, y + 134, x + 169, y + 136, DARK);
        g.fill(x + 9, y + 192, x + 169, y + 194, DARK);
        lowerStatusPanel(g, x + 172, y + 136, w - 184, h - 148, xianqiao);

        leftUpgradeRail(g, x - 27, y + 1, xianqiao);
        topToolButtons(g, x + 173, y + 5, xianqiao);
        capacityMeter(g, x + w - 55, y + 5, 44, 9, accent, accentDark);
    }

    static void beyondStoragePanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        panel(g, x, y, w, h);

        vanillaInset(g, x + 6, y + 16, 166, 96);
        vanillaDivider(g, x + 7, y + 121, 162);
        vanillaInset(g, x + 6, y + 136, 166, 80);
    }

    static void networkTerminalPanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        panel(g, x, y, w, h);
        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);

        g.fill(x + 2, y + 2, x + w - 2, y + 21, PANEL_2);
        g.fill(x + 5, y + 5, x + w - 5, y + 8, 0x66FFFFFF);
        g.hLine(x + 3, x + w - 4, y + 21, MID);

        terminalSearch(g, x + 75, y + 6, 108, 11, accent);
        capacityMeter(g, x + w - 56, y + 6, 44, 9, accent, accentDark);

        vanillaInset(g, x + 24, y + 24, 168, 94);
        scrollbar(g, x + 197, y + 27, 8, 87, accent);
        vanillaDivider(g, x + 24, y + 122, 170);
        vanillaInset(g, x + 40, y + 126, 154, 58);
        moduleDockHints(g, x + 46, y + 134, 138, 42, xianqiao);
        vanillaDivider(g, x + 24, y + 187, 170);
        vanillaInset(g, x + 24, y + 190, 168, 74);
    }

    static void networkInterfacePanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        panel(g, x, y, w, h);
        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);

        g.fill(x + 2, y + 2, x + w - 2, y + 22, PANEL_2);
        g.fill(x + 5, y + 5, x + w - 5, y + 8, 0x66FFFFFF);
        g.hLine(x + 3, x + w - 4, y + 22, MID);
        capacityMeter(g, x + 142, y + 7, 42, 8, accent, accentDark);
        smallButton(g, x + 207, y + 15, accent);

        vanillaInset(g, x + 24, y + 39, 168, 94);
        interfaceTrace(g, x + 197, y + 42, 14, 87, xianqiao);
        vanillaDivider(g, x + 24, y + 142, 170);
        vanillaInset(g, x + 24, y + 150, 168, 74);
    }

    static void storageOnlyPanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        panel(g, x, y, w, h);
        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);

        g.fill(x + 2, y + 2, x + w - 2, y + 22, PANEL_2);
        g.fill(x + 5, y + 5, x + w - 5, y + 8, 0x66FFFFFF);
        g.hLine(x + 3, x + w - 4, y + 22, MID);
        capacityMeter(g, x + w - 64, y + 7, 48, 8, accent, accentDark);

        vanillaInset(g, x + 22, y + 38, 168, 96);
        scrollbar(g, x + 178, y + 42, 8, 86, accent);
        vanillaDivider(g, x + 22, y + 142, 168);
        vanillaInset(g, x + 22, y + h - 84, 168, 74);
    }

    static void terminalPanel(GuiGraphics g, int x, int y, int w, int h, int rows, int totalRows,
                              boolean workspaceExpanded, boolean craftingVisible,
                              boolean xianqiao, float scrollFraction) {
        panel(g, x, y, w, h);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, PANEL_2);
        g.hLine(x + 2, x + w - 3, y + 16, MID);
        terminalSearch(g, x + TerminalLayout.SEARCH_X, y + TerminalLayout.SEARCH_Y,
                TerminalLayout.SEARCH_WIDTH, TerminalLayout.SEARCH_HEIGHT, accent(xianqiao));

        int storageHeight = rows * TerminalLayout.SLOT_PITCH;
        terminalScrollbar(g, x + TerminalLayout.SCROLLBAR_X, y + TerminalLayout.STORAGE_Y,
                TerminalLayout.SCROLLBAR_WIDTH, storageHeight, rows, totalRows, scrollFraction);

        if (workspaceExpanded) {
            int craftTop = TerminalLayout.craftGridY(h) - 10;
            vanillaDivider(g, x + 7, y + craftTop - 4, 162);
        }
        if (craftingVisible) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    slot(g, x + TerminalLayout.craftInputSlotX(column),
                            y + TerminalLayout.craftInputSlotY(h, row), true);
                }
            }
            craftingArrow(g, x + TerminalLayout.craftArrowX(), y + TerminalLayout.craftArrowY(h));
            craftingResultSlot(g,
                    x + TerminalLayout.CRAFT_RESULT_X - TerminalLayout.CRAFT_RESULT_FRAME_MARGIN,
                    y + TerminalLayout.craftResultY(h) - TerminalLayout.CRAFT_RESULT_FRAME_MARGIN);
        }

        int inventoryY = TerminalLayout.inventoryY(h);
        int inventoryX = 30;
        vanillaDivider(g, x + 7, y + inventoryY - 8, 184);
        {
            for (int row = 0; row < 4; row++) {
                slot(g, x + 8, y + inventoryY + row * 18, true);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slot(g, x + inventoryX + column * 18, y + inventoryY + row * 18, true);
            }
        }
        for (int column = 0; column < 9; column++) {
            slot(g, x + inventoryX + column * 18, y + TerminalLayout.hotbarY(h), true);
        }
    }

    static void terminalFurnaceModule(GuiGraphics g, int x, int y, int h,
                                      int flameProgress, int[] cookProgress, boolean lit) {
        int fuelY = TerminalLayout.furnaceFuelY(h);
        int pluginY = TerminalLayout.furnacePluginY(h);
        slot(g, x + TerminalLayout.FURNACE_PLUGIN_X, y + pluginY, true);
        slot(g, x + TerminalLayout.FURNACE_FUEL_X, y + fuelY, true);

        for (int channel = 0; channel < TerminalLayout.FURNACE_LANE_COUNT; channel++) {
            int laneY = TerminalLayout.furnaceLaneY(h, channel);
            slot(g, x + TerminalLayout.FURNACE_INPUT_X, y + laneY, true);
            slot(g, x + TerminalLayout.FURNACE_RESULT_X, y + laneY, true);
            furnaceArrow(g, x + 82, y + laneY);

            int progress = cookProgress != null && channel < cookProgress.length
                    ? Mth.clamp(cookProgress[channel], 0, 24) : 0;
            if (progress > 0) {
                g.blitSprite(FURNACE_BURN_PROGRESS, 24, 16, 0, 0,
                        x + 82, y + laneY, progress, 16);
            }
        }

        furnaceFlame(g, x + TerminalLayout.FURNACE_FUEL_X + 1,
                y + TerminalLayout.furnaceFlameY(h), flameProgress, lit);
    }

    static void terminalSmithingModule(GuiGraphics g, int x, int y, int h) {
        int slotY = TerminalLayout.craftGridY(h) + 18;
        slot(g, x + 30, y + slotY, true);
        slot(g, x + 48, y + slotY, true);
        slot(g, x + 66, y + slotY, true);
        craftingArrow(g, x + 86, y + slotY + 1);
        craftingResultSlot(g, x + 118, y + slotY - 2);
    }

    static void terminalStonecutterModule(GuiGraphics g, int x, int y, int h) {
        int slotY = TerminalLayout.craftGridY(h) + 18;
        int gridTop = slotY - 19;
        vanillaInset(g, x + 52, y + gridTop, 66, 56);
        slot(g, x + 20, y + slotY, true);
        slot(g, x + 143, y + slotY, true);
    }

    static void functionTabs(GuiGraphics g, int x, int y, boolean xianqiao, int selected, boolean expanded) {
        functionTabs(g, x, y, xianqiao, selected, expanded, 4);
    }

    static void functionTabs(GuiGraphics g, int x, int y, boolean xianqiao, int selected, boolean expanded, int count) {
        for (int i = 0; i < count; i++) {
            int tabY = y + i * TerminalLayout.TAB_HEIGHT;
            boolean active = expanded && i == selected;
            advancementRightTab(g, x, tabY, active);
        }
    }

    static void expandedFunctionPanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao, int module) {
        panel(g, x, y, w, h);
        vanillaInset(g, x + 6, y + 18, w - 12, h - 24);

        if (w < 80) {
            compactModule(g, x + 10, y + 23, module, xianqiao);
            return;
        }
        if (module == 0) {
            moduleCraft(g, x + 8, y + (h <= 64 ? 22 : 30), xianqiao);
        } else if (module == 1) {
            moduleFurnace(g, x + 12, y + (h <= 64 ? 22 : 28), xianqiao);
        } else if (module == 2) {
            moduleFilter(g, x + 12, y + (h <= 64 ? 22 : 30), xianqiao);
        } else {
            moduleSettings(g, x + 14, y + (h <= 64 ? 22 : 30), xianqiao);
        }
    }

    static void moduleTabs(GuiGraphics g, int x, int y, boolean xianqiao, int selected) {
        rightModuleTabs(g, x, y, xianqiao, selected);
    }

    static void modulePanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao, int module) {
        insetPanel(g, x, y, w, h);
        int accent = accent(xianqiao);
        g.fill(x + 2, y + 2, x + w - 2, y + 16, PANEL_2);
        g.fill(x + 4, y + 4, x + w - 4, y + 7, 0x77FFFFFF);
        g.hLine(x + 3, x + w - 4, y + 16, MID);
        g.fill(x + 4, y + 18, x + w - 4, y + h - 4, 0x55373737);
        g.fill(x + 5, y + 19, x + w - 5, y + h - 5, PANEL);
        g.hLine(x + 6, x + w - 7, y + 20, 0x66FFFFFF);
        g.fill(x + 6, y + h - 8, x + w - 6, y + h - 6, accent);

        if (module == 0) {
            moduleCraft(g, x + 9, y + 26, xianqiao);
        } else if (module == 1) {
            moduleFurnace(g, x + 10, y + 25, xianqiao);
        } else if (module == 2) {
            moduleFilter(g, x + 10, y + 28, xianqiao);
        } else {
            moduleSettings(g, x + 12, y + 26, xianqiao);
        }
    }

    static void moduleTitle(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y) {
        g.drawString(font, text, x, y, 0xFF404040, false);
    }

    private static void leftUpgradeRail(GuiGraphics g, int x, int y, boolean xianqiao) {
        panel(g, x, y, 31, 112);
        g.fill(x + 3, y + 3, x + 28, y + 15, PANEL_2);
        g.hLine(x + 5, x + 25, y + 15, MID);
        for (int i = 0; i < 5; i++) {
            int slotY = y + 19 + i * 17;
            slot(g, x + 7, slotY, true);
            if (i < (xianqiao ? 3 : 2)) {
                tinyGlyph(g, x + 11, slotY + 4, i, accent(xianqiao));
            } else {
                dashedBox(g, x + 7, slotY, 16, 16);
            }
        }
    }

    private static void storageRowBand(GuiGraphics g, int x, int y, int w, int h, boolean edge) {
        int bottom = y + h;
        g.fill(x + 1, y, x + w - 1, bottom, edge ? PANEL : PANEL_4);
        g.hLine(x + 2, x + w - 3, y, edge ? LIGHT : PANEL_2);
        g.hLine(x + 2, x + w - 3, bottom - 1, MID);
        g.fill(x + 179, y + 1, x + 190, bottom - 1, PANEL_3);
    }

    private static void moduleIcon(GuiGraphics g, int x, int y, int module, boolean xianqiao) {
        int accent = MID;
        int accentDark = DARK;
        if (module == 0) {
            g.fill(x, y, x + 4, y + 4, accentDark);
            g.fill(x + 6, y, x + 10, y + 4, accentDark);
            g.fill(x + 3, y + 6, x + 8, y + 10, accent);
        } else if (module == 1) {
            g.fill(x + 1, y + 6, x + 11, y + 10, RED);
            g.fill(x + 3, y, x + 9, y + 7, GOLD);
        } else if (module == 2) {
            g.hLine(x, x + 12, y + 1, accent);
            g.hLine(x + 2, x + 10, y + 5, accent);
            g.hLine(x + 4, x + 8, y + 9, accent);
        } else {
            g.fill(x + 2, y, x + 10, y + 10, accentDark);
            g.fill(x + 4, y + 2, x + 8, y + 8, DARK);
            g.hLine(x, x + 12, y + 5, accent);
        }
    }

    private static void rightModuleTabs(GuiGraphics g, int x, int y, boolean xianqiao, int selected) {
        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);
        for (int i = 0; i < 4; i++) {
            int tabY = y + i * 24;
            g.fill(x + 2, tabY + 3, x + 25, tabY + 24, 0x66000000);
            g.fill(x, tabY, x + 24, tabY + 22, DARK);
            g.fill(x + 1, tabY + 1, x + 23, tabY + 21, i == selected ? PANEL_2 : PANEL_3);
            g.hLine(x + 2, x + 22, tabY + 2, LIGHT);
            g.vLine(x + 2, tabY + 2, tabY + 20, LIGHT);
            g.hLine(x + 1, x + 22, tabY + 20, MID);
            g.vLine(x + 22, tabY + 2, tabY + 20, MID);
            if (i == selected) {
                g.fill(x + 1, tabY + 17, x + 23, tabY + 20, accent);
                g.fill(x, tabY + 4, x + 3, tabY + 18, accentDark);
            }
            if (i == 0) {
                g.fill(x + 7, tabY + 6, x + 11, tabY + 10, accentDark);
                g.fill(x + 13, tabY + 6, x + 17, tabY + 10, accentDark);
                g.fill(x + 10, tabY + 12, x + 15, tabY + 16, accent);
            } else if (i == 1) {
                g.fill(x + 7, tabY + 12, x + 17, tabY + 16, RED);
                g.fill(x + 9, tabY + 6, x + 15, tabY + 12, GOLD);
            } else if (i == 2) {
                g.hLine(x + 6, x + 18, tabY + 7, accent);
                g.hLine(x + 8, x + 16, tabY + 11, accent);
                g.hLine(x + 10, x + 14, tabY + 15, accent);
            } else {
                g.hLine(x + 7, x + 17, tabY + 8, DARK);
                g.hLine(x + 7, x + 17, tabY + 14, DARK);
                g.vLine(x + 10, tabY + 7, tabY + 15, DARK);
                g.vLine(x + 14, tabY + 7, tabY + 15, DARK);
            }
        }
    }

    private static void topToolButtons(GuiGraphics g, int x, int y, boolean xianqiao) {
        smallButton(g, x, y, 0xFF6B6B6B);
        g.fill(x + 5, y + 4, x + 9, y + 8, LIGHT);
        g.fill(x + 9, y + 8, x + 12, y + 11, LIGHT);

        smallButton(g, x + 18, y, accent(xianqiao));
        g.fill(x + 23, y + 3, x + 28, y + 12, accent(xianqiao));
        g.hLine(x + 21, x + 30, y + 6, DARK);

        smallButton(g, x + 36, y, 0xFF2F2F2F);
        g.fill(x + 41, y + 4, x + 48, y + 12, BLACK);
        g.fill(x + 43, y + 6, x + 46, y + 9, PANEL_2);
    }

    private static void smallButton(GuiGraphics g, int x, int y, int accent) {
        g.fill(x + 1, y + 1, x + 16, y + 16, 0x55000000);
        g.fill(x, y, x + 15, y + 15, DARK);
        g.fill(x + 1, y + 1, x + 14, y + 14, PANEL_3);
        g.hLine(x + 2, x + 13, y + 2, LIGHT);
        g.vLine(x + 2, y + 2, y + 13, LIGHT);
        g.hLine(x + 2, x + 13, y + 13, MID);
        g.vLine(x + 13, y + 2, y + 13, MID);
        g.fill(x + 4, y + 4, x + 11, y + 11, accent);
        g.fill(x + 5, y + 5, x + 10, y + 7, 0x66FFFFFF);
    }

    private static void insetPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, MID);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        g.hLine(x + 1, x + w - 2, y + 1, DARK);
        g.vLine(x + 1, y + 1, y + h - 2, DARK);
        g.hLine(x + 1, x + w - 2, y + h - 2, LIGHT);
        g.vLine(x + w - 2, y + 1, y + h - 2, LIGHT);
    }

    private static void moduleCraft(GuiGraphics g, int x, int y, boolean xianqiao) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                miniSlot(g, x + c * 15, y + r * 15);
            }
        }
        craftingArrow(g, x + 45, y + 17);
        slot(g, x + 67, y + 14, true);
    }

    private static void moduleFurnace(GuiGraphics g, int x, int y, boolean xianqiao) {
        slot(g, x + 2, y, true);
        slot(g, x + 2, y + 36, true);
        furnaceFlame(g, x + 7, y + 21, 13, true);
        furnaceArrow(g, x + 35, y + 19);
        slot(g, x + 62, y + 18, true);
        tinyGlyph(g, x + 67, y + 23, 1, accent(xianqiao));
    }

    private static void moduleFilter(GuiGraphics g, int x, int y, boolean xianqiao) {
        for (int i = 0; i < 4; i++) {
            miniSlot(g, x + i * 17, y);
            miniSlot(g, x + i * 17, y + 17);
        }
        int accent = accent(xianqiao);
        g.fill(x + 7, y + 42, x + 61, y + 45, PANEL_3);
        g.hLine(x + 4, x + 68, y + 48, accent);
        g.hLine(x + 12, x + 60, y + 54, accent);
        g.hLine(x + 22, x + 50, y + 60, accent);
    }

    private static void moduleSettings(GuiGraphics g, int x, int y, boolean xianqiao) {
        int accent = accent(xianqiao);
        for (int i = 0; i < 4; i++) {
            int rowY = y + i * 15;
            g.fill(x, rowY, x + 54, rowY + 10, PANEL_3);
            g.hLine(x + 2, x + 44, rowY + 2, MID);
            g.hLine(x + 2, x + 34 + i * 3, rowY + 6, DARK);
            g.fill(x + 57, rowY + 1, x + 73, rowY + 9, DARK);
            g.fill(x + 58 + (i % 2) * 7, rowY + 2, x + 64 + (i % 2) * 7, rowY + 8, accent);
        }
    }

    private static void compactModule(GuiGraphics g, int x, int y, int module, boolean xianqiao) {
        int accent = accent(xianqiao);
        moduleIcon(g, x + 2, y, module, xianqiao);
        for (int i = 0; i < 3; i++) {
            int rowY = y + 18 + i * 11;
            g.fill(x, rowY, x + 22, rowY + 6, PANEL_3);
            g.fill(x + 2, rowY + 2, x + 10 + i * 4, rowY + 4, accent);
        }
    }

    private static void miniSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 13, y + 13, DARK);
        g.fill(x + 1, y + 1, x + 12, y + 12, MID);
        g.hLine(x + 1, x + 11, y + 12, LIGHT);
        g.vLine(x + 12, y + 1, y + 12, LIGHT);
    }

    private static void tinyGlyph(GuiGraphics g, int x, int y, int variant, int color) {
        g.fill(x, y, x + 8, y + 8, color);
        g.fill(x + 2, y + 2, x + 6, y + 6, DARK);
        if (variant % 2 == 0) {
            g.hLine(x + 1, x + 7, y + 4, color);
        } else {
            g.vLine(x + 4, y + 1, y + 7, color);
        }
    }

    private static void dashedBox(GuiGraphics g, int x, int y, int w, int h) {
        for (int dx = 0; dx < w; dx += 4) {
            g.hLine(x + dx, Math.min(x + dx + 1, x + w - 1), y, BLACK);
            g.hLine(x + dx, Math.min(x + dx + 1, x + w - 1), y + h - 1, BLACK);
        }
        for (int dy = 0; dy < h; dy += 4) {
            g.vLine(x, y + dy, Math.min(y + dy + 1, y + h - 1), BLACK);
            g.vLine(x + w - 1, y + dy, Math.min(y + dy + 1, y + h - 1), BLACK);
        }
    }

    static void slot(GuiGraphics g, int x, int y, boolean active) {
        // Use Minecraft's runtime GUI sprite directly. Its 18x18 tile is anchored
        // one pixel outside the slot's real 16x16 item/hit area.
        g.blitSprite(VANILLA_SLOT, x - 1, y - 1,
                TerminalLayout.SLOT_PITCH, TerminalLayout.SLOT_PITCH);
        if (!active) {
            g.fill(x, y, x + 16, y + 16, 0x66000000);
        }
    }

    static void slots(GuiGraphics g, int originX, int originY, Iterable<Slot> slots) {
        slots(g, originX, originY, slots, Integer.MAX_VALUE);
    }

    static void slots(GuiGraphics g, int originX, int originY, Iterable<Slot> slots, int maxRelativeX) {
        for (Slot slot : slots) {
            if (slot.x >= maxRelativeX) {
                continue;
            }
            if (!slot.isActive()) {
                continue;
            }
            slot(g, originX + slot.x, originY + slot.y, slot.isActive());
        }
    }

    private VanillaGuiPainter() {}

    private static int accent(boolean xianqiao) {
        return xianqiao ? CYAN : GREEN;
    }

    private static int accentDark(boolean xianqiao) {
        return xianqiao ? CYAN_DARK : GREEN_DARK;
    }

    private static void capacityMeter(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_3);
        g.fill(x + 2, y + 2, x + w - 12, y + h - 2, border);
        g.fill(x + 3, y + 3, x + w - 23, y + h - 3, fill);
        g.fill(x + w - 9, y + 2, x + w - 3, y + h - 2, PANEL_2);
    }

    private static void terminalSearch(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BLACK);
        g.hLine(x + 5, x + w - 8, y + 4, 0xFF4A4A4A);
        g.hLine(x + 5, x + w - 28, y + 7, 0xFF303030);
        g.fill(x + w - 13, y + 3, x + w - 8, y + 8, accent);
    }

    private static void scrollbar(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_3);
        g.fill(x + 2, y + 4, x + w - 2, y + 24, PANEL_2);
        g.hLine(x + 2, x + w - 3, y + 5, LIGHT);
        g.hLine(x + 2, x + w - 3, y + 23, MID);
        g.fill(x + 3, y + h - 12, x + w - 3, y + h - 4, accent);
    }

    private static void terminalScrollbar(GuiGraphics g, int x, int y, int w, int h,
                                           int visibleRows, int totalRows, float scrollFraction) {
        g.fill(x, y, x + w, y + h, DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_3);
        g.hLine(x + 1, x + w - 2, y + h - 2, LIGHT);
        g.vLine(x + w - 2, y + 1, y + h - 2, LIGHT);
        int thumbHeight = TerminalLayout.scrollbarThumbHeight(h, visibleRows, totalRows);
        int thumbY = y + 1 + TerminalLayout.scrollbarThumbOffset(
                h, visibleRows, totalRows, scrollFraction);
        int thumbFace = totalRows > visibleRows ? PANEL_2 : PANEL_4;
        // The handle occupies the track's 12px interior and remains a fixed
        // 15px high, matching the compact AE-style control proportions.
        g.fill(x + 1, thumbY, x + w - 1, thumbY + thumbHeight, thumbFace);
        g.hLine(x + 1, x + w - 2, thumbY, LIGHT);
        g.vLine(x + 1, thumbY, thumbY + thumbHeight - 1, LIGHT);
        g.hLine(x + 1, x + w - 2, thumbY + thumbHeight - 1, DARK);
        g.vLine(x + w - 2, thumbY, thumbY + thumbHeight - 1, DARK);
        for (int ridgeY = thumbY + 4; ridgeY < thumbY + thumbHeight - 2; ridgeY += 3) {
            g.hLine(x + 3, x + w - 4, ridgeY, MID);
            if (ridgeY + 1 < thumbY + thumbHeight - 2) {
                g.hLine(x + 3, x + w - 4, ridgeY + 1, LIGHT);
            }
        }
    }

    private static void advancementRightTab(GuiGraphics g, int x, int y, boolean active) {
        int width = TerminalLayout.TAB_WIDTH;
        int height = TerminalLayout.TAB_HEIGHT;
        int face = active ? PANEL_2 : PANEL;
        g.fill(x + 1, y + 1, x + width, y + height - 1, DARK);
        g.fill(x, y, x + width - 1, y + height - 2, face);
        g.hLine(x + 1, x + width - 2, y, LIGHT);
        g.hLine(x + 2, x + width - 2, y + height - 2, MID);
        g.vLine(x + width - 2, y + 1, y + height - 3, MID);
        if (active) {
            g.fill(x, y + 2, x + 3, y + height - 3, face);
        }
    }

    private static void interfaceTrace(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        int accent = accent(xianqiao);
        g.fill(x, y, x + w, y + h, PANEL_3);
        g.vLine(x + w / 2, y + 6, y + h - 8, DARK);
        for (int i = 0; i < 4; i++) {
            int nodeY = y + 10 + i * 20;
            g.fill(x + 3, nodeY, x + w - 3, nodeY + 6, accent);
            g.fill(x + 5, nodeY + 2, x + w - 5, nodeY + 4, PANEL_2);
        }
    }

    private static void moduleDockHints(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        int accent = accent(xianqiao);
        for (int i = 0; i < 4; i++) {
            int bx = x + i * 34;
            vanillaButton(g, bx, y, 24, 20, false);
            moduleIcon(g, bx + 6, y + 5, i, xianqiao);
        }
        g.fill(x + 3, y + h - 8, x + w - 3, y + h - 6, PANEL_3);
        g.hLine(x + 3, x + w - 4, y + h - 4, accent);
    }

    private static void lowerStatusPanel(GuiGraphics g, int x, int y, int w, int h, boolean xianqiao) {
        insetPanel(g, x, y, w, h);
        int accent = accent(xianqiao);
        int accentDark = accentDark(xianqiao);
        g.fill(x + 4, y + 4, x + w - 4, y + 16, PANEL_2);
        g.hLine(x + 6, x + w - 7, y + 17, MID);
        for (int row = 0; row < 3; row++) {
            int rowY = y + 24 + row * 14;
            g.fill(x + 8, rowY, x + w - 8, rowY + 8, PANEL_3);
            g.fill(x + 10, rowY + 2, x + 24 + row * 8, rowY + 6, accentDark);
            g.fill(x + 10, rowY + 2, x + 17 + row * 5, rowY + 6, accent);
            g.fill(x + w - 20, rowY + 1, x + w - 10, rowY + 7, row % 2 == 0 ? accent : PANEL_4);
        }
    }

    static void craftingArrow(GuiGraphics g, int x, int y) {
        // Exact 22x15 vanilla crafting arrow; the surrounding pixels are the
        // same #C6C6C6 panel colour used by this screen.
        g.blit(CRAFTING_TABLE_TEXTURE, x, y, 90.0F, 35.0F, 22, 15, 256, 256);
    }

    static void craftingResultSlot(GuiGraphics g, int x, int y) {
        // Runtime vanilla 1.21.1 crafting-table output recess (119,30)..(145,56).
        // It already contains the complete 26x26 frame around the real 16x16
        // result slot, so drawing a generic 18x18 slot on top would double its
        // lower/right highlight and visibly shift the window.
        g.blit(CRAFTING_TABLE_TEXTURE, x, y, 119.0F, 30.0F, 26, 26, 256, 256);
    }

    private static void furnaceArrow(GuiGraphics g, int x, int y) {
        // The furnace texture supplies the unfilled arrow. The progress sprite
        // is cropped over it by the caller as cooking advances.
        g.blit(FURNACE_TEXTURE, x, y, 79.0F, 34.0F, 24, 16, 256, 256);
    }

    static void furnaceFlame(GuiGraphics g, int x, int y, int flameProgress, boolean lit) {
        // Runtime vanilla furnace supplies the complete unlit 14x14 flame
        // recess.  The cyan-white sprite is a deterministic palette conversion
        // of the matching runtime lit-progress sprite, preserving every pixel.
        g.blit(FURNACE_TEXTURE, x, y, 56.0F, 36.0F, 14, 14, 256, 256);
        if (!lit) return;

        int height = Mth.clamp(flameProgress, 0, 13) + 1;
        g.blitSprite(IMMORTAL_FURNACE_LIT_PROGRESS, 14, 14, 0, 14 - height,
                x, y + 14 - height, 14, height);
    }

    private static void vanillaInset(GuiGraphics g, int x, int y, int w, int h) {
        chamferFill(g, x, y, w, h, PANEL);
        g.hLine(x + 1, x + w - 2, y, MID);
        g.vLine(x, y + 1, y + h - 2, MID);
        g.hLine(x + 1, x + w - 1, y + 1, DARK);
        g.vLine(x + 1, y + 1, y + h - 1, DARK);
        g.hLine(x + 1, x + w - 2, y + h - 1, LIGHT);
        g.vLine(x + w - 1, y + 1, y + h - 2, LIGHT);
    }

    private static void vanillaDivider(GuiGraphics g, int x, int y, int w) {
        g.hLine(x, x + w, y, MID);
        g.hLine(x, x + w, y + 1, LIGHT);
    }

    private static void vanillaButton(GuiGraphics g, int x, int y, int w, int h, boolean pressed) {
        int top = pressed ? DARK : LIGHT;
        int bottom = pressed ? LIGHT : DARK;
        chamferFill(g, x, y, w, h, PANEL);
        g.hLine(x + 1, x + w - 2, y, top);
        g.vLine(x, y + 1, y + h - 2, top);
        g.hLine(x + 1, x + w - 2, y + h - 1, bottom);
        g.vLine(x + w - 1, y + 1, y + h - 2, bottom);
        g.hLine(x + 1, x + w - 2, y + 1, pressed ? MID : PANEL_2);
        g.vLine(x + 1, y + 1, y + h - 2, pressed ? MID : PANEL_2);
        g.hLine(x + 1, x + w - 2, y + h - 2, pressed ? PANEL_2 : MID);
        g.vLine(x + w - 2, y + 1, y + h - 2, pressed ? PANEL_2 : MID);
    }

    private static void chamferFill(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
    }

    private static void creativeSideTab(GuiGraphics g, int x, int y, boolean active) {
        int w = active ? 30 : 27;
        int h = 24;
        int fill = active ? PANEL : PANEL_4;
        chamferFill(g, x, y, w, h, fill);
        g.hLine(x + 2, x + w - 3, y, LIGHT);
        g.hLine(x + 1, x + w - 2, y + 1, LIGHT);
        g.vLine(x, y + 2, y + h - 3, LIGHT);
        g.vLine(x + 1, y + 1, y + h - 2, PANEL_2);
        g.hLine(x + 2, x + w - 3, y + h - 1, DARK);
        g.hLine(x + 1, x + w - 2, y + h - 2, MID);
        g.vLine(x + w - 1, y + 2, y + h - 3, DARK);
        g.vLine(x + w - 2, y + 1, y + h - 2, MID);
        if (active) {
            g.fill(x, y + 3, x + 3, y + h - 3, PANEL);
            g.vLine(x + 1, y + 3, y + h - 4, PANEL_2);
        }
    }

    private static void creativeRightTab(GuiGraphics g, int x, int y, boolean active) {
        int w = active ? 31 : 28;
        int h = 24;
        int fill = active ? PANEL : PANEL_4;
        chamferFill(g, x, y, w, h, fill);
        g.hLine(x + 2, x + w - 3, y, LIGHT);
        g.hLine(x + 1, x + w - 2, y + 1, LIGHT);
        g.vLine(x, y + 2, y + h - 3, LIGHT);
        g.vLine(x + 1, y + 1, y + h - 2, PANEL_2);
        g.hLine(x + 2, x + w - 3, y + h - 1, DARK);
        g.hLine(x + 1, x + w - 2, y + h - 2, MID);
        g.vLine(x + w - 1, y + 2, y + h - 3, DARK);
        g.vLine(x + w - 2, y + 1, y + h - 2, MID);
        if (active) {
            g.fill(x, y + 3, x + 4, y + h - 3, PANEL);
            g.vLine(x + 1, y + 3, y + h - 4, PANEL_2);
        }
    }
}
