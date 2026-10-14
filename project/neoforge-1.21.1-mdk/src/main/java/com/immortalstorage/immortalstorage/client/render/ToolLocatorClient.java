package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.locator.ToolLocatorPermissions;
import com.immortalstorage.immortalstorage.network.ToolLocatorFilterSync;
import net.minecraft.core.registries.BuiltInRegistries;
import com.immortalstorage.immortalstorage.locator.ToolLocatorRules;
import com.immortalstorage.immortalstorage.network.ToolLocatorSync;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;

/** Receives server results only; no client world lookup or scanner. */
public final class ToolLocatorClient {
    public record Visible(BlockPos pos, int category) {}
    private static final Map<Long,ToolLocatorSync.Target> TARGETS = new HashMap<>();
    private static ResourceLocation dimension;
    private static int radius=32, miningRadius=16;
    private static ResourceLocation filterItem,lastOffhand;
    private static boolean filterReady,filtering;
    private static final Set<ResourceLocation> FILTER_ORES=new HashSet<>();
    private static boolean dirty, lastExplore,lastMine;
    private static BlockPos lastCentre;
    private static List<Visible> visible=List.of();
    private ToolLocatorClient() {}
    public static void install(ToolLocatorSync packet) {
        if (packet.reset() || !Objects.equals(dimension,packet.dimension())) { TARGETS.clear(); FILTER_ORES.clear();filterItem=null;filterReady=false; }
        dimension=packet.dimension();radius=packet.radius();miningRadius=packet.miningRadius();
        for (var target : packet.targets()) {
            if (target.mask()==0) TARGETS.remove(target.position());
            else if (TARGETS.containsKey(target.position()) || TARGETS.size()<65_536) TARGETS.put(target.position(),target);
        }
        dirty=true;
    }
    public static void installFilter(ToolLocatorFilterSync packet) {
        if(packet.reset()||!Objects.equals(filterItem,packet.heldItem())){FILTER_ORES.clear();filterReady=false;}
        filterItem=packet.heldItem();filtering=packet.filtering();FILTER_ORES.addAll(packet.ores());
        if(packet.complete())filterReady=packet.ready();
        dirty=true;
    }
    public static void onTick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null) {
            TARGETS.clear();FILTER_ORES.clear();filterItem=null;filterReady=false;dimension=null;lastCentre=null;visible=List.of();return;
        }
        if (!Objects.equals(dimension,mc.level.dimension().location())) { visible=List.of();lastCentre=null;return; }
        int permission=ToolLocatorPermissions.enabledMask(mc.player);
        boolean explore=(permission&7)!=0;
        boolean mine=(permission&ToolLocatorRules.ORE)!=0;
        var offhand=BuiltInRegistries.ITEM.getKey(mc.player.getOffhandItem().getItem());
        BlockPos centre=mc.player.blockPosition();
        if (!dirty && Objects.equals(centre,lastCentre) && explore==lastExplore && mine==lastMine && Objects.equals(offhand,lastOffhand)) return;
        var selected=new ArrayList<Visible>();
        if (explore || mine) for (var entry : TARGETS.entrySet()) {
            BlockPos pos=BlockPos.of(entry.getKey());
            int category=ToolLocatorRules.visibleCategory(entry.getValue().mask(),explore,mine);
            int range=category==ToolLocatorRules.ORE ? miningRadius : radius;
            if (pos.distSqr(centre)>(long)range*range) continue;
            if(category==ToolLocatorRules.ORE&&!mc.player.getOffhandItem().isEmpty()) {
                if(!filterReady||!Objects.equals(offhand,filterItem)||filtering&&!FILTER_ORES.contains(entry.getValue().oreBlock()))continue;
            }
            if (category!=0) selected.add(new Visible(pos,category));
        }
        visible=List.copyOf(selected);lastCentre=centre.immutable();lastExplore=explore;lastMine=mine;lastOffhand=offhand;dirty=false;
    }
    public static List<Visible> visible() { return visible; }
}
