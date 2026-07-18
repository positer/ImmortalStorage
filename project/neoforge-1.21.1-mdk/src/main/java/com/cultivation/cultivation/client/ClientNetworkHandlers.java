package com.cultivation.cultivation.client;

import com.cultivation.cultivation.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClientNetworkHandlers {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(ModPayloads.TerminalViewSnapshot.TYPE,
                ModPayloads.TerminalViewSnapshot.STREAM_CODEC,
                ClientNetworkHandlers::handleTerminalViewSnapshot);
        registrar.playToClient(ModPayloads.TerminalFluidViewSnapshot.TYPE,
                ModPayloads.TerminalFluidViewSnapshot.STREAM_CODEC,
                ClientNetworkHandlers::handleTerminalFluidViewSnapshot);
        registrar.playToClient(ModPayloads.TerminalRecipeSources.TYPE,
                ModPayloads.TerminalRecipeSources.STREAM_CODEC,
                ClientNetworkHandlers::handleTerminalRecipeSources);
        registrar.playToClient(ModPayloads.OpenJadeGuideScreen.TYPE,
                ModPayloads.OpenJadeGuideScreen.STREAM_CODEC,
                ClientNetworkHandlers::handleOpenJadeGuide);
        registrar.playToClient(ModPayloads.SpiritStaffBuildPreviewSnapshot.TYPE,
                ModPayloads.SpiritStaffBuildPreviewSnapshot.STREAM_CODEC,
                ClientNetworkHandlers::handleSpiritStaffBuildPreview);
    }

    private static void handleOpenJadeGuide(ModPayloads.OpenJadeGuideScreen ignored, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new com.cultivation.cultivation.client.screen.JadeGuideScreen()));
    }

    private static void handleSpiritStaffBuildPreview(
            ModPayloads.SpiritStaffBuildPreviewSnapshot snapshot, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.cultivation.cultivation.client.render.SpiritStaffBuildPreview
                .applyServerSnapshot(snapshot));
    }

    private static void handleTerminalRecipeSources(ModPayloads.TerminalRecipeSources chunk, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.player.containerMenu.containerId != chunk.containerId()
                    || !(minecraft.player.containerMenu instanceof com.cultivation.cultivation.api.storage.terminal.CraftingTransferTarget target)) {
                return;
            }
            java.util.List<com.cultivation.cultivation.api.storage.terminal.CraftingTransferTarget.TransferIngredient> entries =
                    chunk.entries().stream()
                            .map(entry -> new com.cultivation.cultivation.api.storage.terminal.CraftingTransferTarget.TransferIngredient(
                                    entry.stack(), entry.amount()))
                            .toList();
            target.applyRecipeSourceChunk(chunk.revision(), chunk.chunkIndex(), chunk.chunkCount(), entries);
        });
    }

    public static void handleTerminalViewSnapshot(ModPayloads.TerminalViewSnapshot snapshot, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                    || !(minecraft.player.containerMenu instanceof com.cultivation.cultivation.menu.custom.XianqiaoStorageMenu menu)
                    || menu.containerId != snapshot.containerId()) {
                return;
            }
            java.util.List<com.cultivation.cultivation.api.storage.terminal.TerminalEntry> entries = new java.util.ArrayList<>();
            for (ModPayloads.TerminalViewSnapshot.Entry entry : snapshot.entries()) {
                entries.add(new com.cultivation.cultivation.api.storage.terminal.TerminalEntry(
                        entry.entryId(), entry.stack(), entry.amount()));
            }
            menu.applyClientSnapshot(snapshot.revision(), snapshot.visibleRows(), snapshot.baseRow(), snapshot.bufferBaseRow(),
                    snapshot.totalRows(), snapshot.totalItemEntries(), java.util.List.copyOf(entries));
        });
    }

    public static void handleTerminalFluidViewSnapshot(ModPayloads.TerminalFluidViewSnapshot snapshot,
                                                       IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                    || !(minecraft.player.containerMenu instanceof com.cultivation.cultivation.menu.custom.XianqiaoStorageMenu menu)
                    || menu.containerId != snapshot.containerId()) {
                return;
            }
            java.util.List<com.cultivation.cultivation.api.storage.terminal.TerminalFluidEntry> entries =
                    new java.util.ArrayList<>();
            for (ModPayloads.TerminalFluidViewSnapshot.Entry entry : snapshot.entries()) {
                entries.add(new com.cultivation.cultivation.api.storage.terminal.TerminalFluidEntry(
                        entry.entryId(), entry.stack(), entry.amountMb()));
            }
            menu.applyClientFluidSnapshot(snapshot.revision(), snapshot.visibleRows(), snapshot.baseRow(),
                    snapshot.bufferBaseRow(), snapshot.totalRows(), snapshot.totalItemEntries(),
                    java.util.List.copyOf(entries));
        });
    }

    private ClientNetworkHandlers() {}
}
