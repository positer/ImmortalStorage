package com.cultivation.cultivation.command;

import com.cultivation.cultivation.player.CultivationPlayerData;
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
public final class CultivationCommands {
    private static final SimpleCommandExceptionType INVALID_SPEED = new SimpleCommandExceptionType(
            Component.literal("Speed must be a fixed gear allowed by the target player's stage, for example 2x."));

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("cultivation").requires(source -> source.hasPermission(2));
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
        CultivationPlayerData data = CultivationPlayerData.get(player);
        data.setStage(stage, player);
        data.syncTo(player);
        com.cultivation.cultivation.event.CommonEvents.restoreStageEffects(player);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.cultivation.stage.success", player.getName(), stage), true);
        return stage;
    }

    private static int unloadRealm(ServerPlayer player,
                                   com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        if (com.cultivation.cultivation.dimension.CultivationDimensions
                .isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
            com.cultivation.cultivation.dimension.RealmHelper.exitRealm(player);
        }
        com.cultivation.cultivation.dimension.RealmHelper.suspendRealmLoading(player.server, player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal(
                "Suspended Xianqiao realm loading for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int reloadRealm(ServerPlayer player,
                                   com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        com.cultivation.cultivation.dimension.RealmHelper.resumeRealmLoading(player);
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
        CultivationPlayerData data = CultivationPlayerData.get(player);
        if (!com.cultivation.cultivation.dimension.RealmTimeScalePolicy
                .isAllowedStep(data.getStage(), permille)) {
            throw INVALID_SPEED.create();
        }
        data.setRealmTimeRatePermille(permille);
        if (!com.cultivation.cultivation.dimension.RealmHelper.isRealmLoadingSuspended(player.getUUID())) {
            com.cultivation.cultivation.dimension.RealmHelper.refreshRealmTickRate(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + " Xianqiao speed to "
                        + data.getTimeScale() + "x"), true);
        return permille;
    }

    private CultivationCommands() {}
}
