package com.immortalstorage.immortalstorage.network;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Payloads used by the ImmortalStorage mod. */
public enum ModPayloads {
    ;

    /** Request to open the player's kongqiao (1 ?  ? inventory. */
    public record OpenKongqiao() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenKongqiao> STREAM_CODEC =
                StreamCodec.unit(new OpenKongqiao());
        public static final CustomPacketPayload.Type<OpenKongqiao> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "open_kongqiao"));
        @Override public CustomPacketPayload.Type<OpenKongqiao> type() { return TYPE; }
    }

    /** Request to open the player's xianqiao-storage (6 ?  ? inventory. */
    public record OpenXianqiaoStorage() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenXianqiaoStorage> STREAM_CODEC =
                StreamCodec.unit(new OpenXianqiaoStorage());
        public static final CustomPacketPayload.Type<OpenXianqiaoStorage> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "open_xianqiao"));
        @Override public CustomPacketPayload.Type<OpenXianqiaoStorage> type() { return TYPE; }
    }

    /** Request to start a tribulation (   ).  Server checks stage 6 ? and
     *  spawns the appropriate 1/3/5        1/3/5  ?raid. */
    public record TriggerTribulation(int containerId) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TriggerTribulation> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, TriggerTribulation::containerId, TriggerTribulation::new);
        public static final CustomPacketPayload.Type<TriggerTribulation> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "trigger_tribulation"));
        @Override public CustomPacketPayload.Type<TriggerTribulation> type() { return TYPE; }
    }

    /** Tick-rate delta for the personal realm's time scale ( ? / +1). */
    public record TimeFlow(int containerId, int delta) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TimeFlow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, TimeFlow::containerId,
                        ByteBufCodecs.VAR_INT, TimeFlow::delta,
                        TimeFlow::new);
        public static final CustomPacketPayload.Type<TimeFlow> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "time_flow"));
        @Override public CustomPacketPayload.Type<TimeFlow> type() { return TYPE; }
    }

    /** Synchronize the active right-side storage tab with the authoritative server menu. */
    public record SetStorageModule(int module) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetStorageModule> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetStorageModule::module, SetStorageModule::new);
        public static final CustomPacketPayload.Type<SetStorageModule> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_storage_module"));
        @Override public CustomPacketPayload.Type<SetStorageModule> type() { return TYPE; }
    }

    /** Bind the authoritative terminal row window after client-side height clamping or scrolling. */
    public record SetTerminalViewport(int visibleRows, int baseRow) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetTerminalViewport> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SetTerminalViewport::visibleRows,
                        ByteBufCodecs.VAR_INT, SetTerminalViewport::baseRow,
                        SetTerminalViewport::new);
        public static final CustomPacketPayload.Type<SetTerminalViewport> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_terminal_viewport"));
        @Override public CustomPacketPayload.Type<SetTerminalViewport> type() { return TYPE; }
    }

    /** Apply a bounded query and sort order to the authoritative terminal view. */
    public record SetTerminalQuery(String text, int sortOrder, int sortDirection) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetTerminalQuery> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.MAX_SEARCH_LENGTH), SetTerminalQuery::text,
                        ByteBufCodecs.VAR_INT, SetTerminalQuery::sortOrder,
                        ByteBufCodecs.VAR_INT, SetTerminalQuery::sortDirection,
                        SetTerminalQuery::new);
        public static final CustomPacketPayload.Type<SetTerminalQuery> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_terminal_query"));
        @Override public CustomPacketPayload.Type<SetTerminalQuery> type() { return TYPE; }
    }

    /** Operate one stable aggregated entry in one exact terminal menu snapshot. */
    public record TerminalEntryAction(int containerId, long revision, long entryId, int action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalEntryAction> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TerminalEntryAction decode(RegistryFriendlyByteBuf buffer) {
                return new TerminalEntryAction(buffer.readVarInt(), buffer.readVarLong(),
                        buffer.readVarLong(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, TerminalEntryAction payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarLong(payload.revision());
                buffer.writeVarLong(payload.entryId());
                buffer.writeVarInt(payload.action());
            }
        };
        public static final CustomPacketPayload.Type<TerminalEntryAction> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "terminal_entry_action"));
        @Override public CustomPacketPayload.Type<TerminalEntryAction> type() { return TYPE; }
    }

    /** Toggle locked day/night (action 0) or cycle locked weather (action 1). */
    public record RealmEnvironment(int containerId, int action) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, RealmEnvironment> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, RealmEnvironment::containerId,
                        ByteBufCodecs.VAR_INT, RealmEnvironment::action,
                        RealmEnvironment::new);
        public static final CustomPacketPayload.Type<RealmEnvironment> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "realm_environment"));
        @Override public CustomPacketPayload.Type<RealmEnvironment> type() { return TYPE; }
    }

    public record ShowSubstitutePuppetActivation(ItemStack stack) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShowSubstitutePuppetActivation> STREAM_CODEC =
                StreamCodec.composite(ItemStack.STREAM_CODEC, ShowSubstitutePuppetActivation::stack,
                        ShowSubstitutePuppetActivation::new);
        public static final CustomPacketPayload.Type<ShowSubstitutePuppetActivation> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "show_substitute_puppet_activation"));
        @Override public CustomPacketPayload.Type<ShowSubstitutePuppetActivation> type() { return TYPE; }
    }

    public record SetStabilizedRuinValue(int containerId, int index, int value) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetStabilizedRuinValue> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetStabilizedRuinValue::containerId,
                ByteBufCodecs.VAR_INT, SetStabilizedRuinValue::index,
                ByteBufCodecs.VAR_INT, SetStabilizedRuinValue::value,
                SetStabilizedRuinValue::new);
        public static final CustomPacketPayload.Type<SetStabilizedRuinValue> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_stabilized_ruin_value"));
        @Override public CustomPacketPayload.Type<SetStabilizedRuinValue> type() { return TYPE; }
    }

    public record SetStabilizedRuinFilter(int containerId, int slot, ItemStack stack) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetStabilizedRuinFilter> STREAM_CODEC = new StreamCodec<>() {
            @Override public SetStabilizedRuinFilter decode(RegistryFriendlyByteBuf buffer) {
                return new SetStabilizedRuinFilter(buffer.readVarInt(), buffer.readVarInt(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, SetStabilizedRuinFilter value) {
                buffer.writeVarInt(value.containerId); buffer.writeVarInt(value.slot);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, value.stack);
            }
        };
        public static final CustomPacketPayload.Type<SetStabilizedRuinFilter> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_stabilized_ruin_filter"));
        @Override public CustomPacketPayload.Type<SetStabilizedRuinFilter> type() { return TYPE; }
    }

    public record ToggleStabilizedRuinFilterMode(int containerId, int mode) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleStabilizedRuinFilterMode> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToggleStabilizedRuinFilterMode::containerId,
                ByteBufCodecs.VAR_INT, ToggleStabilizedRuinFilterMode::mode,
                ToggleStabilizedRuinFilterMode::new);
        public static final CustomPacketPayload.Type<ToggleStabilizedRuinFilterMode> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "toggle_stabilized_ruin_filter_mode"));
        @Override public CustomPacketPayload.Type<ToggleStabilizedRuinFilterMode> type() { return TYPE; }
    }

    /** Selects the item or typed-fluid directory within the same Xianqiao menu. */
    public record SetTerminalChannel(int containerId, boolean fluid) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SetTerminalChannel> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SetTerminalChannel::containerId,
                        ByteBufCodecs.BOOL, SetTerminalChannel::fluid,
                        SetTerminalChannel::new);
        public static final CustomPacketPayload.Type<SetTerminalChannel> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_terminal_channel"));
        @Override public CustomPacketPayload.Type<SetTerminalChannel> type() { return TYPE; }
    }

    /** Deposits a carried fluid container or fills it from one stable fluid entry. */
    public record TerminalFluidEntryAction(int containerId, long revision, long entryId,
                                           boolean deposit) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalFluidEntryAction> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TerminalFluidEntryAction decode(RegistryFriendlyByteBuf buffer) {
                return new TerminalFluidEntryAction(buffer.readVarInt(), buffer.readVarLong(),
                        buffer.readVarLong(), buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, TerminalFluidEntryAction payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarLong(payload.revision());
                buffer.writeVarLong(payload.entryId());
                buffer.writeBoolean(payload.deposit());
            }
        };
        public static final CustomPacketPayload.Type<TerminalFluidEntryAction> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "terminal_fluid_entry_action"));
        @Override public CustomPacketPayload.Type<TerminalFluidEntryAction> type() { return TYPE; }
    }

    /** Current 2R-buffered typed-fluid directory; amounts remain exact long mB. */
    public record TerminalFluidViewSnapshot(int containerId, long revision, int visibleRows,
                                             int baseRow, int bufferBaseRow, int totalRows,
                                             int totalItemEntries, int totalFluidEntries,
                                             List<Entry> entries) implements CustomPacketPayload {
        public static final int MAX_ENTRIES = TerminalViewSnapshot.MAX_ENTRIES;
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalFluidViewSnapshot> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TerminalFluidViewSnapshot decode(RegistryFriendlyByteBuf buffer) {
                int containerId = buffer.readVarInt();
                long revision = buffer.readVarLong();
                int visibleRows = buffer.readVarInt();
                int baseRow = buffer.readVarInt();
                int bufferBaseRow = buffer.readVarInt();
                int totalRows = buffer.readVarInt();
                int totalItemEntries = buffer.readVarInt();
                int totalFluidEntries = buffer.readVarInt();
                int size = buffer.readVarInt();
                if (size < 0 || size > MAX_ENTRIES) {
                    throw new io.netty.handler.codec.DecoderException("Invalid terminal fluid snapshot size: " + size);
                }
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buffer.readVarLong(), FluidStack.STREAM_CODEC.decode(buffer),
                            buffer.readVarLong()));
                }
                return new TerminalFluidViewSnapshot(containerId, revision, visibleRows, baseRow,
                        bufferBaseRow, totalRows, totalItemEntries, totalFluidEntries, List.copyOf(entries));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, TerminalFluidViewSnapshot snapshot) {
                buffer.writeVarInt(snapshot.containerId());
                buffer.writeVarLong(snapshot.revision());
                buffer.writeVarInt(snapshot.visibleRows());
                buffer.writeVarInt(snapshot.baseRow());
                buffer.writeVarInt(snapshot.bufferBaseRow());
                buffer.writeVarInt(snapshot.totalRows());
                buffer.writeVarInt(snapshot.totalItemEntries());
                buffer.writeVarInt(snapshot.totalFluidEntries());
                int size = Math.min(MAX_ENTRIES, snapshot.entries().size());
                buffer.writeVarInt(size);
                for (int index = 0; index < size; index++) {
                    Entry entry = snapshot.entries().get(index);
                    buffer.writeVarLong(entry.entryId());
                    FluidStack.STREAM_CODEC.encode(buffer, entry.stack().copyWithAmount(1));
                    buffer.writeVarLong(entry.amountMb());
                }
            }
        };
        public static final CustomPacketPayload.Type<TerminalFluidViewSnapshot> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "terminal_fluid_view_snapshot"));
        @Override public CustomPacketPayload.Type<TerminalFluidViewSnapshot> type() { return TYPE; }

        public record Entry(long entryId, FluidStack stack, long amountMb) {
            public Entry {
                if (entryId == 0L || stack == null || stack.isEmpty() || amountMb <= 0L) {
                    throw new IllegalArgumentException("Invalid terminal fluid snapshot entry");
                }
                stack = stack.copyWithAmount(1);
            }
            @Override public FluidStack stack() { return stack.copyWithAmount(1); }
        }
    }

    /** Current external-resource rows in the same unified item/fluid terminal directory. */
    public record TerminalExternalViewSnapshot(int containerId, long revision, int totalExternalEntries,
                                               List<Entry> entries) implements CustomPacketPayload {
        public static final int MAX_ENTRIES = TerminalViewSnapshot.MAX_ENTRIES;
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExternalViewSnapshot> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override public TerminalExternalViewSnapshot decode(RegistryFriendlyByteBuf buffer) {
                        int containerId = buffer.readVarInt();
                        long revision = buffer.readVarLong();
                        int total = buffer.readVarInt();
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_ENTRIES) {
                            throw new io.netty.handler.codec.DecoderException(
                                    "Invalid terminal external snapshot size: " + size);
                        }
                        List<Entry> entries = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            entries.add(new Entry(buffer.readVarLong(), buffer.readUtf(), buffer.readUtf(),
                                    buffer.readVarLong()));
                        }
                        return new TerminalExternalViewSnapshot(containerId, revision, total, List.copyOf(entries));
                    }
                    @Override public void encode(RegistryFriendlyByteBuf buffer,
                                                 TerminalExternalViewSnapshot snapshot) {
                        buffer.writeVarInt(snapshot.containerId());
                        buffer.writeVarLong(snapshot.revision());
                        buffer.writeVarInt(snapshot.totalExternalEntries());
                        int size = Math.min(MAX_ENTRIES, snapshot.entries().size());
                        buffer.writeVarInt(size);
                        for (int i = 0; i < size; i++) {
                            Entry entry = snapshot.entries().get(i);
                            buffer.writeVarLong(entry.entryId());
                            buffer.writeUtf(entry.channel());
                            buffer.writeUtf(entry.resourceId());
                            buffer.writeVarLong(entry.amount());
                        }
                    }
                };
        public static final CustomPacketPayload.Type<TerminalExternalViewSnapshot> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "terminal_external_view_snapshot"));
        @Override public CustomPacketPayload.Type<TerminalExternalViewSnapshot> type() { return TYPE; }

        public record Entry(long entryId, String channel, String resourceId, long amount) {}
    }

    /** Server-authoritative terminal recipe placement requested by an optional recipe viewer. */
    public record TransferTerminalRecipe(int containerId, long revision, ResourceLocation recipeId,
                                         int requestedSets) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, TransferTerminalRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TransferTerminalRecipe decode(RegistryFriendlyByteBuf buffer) {
                int containerId = buffer.readVarInt();
                long revision = buffer.readVarLong();
                ResourceLocation recipeId = buffer.readResourceLocation();
                int requestedSets = buffer.readVarInt();
                return new TransferTerminalRecipe(containerId, revision, recipeId, requestedSets);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, TransferTerminalRecipe payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarLong(payload.revision());
                buffer.writeResourceLocation(payload.recipeId());
                buffer.writeVarInt(payload.requestedSets());
            }
        };
        public static final CustomPacketPayload.Type<TransferTerminalRecipe> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "transfer_terminal_recipe"));
        @Override public CustomPacketPayload.Type<TransferTerminalRecipe> type() { return TYPE; }
    }

    /** Current server query window for a client terminal. Full catalogs remain server-owned. */
    public record TerminalViewSnapshot(int containerId, long revision, int visibleRows, int baseRow, int bufferBaseRow,
                                       int totalRows, int totalItemEntries,
                                       List<Entry> entries) implements CustomPacketPayload {
        public static final int MAX_ENTRIES = (com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.MAX_ROWS * 2)
                * com.immortalstorage.immortalstorage.api.storage.terminal.TerminalViewport.COLUMNS;
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewSnapshot> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TerminalViewSnapshot decode(RegistryFriendlyByteBuf buffer) {
                int containerId = buffer.readVarInt();
                long revision = buffer.readVarLong();
                int visibleRows = buffer.readVarInt();
                int baseRow = buffer.readVarInt();
                int bufferBaseRow = buffer.readVarInt();
                int totalRows = buffer.readVarInt();
                int totalItemEntries = buffer.readVarInt();
                int size = Math.min(MAX_ENTRIES, Math.max(0, buffer.readVarInt()));
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buffer.readVarLong(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarLong()));
                }
                return new TerminalViewSnapshot(containerId, revision, visibleRows, baseRow, bufferBaseRow,
                        totalRows, totalItemEntries, List.copyOf(entries));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, TerminalViewSnapshot snapshot) {
                buffer.writeVarInt(snapshot.containerId());
                buffer.writeVarLong(snapshot.revision());
                buffer.writeVarInt(snapshot.visibleRows());
                buffer.writeVarInt(snapshot.baseRow());
                buffer.writeVarInt(snapshot.bufferBaseRow());
                buffer.writeVarInt(snapshot.totalRows());
                buffer.writeVarInt(snapshot.totalItemEntries());
                int size = Math.min(MAX_ENTRIES, snapshot.entries().size());
                buffer.writeVarInt(size);
                for (int i = 0; i < size; i++) {
                    Entry entry = snapshot.entries().get(i);
                    buffer.writeVarLong(entry.entryId());
                    ItemStack.STREAM_CODEC.encode(buffer, entry.stack().copyWithCount(1));
                    buffer.writeVarLong(entry.amount());
                }
            }
        };
        public static final CustomPacketPayload.Type<TerminalViewSnapshot> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "terminal_view_snapshot"));
        @Override public CustomPacketPayload.Type<TerminalViewSnapshot> type() { return TYPE; }

        public record Entry(long entryId, ItemStack stack, long amount) {
            public Entry {
                stack = stack.copyWithCount(1);
                amount = Math.max(1L, amount);
            }
        }
    }

    /** One bounded chunk of the read-only full storage catalog used by recipe viewers. */
    public record TerminalRecipeSources(int containerId, long revision, int chunkIndex, int chunkCount,
                                        List<Entry> entries) implements CustomPacketPayload {
        public static final int MAX_ENTRIES = 256;
        public TerminalRecipeSources {
            if (chunkIndex < 0 || chunkCount < 1 || chunkIndex >= chunkCount || entries == null || entries.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("Invalid terminal recipe-source chunk");
            }
            entries = List.copyOf(entries);
        }
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRecipeSources> STREAM_CODEC = new StreamCodec<>() {
            @Override public TerminalRecipeSources decode(RegistryFriendlyByteBuf buffer) {
                int containerId = buffer.readVarInt();
                long revision = buffer.readVarLong();
                int chunkIndex = buffer.readVarInt();
                int chunkCount = Math.max(1, buffer.readVarInt());
                int size = Math.min(MAX_ENTRIES, Math.max(0, buffer.readVarInt()));
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarLong()));
                }
                return new TerminalRecipeSources(containerId, revision, chunkIndex, chunkCount, entries);
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, TerminalRecipeSources payload) {
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarLong(payload.revision());
                buffer.writeVarInt(payload.chunkIndex());
                buffer.writeVarInt(payload.chunkCount());
                buffer.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    ItemStack.STREAM_CODEC.encode(buffer, entry.stack().copyWithCount(1));
                    buffer.writeVarLong(entry.amount());
                }
            }
        };
        public static final CustomPacketPayload.Type<TerminalRecipeSources> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "terminal_recipe_sources"));
        @Override public CustomPacketPayload.Type<TerminalRecipeSources> type() { return TYPE; }

        public record Entry(ItemStack stack, long amount) {
            public Entry {
                if (stack == null || stack.isEmpty() || amount <= 0L) throw new IllegalArgumentException("Invalid recipe source");
                stack = stack.copyWithCount(1);
            }
            @Override public ItemStack stack() { return stack.copy(); }
        }
    }

    /** Request to enter or exit the personal    (      ). */
    public record ToggleRealm() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleRealm> STREAM_CODEC =
                StreamCodec.unit(new ToggleRealm());
        public static final CustomPacketPayload.Type<ToggleRealm> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "toggle_realm"));
        @Override public CustomPacketPayload.Type<ToggleRealm> type() { return TYPE; }
    }

    /** Cycle the held Spirit Staff mode on the authoritative server stack. */
    public record CycleStaffMode(int delta) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleStaffMode> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, CycleStaffMode::delta, CycleStaffMode::new);
        public static final CustomPacketPayload.Type<CycleStaffMode> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "cycle_staff_mode"));
        @Override public CustomPacketPayload.Type<CycleStaffMode> type() { return TYPE; }
    }

    public record AdjustStaffTeleportDistance(int delta) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, AdjustStaffTeleportDistance> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, AdjustStaffTeleportDistance::delta,
                        AdjustStaffTeleportDistance::new);
        public static final CustomPacketPayload.Type<AdjustStaffTeleportDistance> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "adjust_staff_teleport_distance"));
        @Override public CustomPacketPayload.Type<AdjustStaffTeleportDistance> type() { return TYPE; }
    }

    /** Request one non-mutating, server-authoritative Spirit Staff build job. */
    public record RequestSpiritStaffBuildPreview(int requestId, long pos, int face, int hand, boolean removal)
            implements CustomPacketPayload {
        public RequestSpiritStaffBuildPreview(int requestId, BlockPos pos, int face, int hand, boolean removal) {
            this(requestId, pos.asLong(), face, hand, removal);
        }

        public BlockPos blockPos() { return BlockPos.of(pos); }

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSpiritStaffBuildPreview> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, RequestSpiritStaffBuildPreview::requestId,
                        ByteBufCodecs.VAR_LONG, RequestSpiritStaffBuildPreview::pos,
                        ByteBufCodecs.VAR_INT, RequestSpiritStaffBuildPreview::face,
                        ByteBufCodecs.VAR_INT, RequestSpiritStaffBuildPreview::hand,
                        ByteBufCodecs.BOOL, RequestSpiritStaffBuildPreview::removal,
                        RequestSpiritStaffBuildPreview::new);
        public static final CustomPacketPayload.Type<RequestSpiritStaffBuildPreview> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "request_spirit_staff_build_preview"));
        @Override public CustomPacketPayload.Type<RequestSpiritStaffBuildPreview> type() { return TYPE; }
    }

    /** Bound special-operation request for one no-drop connected-layer removal. */
    public record RemoveSpiritStaffBuildLayer(long pos, int face, int hand) implements CustomPacketPayload {
        public RemoveSpiritStaffBuildLayer(BlockPos pos, int face, int hand) {
            this(pos.asLong(), face, hand);
        }
        public BlockPos blockPos() { return BlockPos.of(pos); }
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveSpiritStaffBuildLayer> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG, RemoveSpiritStaffBuildLayer::pos,
                        ByteBufCodecs.VAR_INT, RemoveSpiritStaffBuildLayer::face,
                        ByteBufCodecs.VAR_INT, RemoveSpiritStaffBuildLayer::hand,
                        RemoveSpiritStaffBuildLayer::new);
        public static final CustomPacketPayload.Type<RemoveSpiritStaffBuildLayer> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "remove_spirit_staff_build_layer"));
        @Override public CustomPacketPayload.Type<RemoveSpiritStaffBuildLayer> type() { return TYPE; }
    }

    /** Idempotent intent for summoning or returning a Spirit Sword. */
    public record SpiritSwordFurnaceOperation(int action, int hand) implements CustomPacketPayload {
        public static final int SUMMON = 0;
        public static final int STORE = 1;
        public static final StreamCodec<RegistryFriendlyByteBuf, SpiritSwordFurnaceOperation> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SpiritSwordFurnaceOperation::action,
                        ByteBufCodecs.VAR_INT, SpiritSwordFurnaceOperation::hand,
                        SpiritSwordFurnaceOperation::new);
        public static final CustomPacketPayload.Type<SpiritSwordFurnaceOperation> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "spirit_sword_furnace_operation"));
        @Override public CustomPacketPayload.Type<SpiritSwordFurnaceOperation> type() { return TYPE; }
    }

    /** Bounded server build job rendered without recalculating storage on the client. */
    public record SpiritStaffBuildPreviewSnapshot(
            int requestId, long pos, int face, int hand, boolean removal, int failure, List<Long> positions)
            implements CustomPacketPayload {
        public static final int MAX_POSITIONS = 4096;

        public SpiritStaffBuildPreviewSnapshot {
            if (positions == null || positions.size() > MAX_POSITIONS) {
                throw new IllegalArgumentException("Invalid Spirit Staff preview size");
            }
            positions = List.copyOf(positions);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, SpiritStaffBuildPreviewSnapshot> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SpiritStaffBuildPreviewSnapshot decode(RegistryFriendlyByteBuf buffer) {
                        int requestId = buffer.readVarInt();
                        long pos = buffer.readVarLong();
                        int face = buffer.readVarInt();
                        int hand = buffer.readVarInt();
                        boolean removal = buffer.readBoolean();
                        int failure = buffer.readVarInt();
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_POSITIONS) {
                            throw new IllegalArgumentException("Invalid Spirit Staff preview size: " + size);
                        }
                        List<Long> positions = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) positions.add(buffer.readVarLong());
                        return new SpiritStaffBuildPreviewSnapshot(
                                requestId, pos, face, hand, removal, failure, positions);
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer, SpiritStaffBuildPreviewSnapshot snapshot) {
                        buffer.writeVarInt(snapshot.requestId());
                        buffer.writeVarLong(snapshot.pos());
                        buffer.writeVarInt(snapshot.face());
                        buffer.writeVarInt(snapshot.hand());
                        buffer.writeBoolean(snapshot.removal());
                        buffer.writeVarInt(snapshot.failure());
                        buffer.writeVarInt(snapshot.positions().size());
                        for (long position : snapshot.positions()) buffer.writeVarLong(position);
                    }
                };
        public static final CustomPacketPayload.Type<SpiritStaffBuildPreviewSnapshot> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "spirit_staff_build_preview"));
        @Override public CustomPacketPayload.Type<SpiritStaffBuildPreviewSnapshot> type() { return TYPE; }
    }

    /** Set one source-vein side I/O mode. */
    public record SetSourceSideMode(long pos, int side, int mode) implements CustomPacketPayload {
        public SetSourceSideMode(BlockPos pos, int side, int mode) {
            this(pos.asLong(), side, mode);
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetSourceSideMode> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG, SetSourceSideMode::pos,
                        ByteBufCodecs.VAR_INT, SetSourceSideMode::side,
                        ByteBufCodecs.VAR_INT, SetSourceSideMode::mode,
                        SetSourceSideMode::new);
        public static final CustomPacketPayload.Type<SetSourceSideMode> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_source_side_mode"));
        @Override public CustomPacketPayload.Type<SetSourceSideMode> type() { return TYPE; }
    }

    /** Increase or decrease the open source vein throughput by one fixed step. */
    public record AdjustSourceFlux(int containerId, long pos, int direction) implements CustomPacketPayload {
        public AdjustSourceFlux(int containerId, BlockPos pos, int direction) {
            this(containerId, pos.asLong(), direction);
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, AdjustSourceFlux> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, AdjustSourceFlux::containerId,
                        ByteBufCodecs.VAR_LONG, AdjustSourceFlux::pos,
                        ByteBufCodecs.VAR_INT, AdjustSourceFlux::direction,
                        AdjustSourceFlux::new);
        public static final CustomPacketPayload.Type<AdjustSourceFlux> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "adjust_source_flux"));
        @Override public CustomPacketPayload.Type<AdjustSourceFlux> type() { return TYPE; }
    }

    /** Set an exact source-vein throughput from its open, server-authoritative menu. */
    public record SetSourceFluxLimit(int containerId, long pos, long fluxLimit) implements CustomPacketPayload {
        public SetSourceFluxLimit(int containerId, BlockPos pos, long fluxLimit) {
            this(containerId, pos.asLong(), fluxLimit);
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetSourceFluxLimit> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SetSourceFluxLimit::containerId,
                        ByteBufCodecs.VAR_LONG, SetSourceFluxLimit::pos,
                        ByteBufCodecs.VAR_LONG, SetSourceFluxLimit::fluxLimit,
                        SetSourceFluxLimit::new);
        public static final CustomPacketPayload.Type<SetSourceFluxLimit> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "set_source_flux_limit"));
        @Override public CustomPacketPayload.Type<SetSourceFluxLimit> type() { return TYPE; }
    }

    /** Changes one physical face of an open, owner-bound Xianqiao Interface. */
    public record SetXianqiaoInterfaceSideMode(
            int containerId, long pos, long configRevision, int side, int mode)
            implements CustomPacketPayload {
        public SetXianqiaoInterfaceSideMode(
                int containerId, BlockPos pos, long configRevision, int side, int mode) {
            this(containerId, pos.asLong(), configRevision, side, mode);
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceSideMode> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SetXianqiaoInterfaceSideMode decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceSideMode(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer,
                                       SetXianqiaoInterfaceSideMode payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.side());
                        buffer.writeVarInt(payload.mode());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceSideMode> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_side_mode"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceSideMode> type() { return TYPE; }
    }

    /** Sets the real one-stack cache target for one configured interface slot. */
    public record SetXianqiaoInterfaceTargetAmount(
            int containerId, long pos, long configRevision, int slot, long amount)
            implements CustomPacketPayload {
        public SetXianqiaoInterfaceTargetAmount(
                int containerId, BlockPos pos, long configRevision, int slot, long amount) {
            this(containerId, pos.asLong(), configRevision, slot, amount);
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceTargetAmount> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SetXianqiaoInterfaceTargetAmount decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceTargetAmount(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readVarInt(), buffer.readVarLong());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer,
                                       SetXianqiaoInterfaceTargetAmount payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.slot());
                        buffer.writeVarLong(payload.amount());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceTargetAmount> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_target_amount"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceTargetAmount> type() { return TYPE; }
    }

    /** Toggles one selected cache slot's participation on one physical face. */
    public record SetXianqiaoInterfaceSlotFaceMask(
            int containerId, long pos, long configRevision, int slot, int side, boolean enabled)
            implements CustomPacketPayload {
        public SetXianqiaoInterfaceSlotFaceMask(
                int containerId, BlockPos pos, long configRevision, int slot, int side, boolean enabled) {
            this(containerId, pos.asLong(), configRevision, slot, side, enabled);
        }
        public BlockPos blockPos() { return BlockPos.of(pos); }
        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceSlotFaceMask> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override public SetXianqiaoInterfaceSlotFaceMask decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceSlotFaceMask(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
                    }
                    @Override public void encode(RegistryFriendlyByteBuf buffer, SetXianqiaoInterfaceSlotFaceMask payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.slot());
                        buffer.writeVarInt(payload.side());
                        buffer.writeBoolean(payload.enabled());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceSlotFaceMask> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_slot_face_mask"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceSlotFaceMask> type() { return TYPE; }
    }

    /** Enables or disables the interface's active pull/push scheduler. */
    public record SetXianqiaoInterfaceActiveTransfer(
            int containerId, long pos, long configRevision, boolean pull, boolean enabled)
            implements CustomPacketPayload {
        public SetXianqiaoInterfaceActiveTransfer(
                int containerId, BlockPos pos, long configRevision, boolean pull, boolean enabled) {
            this(containerId, pos.asLong(), configRevision, pull, enabled);
        }
        public BlockPos blockPos() { return BlockPos.of(pos); }
        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceActiveTransfer> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override public SetXianqiaoInterfaceActiveTransfer decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceActiveTransfer(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readBoolean(), buffer.readBoolean());
                    }
                    @Override public void encode(RegistryFriendlyByteBuf buffer, SetXianqiaoInterfaceActiveTransfer payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeBoolean(payload.pull());
                        buffer.writeBoolean(payload.enabled());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceActiveTransfer> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_active_transfer"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceActiveTransfer> type() { return TYPE; }
    }

    /** Configures one item identity from a viewer ghost ingredient or equivalent client action. */
    public record SetXianqiaoInterfaceItemTarget(
            int containerId, long pos, long configRevision, int slot,
            ItemStack identity, long requestedAmount) implements CustomPacketPayload {
        public SetXianqiaoInterfaceItemTarget {
            if (identity == null || identity.isEmpty()) {
                throw new IllegalArgumentException("item target identity is required");
            }
            identity = identity.copyWithCount(1);
        }

        public SetXianqiaoInterfaceItemTarget(
                int containerId, BlockPos pos, long configRevision, int slot,
                ItemStack identity, long requestedAmount) {
            this(containerId, pos.asLong(), configRevision, slot, identity, requestedAmount);
        }

        @Override public ItemStack identity() { return identity.copyWithCount(1); }
        public BlockPos blockPos() { return BlockPos.of(pos); }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceItemTarget> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SetXianqiaoInterfaceItemTarget decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceItemTarget(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readVarInt(),
                                ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarLong());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer,
                                       SetXianqiaoInterfaceItemTarget payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.slot());
                        ItemStack.STREAM_CODEC.encode(buffer, payload.identity());
                        buffer.writeVarLong(payload.requestedAmount());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceItemTarget> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_item_target"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceItemTarget> type() { return TYPE; }
    }

    /** Configures one component-aware fluid identity; requested amount is always expressed in mB. */
    public record SetXianqiaoInterfaceFluidTarget(
            int containerId, long pos, long configRevision, int slot,
            FluidStack identity, long requestedAmountMb) implements CustomPacketPayload {
        public SetXianqiaoInterfaceFluidTarget {
            if (identity == null || identity.isEmpty()) {
                throw new IllegalArgumentException("fluid target identity is required");
            }
            identity = identity.copyWithAmount(1);
        }

        public SetXianqiaoInterfaceFluidTarget(
                int containerId, BlockPos pos, long configRevision, int slot,
                FluidStack identity, long requestedAmountMb) {
            this(containerId, pos.asLong(), configRevision, slot, identity, requestedAmountMb);
        }

        @Override public FluidStack identity() { return identity.copyWithAmount(1); }
        public BlockPos blockPos() { return BlockPos.of(pos); }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceFluidTarget> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override
                    public SetXianqiaoInterfaceFluidTarget decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceFluidTarget(buffer.readVarInt(), buffer.readVarLong(),
                                buffer.readVarLong(), buffer.readVarInt(),
                                FluidStack.STREAM_CODEC.decode(buffer), buffer.readVarLong());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer,
                                       SetXianqiaoInterfaceFluidTarget payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.slot());
                        FluidStack.STREAM_CODEC.encode(buffer, payload.identity());
                        buffer.writeVarLong(payload.requestedAmountMb());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceFluidTarget> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_fluid_target"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceFluidTarget> type() { return TYPE; }
    }

    /** Configures one installed external-resource identity and its long cache target. */
    public record SetXianqiaoInterfaceExternalTarget(
            int containerId, long pos, long configRevision, int slot,
            String channel, String resourceId, long requestedAmount) implements CustomPacketPayload {
        public SetXianqiaoInterfaceExternalTarget {
            if (channel == null || channel.length() > 64
                    || resourceId == null || resourceId.length() > 256) {
                throw new IllegalArgumentException("invalid external resource identity");
            }
        }

        public SetXianqiaoInterfaceExternalTarget(
                int containerId, BlockPos pos, long configRevision, int slot,
                String channel, String resourceId, long requestedAmount) {
            this(containerId, pos.asLong(), configRevision, slot, channel, resourceId, requestedAmount);
        }

        public BlockPos blockPos() { return BlockPos.of(pos); }

        public static final StreamCodec<RegistryFriendlyByteBuf, SetXianqiaoInterfaceExternalTarget> STREAM_CODEC =
                new StreamCodec<>() {
                    @Override public SetXianqiaoInterfaceExternalTarget decode(RegistryFriendlyByteBuf buffer) {
                        return new SetXianqiaoInterfaceExternalTarget(
                                buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong(),
                                buffer.readVarInt(), buffer.readUtf(64), buffer.readUtf(256),
                                buffer.readVarLong());
                    }

                    @Override public void encode(
                            RegistryFriendlyByteBuf buffer,
                            SetXianqiaoInterfaceExternalTarget payload) {
                        buffer.writeVarInt(payload.containerId());
                        buffer.writeVarLong(payload.pos());
                        buffer.writeVarLong(payload.configRevision());
                        buffer.writeVarInt(payload.slot());
                        buffer.writeUtf(payload.channel(), 64);
                        buffer.writeUtf(payload.resourceId(), 256);
                        buffer.writeVarLong(payload.requestedAmount());
                    }
                };
        public static final CustomPacketPayload.Type<SetXianqiaoInterfaceExternalTarget> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                        ImmortalStorageMod.MODID, "set_xianqiao_interface_external_target"));
        @Override public CustomPacketPayload.Type<SetXianqiaoInterfaceExternalTarget> type() { return TYPE; }
    }
}
