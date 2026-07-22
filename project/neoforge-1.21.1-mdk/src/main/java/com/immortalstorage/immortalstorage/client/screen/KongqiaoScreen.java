package com.immortalstorage.immortalstorage.client.screen;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery;
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
    private boolean smithingVisible;
    private TerminalTabButton craftModuleButton;
    private TerminalTabButton smithingModuleButton;
    private TerminalTabButton furnaceModuleButton;
    private Button autoFurnaceFillButton;
    private Button autoFurnaceFuelButton;
    private Button magnetButton;

    public KongqiaoScreen(KongqiaoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int railX = this.leftPos + TerminalLayout.MODULE_RAIL_X;
        Component craftLabel = Component.translatable(this.menu.isCraftingUnlocked()
                ? "container.immortalstorage.terminal.craft"
                : "container.immortalstorage.terminal.craft_locked");
        Component furnaceLabel = Component.translatable(this.menu.isFurnaceUnlocked()
                ? "container.immortalstorage.terminal.furnace"
                : "container.immortalstorage.terminal.furnace_locked");
        Component smithingLabel = Component.translatable(this.menu.isSmithingUnlocked()
                ? "container.immortalstorage.terminal.smithing"
                : "container.immortalstorage.terminal.smithing_locked");
        this.craftModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(0),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(0, 3),
                new ItemStack(Items.CRAFTING_TABLE), craftLabel, Tooltip.create(craftLabel),
                () -> this.craftingVisible, button -> selectModule(0)));
        this.smithingModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(1),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(1, 3),
                new ItemStack(Items.SMITHING_TABLE), smithingLabel, Tooltip.create(smithingLabel),
                () -> this.smithingVisible, button -> selectModule(1)));
        this.furnaceModuleButton = this.addRenderableWidget(new TerminalTabButton(
                railX, this.topPos + TerminalLayout.moduleTabY(2),
                TerminalTabStyle.Side.LEFT, TerminalTabStyle.segment(2, 3),
                new ItemStack(ModBlocks.IMMORTAL_FURNACE.get()), furnaceLabel, Tooltip.create(furnaceLabel),
                () -> this.furnaceVisible, button -> selectModule(2)));
        this.craftModuleButton.active = this.menu.isCraftingUnlocked();
        this.smithingModuleButton.active = this.menu.isSmithingUnlocked();
        this.furnaceModuleButton.active = this.menu.isFurnaceUnlocked();
        this.autoFurnaceFillButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFillLabel(), button -> requestMenuButton(KongqiaoMenu.AUTO_FURNACE_FILL_BUTTON))
                .bounds(this.leftPos + 16, this.topPos + this.imageHeight - 179, 72, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.auto_fill_hint")))
                .build());
        this.autoFurnaceFuelButton = this.addRenderableWidget(Button.builder(
                        autoFurnaceFuelLabel(), button -> requestMenuButton(KongqiaoMenu.AUTO_FURNACE_FUEL_BUTTON))
                .bounds(this.leftPos + 92, this.topPos + this.imageHeight - 179, 76, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.auto_fuel_hint")))
                .build());
        this.magnetButton = this.addRenderableWidget(Button.builder(magnetLabel(),
                        button -> requestMenuButton(KongqiaoMenu.MAGNET_BUTTON))
                .bounds(this.leftPos + 16, this.topPos + this.imageHeight - 179, 152, 16)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.immortalstorage.terminal.magnet_hint"))).build());
        updateFurnaceButtons();
        if (this.magnetButton != null) {
            this.magnetButton.visible = this.menu.getData().getStage() >= 4 && !this.furnaceVisible;
            this.magnetButton.active = this.magnetButton.visible;
            this.magnetButton.setMessage(magnetLabel());
        }
    }

    private Component magnetLabel() { return Component.translatable(this.menu.getData().isMagnetEnabled()
            ? "container.immortalstorage.terminal.magnet_on"
            : "container.immortalstorage.terminal.magnet_off"); }

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
        if (this.smithingVisible) VanillaGuiPainter.terminalSmithingModule(
                graphics, this.leftPos, this.topPos, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!renderExactStorageTooltip(graphics, mouseX, mouseY)) this.renderTooltip(graphics, mouseX, mouseY);
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
                ? "container.immortalstorage.terminal.auto_fill_on"
                : "container.immortalstorage.terminal.auto_fill_off");
    }

    private Component autoFurnaceFuelLabel() {
        return Component.translatable(this.menu.isFurnaceAutoConsume()
                ? "container.immortalstorage.terminal.auto_true_yuan_on"
                : "container.immortalstorage.terminal.auto_true_yuan_off");
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, TerminalLayout.TITLE_X, TerminalLayout.TITLE_Y,
                0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
        if (this.craftingVisible && this.menu.getData().getStage() < 3) {
            graphics.drawString(this.font, Component.translatable("container.immortalstorage.terminal.craft_locked"),
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
        if (KongqiaoMenu.isSmithingSlotIndex(menuIndex)) {
            return TerminalLayout.craftGridY(this.imageHeight) + 18;
        }
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
        if (KongqiaoMenu.isSmithingSlotIndex(menuIndex)) return this.smithingVisible;
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
        if ((module == 0) != this.craftingVisible || (module == 1) != this.smithingVisible
                || (module == 2) != this.furnaceVisible) {
            applyModuleState(module);
        }
        updateFurnaceButtons();
    }

    @Override
    protected int railControlOffset() {
        return TerminalLayout.railControlOffset(3);
    }

    @Override
    public List<Rect2i> immortalstorage$getExtraAreas() {
        int railHeight = TerminalLayout.railHeight(3);
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
        this.smithingVisible = module == 1;
        this.furnaceVisible = module == 2;
        setWorkspaceState(module == 0, module >= 0 && module <= 2);
    }

}
