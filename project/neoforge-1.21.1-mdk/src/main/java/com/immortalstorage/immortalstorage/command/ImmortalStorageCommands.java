package com.immortalstorage.immortalstorage.command;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Small operator-only progression command retained for real-client stage QA. */
public final class ImmortalStorageCommands {
    private static final SimpleCommandExceptionType INVALID_SPEED = new SimpleCommandExceptionType(
            Component.literal("Speed must be a fixed gear allowed by the target player's stage, for example 2x."));

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("immortalstorage").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("stage")
                .then(Commands.argument("stage", IntegerArgumentType.integer(0, 10))
                        .executes(context -> setStage(context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "stage"), context))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setStage(EntityArgument.getPlayer(context, "player"),
                                        IntegerArgumentType.getInteger(context, "stage"), context)))));
        root.then(Commands.literal("unload")
                .executes(context -> unloadRealm(context.getSource().getPlayerOrException(), context))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> unloadRealm(EntityArgument.getPlayer(context, "player"), context))));
        root.then(Commands.literal("reload")
                .executes(context -> reloadRealm(context.getSource().getPlayerOrException(), context))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> reloadRealm(EntityArgument.getPlayer(context, "player"), context))));
        root.then(Commands.literal("speed")
                .then(Commands.argument("speed", StringArgumentType.word())
                        .executes(context -> setSpeed(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "speed"), context))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setSpeed(EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "speed"), context)))));
        event.getDispatcher().register(root);
    }

    private static int setStage(ServerPlayer player, int stage,
                                com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        data.setStage(stage, player);
        data.syncTo(player);
        com.immortalstorage.immortalstorage.event.CommonEvents.restoreStageEffects(player);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.immortalstorage.stage.success", player.getName(), stage), true);
        return stage;
    }

    private static int unloadRealm(ServerPlayer player,
                                   com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        if (com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions
                .isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
            com.immortalstorage.immortalstorage.dimension.RealmHelper.exitRealm(player);
        }
        com.immortalstorage.immortalstorage.dimension.RealmHelper.suspendRealmLoading(player.server, player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(
                "Suspended Xianqiao realm loading for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reloadRealm(ServerPlayer player,
                                   com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        com.immortalstorage.immortalstorage.dimension.RealmHelper.resumeRealmLoading(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Reloaded Xianqiao realm loading for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int setSpeed(ServerPlayer player, String rawSpeed,
                                com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context)
            throws CommandSyntaxException {
        String normalized = rawSpeed.endsWith("x") || rawSpeed.endsWith("X")
                ? rawSpeed.substring(0, rawSpeed.length() - 1) : rawSpeed;
        final int permille;
        try {
            float parsed = Float.parseFloat(normalized);
            if (!Float.isFinite(parsed)) throw new NumberFormatException("non-finite speed");
            permille = Math.round(parsed * 1_000.0F);
        } catch (NumberFormatException exception) {
            throw INVALID_SPEED.create();
        }
        ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
        if (!com.immortalstorage.immortalstorage.dimension.RealmTimeScalePolicy
                .isAllowedStep(data.getStage(), permille)) {
            throw INVALID_SPEED.create();
        }
        data.setRealmTimeRatePermille(permille);
        if (!com.immortalstorage.immortalstorage.dimension.RealmHelper.isRealmLoadingSuspended(player.getUUID())) {
            com.immortalstorage.immortalstorage.dimension.RealmHelper.refreshRealmTickRate(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + " Xianqiao speed to "
                        + data.getTimeScale() + "x"), true);
        return permille;
    }

    private ImmortalStorageCommands() {}
}
