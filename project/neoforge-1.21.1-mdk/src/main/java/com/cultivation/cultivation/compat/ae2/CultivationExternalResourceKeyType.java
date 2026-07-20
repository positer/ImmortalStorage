package com.cultivation.cultivation.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import com.cultivation.cultivation.CultivationMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** AE2 storage channel owned and shipped by Cultivation itself. */
public final class CultivationExternalResourceKeyType extends AEKeyType {
    static final CultivationExternalResourceKeyType TYPE =
            new CultivationExternalResourceKeyType();
    private static boolean registered;

    private CultivationExternalResourceKeyType() {
        super(ResourceLocation.fromNamespaceAndPath(CultivationMod.MODID, "external_resource"),
                CultivationExternalResourceKey.class,
                Component.translatable("resource.cultivation.external_resources"));
    }

    static synchronized void register() {
        if (registered) return;
        AEKeyTypes.register(TYPE);
        registered = true;
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return CultivationExternalResourceKey.MAP_CODEC;
    }

    @Override
    public CultivationExternalResourceKey readFromPacket(RegistryFriendlyByteBuf input) {
        return CultivationExternalResourceKey.fromPacket(input);
    }

    @Override
    public AEKey loadKeyFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        return CultivationExternalResourceKey.fromTag(tag);
    }

    @Override
    public int getAmountPerOperation() {
        return 1;
    }

    @Override
    public int getAmountPerByte() {
        return 1;
    }

    @Override
    public int getAmountPerUnit() {
        return 1;
    }
}
