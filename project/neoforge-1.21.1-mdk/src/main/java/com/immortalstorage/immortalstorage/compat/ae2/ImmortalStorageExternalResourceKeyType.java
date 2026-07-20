package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** AE2 storage channel owned and shipped by ImmortalStorage itself. */
public final class ImmortalStorageExternalResourceKeyType extends AEKeyType {
    static final ImmortalStorageExternalResourceKeyType TYPE =
            new ImmortalStorageExternalResourceKeyType();
    private static boolean registered;

    private ImmortalStorageExternalResourceKeyType() {
        super(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "external_resource"),
                ImmortalStorageExternalResourceKey.class,
                Component.translatable("resource.immortalstorage.external_resources"));
    }

    static synchronized void register() {
        if (registered) return;
        AEKeyTypes.register(TYPE);
        registered = true;
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return ImmortalStorageExternalResourceKey.MAP_CODEC;
    }

    @Override
    public ImmortalStorageExternalResourceKey readFromPacket(RegistryFriendlyByteBuf input) {
        return ImmortalStorageExternalResourceKey.fromPacket(input);
    }

    @Override
    public AEKey loadKeyFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        return ImmortalStorageExternalResourceKey.fromTag(tag);
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
