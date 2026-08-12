package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Persistent, death-copying owner for all per-player cultivation state.
 *
 * <p>NeoForge 1.21.1 data-attachment pattern:
 * https://docs.neoforged.net/docs/1.21.1/datastorage/attachments/
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalStorageMod.MODID);

    public static final Supplier<AttachmentType<ImmortalStoragePlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register(
            "player_data",
            () -> AttachmentType.serializable(holder -> new ImmortalStoragePlayerData(asPlayer(holder)))
                    .copyOnDeath()
                    .sync(new OwnerSyncHandler())
                    .build());

    private static @Nullable Player asPlayer(IAttachmentHolder holder) {
        return holder instanceof Player player ? player : null;
    }

    private static final class OwnerSyncHandler implements AttachmentSyncHandler<ImmortalStoragePlayerData> {
        @Override
        public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
            return holder == to;
        }

        @Override
        public void write(RegistryFriendlyByteBuf buffer, ImmortalStoragePlayerData attachment, boolean initialSync) {
            attachment.writeClientSync(buffer);
        }

        @Override
        public ImmortalStoragePlayerData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buffer,
                                          @Nullable ImmortalStoragePlayerData previousValue) {
            ImmortalStoragePlayerData data = previousValue == null
                    ? new ImmortalStoragePlayerData(asPlayer(holder)) : previousValue;
            data.readClientSync(buffer);
            return data;
        }
    }

    private ModAttachments() {}
}
