package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepositoryMapper;
import com.refinedmods.refinedstorage.common.api.grid.GridResourceAttributeKeys;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResourceAttributeKey;

import java.util.Set;
import java.util.function.Function;

/** Maps ImmortalStorage's fallback key into the client-side RS grid view model. */
final class RsExternalGridResourceMapper implements ResourceRepositoryMapper<GridResource> {
    static final RsExternalGridResourceMapper INSTANCE = new RsExternalGridResourceMapper();

    @Override
    public GridResource apply(ResourceKey resource) {
        if (!(resource instanceof RsExternalResource external)) {
            throw new IllegalArgumentException("Unsupported external resource key: " + resource);
        }
        String name = RsExternalResourceRendering.INSTANCE.getDisplayName(external).getString();
        Function<GridResourceAttributeKey, Set<String>> attributes = key -> {
            if (key == GridResourceAttributeKeys.MOD_ID) return Set.of("immortalstorage");
            if (key == GridResourceAttributeKeys.MOD_NAME) return Set.of("ImmortalStorage");
            if (key == GridResourceAttributeKeys.TOOLTIP) return Set.of(name);
            return Set.of();
        };
        return new RsExternalGridResource(external, name, attributes);
    }

    private RsExternalGridResourceMapper() {}
}
