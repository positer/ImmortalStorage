package com.immortalstorage.immortalstorage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Sent only when offhand selection or loot-index readiness changes. */
public record ToolLocatorFilterSync(ResourceLocation heldItem,boolean ready,boolean filtering,boolean reset,boolean complete,List<ResourceLocation> ores)
        implements CustomPacketPayload {
    public static final Type<ToolLocatorFilterSync> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("immortalstorage","tool_locator_filter"));
    public ToolLocatorFilterSync { if(ores.size()>512)throw new IllegalArgumentException("ore filter size");ores=List.copyOf(ores); }
    @Override public Type<ToolLocatorFilterSync> type(){return TYPE;}
    public static final StreamCodec<RegistryFriendlyByteBuf,ToolLocatorFilterSync> STREAM_CODEC=StreamCodec.of((buf,p)->{
        buf.writeResourceLocation(p.heldItem);buf.writeBoolean(p.ready);buf.writeBoolean(p.filtering);buf.writeBoolean(p.reset);buf.writeBoolean(p.complete);buf.writeVarInt(p.ores.size());
        for(var ore:p.ores)buf.writeResourceLocation(ore);
    },buf->{
        var item=buf.readResourceLocation();boolean ready=buf.readBoolean(),filter=buf.readBoolean(),reset=buf.readBoolean(),complete=buf.readBoolean();int size=buf.readVarInt();
        if(size<0||size>512)throw new IllegalArgumentException("ore filter size");
        var ores=new ArrayList<ResourceLocation>(size);for(int i=0;i<size;i++)ores.add(buf.readResourceLocation());
        return new ToolLocatorFilterSync(item,ready,filter,reset,complete,ores);
    });
}
