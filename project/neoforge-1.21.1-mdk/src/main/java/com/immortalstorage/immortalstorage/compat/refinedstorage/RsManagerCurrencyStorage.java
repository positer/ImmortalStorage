package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * RS-visible view of one owner's immortal-yuan account.
 *
 * <p>WHY THIS EXISTS (measured, not assumed): immortal yuan is an <b>account</b>, not a stack.
 * {@code ImmortalStoragePlayerData#getImmortalYuan()} is a long ledger value, and the only
 * charge path in this mod is {@code SourceChargeRegistry} over that ledger
 * ({@code api/source/ImmortalStorageChargeProviders.java}). RS 2.0.9's crafting model, on the
 * other hand, can only charge an {@code EXTERNAL} pattern by <b>extracting the pattern's declared
 * ingredients from the network root storage</b> before it calls the pattern sink
 * (verified: {@code ExternalTaskPattern.acceptsIterationInputs} -> {@code extractAll(..., EXECUTE)}
 * then {@code sink.accept(pattern, inputs, EXECUTE)}). A ledger therefore has to be projected as a
 * storage resource for a yuan-priced recipe to be craftable at all.</p>
 *
 * <p>This class is that projection: it advertises exactly one item resource
 * ({@code immortalstorage:immortal_yuan}) whose amount is the account balance, debits the account
 * on {@code EXECUTE} extract and deposits into it on {@code EXECUTE} insert. It is the same
 * "yuan shown as an ordinary item" projection the mod already performs elsewhere
 * ({@code PersonalStorageItemHandler} virtual yuan slot, {@code CommonEvents} pickup deposit).</p>
 *
 * <p>VERIFIED signatures: {@code Storage extends StorageView(+InsertableStorage+ExtractableStorage)}
 * with {@code getAll()/getStored()/insert(ResourceKey,long,Action,Actor)/extract(...)};
 * {@code CompositeAwareChild} adds {@code onAddedIntoComposite/onRemovedFromComposite/
 * compositeInsert/compositeExtract} and {@code Amount(long amount, long amountForList)}.</p>
 *
 * <p>OPEN RISK for the Lead: this makes yuan pullable through an RS grid, which materialises
 * items through the existing pickup-deposit loop. If that surface is not wanted, do not attach
 * this node - only {@link RsManagerNetworkAttachment}'s Xianqiao storage node has to exist for
 * the "disk connected" half of the feature.</p>
 */
final class RsManagerCurrencyStorage implements CompositeAwareChild {
    private final ServerLevel level;
    private final UUID owner;
    private final ItemResource yuanResource;

    RsManagerCurrencyStorage(ServerLevel level, UUID owner, ItemStack yuanPrototype) {
        this.level = Objects.requireNonNull(level, "level");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.yuanResource = ItemResource.ofItemStack(Objects.requireNonNull(yuanPrototype, "yuanPrototype"));
    }

    /** The ingredient identity every exchange pattern uses to price one batch. */
    ResourceKey resource() {
        return yuanResource;
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        long amount = balance();
        return amount <= 0L ? List.of() : List.of(new ResourceAmount(yuanResource, amount));
    }

    @Override
    public long getStored() {
        return balance();
    }

    long balance() {
        ImmortalStoragePlayerData data = data();
        return data == null ? 0L : RsAmountPolicy.advertised(data.getImmortalYuan());
    }

    long capacity() {
        ImmortalStoragePlayerData data = data();
        return data == null ? 0L : data.getImmortalYuanCapLong();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!isYuan(resource) || amount <= 0L || action == null) return 0L;
        ImmortalStoragePlayerData data = data();
        if (data == null) return 0L;
        long headroom = data.isInfiniteImmortalYuan()
                ? amount
                : Math.max(0L, data.getImmortalYuanCapLong() - data.getImmortalYuan());
        long acceptable = Math.min(amount, headroom);
        if (acceptable <= 0L) return 0L;
        if (action == Action.SIMULATE) return acceptable;
        return RsAmountPolicy.boundedTransfer(data.depositImmortalYuan(acceptable), amount);
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!isYuan(resource) || amount <= 0L || action == null) return 0L;
        ImmortalStoragePlayerData data = data();
        if (data == null) return 0L;
        if (data.isInfiniteImmortalYuan()) return amount;
        long available = data.getImmortalYuan();
        long extractable = Math.min(amount, Math.max(0L, available));
        if (extractable <= 0L) return 0L;
        if (action == Action.SIMULATE) return extractable;
        return data.consumeImmortalYuan(extractable) ? extractable : 0L;
    }

    @Override
    public void onAddedIntoComposite(ParentComposite parentComposite) {
        // Dedup does not apply: a currency ledger is never also mounted as an RS disk.
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parentComposite) {
        // Nothing to release.
    }

    @Override
    public Amount compositeInsert(ResourceKey resource, long amount, Action action, Actor actor) {
        long inserted = insert(resource, amount, action, actor);
        return inserted <= 0L ? Amount.ZERO : new Amount(inserted, inserted);
    }

    @Override
    public Amount compositeExtract(ResourceKey resource, long amount, Action action, Actor actor) {
        long extracted = extract(resource, amount, action, actor);
        return extracted <= 0L ? Amount.ZERO : new Amount(extracted, extracted);
    }

    private boolean isYuan(@Nullable ResourceKey resource) {
        return yuanResource.equals(resource);
    }

    private @Nullable ImmortalStoragePlayerData data() {
        MinecraftServer server = level.getServer();
        if (server == null) return null;
        ServerPlayer player = PersistentPlayerIdentity.onlinePlayer(server, owner);
        return player == null ? null : ImmortalStoragePlayerData.get(player);
    }
}
