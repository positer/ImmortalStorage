package com.immortalstorage.immortalstorage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Sent only when offhand selection or loot-index readiness changes. */
public record ToolLocatorFilterSync(Identifier heldItem,boolean ready,boolean filtering,boolean reset,boolean complete,List<Identifier> ores)
        implements CustomPacketPayload {
    public static final Type<ToolLocatorFilterSync> TYPE=new Type<>(Identifier.fromNamespaceAndPath("immortalstorage","tool_locator_filter"));
    public ToolLocatorFilterSync { if(ores.size()>512)throw new IllegalArgumentException("ore filter size");ores=List.copyOf(ores); }
    @Override public Type<ToolLocatorFilterSync> type(){return TYPE;}
    public static final StreamCodec<RegistryFriendlyByteBuf,ToolLocatorFilterSync> STREAM_CODEC=StreamCodec.of((buf,p)->{
        buf.writeIdentifier(p.heldItem);buf.writeBoolean(p.ready);buf.writeBoolean(p.filtering);buf.writeBoolean(p.reset);buf.writeBoolean(p.complete);buf.writeVarInt(p.ores.size());
        for(var ore:p.ores)buf.writeIdentifier(ore);
    },buf->{
        var item=buf.readIdentifier();boolean ready=buf.readBoolean(),filter=buf.readBoolean(),reset=buf.readBoolean(),complete=buf.readBoolean();int size=buf.readVarInt();
        if(size<0||size>512)throw new IllegalArgumentException("ore filter size");
        var ores=new ArrayList<Identifier>(size);for(int i=0;i<size;i++)ores.add(buf.readIdentifier());
        return new ToolLocatorFilterSync(item,ready,filter,reset,complete,ores);
    });
}
