package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepository;
import com.refinedmods.refinedstorage.common.api.grid.GridScrollMode;
import com.refinedmods.refinedstorage.common.api.grid.strategy.GridExtractionStrategy;
import com.refinedmods.refinedstorage.common.api.grid.strategy.GridScrollingStrategy;
import com.refinedmods.refinedstorage.common.api.grid.view.AbstractGridResource;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResourceAttributeKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Display-safe RS grid entry for an ImmortalStorage external-resource key. */
final class RsExternalGridResource extends AbstractGridResource<RsExternalResource> {
    private final int registryId;
    private final List<Component> tooltip;

    RsExternalGridResource(
            RsExternalResource resource,
            String name,
            Function<GridResourceAttributeKey, Set<String>> attributes) {
        super(resource, name, attributes);
        this.registryId = resource.resource().hashCode() & Integer.MAX_VALUE;
        this.tooltip = RsExternalResourceRendering.INSTANCE.getTooltip(resource);
    }

    @Override public int getRegistryId() { return registryId; }

    @Override
    public List<ClientTooltipComponent> getExtractionHints(
            ItemStack carried, ResourceRepository<GridResource> repository) {
        return List.of();
    }

    @Override public ResourceAmount getAutocraftingRequest() {
        return new ResourceAmount(resource, 1L);
    }

    @Override
    public boolean canExtract(ItemStack carried, ResourceRepository<GridResource> repository) {
        return false;
    }

    @Override
    public void onExtract(GridExtractMode mode, boolean byMouse, GridExtractionStrategy strategy) {
        strategy.onExtract(resource, mode, byMouse);
    }

    @Override public void onScroll(GridScrollMode mode, GridScrollingStrategy strategy) {}

    @Override public void render(GuiGraphics graphics, int x, int y) {
        RsExternalResourceRendering.INSTANCE.render(resource, graphics, x, y);
    }

    @Override
    public String getDisplayedAmount(ResourceRepository<GridResource> repository) {
        return RsExternalResourceRendering.INSTANCE.formatAmount(getAmount(repository), true);
    }

    @Override
    public String getAmountInTooltip(ResourceRepository<GridResource> repository) {
        return RsExternalResourceRendering.INSTANCE.formatAmount(getAmount(repository));
    }

    @Override public boolean belongsToResourceType(ResourceType type) {
        return type == RsExternalResourceType.INSTANCE;
    }

    @Override public List<Component> getTooltip() { return tooltip; }
    @Override public Optional<TooltipComponent> getTooltipImage() { return Optional.empty(); }
}
