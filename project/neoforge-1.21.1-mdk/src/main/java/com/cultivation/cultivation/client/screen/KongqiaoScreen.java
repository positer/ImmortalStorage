package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.block.ModBlocks;
import com.cultivation.cultivation.menu.custom.KongqiaoMenu;
import com.cultivation.cultivation.api.storage.terminal.TerminalQuery;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class KongqiaoScreen extends AbstractTerminalScreen<KongqiaoMenu> {
    private boolean furnaceVisible;
    private TerminalTabButton craftModuleButton;
    private TerminalTabButton furnaceModuleButton;
    private Button autoFurnaceFillButton;
    private Button autoFurnaceFuelButton;

    public KongqiaoScreen(KongqiaoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int railX = this.leftPos + TerminalLayout.MODULE_RAIL_X;
        Component craftLabel = Component.translatable(this.menu.isCraftingUnlocked()
                ? "container.cultivation.terminal.craft"
                : "container.cultivation.terminal.craft_locked");
        Component furnaceLabel = Component.translatable(this.menu.isFurnaceUnlocked()
                ? "container.cultivation.terminal.furnace"
                : "container.cultivation.terminal.furnace_locked");
        this.craftModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(0),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(0, 2),
                new ItemStack(Items.CRAFTING_TABLE), craftLabel, Tooltip.create(craftLabel),
                () -> this.craftingVisible, button -> selectModule(0)));
        this.furnaceModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(1),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(1, 2),
                new ItemStack(ModBlocks.IMMORTAL_FURNACE.get()), furnaceLabel, Tooltip.create(furnaceLabel),
                () -> this.furnaceVisible, button -> selectModule(1)));
        this.craftModuleButton.active = this.menu.isCraftingUnlocked();
        this.furnaceModuleButton.active = this.menu.isFurnaceUnlocked();
        this.autoFurnaceFillButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFillLabel(), button -> requestMenuButton(KongqiaoMenu.AUTO_FURNACE_FILL_BUTTON))
                .bounds(this.leftPos + 16, this.topPos + this.imageHeight - 179, 72, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.cultivation.terminal.auto_fill_hint")))
                .build());
        this.autoFurnaceFuelButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFuelLabel(), button -> requestMenuButton(KongqiaoMenu.AUTO_FURNACE_FUEL_BUTTON))
                .bounds(this.leftPos + 92, this.topPos + this.imageHeight - 179, 76, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.cultivation.terminal.auto_fuel_hint")))
                .build());
        updateFurnaceButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderTerminalChrome(graphics, false);
        renderStorageSlotsClipped(graphics, storageSlotCountInternal());
        if (this.furnaceVisible) {
            VanillaGuiPainter.terminalFurnaceModule(graphics, this.leftPos, this.topPos, this.imageHeight,
                    this.menu.getFurnaceLitProgress(), new int[] {
                            this.menu.getFurnaceBurnProgress(0),
                            this.menu.getFurnaceBurnProgress(1),
                            this.menu.getFurnaceBurnProgress(2)
                    }, this.menu.isFurnaceLit());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void updateFurnaceButtons() {
        if (this.autoFurnaceFillButton != null) {
            this.autoFurnaceFillButton.visible = this.furnaceVisible;
            this.autoFurnaceFillButton.active = this.furnaceVisible;
            this.autoFurnaceFillButton.setMessage(autoFurnaceFillLabel());
        }
        if (this.autoFurnaceFuelButton != null) {
            this.autoFurnaceFuelButton.visible = this.furnaceVisible;
            this.autoFurnaceFuelButton.active = this.furnaceVisible;
            this.autoFurnaceFuelButton.setMessage(autoFurnaceFuelLabel());
        }
    }

    private Component autoFurnaceFillLabel() {
        return Component.translatable(this.menu.isFurnaceAutoFill()
                ? "container.cultivation.terminal.auto_fill_on"
                : "container.cultivation.terminal.auto_fill_off");
    }

    private Component autoFurnaceFuelLabel() {
        return Component.translatable(this.menu.isFurnaceAutoConsume()
                ? "container.cultivation.terminal.auto_true_yuan_on"
                : "container.cultivation.terminal.auto_true_yuan_off");
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, TerminalLayout.TITLE_X, TerminalLayout.TITLE_Y,
                0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
        if (this.craftingVisible && this.menu.getData().getStage() < 3) {
            graphics.drawString(this.font, Component.translatable("container.cultivation.terminal.craft_locked"),
                    68, TerminalLayout.craftGridY(this.imageHeight) + 24, 0xFF805000, false);
        }
        renderStorageAmountOverlays(graphics);
    }

    @Override
    protected int totalStorageRows() {
        return this.menu.getTotalRows();
    }

    @Override
    protected int maximumContentRows() {
        return Math.max(TerminalLayout.MIN_ROWS, this.menu.getTotalRows());
    }

    @Override
    protected void onViewportChanged(int rows, int baseRow) {
        super.onViewportChanged(rows, baseRow);
    }

    @Override
    protected int storageSlotStart() {
        return 0;
    }

    @Override
    protected int storageSlotCount() {
        return Math.min(KongqiaoMenu.VISIBLE_STORAGE_SLOTS, this.menu.slots.size());
    }

    @Override
    protected int craftingSlotStart() {
        return KongqiaoMenu.CRAFT_START;
    }

    @Override
    protected int craftingResultSlot() {
        return KongqiaoMenu.CRAFT_RESULT_SLOT;
    }

    @Override protected int craftMatchComponentsButtonId() {
        return KongqiaoMenu.CRAFT_MATCH_COMPONENTS_BUTTON;
    }

    @Override protected boolean craftMatchComponentsEnabled() {
        return this.menu.isCraftAutofillMatchComponents();
    }

    @Override
    protected int playerInventoryStart() {
        return KongqiaoMenu.PLAYER_START;
    }

    @Override
    protected int visualSlotX(int menuIndex, Slot slot) {
        if (menuIndex == KongqiaoMenu.FURNACE_FUEL_SLOT) return TerminalLayout.FURNACE_FUEL_X;
        if (KongqiaoMenu.isFurnaceInputSlotIndex(menuIndex)) return TerminalLayout.FURNACE_INPUT_X;
        if (KongqiaoMenu.isFurnaceResultSlotIndex(menuIndex)) return TerminalLayout.FURNACE_RESULT_X;
        int visual = super.visualSlotX(menuIndex, slot);
        return menuIndex >= KongqiaoMenu.PLAYER_START ? visual + 22 : visual;
    }

    @Override
    protected int visualSlotY(int menuIndex, Slot slot) {
        if (menuIndex >= KongqiaoMenu.ARMOR_START && menuIndex < KongqiaoMenu.ARMOR_END) {
            return TerminalLayout.inventoryY(this.imageHeight)
                    + (menuIndex - KongqiaoMenu.ARMOR_START) * TerminalLayout.SLOT_PITCH;
        }
        if (menuIndex == KongqiaoMenu.FURNACE_FUEL_SLOT) return TerminalLayout.furnaceFuelY(this.imageHeight);
        int channel = KongqiaoMenu.furnaceChannelForSlot(menuIndex);
        if (KongqiaoMenu.isFurnaceInputSlotIndex(menuIndex)) {
            return TerminalLayout.furnaceInputY(this.imageHeight, channel);
        }
        if (KongqiaoMenu.isFurnaceResultSlotIndex(menuIndex)) {
            return TerminalLayout.furnaceResultY(this.imageHeight, channel);
        }
        return super.visualSlotY(menuIndex, slot);
    }

    @Override
    protected boolean shouldRenderMenuSlot(int menuIndex) {
        if (KongqiaoMenu.isFurnaceSlotIndex(menuIndex)) return this.furnaceVisible;
        return super.shouldRenderMenuSlot(menuIndex);
    }

    @Override
    protected void setLocalQuery(TerminalQuery query) {
        // The server keeps Kongqiao's physical slot order; the query packet is ignored for this menu.
    }

    @Override
    protected void setLocalActiveModule(int module) {
        this.menu.applyClientActiveModule(module);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int module = this.menu.getActiveModule();
        if ((module == 0) != this.craftingVisible || (module == 1) != this.furnaceVisible) {
            applyModuleState(module);
        }
        updateFurnaceButtons();
    }

    @Override
    protected int railControlOffset() {
        return TerminalLayout.railControlOffset(2);
    }

    @Override
    public List<Rect2i> cultivation$getExtraAreas() {
        int railHeight = TerminalLayout.railHeight(2);
        return List.of(new Rect2i(this.leftPos + TerminalLayout.MODULE_RAIL_X, this.topPos,
                TerminalLayout.TAB_WIDTH, railHeight));
    }

    private int storageSlotCountInternal() {
        int buffered = Math.min(KongqiaoMenu.MAX_VISIBLE_ROWS, this.visibleRows + 1) * TerminalLayout.COLUMNS;
        return Math.min(buffered, this.menu.slots.size());
    }

    private void selectModule(int module) {
        int next = this.menu.getActiveModule() == module ? -1 : module;
        requestMenuButton(module);
        this.menu.applyClientActiveModule(next);
        applyModuleState(next);
    }

    private void applyModuleState(int module) {
        this.furnaceVisible = module == 1;
        setWorkspaceState(module == 0, module == 0 || module == 1);
    }

}
