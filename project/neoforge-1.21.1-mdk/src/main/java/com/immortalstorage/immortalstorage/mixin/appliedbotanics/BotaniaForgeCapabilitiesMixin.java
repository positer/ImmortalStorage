package com.immortalstorage.immortalstorage.mixin.appliedbotanics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.capability.BlockApiNoContext;
import vazkii.botania.api.capability.BlockApiWithContext;
import vazkii.botania.api.capability.ApiIdBlock;

import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.Map;

/** Makes Botania's block lookup registration safe when an addon queries it first. */
@Mixin(value = BotaniaForgeCapabilities.class, remap = false)
abstract class BotaniaForgeCapabilitiesMixin {
    @Shadow @Final
    private static Map<ApiIdBlock<?>, BlockCapability<?, ?>> FOR_BLOCKS;

    @Inject(method = "registerBlockApiLookup(Lvazkii/botania/api/capability/BlockApiNoContext;)V",
            at = @At("HEAD"), cancellable = true)
    private static void immortalstorage$skipRegisteredNoContext(
            BlockApiNoContext<?> id, CallbackInfo callback) {
        if (FOR_BLOCKS.containsKey(id)) callback.cancel();
    }

    @Inject(method = "registerBlockApiLookup(Lvazkii/botania/api/capability/BlockApiWithContext;)V",
            at = @At("HEAD"), cancellable = true)
    private static void immortalstorage$skipRegisteredWithContext(
            BlockApiWithContext<?, ?> id, CallbackInfo callback) {
        if (FOR_BLOCKS.containsKey(id)) callback.cancel();
    }

    @Inject(method = "getBlockApiLookupById(Lvazkii/botania/api/capability/BlockApiNoContext;)Lnet/neoforged/neoforge/capabilities/BlockCapability;",
            at = @At("HEAD"))
    private static void immortalstorage$ensureNoContext(
            BlockApiNoContext<?> id, CallbackInfoReturnable<?> callback) {
        ensureNoContext(id);
    }

    @Inject(method = "getBlockApiLookupById(Lvazkii/botania/api/capability/BlockApiWithContext;)Lnet/neoforged/neoforge/capabilities/BlockCapability;",
            at = @At("HEAD"))
    private static void immortalstorage$ensureWithContext(
            BlockApiWithContext<?, ?> id, CallbackInfoReturnable<?> callback) {
        ensureWithContext(id);
    }

    private static void ensureNoContext(BlockApiNoContext<?> id) {
        if (!FOR_BLOCKS.containsKey(id)) BotaniaForgeCapabilities.registerBlockApiLookup(id);
    }

    private static void ensureWithContext(BlockApiWithContext<?, ?> id) {
        if (!FOR_BLOCKS.containsKey(id)) BotaniaForgeCapabilities.registerBlockApiLookup(id);
    }
}
