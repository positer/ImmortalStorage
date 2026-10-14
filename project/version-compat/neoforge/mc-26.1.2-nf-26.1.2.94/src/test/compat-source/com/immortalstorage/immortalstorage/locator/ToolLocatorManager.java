package com.immortalstorage.immortalstorage.locator;

import com.immortalstorage.immortalstorage.network.ToolLocatorSync;
import com.immortalstorage.immortalstorage.network.ToolLocatorFilterSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Event-triggered per-player server caches, with a shared bounded work queue. No idle world scan. */
public final class ToolLocatorManager {
    private static final int WORK_BUDGET = 16_384;
    private static final Map<UUID, Cache> PLAYERS = new LinkedHashMap<>();
    private static final ToolLocatorWatchIndex<ServerLevel> WATCHERS = new ToolLocatorWatchIndex<>();
    private static final ToolLocatorWorkQueue WORK = new ToolLocatorWorkQueue();
    private static final Map<BlockState, Integer> STATE_MASKS = new IdentityHashMap<>();
    private static long appliedGeneration = -1;
    private static int dirtyStart;
    private static volatile boolean watching;
    private ToolLocatorManager() {}

    private static int mask(BlockState state) { return STATE_MASKS.computeIfAbsent(state, ToolLocatorRules::stateMask); }
    private static ToolLocatorSync.Target target(long pos, int mask, BlockState state) {
        return new ToolLocatorSync.Target(pos,mask,(mask & ToolLocatorRules.ORE) != 0 ? BuiltInRegistries.BLOCK.getKey(state.getBlock()) : null);
    }
    private static boolean eligible(ServerPlayer player) {
        return ToolLocatorPermissions.enabledMask(player) != 0;
    }
    public static void onServerTick(ServerTickEvent.Post event) {
        long generation = ToolLocatorSettings.generation();
        if (appliedGeneration != generation) { STATE_MASKS.clear(); appliedGeneration = generation; }
        Set<UUID> alive = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            alive.add(player.getUUID());
            Cache cache = PLAYERS.get(player.getUUID());
            if (!eligible(player)) {
                if (cache != null) { cache.clearClient(); remove(cache); }
                continue;
            }
            if (cache == null || cache.player != player || cache.level != (net.minecraft.server.level.ServerLevel) player.level() || cache.generation != generation) {
                if (cache != null) remove(cache);
                cache = new Cache(player, generation); PLAYERS.put(player.getUUID(), cache);
                cache.clearClient(); cache.move(player.chunkPosition());
            } else if (!cache.window.equals(new ToolLocatorWindow(player.chunkPosition().x(), player.chunkPosition().z()))) {
                cache.move(player.chunkPosition());
            }
            int radius = ToolLocatorSettings.radius();
            int miningRadius = ToolLocatorSettings.miningRadius();
            if (cache.radius != radius || cache.miningRadius != miningRadius) {
                cache.radius = radius; cache.miningRadius = miningRadius; cache.policyDirty = true;
            }
        }
        for (Cache cache : new ArrayList<>(PLAYERS.values())) if (!alive.contains(cache.player.getUUID())) remove(cache);
        watching = !PLAYERS.isEmpty();
        for(Cache cache:PLAYERS.values())cache.refreshFilter();
        var caches = new ArrayList<>(PLAYERS.values());
        int dirtyLeft = 4096;
        for (int i = 0; i < caches.size() && dirtyLeft > 0; i++)
            dirtyLeft -= caches.get((dirtyStart + i) % caches.size()).applyDirty(Math.min(512, dirtyLeft));
        dirtyStart = caches.isEmpty() ? 0 : (dirtyStart + 1) % caches.size();
        WORK.drain(WORK_BUDGET - (4096 - dirtyLeft));
        for (Cache cache : PLAYERS.values()) if (cache.policyDirty || cache.player.tickCount % 5 == 0) cache.sendChanges();
    }

    /** A block change affects only players whose existing three-chunk window contains that chunk. */
    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState state) {
        if (!watching) return;
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).isSameThread()) {
            BlockPos stable = pos.immutable();
            com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).execute(() -> blockChanged(level, stable, oldState, state)); return;
        }
        if (mask(oldState) == mask(state) && !oldState.hasBlockEntity() && !state.hasBlockEntity()
                && ((mask(state) & ToolLocatorRules.ORE) == 0 || oldState.getBlock() == state.getBlock())) return;
        dirty(level, pos);
    }
    public static void entityChanged(ServerLevel level, BlockPos pos) { dirty(level, pos); }
    private static void dirty(ServerLevel level, BlockPos pos) {
        if (!watching) return;
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).isSameThread()) {
            BlockPos stable = pos.immutable(); com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).execute(() -> dirty(level, stable)); return;
        }
        var players = WATCHERS.nearby(level,ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        for (UUID id : players) { Cache cache = PLAYERS.get(id); if (cache != null && cache.authorized) cache.dirty.add(pos.asLong()); }
    }
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!watching) return;
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).isSameThread()) { com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).execute(() -> onChunkLoad(event)); return; }
        var players = WATCHERS.nearby(level,chunk.getPos().pack());
        for (UUID id : players) { Cache cache = PLAYERS.get(id); if (cache != null) cache.queue(chunk); }
    }
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!watching) return;
        if (!com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).isSameThread()) { com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(level).execute(() -> onChunkUnload(event)); return; }
        var players = WATCHERS.nearby(level,chunk.getPos().pack());
        for (UUID id : players) {
            Cache cache = PLAYERS.get(id); if (cache == null) continue;
            cache.jobs.remove(chunk.getPos().pack()); cache.install(chunk.getPos().pack(), Map.of());
        }
    }
    public static void onServerStopped(ServerStoppedEvent event) {
        watching = false;
        PLAYERS.clear(); WATCHERS.clear(); WORK.clear(); STATE_MASKS.clear(); ToolLocatorLootCatalog.clear(); appliedGeneration = -1; watching = false; dirtyStart = 0;
    }
    private static void remove(Cache cache) {
        cache.authorized = false;
        PLAYERS.remove(cache.player.getUUID()); cache.unwatch(); cache.jobs.clear();
    }

    private static final class Cache {
        final ServerPlayer player; final ServerLevel level; final long generation;
        ToolLocatorWindow window;
        int radius = ToolLocatorSettings.radius(), miningRadius = ToolLocatorSettings.miningRadius(); boolean policyDirty;
        boolean authorized = true;
        long filterRevision=-1;
        net.minecraft.resources.Identifier filterItem;
        final Map<Long, Map<Long, ToolLocatorSync.Target>> chunks = new HashMap<>();
        final Map<Long, Job> jobs = new HashMap<>();
        final Set<Long> dirty = new HashSet<>();
        final LinkedHashMap<Long, ToolLocatorSync.Target> changes = new LinkedHashMap<>();
        Cache(ServerPlayer player, long generation) { this.player = player; this.level = (net.minecraft.server.level.ServerLevel) player.level(); this.generation = generation; }
        void refreshFilter() {
            if((ToolLocatorPermissions.enabledMask(player)&ToolLocatorRules.ORE)==0)return;
            var item=BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem());
            long revision=ToolLocatorLootCatalog.revision();
            if(revision==filterRevision&&Objects.equals(item,filterItem))return;
            var selection=ToolLocatorLootCatalog.select(player.getOffhandItem());
            filterRevision=revision;filterItem=item;
            var ores=selection.ores();
            for(int start=0;start<Math.max(1,ores.size());start+=512) {
                int end=Math.min(start+512,ores.size());
                PacketDistributor.sendToPlayer(player,new ToolLocatorFilterSync(item,selection.ready(),selection.filtering(),start==0,end==ores.size(),ores.subList(start,end)));
            }
        }
        void clearClient() {
            PacketDistributor.sendToPlayer(player, new ToolLocatorSync(level.dimension().identifier(), radius, miningRadius, true, List.of()));
        }
        void unwatch() {
            WATCHERS.remove(player.getUUID());
        }
        void move(ChunkPos centre) {
            unwatch(); window = new ToolLocatorWindow(centre.x(), centre.z());
            var watches = new ArrayList<Long>();
            for (long key : new ArrayList<>(chunks.keySet())) {
                ChunkPos pos = ChunkPos.unpack(key);
                if (!window.containsChunk(pos.x(),pos.z())) { install(key, Map.of()); chunks.remove(key); jobs.remove(key); }
            }
            jobs.entrySet().removeIf(e -> { var p = ChunkPos.unpack(e.getKey()); return !window.containsChunk(p.x(),p.z()); });
            var loaded = new ArrayList<LevelChunk>();
            for (int x = centre.x() - 3; x <= centre.x() + 3; x++) for (int z = centre.z() - 3; z <= centre.z() + 3; z++) {
                long key = ChunkPos.pack(x,z);
                watches.add(key);
                // Existing clean chunks are retained; only newly entered/missing chunks need initial scans.
                if (!chunks.containsKey(key) && !jobs.containsKey(key)) {
                    var chunk = level.getChunkSource().getChunkNow(x,z); if (chunk != null) loaded.add(chunk);
                }
            }
            WATCHERS.watch(player.getUUID(),level,watches);
            loaded.sort(Comparator.comparingLong(chunk -> {
                long x = chunk.getPos().x() - centre.x(), z = chunk.getPos().z() - centre.z(); return x*x+z*z;
            }));
            for (var chunk : loaded) queue(chunk);
        }
        void queue(LevelChunk chunk) {
            long key = chunk.getPos().pack();
            if (jobs.containsKey(key) && jobs.get(key).chunk == chunk) return;
            Job job = new Job(this, chunk); jobs.put(key,job); WORK.add(job);
        }
        void discovered(long key,long packed,ToolLocatorSync.Target target) {
            var current=chunks.computeIfAbsent(key,ignored->new HashMap<>());
            if (!Objects.equals(current.put(packed,target),target)) changes.put(packed,target);
        }
        int applyDirty(int budget) {
            int used = 0;
            var iterator = dirty.iterator();
            while (iterator.hasNext() && used < budget) {
                long packed = iterator.next(); iterator.remove(); used++;
                BlockPos pos = BlockPos.of(packed); long key = ChunkPos.pack(pos.getX() >> 4,pos.getZ() >> 4);
                if (!window.containsChunk(pos.getX() >> 4,pos.getZ() >> 4)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4,pos.getZ() >> 4); if (chunk == null) continue;
                BlockState state = chunk.getBlockState(pos);
                int mask = ToolLocatorRules.withEntity(ToolLocatorManager.mask(state), chunk.getBlockEntities().get(pos));
                Map<Long,ToolLocatorSync.Target> targets = chunks.computeIfAbsent(key, ignored -> new HashMap<>());
                var previous = targets.get(packed);
                var target = target(packed, mask, state);
                if (mask == 0) targets.remove(packed); else targets.put(packed,target);
                if (!Objects.equals(previous, mask == 0 ? null : target)) changes.put(packed,target);
                Job pending = jobs.get(key);
                if (pending != null) { if (mask == 0) pending.found.remove(packed); else pending.found.put(packed,target); }
            }
            return used;
        }
        void install(long key, Map<Long,ToolLocatorSync.Target> found) {
            Map<Long,ToolLocatorSync.Target> old = chunks.getOrDefault(key,Map.of());
            for (long position : old.keySet()) if (!found.containsKey(position)) changes.put(position,new ToolLocatorSync.Target(position,0,null));
            for (var entry : found.entrySet()) if (!Objects.equals(old.get(entry.getKey()),entry.getValue())) changes.put(entry.getKey(),entry.getValue());
            chunks.put(key,new HashMap<>(found));
        }
        void sendChanges() {
            if (changes.isEmpty() && !policyDirty) return;
            var targets = new ArrayList<ToolLocatorSync.Target>();
            var iterator = changes.entrySet().iterator();
            while (iterator.hasNext() && targets.size() < 4096) {
                var entry = iterator.next(); targets.add(entry.getValue()); iterator.remove();
            }
            PacketDistributor.sendToPlayer(player, new ToolLocatorSync(level.dimension().identifier(),radius,miningRadius,false,targets));
            policyDirty = false;
        }
    }

    private static final class Job implements ToolLocatorWorkQueue.Task {
        final Cache cache; final LevelChunk chunk; final long key;
        final Map<Long,ToolLocatorSync.Target> found = new HashMap<>();
        final int[] sections;
        int section, cell; boolean prepared;
        Job(Cache cache, LevelChunk chunk) {
            this.cache=cache;this.chunk=chunk;key=chunk.getPos().pack();
            sections=java.util.stream.IntStream.range(0,chunk.getSections().length).boxed()
                    .sorted(Comparator.comparingInt(index -> Math.abs((chunk.getSectionYFromSectionIndex(index)<<4)+8-cache.player.blockPosition().getY())))
                    .mapToInt(Integer::intValue).toArray();
        }
        @Override public boolean valid() {
            return cache.authorized && PLAYERS.get(cache.player.getUUID())==cache && cache.jobs.get(key)==this
                    && cache.level.getChunkSource().getChunkNow(chunk.getPos().x(),chunk.getPos().z())==chunk;
        }
        @Override public void cancel() { cache.jobs.remove(key,this); }
        @Override public void finish() { cache.install(key,found);cache.jobs.remove(key,this); }
        @Override public boolean done() { return section >= sections.length; }
        @Override public int advance(int budget) {
            int used=0;
            while (!done() && used < budget) {
                var data=chunk.getSections()[sections[section]];
                if (!prepared) {
                    if (data.hasOnlyAir() || !data.maybeHas(state -> mask(state)!=0 || state.hasBlockEntity())) { section++;continue; }
                    prepared=true;
                }
                int x=cell&15,y=(cell>>>8)&15,z=(cell>>>4)&15;
                BlockState state=data.getBlockState(x,y,z); int mask=ToolLocatorManager.mask(state);
                if (mask!=0 || state.hasBlockEntity()) {
                    BlockPos pos=new BlockPos((chunk.getPos().x()<<4)+x,(chunk.getSectionYFromSectionIndex(sections[section])<<4)+y,(chunk.getPos().z()<<4)+z);
                    mask=ToolLocatorRules.withEntity(mask,chunk.getBlockEntities().get(pos));
                    if (mask!=0) { var target = target(pos.asLong(),mask,state);found.put(pos.asLong(),target);cache.discovered(key,pos.asLong(),target); }
                }
                cell++;used++;
                if (cell==4096) { cell=0;section++;prepared=false; }
            }
            return used;
        }
    }
}
